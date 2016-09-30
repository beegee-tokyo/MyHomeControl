package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class MessageListener extends Service {

	/** Tag for debug messages */
	private static final String DEBUG_LOG_TAG = "MHC-LISTENER";

	/** UDP server port where we receive the UDP broadcasts */
	private static final int UDP_SERVER_PORT = 5000;
	/** Action for broadcast message to main activity */
	public static final String BROADCAST_RECEIVED = "BC_RECEIVED";

	/** Flag if UDP listener is restarted after a broadcast was received */
	static Boolean shouldRestartSocketListen=true;
	/** Socket for broadcast datagram */
	static DatagramSocket socket;
	/** Socket for TCP messages */
	private ServerSocket serverSocket;

	/** Multicast wifiWakeLock to keep WiFi awake until broadcast is received */
	private WifiManager.MulticastLock wifiWakeLock = null;

	/** MQTT client */
	public static volatile IMqttAsyncClient mqttClient = null;
	/** Flag if connection retry has already been initiated */
	private boolean doConnectStarted = false;

	/** Array to calculate average consumption (to avoid too many alerts */
	private static final ArrayList<Float> avgConsumption = new ArrayList<>();
	/** Counter for entries in average consumption array */
	private static int avgConsIndex = 0;

	public MessageListener() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Enable access to internet
		if (android.os.Build.VERSION.SDK_INT > 9) {
			/** ThreadPolicy to get permission to access internet */
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		// Get the MulticastLock to be able to receive multicast UDP messages
		/** Wifi manager to check wifi status */
		WifiManager wifi = (WifiManager)getSystemService( Context.WIFI_SERVICE );
		if(wifi != null){
			if (wifiWakeLock != null) { // In case we restart after receiver problem
				wifiWakeLock = wifi.createMulticastLock("MyHomeControl");
				wifiWakeLock.acquire();
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// Start in foreground (to avoid being killed)
		startForeground(1, ServiceNotification(this));

		boolean hasConnection[] = Utilities.connectionAvailable(this);
		if (hasConnection[0] && Utilities.isHomeWiFi(this)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start UDP listener");
			try {
				// Start listener for UDP broadcast messages
				shouldRestartSocketListen = true;
				startListenForUDPBroadcast();
			} catch (Exception ignore) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Failed to start UDP listener");
			}
			try {
				// Start listener for TCP messages
				startListenForTCPMessage();
			} catch (Exception ignore) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Failed to start TCP listener");
			}
		}

		IntentFilter intentf = new IntentFilter();
		intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(connChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		// Connect to MQTT broker
		if ((mqttClient == null) || (!mqttClient.isConnected())) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start MQTT listener");
			new doConnect().execute();
		}
		//this service will run until we stop it
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Service onDestroy()");
		shouldRestartSocketListen = false;
		if (connChangeReceiver != null) {
			unregisterReceiver(connChangeReceiver);
			connChangeReceiver = null;
		}
		if (wifiWakeLock != null) { // In case we restart after receiver problem
			wifiWakeLock.release();
		}
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException ignore) {
			}
		}
	}

	private BroadcastReceiver connChangeReceiver = new BroadcastReceiver() {
		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(final Context context, Intent intent) {

			// Connectivity has changed
			if (intent.getAction().equals (android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Connection Change (MessageListener)");

				boolean bHasConnection[] = Utilities.connectionAvailable(context);
				if (!bHasConnection[0] && !bHasConnection[1]) { // No connection available?
					// Stop service to listen to UDP broadcast messages
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Try to stop UDP/MQTT listener WiFi = " + bHasConnection[0]
							+ " MOBILE = " + bHasConnection[1]);
					shouldRestartSocketListen = false;
					if (socket != null) {
						socket.disconnect();
						socket.close();
					}
					if (connChangeReceiver != null) {
						unregisterReceiver(connChangeReceiver);
						connChangeReceiver = null;
					}
					stopSelf();
				} else {
					if (mqttClient == null) {
						new doConnect().execute();
					}
				}
			}
		}
	};

	// TCP & UDP stuff starts here
	/**
	 * Start listener for UDP messages
	 */
	private void startListenForUDPBroadcast() {
		Thread UDPBroadcastThread = new Thread(new Runnable() {
			public void run() {
				try {
					/** IP mask from where we expect the UDP broadcasts */
					InetAddress broadcastIP = InetAddress.getByName("192.168.0.255"); //172.16.238.42 //192.168.1.255
					/** Port from where we expect the UDP broadcasts */
					Integer port = UDP_SERVER_PORT;
					while (shouldRestartSocketListen) {
						listenUDPBroadCast(broadcastIP, port);
					}
				} catch (Exception e) {
					if (shouldRestartSocketListen) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart UDP listener after error " + e.getMessage());
						startListenForUDPBroadcast();
					} else {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop UDP listener");
					}
				}
			}
		});

		UDPBroadcastThread.start();
	}

	/**
	 * Wait for UDP broadcasts
	 *
	 * @param broadcastIP
	 * 		IP mask to listen to
	 * @param port
	 * 		Port to listen to
	 */
	private void listenUDPBroadCast(InetAddress broadcastIP, Integer port) {
		/** Byte buffer for incoming data */
		byte[] recvBuf = new byte[1000];
		if (socket == null || socket.isClosed()) {
			try {
				socket = new DatagramSocket(port, broadcastIP);
			} catch (SocketException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Cannot open socket " + e.getMessage());
				socket.disconnect();
				socket.close();
				socket = null;
				return;
			}
		}
		/** Datagram packet for incoming data */
		DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Waiting for UDP broadcast");
		try {
			socket.receive(packet);
		} catch (IOException e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Socket receive failed " + e.getMessage());
		}

		/** IP address of UDP broadcast sender */
		String senderIP = packet.getAddress().getHostAddress();
		/** Message attached to UDP broadcast */
 		String message = new String(packet.getData()).trim();

		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Got UDP broadcast from " + senderIP + ", message: " + message);

		Context context = getApplicationContext();
		// Check if response is a JSON array
		if (Utilities.isJSONValid(message)) {
			/** Json object for received data */
			JSONObject jsonResult;
			try {
				jsonResult = new JSONObject(message);
				try {
					/** Device ID from UDP broadcast message */
					String broadCastDevice = jsonResult.getString("de");
					if (broadCastDevice.startsWith("sf") || broadCastDevice.startsWith("sb")) {
						// Broadcast from front security
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update security widgets");									/** Flag for alarm on or off */

						boolean alarmIsActive = (jsonResult.getInt("ao") == 1);
						boolean alarmIsOn = (jsonResult.getInt("al") == 1);

						// Activate/deactivate alarm sound and update widget
						securityAlarmAndWidgetUpdate(alarmIsActive, alarmIsOn, broadCastDevice, context);
					}

					if (broadCastDevice.startsWith("sp")) { // Broadcast from solar panel monitor
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update solar panel widgets");

						/** Value of solar production */
						float solarPower = jsonResult.getInt("s");
						/** Value of house consumption */
						float consPower = jsonResult.getInt("c");

						// Activate/deactivate alarm sound and update widget
						solarAlarmAndWidgetUpdate(solarPower, consPower, context);
					}
					if (broadCastDevice.startsWith("fd")) { // Broadcast from office aircon
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update office aircon widgets");

						/** Flag for alarm switched on or off */
						boolean timerOn = (jsonResult.getInt("ti") == 1);
						/** Timer time */
						int timerTime = jsonResult.getInt("ot");

						// Update widget
						airconWidgetUpdate(timerOn, timerTime, context);

					}
				} catch (JSONException ignore) {
					return;
				}
			} catch (JSONException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
				return;
			}
			// Send broadcast to listening activities
			sendMyBroadcast(message, "UDP");
		}
	}

	/**
	 * Start listener for TCP messages
	 */
	private void startListenForTCPMessage() {
		Thread TCPMessageThread = new Thread(new Runnable() {
			@SuppressWarnings("InfiniteLoopStatement")
			public void run() {
				try {
					if (serverSocket == null) {
						serverSocket = new ServerSocket();
						serverSocket.setReuseAddress(true);
						serverSocket.bind(new InetSocketAddress(9999));
					}
					while (shouldRestartSocketListen) { //(serverSocket != null) {
						// LISTEN FOR INCOMING CLIENTS
						Socket client = serverSocket.accept();
						try {
							BufferedReader in = new BufferedReader(
									new InputStreamReader(client.getInputStream()));
							String inMsg;
							while ((inMsg = in.readLine()) != null) {
								// Send broadcast to listening activities
								sendMyBroadcast(inMsg, "DEBUG");
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received TCP data: " + inMsg);
							}
						} catch (Exception e) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Receiving TCP data failed " + e.getMessage());
						}
					}
				} catch (Exception e) {
					if (serverSocket != null) {
						try {
							serverSocket.close();
						} catch (IOException ignore) {
						}
					}
					if (shouldRestartSocketListen) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart TCP listener after error " + e.getMessage());
						startListenForTCPMessage();
					}
				}
			}
		});

		TCPMessageThread.start();
	}

	// MQTT stuff starts here
	/**
	 * Connect to MQTT broker and subscribe to topics
	 */
	private class doConnect extends AsyncTask<String, Void, Boolean> {

		@SuppressLint("CommitPrefEdits")
		@Override
		protected Boolean doInBackground(String... params) {
			boolean connected = true;
			if (shouldRestartSocketListen) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "doConnect()");
				if ((mqttClient != null) && (mqttClient.isConnected())) {
					return true;
				}
				String deviceId = Utilities.getDeviceName();
				String subscriberID = "/" + deviceId.toUpperCase();
				String mqttIP = getResources().getString(R.string.MQTT_IP);
				String mqttUser = getResources().getString(R.string.MQTT_USER);
				String mqttPw = getResources().getString(R.string.MQTT_PW);
				IMqttToken token;
				MqttConnectOptions options = new MqttConnectOptions();
				options.setCleanSession(false);
				options.setUserName(mqttUser);
				options.setPassword(mqttPw.toCharArray());
				options.setConnectionTimeout(60);
				options.setAutomaticReconnect(true);

				try {
					options.setKeepAliveInterval(300);
					byte[] lastWill;
					lastWill = "Dead".getBytes("UTF-8");
					options.setWill(subscriberID, lastWill, 2, true);

					mqttClient = new MqttAsyncClient(mqttIP, deviceId, new MemoryPersistence());
					token = mqttClient.connect(options);
					token.waitForCompletion(60000);
					mqttClient.setCallback(new MqttEventCallback());
					token = mqttClient.subscribe("/SPM", 0);
					token.waitForCompletion(10000);
					token = mqttClient.subscribe("/AC1", 0);
					token.waitForCompletion(10000);
					token = mqttClient.subscribe("/AC2", 0);
					token.waitForCompletion(10000);
					token = mqttClient.subscribe("/WEI", 0);
					token.waitForCompletion(10000);
					token = mqttClient.subscribe("/WEO", 0);
					token.waitForCompletion(10000);
					token = mqttClient.subscribe("/SEF", 0);
					token.waitForCompletion(10000);
					token = mqttClient.subscribe("/SEB", 0);
					token.waitForCompletion(10000);
					// Send one time topic to announce device as subscriber
					deviceId = "/" + deviceId.toUpperCase();
					byte[] encodedPayload;
					encodedPayload = "Subscriber".getBytes("UTF-8");
					token = mqttClient.publish(deviceId, encodedPayload, 2, true);
					token.waitForCompletion(5000);
				} catch (MqttSecurityException | UnsupportedEncodingException e) {
					connected = false;
				} catch (MqttException e) {
					switch (e.getReasonCode()) {
						case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "BROKER_UNAVAILABLE " +e.getMessage());
							break;
						case MqttException.REASON_CODE_CLIENT_TIMEOUT:
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "CLIENT_TIMEOUT " +e.getMessage());
							break;
						case MqttException.REASON_CODE_CONNECTION_LOST:
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "CONNECTION_LOST " +e.getMessage());
							break;
						case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "SERVER_CONNECT_ERROR " +e.getMessage());
							break;
						case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "FAILED_AUTHENTICATION "+ e.getMessage());
							break;
						default:
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT unknown error " + e.getMessage());
							break;
					}
					connected = false;
				}
			} else {
				connected = false;
			}
			return connected;
		}

		/**
		 * Called when AsyncTask background process is finished
		 *
		 * @param isConnected
		 * 		Boolean if connection to MQTT broker was successful
		 */
		protected void onPostExecute(Boolean isConnected) {
			if (isConnected) {
				doConnectStarted = false;
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT connection successful");
			} else { // retry in 30 seconds
				boolean bHasConnection[] = Utilities.connectionAvailable(getApplicationContext());
					if (bHasConnection[0] || bHasConnection[1]) {
						new doConnect().execute();
					}
			}
		}
	}

	private class MqttEventCallback implements MqttCallback {

		@Override
		public void connectionLost(Throwable arg0) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT lost connection:" + arg0);
			if (!doConnectStarted) {
				try {
					mqttClient.disconnect();
				} catch (MqttException ignore) {
				}
				boolean bHasConnection[] = Utilities.connectionAvailable(getApplicationContext());
				if (bHasConnection[0] || bHasConnection[1]) {
					if (shouldRestartSocketListen) {
						new doConnect().execute();
					}
				}
			}
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken arg0) {

		}

		@Override
		@SuppressLint("NewApi")
		public void messageArrived(final String topic, final MqttMessage msg) throws Exception {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Message arrived from topic " + topic);
			Handler h = new Handler(getMainLooper());
			h.post(new Runnable() {
				@Override
				public void run() {

					String receivedMessage = new String(msg.getPayload());
					if (receivedMessage.length() == 0) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Empty payload");
						return;
					}
					// Check if screen is locked
					/** Keyguard manager instance */
					KeyguardManager myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
					/** Flag for locked phone */
					boolean phoneIsLocked = myKM.inKeyguardRestrictedInputMode();
					// Check if screen is off
					/** Instance of power manager */
					PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
					/** Flag for screen off */
					boolean screenIsOff = true;
					if (Build.VERSION.SDK_INT >= 20) {
						if (powerManager.isInteractive()) {
							screenIsOff = false;
						}
					}
					else {
						//noinspection deprecation
						if(powerManager.isScreenOn()){
							screenIsOff = false;
						}
					}
					// Check if we are connected to the home WiFi
					/** Flag for local Wifi */
					boolean notOnHomeWifi = !Utilities.isHomeWiFi(getApplicationContext());

					// If we are not on home Wifi or screen is off or locked => process the message
					if (notOnHomeWifi || phoneIsLocked || screenIsOff) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Not home or phone is locked or screen is off");

						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Payload is " + receivedMessage);

						if (receivedMessage.length() == 0) { // Empty message
							return;
						}
						// Update widgets
						/** Application context */
						Context context = getApplicationContext();
						// Check if response is a JSON array
						if (Utilities.isJSONValid(receivedMessage)) {
							try {
								JSONObject jsonResult = new JSONObject(receivedMessage);
								try {
									/** Device name of sender */
									String broadCastDevice;
									broadCastDevice = jsonResult.getString("de");
									if (broadCastDevice.startsWith("sf")) { // Broadcast from front security front yard
										if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update front yard security widgets");

										/** Flag for alarm switched on or off */
										boolean alarmIsActive = (jsonResult.getInt("ao") == 1);
										/** Flag for alarm on or off */
										boolean alarmIsOn = (jsonResult.getInt("al") == 1);

										// Activate/deactivate alarm sound and update widget
										securityAlarmAndWidgetUpdate(alarmIsActive, alarmIsOn, broadCastDevice, context);
									}
									if (broadCastDevice.startsWith("sb")) { // Broadcast from security back yard
										if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update back yard security widgets");

										/** Flag for alarm switched on or off */
										boolean alarmIsActive = (jsonResult.getInt("ao") == 1);
										/** Flag for alarm on or off */
										boolean alarmIsOn = (jsonResult.getInt("al") == 1);

										// Activate/deactivate alarm sound and update widget
										securityAlarmAndWidgetUpdate(alarmIsActive, alarmIsOn, broadCastDevice, context);
									}
									if (broadCastDevice.startsWith("sp")) { // Broadcast from solar panel monitor
										if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update solar panel widgets");

										/** Value of solar production */
										float solarPower = jsonResult.getInt("s");
										/** Value of house consumption */
										float consPower = jsonResult.getInt("c");

										// Activate/deactivate alarm sound and update widget
										solarAlarmAndWidgetUpdate(solarPower, consPower, context);
									}
									if (broadCastDevice.startsWith("fd")) { // Broadcast from office aircon
										if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update office aircon widgets");

										/** Flag for alarm switched on or off */
										boolean timerOn = (jsonResult.getInt("ti") == 1);
										/** Timer time */
										int timerTime = jsonResult.getInt("ot");

										// Update widget
										airconWidgetUpdate(timerOn, timerTime, context);

									}
									if (broadCastDevice.startsWith("ca")) { // Broadcast from living room aircon
										if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update living room aircon widgets");
									}
								} catch (JSONException ignore) {
								}
							} catch (JSONException e) {
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
							}
						}

						// Forward to all local listeners
						sendMyBroadcast(receivedMessage, "MQTT");
					}
				}
			});
		}
	}

	// send broadcast from activity to all receivers listening to the action "BROADCAST_RECEIVED"
	private void sendMyBroadcast(String msgReceived, String fromSender) {
		/** Intent for activity internal broadcast message */
		Intent broadCastIntent = new Intent();
		broadCastIntent.setAction(BROADCAST_RECEIVED);
		broadCastIntent.putExtra("from", fromSender);
		broadCastIntent.putExtra("message", msgReceived);
		sendBroadcast(broadCastIntent);
	}

	/**
	 * Update security widgets
	 *
	 * @param alarmIsActive
	 *            Flag if alarm is active
	 * @param alarmIsOn
	 *            Flag if alarm is on
	 * @param device
	 *            Name of security device
	 * @param context
	 *            Application context
	 */
	private static void securityAlarmAndWidgetUpdate(boolean alarmIsActive,
	                                                 boolean alarmIsOn,
	                                                 String device,
	                                                 Context context) {
		// Show notification only if it is an alarm
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update Security Notification");
		if (alarmIsActive && alarmIsOn) {
			/** String for notification */
			String notifText;
			/** Icon for notification */
			int notifIcon;
			/** Background color for notification icon in SDK Lollipop and newer */
			int notifColor;

			notifIcon = R.drawable.detection;
			notifText = "Intruder! in " + context.getResources().getString(R.string.sec_front_device);
			if (device.equalsIgnoreCase("sb1")) {
				notifText = "Intruder! in " + context.getResources().getString(R.string.sec_back_device);
			}

			//noinspection deprecation
			notifColor = context.getResources().getColor(android.R.color.holo_red_light);

			/** Pointer to notification builder for export/import arrow */
			NotificationCompat.Builder myNotifBuilder;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				myNotifBuilder = new NotificationCompat.Builder(context)
						.setContentTitle(context.getString(R.string.app_name))
						.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MyHomeControl.class), 0))
						.setAutoCancel(false)
						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
						.setVisibility(Notification.VISIBILITY_PUBLIC)
						.setWhen(System.currentTimeMillis());
			} else {
				myNotifBuilder = new NotificationCompat.Builder(context)
						.setContentTitle(context.getString(R.string.app_name))
						.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MyHomeControl.class), 0))
						.setAutoCancel(false)
						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
						.setWhen(System.currentTimeMillis());
			}

			/** Pointer to notification manager for export/import arrow */
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

			/** Access to shared preferences of app widget */
			String selUri = context.getSharedPreferences(MyHomeControl.sharedPrefName, 0)
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

			/** Pointer to notification */
			Notification alarmNotification = myNotifBuilder.build();
			alarmNotification.flags |= Notification.FLAG_INSISTENT;
			notificationManager.notify(2, alarmNotification);
		}

		// Update security widget
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update Security Widget");
		/** App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/** Component name of this widget */
		ComponentName thisAppWidget;
		if (device.equalsIgnoreCase("sf1")) {
			thisAppWidget = new ComponentName(context.getPackageName(),
					SecurityWidget.class.getName());
			/** List of all active widgets */
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

			for (int appWidgetId : appWidgetIds) {
				SecurityWidget.updateAppWidget(context,appWidgetManager,appWidgetId, alarmIsActive);
			}
//		} else {
//			// TODO missing widget for back yard security
		}
	}

	/**
	 * Update solar panel widgets
	 *
	 * @param solarPower
	 *            Solar power production value
	 * @param consPower
	 *            House power consumption value
	 * @param context
	 *            Application context
	 */
	@SuppressLint("DefaultLocale")
	@SuppressWarnings("deprecation")
	private static void solarAlarmAndWidgetUpdate(float solarPower, float consPower, Context context) {
		/****************************************/
		/* Update notification                  */
		/****************************************/
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update Solar Notification");
		/** Icon for notification */
		int notifIcon;
		/** String for notification */
		String notifText;
		/** Background color for notification icon in SDK Lollipop and newer */
		int notifColor;
		/** Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences(MyHomeControl.sharedPrefName, 0);

		// Average power consumption
		if (avgConsIndex < 10) { // Still filling the array?
			avgConsIndex++;
			avgConsumption.add(consPower);
			if (BuildConfig.DEBUG)
				Log.d(DEBUG_LOG_TAG,
						"Building up avg. consumption: i=" +
								Integer.toString(avgConsIndex)  +
								" Array = " + Integer.toString(avgConsumption.size()));
		} else {
			avgConsumption.remove(0);
			avgConsumption.add(consPower);
		}

		float newAvgConsumption = 0;
		for (int i=0; i<avgConsIndex; i++) {
			newAvgConsumption += avgConsumption.get(i);
		}
		newAvgConsumption = newAvgConsumption/(avgConsIndex);
		if (BuildConfig.DEBUG)
			Log.d(DEBUG_LOG_TAG,
					"Avg. consumption: " +
							String.format("%.0f", newAvgConsumption));

		notifIcon = Utilities.getNotifIcon(consPower);

		if (consPower > 0.0d) {
			notifText = context.getString(R.string.tv_result_txt_im) + " " +
					String.format("%.0f", Math.abs(consPower)) + "W";
			notifColor = context.getResources()
					.getColor(android.R.color.holo_red_light);
		} else {
			notifText = context.getString(R.string.tv_result_txt_ex) + " " +
					String.format("%.0f", Math.abs(consPower)) + "W";
			notifColor = context.getResources()
					.getColor(android.R.color.holo_green_light);
			if (newAvgConsumption < -300.0d) {
				/** Uri of selected alarm */
				String selUri = mPrefs.getString(MyHomeControl.prefsSolarWarning,"");
				if (!selUri.equalsIgnoreCase("")) {
					@SuppressLint("InlinedApi") NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
							.setContentTitle(context.getString(R.string.app_name))
							.setContentIntent(PendingIntent.getActivity(context, 0,
									new Intent(context, MyHomeControl.class), 0))
							.setContentText(context.getString(R.string.notif_export,
									String.format("%.0f", Math.abs(consPower)),
									Utilities.getCurrentTime()))
							.setAutoCancel(true)
							.setSound(Uri.parse(selUri))
							.setDefaults(Notification.FLAG_ONLY_ALERT_ONCE)
							.setPriority(NotificationCompat.PRIORITY_DEFAULT)
							.setVisibility(Notification.VISIBILITY_PUBLIC)
							.setWhen(System.currentTimeMillis())
							.setSmallIcon(android.R.drawable.ic_dialog_info);

					Notification notification = builder.build();
					NotificationManager notificationManager =
							(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
					notificationManager.notify(2, notification);
				}
			} else {
				// Instance of notification manager to cancel the existing notification */
				NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				nMgr.cancel(2);
			}
		}

		/* Pointer to notification builder for export/import arrow */
		@SuppressLint("InlinedApi") NotificationCompat.Builder builder1 = new NotificationCompat.Builder(context)
				.setContentTitle(context.getString(R.string.app_name))
				.setContentIntent(PendingIntent.getActivity(context,
						0,
						new Intent(context,MyHomeControl.class),
						0))
				.setAutoCancel(false)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setVisibility(Notification.VISIBILITY_PUBLIC)
				.setWhen(System.currentTimeMillis());
						/* Pointer to notification manager for export/import arrow */
		NotificationManager notificationManager1 = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		builder1.setSmallIcon(notifIcon);
		builder1.setContentText(notifText);
		builder1.setTicker(String.format("%.0f", Math.abs(consPower)) + "W");
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			builder1.setColor(notifColor);
		}
						/* Pointer to notification for export/import arrow */
		Notification notification1 = builder1.build();
		notificationManager1.notify(1, notification1);

		// Update solar panel widgets if any
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update Solar Widget");
		/** App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/** Component name of this widget */
		ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
				SPwidget.class.getName());
		/** List of all active widgets */
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

		for (int appWidgetId : appWidgetIds) {
			SPwidget.updateAppWidget(context,appWidgetManager,appWidgetId, solarPower, consPower);
		}
	}

	/**
	 * Update solar panel widgets
	 *
	 * @param timerOn
	 *            Flag if timer is active
	 * @param timerTime
	 *            Selected timer on time
	 * @param context
	 *            Application context
	 */
	private static void airconWidgetUpdate(boolean timerOn, int timerTime, Context context) {
		// Update aircon panel widgets if any
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update Aircon Widget");
		/** App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/** Component name of this widget */
		ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
				AirconWidget.class.getName());
		/** List of all active widgets */
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

		for (int appWidgetId : appWidgetIds) {
			AirconWidget.updateAppWidget(context, appWidgetManager,
					appWidgetId, timerTime, timerOn);
		}
	}

	/**
	 * Display notification that UDP/MQTT listener is active
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
		notifText = context.getResources().getString(R.string.msg_listener);
		//noinspection deprecation
		notifColor = context.getResources()
				.getColor(android.R.color.holo_green_light);

		/** Pointer to notification builder for export/import arrow */
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
}
