package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class UDPlistener extends Service {

	private static final String DEBUG_LOG_TAG = "MHC-BROAD";

	private static final int UDP_SERVER_PORT = 5000;
	public static final String BROADCAST_RECEIVED = "BC_RECEIVED";

	private final Boolean shouldRestartSocketListen=true;
	private DatagramSocket socket;
	private static Context intentContext;
	private WifiManager.MulticastLock lock = null;

	public static ArrayList<Float> avgConsumption = new ArrayList<>();
	public static int avgConsIndex = 0;

	public UDPlistener() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// Start in foreground (to avoid being killed)
		startForeground(1, ServiceNotification(this));

		//this service will run until we stop it
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		intentContext = getApplicationContext();
		// Enable access to internet
		if (android.os.Build.VERSION.SDK_INT > 9) {
			/** ThreadPolicy to get permission to access internet */
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		// Continue if we are on home WiFi only
		if (!Utilities.isHomeWiFi(getApplicationContext())) { // We have no local WiFi connection!
			return;
		}

		// Get the MulticastLock to be able to receive multicast UDP messages
		WifiManager wifi = (WifiManager)getSystemService( Context.WIFI_SERVICE );
		if(wifi != null){
			if (lock != null) { // In case we restart after receiver problem
				lock = wifi.createMulticastLock("Security");
				lock.acquire();
			}
		}

		// Start listener for UDP broadcast messages
		startListenForUDPBroadcast();
	}

	// UDP stuff starts here
	private void listenAndWaitAndThrowIntent(InetAddress broadcastIP, Integer port) {
		byte[] recvBuf = new byte[250];
		if (socket == null || socket.isClosed()) {
			try {
				socket = new DatagramSocket(port, broadcastIP);
			} catch (SocketException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Cannot open socket " + e.getMessage());
			}
		}
		DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Waiting for UDP broadcast");
		try {
			socket.receive(packet);
		} catch (IOException e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Socket receive failed " + e.getMessage());
		}

		String senderIP = packet.getAddress().getHostAddress();
		String message = new String(packet.getData()).trim();


		// Check if response is a JSON array
		if (Utilities.isJSONValid(message)) {
			JSONObject jsonResult;
			try {
				jsonResult = new JSONObject(message);
				try {
					String broadCastDevice = jsonResult.getString("device");
					if (broadCastDevice.startsWith("sf")) { // Broadcast from front security device
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Got UDP broadcast from " + senderIP + ", message: " + message);
						alarmNotif(jsonResult, intentContext);
					}
					if (broadCastDevice.startsWith("sb")) { // Broadcast from back security device
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Got UDP broadcast from " + senderIP + ", message: " + message);
						alarmNotif(jsonResult, intentContext);
					}
				} catch (JSONException ignore) {
					return;
				}
			} catch (JSONException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
				return;
			}
			// Send broadcast to listening activities
			sendMyBroadcast(senderIP, message);
		}
	}

	private void startListenForUDPBroadcast() {
		Thread UDPBroadcastThread = new Thread(new Runnable() {
			public void run() {
				try {
					InetAddress broadcastIP = InetAddress.getByName("192.168.0.255"); //172.16.238.42 //192.168.1.255
					Integer port = UDP_SERVER_PORT;
					while (shouldRestartSocketListen) {
						listenAndWaitAndThrowIntent(broadcastIP, port);
					}
				} catch (Exception e) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart UDP listener after error " + e.getMessage());
					onCreate();
				}
			}
		});

		UDPBroadcastThread.start();
	}

	// send broadcast from activity to all receivers listening to the action "ACTION_STRING_ACTIVITY"
	private void sendMyBroadcast(String ipAddress, String msgReceived) {
		Intent broadCastIntent = new Intent();
		broadCastIntent.setAction(BROADCAST_RECEIVED);
		broadCastIntent.putExtra("sender", ipAddress);
		broadCastIntent.putExtra("message", msgReceived);
		sendBroadcast(broadCastIntent);
	}

	/**
	 * Prepare notification from local broadcast or GCM broadcast
	 *
	 * @param jsonValues
	 *            JSON object with values
	 */
	public static void alarmNotif(JSONObject jsonValues, Context notifContext) {
		int hasAlarmInt;
		int hasAlarmActive;
		String deviceIDString;
		try {
			deviceIDString = jsonValues.getString("device");
		} catch (JSONException e) {
			deviceIDString = "unknown";
		}

		try {
			hasAlarmInt = jsonValues.getInt("alarm");
		} catch (JSONException e) {
			hasAlarmInt = 0;
		}
		try {
			hasAlarmActive = jsonValues.getInt("alarm_on");
		} catch (JSONException e) {
			hasAlarmActive = 0;
		}

		// Show notification only if it is an alarm
		if (hasAlarmInt == 1 && hasAlarmActive == 1) {
			/** String for notification */
			String notifText;
			/** Icon for notification */
			int notifIcon;
			/** Background color for notification icon in SDK Lollipop and newer */
			int notifColor;

			notifIcon = R.drawable.detection;
			notifText = "Intruder! in " + notifContext.getResources().getString(R.string.sec_front_device_1);
			if (deviceIDString.equalsIgnoreCase("sb1")) {
				notifText = "Intruder! in " + notifContext.getResources().getString(R.string.sec_back_device_1);
			}

			//noinspection deprecation
			notifColor = notifContext.getResources().getColor(android.R.color.holo_red_light);

			/* Pointer to notification builder for export/import arrow */
			NotificationCompat.Builder myNotifBuilder;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				myNotifBuilder = new NotificationCompat.Builder(notifContext)
						.setContentTitle(notifContext.getString(R.string.app_name))
						.setContentIntent(PendingIntent.getActivity(notifContext, 0, new Intent(notifContext, MyHomeControl.class), 0))
						.setAutoCancel(false)
						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
						.setVisibility(Notification.VISIBILITY_PUBLIC)
						.setWhen(System.currentTimeMillis());
			} else {
				myNotifBuilder = new NotificationCompat.Builder(notifContext)
						.setContentTitle(notifContext.getString(R.string.app_name))
						.setContentIntent(PendingIntent.getActivity(notifContext, 0, new Intent(notifContext, MyHomeControl.class), 0))
						.setAutoCancel(false)
						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
						.setWhen(System.currentTimeMillis());
			}

			/* Pointer to notification manager for export/import arrow */
			NotificationManager notificationManager = (NotificationManager) notifContext.getSystemService(Context.NOTIFICATION_SERVICE);

			/** Access to shared preferences of app widget */
			String selUri = intentContext.getSharedPreferences(MyHomeControl.sharedPrefName, 0)
					.getString(MyHomeControl.prefsSecurityAlarm, "");/** Uri of selected alarm */
			myNotifBuilder.setSound(Uri.parse(selUri));

			myNotifBuilder.setSmallIcon(notifIcon)
					.setContentText(notifText)
					.setContentText(notifText)
					.setStyle(new NotificationCompat.BigTextStyle().bigText(notifText))
					.setTicker(notifText);
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				myNotifBuilder.setColor(notifColor);
			}

			/* Pointer to notification */
			Notification alarmNotification = myNotifBuilder.build();
			notificationManager.notify(2, alarmNotification);
		}
		// Update security widget
		updateWidgets(notifContext,(hasAlarmInt == 1));
	}

	/**
	 * Display notification that UDP listener is active
	 *
	 * @param context
	 *          Context of the application
	 * @return <code>Notification</code>
	 *          Instance of the notification
	 */
	private Notification ServiceNotification(Context context) {
		/** String for notification */
		String notifText;
		/** Icon for notification */
		int notifIcon;
		/** Background color for notification icon in SDK Lollipop and newer */
		int notifColor;

		// Prepare notification for foreground service
		notifIcon = R.drawable.no_detection;
		notifText = context.getResources().getString(R.string.udp_listener);
		//noinspection deprecation
		notifColor = context.getResources()
				.getColor(android.R.color.holo_green_light);

		/* Pointer to notification builder for export/import arrow */
		NotificationCompat.Builder myNotifBuilder;
		myNotifBuilder = new NotificationCompat.Builder(context)
				.setContentTitle(context.getString(R.string.app_name))
				.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MyHomeControl.class), 0))
				.setAutoCancel(false)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setWhen(System.currentTimeMillis());

		myNotifBuilder.setSmallIcon(notifIcon)
				.setContentText(notifText)
				.setContentText(notifText)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(notifText))
				.setTicker(notifText);

		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			myNotifBuilder
					.setColor(notifColor)
					.setVisibility(Notification.VISIBILITY_PUBLIC);
		}
		/* Pointer to notification */
		return myNotifBuilder.build();
	}

	@SuppressLint("CommitPrefEdits")
	private static void updateWidgets(Context updateContext, boolean hasAlarm) {
		/** Pointer to shared preferences */
		SharedPreferences mPrefs = updateContext.getSharedPreferences(MyHomeControl.sharedPrefName, 0);
		if (hasAlarm) {
			mPrefs.edit().putBoolean(MyHomeControl.prefsSecurityAlarmOn, true).commit();
		} else {
			mPrefs.edit().putBoolean(MyHomeControl.prefsSecurityAlarmOn, false).commit();
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update Widget");
		/** App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(updateContext);
		/** Component name of this widget */
		ComponentName thisAppWidget = new ComponentName(updateContext.getPackageName(),
				SecurityWidget.class.getName());
		/** List of all active widgets */
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

		for (int appWidgetId : appWidgetIds) {
			SecurityWidget.updateAppWidget(updateContext,appWidgetManager,appWidgetId, false);
		}

	}
}
