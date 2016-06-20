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
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
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

public class MQTTService extends Service {

	private static final String DEBUG_LOG_TAG = "MQTTService";
	private static boolean hasWifi = false;
	private static boolean hasMmobile = false;
	private ConnectivityManager mConnMan;
	public static volatile IMqttAsyncClient mqttClient;
	private static String deviceId;

	class MQTTBroadcastReceiver extends BroadcastReceiver {
		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent) {
			IMqttToken token;
			boolean hasConnectivity;
			boolean hasChanged = false;
			NetworkInfo infos[] = mConnMan.getAllNetworkInfo();

			for (NetworkInfo info : infos) {
				if (info.getTypeName().equalsIgnoreCase("MOBILE")) {
					if ((info.isConnected() != hasMmobile)) {
						hasChanged = true;
						hasMmobile = info.isConnected();
					}
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, info.getTypeName() + " is " + info.isConnected());
				} else if (info.getTypeName().equalsIgnoreCase("WIFI")) {
					if ((info.isConnected() != hasWifi)) {
						hasChanged = true;
						hasWifi = info.isConnected();
					}
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, info.getTypeName() + " is " + info.isConnected());
				}
			}

			hasConnectivity = hasMmobile || hasWifi;
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "hasConn: " + hasConnectivity + " hasChange: " + hasChanged + " - "+(mqttClient == null || !mqttClient.isConnected()));
			if (hasConnectivity && hasChanged && (mqttClient == null || !mqttClient.isConnected())) {
				doConnect();
			} else if (!hasConnectivity && mqttClient != null && mqttClient.isConnected()) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "doDisconnect()");
				try {
					token = mqttClient.disconnect();
					token.waitForCompletion(1000);
				} catch (MqttException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onCreate() {
		IntentFilter intentf = new IntentFilter();
		setClientID();
		intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(new MQTTBroadcastReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "onConfigurationChanged()");
		android.os.Debug.waitForDebugger();
		super.onConfigurationChanged(newConfig);

	}

	private void setClientID(){
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = wifiManager.getConnectionInfo();
		deviceId = wInfo.getMacAddress();
		if(deviceId == null){
			deviceId = MqttAsyncClient.generateClientId();
		}
		deviceId = Utilities.getDeviceName() + "--" + deviceId;
	}

	private void doConnect(){
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "doConnect()");
		IMqttToken token;
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setUserName("Android-Listener");
		options.setPassword("c16177b52007440b".toCharArray());
		try {
			mqttClient = new MqttAsyncClient("tcp://broker.shiftr.io:1883", deviceId, new MemoryPersistence());
			token = mqttClient.connect(options);
			token.waitForCompletion(10000);
			mqttClient.setCallback(new MqttEventCallback());
			token = mqttClient.subscribe("SPM", 0);
			token.waitForCompletion(10000);
			token = mqttClient.subscribe("AC1", 0);
			token.waitForCompletion(10000);
			token = mqttClient.subscribe("AC2", 0);
			token.waitForCompletion(10000);
			token = mqttClient.subscribe("WEI", 0);
			token.waitForCompletion(10000);
			token = mqttClient.subscribe("WEO", 0);
			token.waitForCompletion(10000);
			token = mqttClient.subscribe("SEF", 0);
			token.waitForCompletion(10000);
			token = mqttClient.subscribe("SEB", 0);
			token.waitForCompletion(10000);
		} catch (MqttSecurityException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			switch (e.getReasonCode()) {
				case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
				case MqttException.REASON_CODE_CLIENT_TIMEOUT:
				case MqttException.REASON_CODE_CONNECTION_LOST:
				case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "c" +e.getMessage());
					e.printStackTrace();
					break;
				case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
					Intent i = new Intent("RAISEALLARM");
					i.putExtra("ALLARM", e);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "b"+ e.getMessage());
					break;
				default:
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "a" + e.getMessage());
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "onStartCommand()");
		return START_STICKY;
	}

	private class MqttEventCallback implements MqttCallback {

		@Override
		public void connectionLost(Throwable arg0) {
			Log.d(DEBUG_LOG_TAG, "MQTT lost connection");
			doConnect();
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
										int timerTime = jsonResult.getInt("to");

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
						sendMyBroadcast(receivedMessage);
					}
				}
			});
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "onBind called");
		return null;
	}

	// send broadcast from activity to all receivers listening to the action "BROADCAST_RECEIVED"
	private void sendMyBroadcast(String msgReceived) {
		/** Intent for activity internal broadcast message */
		Intent broadCastIntent = new Intent();
		broadCastIntent.setAction(UDPlistener.BROADCAST_RECEIVED);
		broadCastIntent.putExtra("from", "MQTT");
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
	public static void securityAlarmAndWidgetUpdate(boolean alarmIsActive,
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
			notifText = "Intruder! in " + context.getResources().getString(R.string.sec_front_device_1);
			if (device.equalsIgnoreCase("sb1")) {
				notifText = "Intruder! in " + context.getResources().getString(R.string.sec_back_device_1);
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
	public static void solarAlarmAndWidgetUpdate(float solarPower, float consPower, Context context) {
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
		if (UDPlistener.avgConsIndex < 10) { // Still filling the array?
			UDPlistener.avgConsIndex++;
			UDPlistener.avgConsumption.add(consPower);
			if (BuildConfig.DEBUG)
				Log.d(DEBUG_LOG_TAG,
						"Building up avg. consumption: i=" +
								Integer.toString(UDPlistener.avgConsIndex)  +
								" Array = " + Integer.toString(UDPlistener.avgConsumption.size()));
		} else {
			UDPlistener.avgConsumption.remove(0);
			UDPlistener.avgConsumption.add(consPower);
		}

		float avgConsumption = 0;
		for (int i=0; i<UDPlistener.avgConsIndex; i++) {
			avgConsumption += UDPlistener.avgConsumption.get(i);
		}
		avgConsumption = avgConsumption/(UDPlistener.avgConsIndex);
		if (BuildConfig.DEBUG)
			Log.d(DEBUG_LOG_TAG,
					"Avg. consumption: " +
							String.format("%.0f", avgConsumption));

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
			if (avgConsumption < -300.0d) {
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
	public static void airconWidgetUpdate(boolean timerOn, int timerTime, Context context) {
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
}
