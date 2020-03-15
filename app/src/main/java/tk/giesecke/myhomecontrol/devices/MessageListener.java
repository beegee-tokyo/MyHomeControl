package tk.giesecke.myhomecontrol.devices;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
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
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.MyHomeControl;
import tk.giesecke.myhomecontrol.R;
import tk.giesecke.myhomecontrol.Utilities;
import tk.giesecke.myhomecontrol.aircon.AirconWidget;
import tk.giesecke.myhomecontrol.lights.BackYardLightWidget;
import tk.giesecke.myhomecontrol.lights.BedRoomLightWidget;
import tk.giesecke.myhomecontrol.security.SecAlarmWidget;
import tk.giesecke.myhomecontrol.solar.SolarPanelWidget;

import static android.app.Notification.FLAG_AUTO_CANCEL;
import static tk.giesecke.myhomecontrol.MyHomeControl.aircon1Index;
import static tk.giesecke.myhomecontrol.MyHomeControl.aircon2Index;
import static tk.giesecke.myhomecontrol.MyHomeControl.aircon3Index;
import static tk.giesecke.myhomecontrol.MyHomeControl.cam1Index;
import static tk.giesecke.myhomecontrol.MyHomeControl.deviceIPs;
import static tk.giesecke.myhomecontrol.MyHomeControl.deviceIsOn;
import static tk.giesecke.myhomecontrol.MyHomeControl.lb1Index;
import static tk.giesecke.myhomecontrol.MyHomeControl.ly1Index;
import static tk.giesecke.myhomecontrol.MyHomeControl.mhcIndex;
import static tk.giesecke.myhomecontrol.MyHomeControl.secBackIndex;
import static tk.giesecke.myhomecontrol.MyHomeControl.secFrontIndex;
import static tk.giesecke.myhomecontrol.MyHomeControl.spMonitorIndex;

public class MessageListener extends Service {

	/** Tag for debug messages of service*/
	private static final String DEBUG_LOG_TAG = "MHC-LISTENER";
	/** Tag for debug messages of event receiver*/
	private static final String DEBUG_LOG_TAG_EVE = "MHC-LEVE";
	/** Tag for debug messages of WiFi switcher */
	private static final String DEBUG_LOG_TAG_SW = "MHC-LSW";

	/** TCP client port to send commands */
	public static final int TCP_CLIENT_PORT = 9998;
	/** UDP server port where we receive the UDP broadcasts */
	private static final int UDP_SERVER_PORT = 9997;
	/** TCP server port where we receive the TCP debug messages */
	private static final int TCP_SERVER_PORT = 9999;
	/** Broadcast IP address */
	public static String broadCastIP = "192.168.0.255";
	/** Action for broadcast message to main activity */
	public static final String BROADCAST_RECEIVED = "BC_RECEIVED";

	/** Notification channel required for Android 8++ */
	private static final String NOTIFICATION_CHANNEL_ID = "MHC";

	/** Flag if UDP/TCP listener is restarted after a broadcast was received */
	private static boolean shouldRestartSocketListen=true;
	/** Socket for broadcast datagram */
	public static DatagramSocket udpSocket;
	/** Flag if udp listener is active */
	public static boolean udpListenerActive = false;

	/** Socket for TCP messages */
	public static ServerSocket tcpSocket;
	/** Flag if tcp listener is active */
	public static boolean tcpListenerActive = false;

	/** Current connection status (0 = no connection, 1 = home WiFi, 2 WiFi or Mobile) */
	public static int connStatus = 3;
	private static final int NO_CONNECTION = 0;
	public static final int HOME_WIFI = 1;
	public static final int WIFI_MOBILE = 2;
	private static final int UNKNOWN_STATUS = 3;

	/** Flag for event broadcast receiver registered */
	private boolean eventReceiverRegistered = false;

	/** Multicast wifiWakeLock to keep WiFi awake until broadcast is received */
	private WifiManager.MulticastLock wifiWakeLock = null;

	/** WiFi manager to detect changes in home Wifi signal strength */
	private WifiManager wifiMgr;
	/** Connected SSID */
	public static String currentSSID = "";
	/** Flag if WiFi AP switch is expected */
	private boolean wifiSwitchExpected = false;

	/** String for toast used in handler */
	private static String toastMsg;

	/** Notification channel to update the foreground notification */
	static NotificationChannel chan1;
	/** Notification manager */
	static NotificationManager manager;

	/** MQTT client */
	public static volatile IMqttAsyncClient mqttClient = null;
	/** Flag if MQTT listener is restarted after a broadcast was received */
	private static Boolean shouldRestartMQTTListen=true;
	/** Flag if MQTT re/connection has started */
	private boolean mqttIsConnecting = false;
	/** Counter for Connection lost (32109) - java.io.EOFException */
	private static int mqttconnLostNum = 0;

	/** Array to calculate average consumption (to avoid too many alerts */
	private static final ArrayList<Float> avgConsumption = new ArrayList<>();
	/** Counter for entries in average consumption array */
	private static int avgConsIndex = 0;

	/** Flag if user UI has just started */
	public static boolean uiStarted = false;

	/** Pending intent to update device status widget every 5 minutes */
	private static PendingIntent devStatpi;
	/** Alarm manager to update device status widget every 5 minutes */
	private static AlarmManager am;

	/** Last message from SPM */
	private String lastSPM = "";
	/** Time last message was received from SPM */
	public static long lastSpmMsg = 0;
	/** Last message from AC1 */
	private String lastAC1= "";
	/** Time last message was received from AC1 */
	public static long lastAc1Msg = 0;
	/** Last message from AC2 */
	private String lastAC2 = "";
	/** Time last message was received from AC2 */
	public static long lastAc2Msg = 0;
	/** Last message from AC3 */
	private String lastAC3 = "";
	/** Time last message was received from AC3 */
	public static long lastAc3Msg = 0;
	/** Last message from SF1 */
	private String lastSF1 = "";
	/** Time last message was received from SF1 */
	public static long lastSf1Msg = 0;
	/** Last message from SB1 */
	private String lastSB1 = "";
	/** Time last message was received from SB1 */
	public static long lastSb1Msg = 0;
	/** Last message from LB1 */
	private String lastLB1 = "";
	/** Time last message was received from LB1 */
	public static long lastLb1Msg = 0;
	/** Last message from LY1 */
	private String lastLY1 = "";
	/** Time last message was received from LY1 */
	public static long lastLy1Msg = 0;
	/** Last message from WEO */
	private String lastWEO = "";
	/** Last message from WEI */
	private String lastWEI = "";
	/** Last message from CM1 */
	private String lastCM1 = "";
	/** Time last message was received from CM1 */
	public static long lastCm1Msg = 0;
	/** Last message from MHC */
	private String lastMHC = "";
	/** Time last message was received from MHC */
	public static long lastMhcMsg = 0;

	/** Broker status structure */
	public static int clientsConn = 0; // $SYS/broker/clients/connected
	public static Double bytesLoadRcvd = 0.0; // $SYS/broker/load/bytes/received/15min
	public static Double bytesLoadSend = 0.0; // $SYS/broker/load/bytes/sent/15min
	public static Double bytesMsgsRcvd = 0.0; // $SYS/broker/load/publish/received/15min
	public static Double bytesMsgsSend = 0.0; // $SYS/broker/load/publish/sent/15min
	private static Double lastBytesLoadRcvd = 0.0; // $SYS/broker/load/bytes/received/15min
	private static Double lastBytesLoadSend = 0.0; // $SYS/broker/load/bytes/sent/15min
	private static Double lastBytesMsgsRcvd = 0.0; // $SYS/broker/load/publish/received/15min
	private static Double lastBytesMsgsSend = 0.0; // $SYS/broker/load/publish/sent/15min
	public static final ArrayList<String> mqttClientList = new ArrayList<>();

	/** Doorbell flag */
	private boolean doorBellActive = false;

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Enable access to internet
		/* ThreadPolicy to get permission to access internet */
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Get the MulticastLock to be able to receive multicast UDP messages
		/* Wifi manager to check wifi status */
		WifiManager wifi = (WifiManager) getApplicationContext().getSystemService( Context.WIFI_SERVICE );
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
//		startForeground(1, ServiceNotification(this, "general"));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startMyOwnForeground(getApplicationContext());
		} else {
			startForeground(1, ServiceNotification(this));
		}

		boolean[] hasConnection = Utilities.connectionAvailable(this);
		if (hasConnection[2]) { // Home WiFi connection available?
			connStatus = HOME_WIFI;
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start UDP listener");
			shouldRestartSocketListen = true;
			shouldRestartMQTTListen = true;
			try {
				if (!udpListenerActive) {
					// Start listener for UDP broadcast messages
					startListenForUDPBroadcast();
				}
			} catch (Exception ignore) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Failed to start UDP listener");
			}
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start TCP listener");
			try {
				if (!tcpListenerActive) {
					// Start listener for TCP messages
					startListenForTCPMessage();
				}
			} catch (Exception ignore) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Failed to start TCP listener");
			}
			if (devStatpi != null && am != null) {
				am.cancel(devStatpi);
				devStatpi.cancel();
				am = null;
				devStatpi = null;
			}
			/* Pending intent to update device status widget every 3 minutes */
			devStatpi = PendingIntent.getService(this, 5003,
					new Intent(this, DeviceStatus.class), PendingIntent.FLAG_UPDATE_CURRENT);
			/* Alarm manager to update device status widget every 3 minutes */
			am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
			am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 120000,
					180000, devStatpi);
		} else if (hasConnection[1] || hasConnection[0]) { // Mobile or WiFi connection available?
			connStatus = WIFI_MOBILE;
			shouldRestartMQTTListen = true;
			shouldRestartSocketListen = false;
			// Connect to MQTT broker
			if ((mqttClient == null) || !mqttClient.isConnected()) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start MQTT listener");
				if (!mqttIsConnecting) { // Check if already trying to connect?
					new doConnect().execute();
				}
			}
			if (devStatpi != null && am != null) {
				am.cancel(devStatpi);
				devStatpi.cancel();
				am = null;
				devStatpi = null;
			}
			/* Pending intent to update device status widget every 5 minutes */
			devStatpi = PendingIntent.getService(this, 5003,
					new Intent(this, DeviceStatus.class), PendingIntent.FLAG_UPDATE_CURRENT);
			/* Alarm manager to update device status widget every 5 minutes */
			am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
			am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 120000,
					300000, devStatpi);
		} else { // No connection available
			connStatus = NO_CONNECTION;
			shouldRestartSocketListen = false;
			shouldRestartMQTTListen = false;
			if (devStatpi != null && am != null) {
				am.cancel(devStatpi);
				devStatpi.cancel();
				am = null;
				devStatpi = null;
			}
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "No active connection found");
		}

		// Get WifiManager to receive WiFi scan results
		wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

		/* IntentFilter to receive screen on/off & connectivity broadcast msgs */
		if (eventReceiverRegistered) {
			unregisterReceiver(eventReceiver);
			eventReceiverRegistered = false;
		}
		IntentFilter intentf = new IntentFilter();
		intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		intentf.addAction(WifiManager.RSSI_CHANGED_ACTION);
		intentf.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		registerReceiver(eventReceiver, intentf);
		eventReceiverRegistered = true;

		// this service should run until we stop it
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Service onDestroy()");
		shouldRestartSocketListen = false;
		if (wifiWakeLock != null) { // In case we restart after receiver problem
			wifiWakeLock.release();
		}
		if (tcpSocket != null) {
			try {
				tcpSocket.close();
			} catch (IOException ignore) {
			}
		}
		if (udpSocket != null) {
			udpSocket.disconnect();
			udpSocket.close();
		}
		if (mqttClient != null) {
			try {
				mqttClient.disconnect();
			} catch (MqttException ignore) {
			}
		}
		if (devStatpi != null && am != null) {
			am.cancel(devStatpi);
			devStatpi.cancel();
			am = null;
			devStatpi = null;
		}
		if (eventReceiverRegistered) {
			unregisterReceiver(eventReceiver);
		}
		// Restart service with a delay of 5 seconds
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start/Restart MessageListener listener");
				getApplicationContext().startService(new Intent(getApplicationContext(), MessageListener.class));
			}
		}, 5000);
	}

	private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, Intent intent) {
			// Screen on/off
			if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SCREEN_ON)
					|| Objects.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
				if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Screen on Event");
				}
				if (intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Connection Change Event");
				}
				final boolean[] bHasConnection = Utilities.connectionAvailable(context);
				if (bHasConnection[2]) { // Home WiFi connection available?
					wifiSwitchExpected = false;
					if (connStatus == HOME_WIFI) { // Was status HOME_WIFI already?
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "No connection change!");
					} else {
						connStatus = HOME_WIFI;
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Home WiFi connection");
						// Start/Restart service to listen to UDP/MQTT broadcast messages
						shouldRestartSocketListen = true;
						shouldRestartMQTTListen = true;
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Restart Device Status Updates");
						if (devStatpi == null && am == null) {
							/* Pending intent to update device status widget every 3 minutes */
							devStatpi = PendingIntent.getService(context, 5003,
									new Intent(context, DeviceStatus.class), PendingIntent.FLAG_UPDATE_CURRENT);
							/* Alarm manager to update device status widget every 3 minutes */
							am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
							am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 120000,
									180000, devStatpi);
						}
						if (!udpListenerActive) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Restart UDP listener");
							// Start listener for UDP broadcast messages
							startListenForUDPBroadcast();
						}
						if (!tcpListenerActive) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Restart TCP listener");
							// Start listener for TCP messages
							startListenForTCPMessage();
						}
						// Connect to MQTT broker
						if ((mqttClient == null) || (!mqttClient.isConnected())) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Start MQTT listener");
							if (!mqttIsConnecting) { // Check if already trying to connect?
								new doConnect().execute();
							}
						}
					}
				} else if (bHasConnection[1] || bHasConnection[0]) { // Mobile or WiFi connection available?
					if (connStatus == WIFI_MOBILE) { // Was status WIFI_MOBILE already?
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "No connection change!");
					} else {
						currentSSID = "";
						connStatus = WIFI_MOBILE;
						if (bHasConnection[1]) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Mobile connection");
						}
						if (bHasConnection[0]) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "WiFi connection");
						}
						// Start/Restart service to listen to MQTT broadcast messages
						shouldRestartSocketListen = false;
						shouldRestartMQTTListen = true;
						if (udpSocket != null && udpListenerActive) { // Socket still open?
							try {
								udpSocket.disconnect();
								udpSocket.close();
								udpSocket = null;
							} catch (NullPointerException ignore) {
							}
						}
						if (tcpSocket != null && tcpListenerActive) { // Socket still open?
							try {
								tcpSocket.close();
								tcpSocket = null;
							} catch (IOException ignore) {
							}
						}
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Restart Device Status Updates");
						if (devStatpi == null && am == null) {
							/* Pending intent to update device status widget every 5 minutes */
							devStatpi = PendingIntent.getService(context, 5003,
									new Intent(context, DeviceStatus.class), PendingIntent.FLAG_UPDATE_CURRENT);
							/* Alarm manager to update device status widget every 5 minutes */
							am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
							am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 120000,
									300000, devStatpi);
						}
						// Connect to MQTT broker
						if ((mqttClient == null) || (!mqttClient.isConnected())) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Start MQTT listener");
							if (!mqttIsConnecting) { // Check if already trying to connect?
								new doConnect().execute();
							}
						}
					}
				} else { // No connection available
					if (connStatus == NO_CONNECTION) { // Was status NO_CONNECTION already?
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "No connection change!");
					} else {
						currentSSID = "";
						connStatus = NO_CONNECTION;
						shouldRestartSocketListen = false;
						shouldRestartMQTTListen = false;
						// Disconnect all listener
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG_EVE, "Stop MQTT listener because no connection");
						if (mqttClient != null) { // mqttClient initialized?
							try {
								mqttClient.disconnect();
							} catch (MqttException ignore) {
							}
						}
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG_EVE, "Stop UDP listener because no connection");
						if (udpSocket != null && udpListenerActive) { // Socket still open?
							try {
								udpSocket.disconnect();
								udpSocket.close();
								udpSocket = null;
							} catch (NullPointerException ignore) {
							}
						}
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG_EVE, "Stop TCP listener because no connection");
						if (tcpSocket != null && tcpListenerActive) { // Socket still open?
							try {
								tcpSocket.close();
								tcpSocket = null;
							} catch (IOException ignore) {
							}
						}
						// Stop Device Status Update
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Stop Device Status Updates");
						if (devStatpi != null && am != null) {
							am.cancel(devStatpi);
							devStatpi.cancel();
							am = null;
							devStatpi = null;
						}
					}
				}
				deviceStatusWidgetUpdate(context, false);
			}

			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_EVE, "Stop Device Status Updates because Screen is off");
				if (devStatpi != null && am != null) {
					am.cancel(devStatpi);
					devStatpi.cancel();
					am = null;
					devStatpi = null;
				}
				connStatus = UNKNOWN_STATUS;
			}

			if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) && wifiSwitchExpected) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_SW, "SCAN_RESULTS_AVAILABLE_ACTION");
				int net1Level = -100;
				String net1SSID = getResources().getString(R.string.LOCAL_SSID);
				int net2Level = -100;
				String net2SSID = getResources().getString(R.string.ALT_LOCAL_SSID);
				String newNetSSID = "";

				List<ScanResult> wifiList = wifiMgr.getScanResults();
				for (int i = 0; i < wifiList.size(); i++) {
					if (BuildConfig.DEBUG)
						Log.d(DEBUG_LOG_TAG_SW, "Frequence of "
								+ wifiList.get(i).SSID
								+ " is  "
								+ wifiList.get(i).frequency
								+ " with RSSI of "
								+ wifiList.get(i).level);

					if (wifiList.get(i).SSID.equalsIgnoreCase(net1SSID)) {
						net1Level = wifiList.get(i).level;
					} else if (wifiList.get(i).SSID.equalsIgnoreCase(net2SSID)) {
						net2Level = wifiList.get(i).level;
					}
				}
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_SW,
						net1SSID + " level = " + net1Level + " "
								+ net2SSID + " level = " + net2Level);
				if (net1Level > net2Level && net2Level < net1Level-15) {
					// Change if net2Level is better by more than 15db
					newNetSSID = net1SSID;
				} else if (net2Level > net1Level && net1Level < net2Level-15) {
					// Change if net1Level is better by more than 15db
					newNetSSID = net2SSID;
				}

//				newNetSSID = net1Level > net2Level ? net1SSID : net2SSID;

				if (!newNetSSID.isEmpty()) {
					final WifiInfo connectionInfo = wifiMgr.getConnectionInfo();
					String currentSSID = connectionInfo.getSSID();
					if (currentSSID.equalsIgnoreCase("\""+newNetSSID+"\"")) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_SW, "No change needed");
						wifiSwitchExpected = false;
					} else {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_SW, "Switch to " + newNetSSID);
						new requestNewConnAsync().execute("\"" + newNetSSID + "\"");
					}
				} else {
					wifiSwitchExpected = false;
				}
			}

			if(intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)
					&& connStatus == HOME_WIFI
					&& !wifiSwitchExpected) {
				int newRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
				final WifiInfo connectionInfo = wifiMgr.getConnectionInfo();
				String currentSSID = connectionInfo.getSSID();

				if (currentSSID.equalsIgnoreCase("\""+getString(R.string.LOCAL_SSID)+"\"")
						|| currentSSID.equalsIgnoreCase("\""+getString(R.string.ALT_LOCAL_SSID)+"\"")) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_SW, "RSSI changed to " + newRssi);
					if (newRssi < -80) { // if RSSI is worse than -80dB check if other network is better
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG_SW, "RSSI < -80dB check other network");
						wifiSwitchExpected = true;
						wifiMgr.startScan();
					}
				}
			}
		}
	};

	// UDP stuff starts here
	/**
	 * Start listener for UDP messages
	 */
	private void startListenForUDPBroadcast() {
		Thread UDPBroadcastThread = new Thread(new Runnable() {
			public void run() {
				try {
					/* IP mask from where we expect the UDP broadcasts */
					InetAddress broadcastIP = InetAddress.getByName(broadCastIP); //172.16.238.42 //192.168.1.255
					/* Port from where we expect the UDP broadcasts */
					Integer port = UDP_SERVER_PORT;
					udpListenerActive = true;
					while (udpListenerActive) {
						if (shouldRestartSocketListen) { // Should we listen to UDP broadcasts???
							listenUDPBroadCast(broadcastIP, port);
							// Check if UI started
							if (uiStarted) {
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Send last status to UI");
								uiStarted = false;
								new sendLastMsgs().execute();
							}
						} else {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "UDP shouldRestartSocketListen = false");
							udpListenerActive = false;
						}
					}
					if (!shouldRestartSocketListen) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop UDP listener");
						if (udpSocket != null) {
							try {
								udpSocket.disconnect();
								udpSocket.close();
								udpSocket = null;
							} catch (NullPointerException ignore) {}
							udpListenerActive = false;
						}
//						if (tcpSocket != null) {
//							try {
//								tcpSocket.close();
//								tcpSocket = null;
//							} catch (IOException ignore) {}
//							tcpListenerActive = false;
//						}
					}
				} catch (Exception e) {
					if (shouldRestartSocketListen) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart UDP listener after error " + e.getMessage());
						startListenForUDPBroadcast();
					} else {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop UDP listener");
						udpListenerActive = false;
						if (udpSocket != null) {
							try {
								udpSocket.disconnect();
								udpSocket.close();
								udpSocket = null;
							} catch (NullPointerException ignore) {}
						}
//						if (tcpSocket != null) {
//							try {
//								tcpSocket.close();
//								tcpSocket = null;
//							} catch (IOException ignore) {
//							}
//						}
					}
				}
			}
		});

		if (shouldRestartSocketListen) {
			UDPBroadcastThread.start();
		}
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
		/* Byte buffer for incoming data */
		byte[] recvBuf = new byte[1000];
		if (udpSocket == null || udpSocket.isClosed()) {
			try {
				udpSocket = new DatagramSocket(port, broadcastIP);
			} catch (SocketException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Cannot open udpSocket " + e.getMessage());
				udpSocket.disconnect();
				udpSocket.close();
				udpSocket = null;
				return;
			}
		}
		/* Datagram packet for incoming data */
		DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Waiting for UDP broadcast");
		try {
			udpSocket.receive(packet);
		} catch (IOException e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Socket receive failed " + e.getMessage());
		}

		/* IP address of UDP broadcast sender */
		String senderIP = packet.getAddress().getHostAddress();
		/* Message attached to UDP broadcast */
		String message = new String(packet.getData()).trim();

		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Got UDP broadcast from " + senderIP + ", message: " + message);

		// Check if response is a JSON array
		if (Utilities.isJSONValid(message)) {
			/* Json object for received data */
			JSONObject jsonResult;
			SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(MyHomeControl.sharedPrefName,0);
			String newDeviceName = "";
			try {
				jsonResult = new JSONObject(message);
				/* Device ID from UDP broadcast message */
				String broadCastDevice = jsonResult.getString("de");
				/* Store IP address of the device */
				switch (broadCastDevice) {
					case "spm":
						deviceIPs[spMonitorIndex] = senderIP;
						deviceIsOn[spMonitorIndex] = true;
						newDeviceName = MyHomeControl.deviceNames[MyHomeControl.spMonitorIndex];
						break;
					case "fd1":
						deviceIPs[aircon1Index] = senderIP;
						deviceIsOn[aircon1Index] = true;
						newDeviceName = MyHomeControl.deviceNames[MyHomeControl.aircon1Index];
						break;
					case "ca1":
						deviceIPs[aircon2Index] = senderIP;
						deviceIsOn[aircon2Index] = true;
						newDeviceName = MyHomeControl.deviceNames[MyHomeControl.aircon2Index];
						break;
					case "am1":
						deviceIPs[aircon3Index] = senderIP;
						deviceIsOn[aircon3Index] = true;
						newDeviceName = MyHomeControl.deviceNames[MyHomeControl.aircon3Index];
						break;
					case "sf1":
						deviceIPs[secFrontIndex] = senderIP;
						deviceIsOn[secFrontIndex] = true;
						newDeviceName = MyHomeControl.deviceNames[MyHomeControl.secFrontIndex];
						break;
					case "sb1":
						deviceIPs[secBackIndex] = senderIP;
						deviceIsOn[secBackIndex] = true;
						newDeviceName = MyHomeControl.deviceNames[MyHomeControl.secBackIndex];
						break;
					case "lb1":
						deviceIPs[lb1Index] = senderIP;
						deviceIsOn[lb1Index] = true;
						newDeviceName = MyHomeControl.deviceNames[MyHomeControl.lb1Index];
						break;
					case "ly1":
						deviceIPs[ly1Index] = senderIP;
						deviceIsOn[ly1Index] = true;
						newDeviceName = MyHomeControl.deviceNames[MyHomeControl.ly1Index];
						break;
					case "weo":
						lastWEO = message;
						break;
					case "wei":
						lastWEI = message;
						break;
					case "cm1":
						deviceIPs[cam1Index] = senderIP;
						deviceIsOn[cam1Index] = true;
						newDeviceName = MyHomeControl.deviceNames[MyHomeControl.cam1Index];
						break;
					case "mhc":
						deviceIPs[mhcIndex] = senderIP;
						deviceIsOn[mhcIndex] = true;
						newDeviceName = MyHomeControl.deviceNames[MyHomeControl.mhcIndex];
						break;
				}
				if (!newDeviceName.equalsIgnoreCase("")) {
					mPrefs.edit().putString(newDeviceName,senderIP).apply();
				}
			} catch (JSONException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Create JSONObject from String <"+ message +"> failed " + e.getMessage());
			}

			handleMsgs(message);
			// Send broadcast to listening activities
			sendMyBroadcast(message, "UDP");

//			if (mPrefs.getBoolean(MyHomeControl.prefsShowDebug, false)) {
//				toastMsg = message;
//				Handler handler = new Handler(Looper.getMainLooper());
//				handler.post(new Runnable() {
//					@Override
//					public void run() {
//						Toast.makeText(getApplicationContext(),
//								"Received UDP data: " + toastMsg,
//								Toast.LENGTH_SHORT).show();
//					}
//				});
//			}
		}
	}

	// TCP stuff starts here
	/**
	 * Start listener for TCP messages
	 */
	private void startListenForTCPMessage() {
		Thread TCPMessageThread = new Thread(new Runnable() {
			public void run() {
				try {
					tcpListenerActive = true;
					while (tcpListenerActive) {
						if (shouldRestartSocketListen) {
							if (tcpSocket == null) {
								tcpSocket = new ServerSocket();
								tcpSocket.setReuseAddress(true);
								tcpSocket.bind(new InetSocketAddress(TCP_SERVER_PORT));
							}
							// LISTEN FOR INCOMING CLIENTS
							Socket client = tcpSocket.accept();
							try {
								BufferedReader in = new BufferedReader(
										new InputStreamReader(client.getInputStream()));
								String inMsg;
								while ((inMsg = in.readLine()) != null) {
									// Send broadcast to listening activities
									sendMyBroadcast(inMsg, "DEBUG");
									if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received TCP data from: "
											+ client.getInetAddress().toString().substring(1) + " :" + inMsg);
									SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(MyHomeControl.sharedPrefName,0);
									if (mPrefs.getBoolean(MyHomeControl.prefsShowDebug, false)) {
										toastMsg = inMsg;
										Handler handler = new Handler(Looper.getMainLooper());
										handler.post(new Runnable() {
											@Override
											public void run() {
												Toast.makeText(getApplicationContext(),
														"Received TCP data: " + toastMsg,
														Toast.LENGTH_SHORT).show();
											}
										});
									}
								}
							} catch (Exception e) {
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Receiving TCP data failed " + e.getMessage());
							}
						} else {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP shouldRestartSocketListen = false");
							if (tcpSocket != null) {
								try {
									tcpSocket.close();
								} catch (IOException ignore) {
								}
								tcpSocket = null;
								tcpListenerActive = false;
							}
						}
					}
				} catch (Exception e) {
					tcpListenerActive = false;
					if (tcpSocket != null) {
						try {
							tcpSocket.close();
						} catch (IOException | NullPointerException ignore) {
						}
						tcpSocket = null;
					}
					if (shouldRestartSocketListen) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart TCP listener after error " + e.getMessage());
						tcpListenerActive = false;
						startListenForTCPMessage();
					}
				}
			}
		});

		if (shouldRestartSocketListen) {
			TCPMessageThread.start();
		}
	}

	// MQTT stuff starts here
	/**
	 * Connect to MQTT broker and subscribe to topics
	 */
	@SuppressLint("StaticFieldLeak")
	private class doConnect extends AsyncTask<String, Void, Boolean> {

		@SuppressLint("CommitPrefEdits")
		@Override
		protected Boolean doInBackground(String... params) {
			boolean connected = true;
			mqttIsConnecting = true;
			if (shouldRestartMQTTListen) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "doConnect()");
				if ((mqttClient != null) && (mqttClient.isConnected())) {
					return true;
				}
				String deviceId = Utilities.getDeviceName();
				String subscriberID = "/DEV/" + deviceId.toUpperCase();
				String mqttIP = getResources().getString(R.string.MQTT_IP);
				SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(MyHomeControl.sharedPrefName,0);
				int currMqttUser = mPrefs.getInt("MQTT_User",0);
				String mqttUser;
				switch (currMqttUser) {
					case 1:
						mqttUser = getResources().getString(R.string.MQTT_USER_SUB_2);
						break;
					case 2:
						mqttUser = getResources().getString(R.string.MQTT_USER_SUB_3);
						break;
					case 3:
						mqttUser = getResources().getString(R.string.MQTT_USER_SUB_4);
						break;
					default:
						mqttUser = getResources().getString(R.string.MQTT_USER_SUB_1);
						break;
				}
				String mqttPw = getResources().getString(R.string.MQTT_PW);
				IMqttToken token;
				MqttConnectOptions options = new MqttConnectOptions();
				options.setCleanSession(false);
				options.setUserName(mqttUser);
				options.setPassword(mqttPw.toCharArray());
				options.setConnectionTimeout(60);
				options.setAutomaticReconnect(true);
				options.setKeepAliveInterval(300);
				byte[] lastWill;
				lastWill = "Dead".getBytes(StandardCharsets.UTF_8);
				options.setWill(subscriberID, lastWill, 2, true);

				try {
					mqttClient = new MqttAsyncClient(mqttIP, deviceId, new MemoryPersistence());
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "mqttClient.connect()");
					token = mqttClient.connect(options);
					token.waitForCompletion(60000);
					mqttClient.setCallback(new MqttEventCallback());
				} catch (MqttSecurityException e) {
					connected = false;
				} catch (ExceptionInInitializerError e) {
					connected = false;
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "BROKER_UNAVAILABLE " +e.getMessage());
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
							if (e.getMessage().contains("32109")) { // Connection lost due to EOFException
								mqttconnLostNum ++;
								if (mqttconnLostNum >= 1000) { // Same error occured more than 1000 times
									if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "CONNECTION_LOST > 1000 times!!!");
//									stopSelf();
								}
							}
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
				if (!mqttClientList.contains(Utilities.getDeviceName().toUpperCase())){
					mqttClientList.add(Utilities.getDeviceName().toUpperCase());
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
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT connection successful");
				sendMyBroadcast("MQTT connected", "STATUS");
				IMqttToken token;
				Context onPostContext = getApplicationContext();
				SharedPreferences mPrefs = onPostContext.getSharedPreferences(MyHomeControl.sharedPrefName,0);
				int currMqttUser = mPrefs.getInt("MQTT_User",0);
				String mqttUser;
				switch (currMqttUser) {
					case 1:
						mqttUser = getResources().getString(R.string.MQTT_USER_SUB_2);
						break;
					case 2:
						mqttUser = getResources().getString(R.string.MQTT_USER_SUB_3);
						break;
					case 3:
						mqttUser = getResources().getString(R.string.MQTT_USER_SUB_4);
						break;
					default:
						mqttUser = getResources().getString(R.string.MQTT_USER_SUB_1);
						break;
				}
				try {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /SPM");
					token = mqttClient.subscribe("/SPM", 0);
					token.waitForCompletion(10000);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /AC1");
					token = mqttClient.subscribe("/AC1", 0);
					token.waitForCompletion(10000);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /AC2");
					token = mqttClient.subscribe("/AC2", 0);
					token.waitForCompletion(10000);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /WEI");
					token = mqttClient.subscribe("/WEI", 0);
					token.waitForCompletion(10000);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /WEO");
					token = mqttClient.subscribe("/WEO", 0);
					token.waitForCompletion(10000);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /SEF");
					token = mqttClient.subscribe("/SEF", 0);
					token.waitForCompletion(10000);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /SEB");
					token = mqttClient.subscribe("/SEB", 0);
					token.waitForCompletion(10000);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /LB1");
					token = mqttClient.subscribe("/LB1", 0);
					token.waitForCompletion(10000);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /LY1");
					token = mqttClient.subscribe("/LY1", 0);
					token.waitForCompletion(10000);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /MHC");
					token = mqttClient.subscribe("/MHC", 0);
					token.waitForCompletion(10000);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /CM1");
					token = mqttClient.subscribe("/CM1", 0);
					token.waitForCompletion(10000);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /DEV/#");
					token = mqttClient.subscribe("/DEV/#", 0);
					token.waitForCompletion(10000);

					// Send one time topic to announce device as subscriber
					String deviceId = Utilities.getDeviceName();
					deviceId = "/DEV/" + deviceId.toUpperCase();
					byte[] encodedPayload;
					encodedPayload = mqttUser.getBytes(StandardCharsets.UTF_8);
					MqttMessage message = new MqttMessage(encodedPayload);
					message.setRetained(true);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Publishing own device info");
					token = mqttClient.publish(deviceId, message);
					token.waitForCompletion(10000);
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
							if (e.getMessage().contains("32109")) { // Connection lost due to EOFException
								mqttconnLostNum ++;
								if (mqttconnLostNum >= 1000) { // Same error occured more than 1000 times
									if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "CONNECTION_LOST > 1000 times!!!");
//									stopSelf();
								}
							}
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
				}
				deviceStatusWidgetUpdate(onPostContext, true);
			} else { // retry in 30 seconds
				boolean[] bHasConnection = Utilities.connectionAvailable(getApplicationContext());
				if (bHasConnection[0] || bHasConnection[1] || bHasConnection[2]) {
					new doConnect().execute();
				}
			}
			mqttIsConnecting = false;
		}
	}

	private class MqttEventCallback implements MqttCallback {

		@Override
		public void connectionLost(Throwable arg0) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT lost connection:" + arg0);
			// Send broadcast to listening activities
			sendMyBroadcast("MQTT disconnected", "STATUS");
			if (!mqttIsConnecting) { // Check if already trying to connect?
				// Reconnect to MQTT with a delay of 5 seconds
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Reconnect MQTT listener");
						new doConnect().execute();
					}
				}, 5000);
			}
			mqttClientList.remove(Utilities.getDeviceName().toUpperCase());
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken arg0) {

		}

		@Override
		@SuppressLint("NewApi")
		public void messageArrived(final String topic, final MqttMessage msg) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT arrived from topic " + topic);
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT topic " + new String(msg.getPayload()));
			Handler h = new Handler(getMainLooper());
			h.post(new Runnable() {
				@Override
				public void run() {

					String receivedMessage = new String(msg.getPayload());
					if (receivedMessage.length() == 0) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Empty payload");
						return;
					}

					// Check if topic is a broker status
					if (topic.startsWith("$SYS")) {
//						if (topic.contains("load/bytes/received/1min")) {
						if (topic.contains("bytes/received")) {
							Double bLRcvd = Double.parseDouble(receivedMessage);
							bytesLoadRcvd = bLRcvd - lastBytesLoadRcvd;
							lastBytesLoadRcvd = bLRcvd;
//						} else if (topic.contains("load/bytes/sent/1min")) {
						} else if (topic.contains("bytes/sent")) {
							Double bLSend = Double.parseDouble(receivedMessage);
							bytesLoadSend = bLSend - lastBytesLoadSend;
							lastBytesLoadSend = bLSend;
//						} else if (topic.contains("messages/received/1min")) {
						} else if (topic.contains("messages/received")) {
							Double bMRcvd = Double.parseDouble(receivedMessage);
							bytesMsgsRcvd = bMRcvd - lastBytesMsgsRcvd;
							lastBytesMsgsRcvd = bMRcvd;
//						} else if (topic.contains("messages/sent/1min")) {
						} else if (topic.contains("messages/sent")) {
							Double bMSend = Double.parseDouble(receivedMessage);
							bytesMsgsSend = bMSend - lastBytesMsgsSend;
							lastBytesMsgsSend = bMSend;
//						} else if (topic.contains("clients/connected")) {
						} else if (topic.contains("clients/count")) {
							clientsConn = Integer.parseInt(receivedMessage);
						}
						sendMyBroadcast("BrokerStatus", "BROKER");
						return;
					}
					// Check if topic is a device registration
					String deviceId = Utilities.getDeviceName();
					String subscriberID = "/DEV/" + deviceId.toUpperCase();
					if (topic.startsWith("/DEV/")) {
						if (!topic.equalsIgnoreCase(subscriberID)) { // check if it is our own registration
							if (!receivedMessage.equalsIgnoreCase("Dead")) { // Client is disconnected
								SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(MyHomeControl.sharedPrefName,0);
								int currMqttUser = mPrefs.getInt("MQTT_User",0);
								String mqttUser = "";
								switch (currMqttUser) {
									case 1:
										if (receivedMessage.equalsIgnoreCase(getResources().getString(R.string.MQTT_USER_SUB_1))) {
											mqttUser = getResources().getString(R.string.MQTT_USER_SUB_2);
											mPrefs.edit().putInt("MQTT_User",2).apply();
										}
										break;
									case 2:
										if (receivedMessage.equalsIgnoreCase(getResources().getString(R.string.MQTT_USER_SUB_2))) {
											mqttUser = getResources().getString(R.string.MQTT_USER_SUB_3);
											mPrefs.edit().putInt("MQTT_User",3).apply();
										}
										break;
									case 3:
										if (receivedMessage.equalsIgnoreCase(getResources().getString(R.string.MQTT_USER_SUB_3))) {
											mqttUser = getResources().getString(R.string.MQTT_USER_SUB_4);
											mPrefs.edit().putInt("MQTT_User",4).apply();
										}
										break;
									default:
										if (receivedMessage.equalsIgnoreCase(getResources().getString(R.string.MQTT_USER_SUB_4))) {
											mqttUser = getResources().getString(R.string.MQTT_USER_SUB_1);
											mPrefs.edit().putInt("MQTT_User",1).apply();
										}
										break;
								}
								if (!mqttUser.equalsIgnoreCase("")) { // Another device is using the same ID
									if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Reconnect MQTT listener because of double use!");
									new doConnect().execute();
								}
							}
						}
						// Add/delete device to/from client list
						if (receivedMessage.equalsIgnoreCase("Dead")) { // Client is disconnected
							mqttClientList.remove(topic.substring(5).toUpperCase());
						} else { // Client is connected
							if (!mqttClientList.contains(topic.substring(5).toUpperCase())){
								mqttClientList.add(topic.substring(5).toUpperCase());
							}
						}
						sendMyBroadcast("BrokerStatus", "BROKER");
						return;
					}

					// Check if screen is locked
					/* Keyguard manager instance */
					KeyguardManager myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
					/* Flag for locked phone */
					boolean phoneIsLocked = false;
					if (myKM != null) {
						phoneIsLocked = myKM.inKeyguardRestrictedInputMode();
					}
					// Check if screen is off
					/* Instance of power manager */
					PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
					/* Flag for screen off */
					boolean screenIsOff = true;
					if (powerManager != null && powerManager.isInteractive()) {
						screenIsOff = false;
					}
					// Check if we are connected to the home WiFi
					/* Flag for local Wifi */
					boolean notOnHomeWifi = !Utilities.isHomeWiFi(getApplicationContext());

					// If we are not on home Wifi or screen is off or locked => process the message
					if (notOnHomeWifi || phoneIsLocked || screenIsOff || uiStarted) {
//						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Not home or phone is locked or screen is off");

						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Payload is " + receivedMessage);

						// Update widgets
						handleMsgs(receivedMessage);

//						// Forward to all local listeners
						sendMyBroadcast(receivedMessage, "MQTT");

//						// Show toast on screen if debug is enabled
//						SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(MyHomeControl.sharedPrefName,0);
//						if (mPrefs.getBoolean(MyHomeControl.prefsShowDebug, false)) {
//							toastMsg = receivedMessage;
//							Handler handler = new Handler(Looper.getMainLooper());
//							handler.post(new Runnable() {
//								@Override
//								public void run() {
//									Toast.makeText(getApplicationContext(),
//											"Received TCP data: " + toastMsg,
//											Toast.LENGTH_SHORT).show();
//								}
//							});
//						}
					}
				}
			});
		}
	}

	/**
	 * Start runnable to subscribe to broker status
	 */
	public static void subscribeBrokerStatus() {
		new subscribeBrokerStatusRunnable();
	}

	/**
	 * Subscribe to broker status
	 */
	private static class subscribeBrokerStatusRunnable implements Runnable {

		subscribeBrokerStatusRunnable() {
			run();
		}

		public void run() {
			if (mqttClient != null) {
				// Clear old status
				clientsConn = 0; // $SYS/broker/clients/connected
				bytesLoadRcvd = 0.0; // $SYS/broker/load/bytes/received/15min
				lastBytesLoadRcvd = 0.0; // $SYS/broker/load/bytes/received/15min
				bytesLoadSend = 0.0; // $SYS/broker/load/bytes/sent/15min
				lastBytesLoadSend = 0.0; // $SYS/broker/load/bytes/sent/15min
				bytesMsgsRcvd = 0.0; // $SYS/broker/load/publish/received/15min
				lastBytesMsgsRcvd = 0.0; // $SYS/broker/load/publish/received/15min
				bytesMsgsSend = 0.0; // $SYS/broker/load/publish/sent/15min
				lastBytesMsgsSend = 0.0; // $SYS/broker/load/publish/sent/15min
				mqttClientList.clear();

				IMqttToken token;
				try {
					token = mqttClient.unsubscribe("/DEV/#");
					token.waitForCompletion(10000);
//					token = mqttClient.subscribe("$SYS/broker/load/bytes/received/1min", 0);
					token = mqttClient.subscribe("$SYS/brokers/+/metrics/bytes/received", 0);
					token.waitForCompletion(10000);
//					token = mqttClient.subscribe("$SYS/broker/load/bytes/sent/1min", 0);
					token = mqttClient.subscribe("$SYS/brokers/+/metrics/bytes/sent", 0);
					token.waitForCompletion(10000);
//					token = mqttClient.subscribe("$SYS/broker/load/messages/received/1min", 0);
					token = mqttClient.subscribe("$SYS/brokers/+/metrics/messages/received", 0);
					token.waitForCompletion(10000);
//					token = mqttClient.subscribe("$SYS/broker/load/messages/sent/1min", 0);
					token = mqttClient.subscribe("$SYS/brokers/+/metrics/messages/sent", 0);
					token.waitForCompletion(10000);
//					token = mqttClient.subscribe("$SYS/broker/clients/connected", 0);
					token = mqttClient.subscribe("$SYS/brokers/+/stats/clients/count", 0);
					token.waitForCompletion(10000);
					token = mqttClient.subscribe("/DEV/#", 0);
					token.waitForCompletion(10000);
				} catch (MqttException e) {
					switch (e.getReasonCode()) {
						case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
							if (BuildConfig.DEBUG)
								Log.d(DEBUG_LOG_TAG, "BROKER_UNAVAILABLE " + e.getMessage());
							break;
						case MqttException.REASON_CODE_CLIENT_TIMEOUT:
							if (BuildConfig.DEBUG)
								Log.d(DEBUG_LOG_TAG, "CLIENT_TIMEOUT " + e.getMessage());
							break;
						case MqttException.REASON_CODE_CONNECTION_LOST:
							if (BuildConfig.DEBUG)
								Log.d(DEBUG_LOG_TAG, "CONNECTION_LOST " + e.getMessage());
							if (e.getMessage().contains("32109")) { // Connection lost due to EOFException
								mqttconnLostNum++;
							}
							break;
						case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
							if (BuildConfig.DEBUG)
								Log.d(DEBUG_LOG_TAG, "SERVER_CONNECT_ERROR " + e.getMessage());
							break;
						case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
							if (BuildConfig.DEBUG)
								Log.d(DEBUG_LOG_TAG, "FAILED_AUTHENTICATION " + e.getMessage());
							break;
						default:
							if (BuildConfig.DEBUG)
								Log.d(DEBUG_LOG_TAG, "MQTT unknown error " + e.getMessage());
							break;
					}
				}
			}
		}
	}

	/**
	 * Start runnable to unsubscribe from broker status
	 */
	public static void unSubscribeBrokerStatus() {
		new unSubscribeBrokerStatusRunnable();
	}

	/**
	 * Unsubscribe from broker status
	 */
	private static class unSubscribeBrokerStatusRunnable implements Runnable {

		unSubscribeBrokerStatusRunnable() {
			run();
		}

		public void run() {
			if (mqttClient != null) {
				IMqttToken token;
				try {
//			    	token = mqttClient.unsubscribe("");
//				    token.waitForCompletion(10000);

//					token = mqttClient.unsubscribe("$SYS/broker/clients/connected");
					token = mqttClient.unsubscribe("$SYS/brokers/+/stats/clients/count");
					token.waitForCompletion(10000);
//					token = mqttClient.unsubscribe("$SYS/broker/load/bytes/received/1min");
					token = mqttClient.unsubscribe("$SYS/brokers/+/metrics/bytes/received");
					token.waitForCompletion(10000);
//					token = mqttClient.unsubscribe("$SYS/broker/load/bytes/sent/1min");
					token = mqttClient.unsubscribe("$SYS/brokers/+/metrics/bytes/sent");
					token.waitForCompletion(10000);
//					token = mqttClient.unsubscribe("$SYS/broker/load/messages/received/1min");
					token = mqttClient.unsubscribe("$SYS/brokers/+/metrics/messages/received");
					token.waitForCompletion(10000);
//					token = mqttClient.unsubscribe("$SYS/broker/load/messages/sent/1min");
					token = mqttClient.unsubscribe("$SYS/brokers/+/metrics/messages/sent");
					token.waitForCompletion(10000);
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
							if (e.getMessage().contains("32109")) { // Connection lost due to EOFException
								mqttconnLostNum ++;
							}
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
				}
			}
		}
	}

	/**
	 * Send received message to all listing threads
	 *
	 * @param msgReceived
	 *            Flag if alarm is active
	 * @param fromSender
	 *            Flag if alarm is on
	 */
	private void sendMyBroadcast(String msgReceived, String fromSender) {
		/* Intent for activity internal broadcast message */
		Intent broadCastIntent = new Intent();
		broadCastIntent.setAction(BROADCAST_RECEIVED);
		broadCastIntent.putExtra("from", fromSender);
		broadCastIntent.putExtra("message", msgReceived);
		sendBroadcast(broadCastIntent);
	}

	/**
	 * Handle received message
	 *
	 * @param message
	 *          String with received message as JSON
	 */
	private void handleMsgs(String message) {
		Context context = getApplicationContext();
		/* Json object for received data */
		JSONObject jsonResult;
		try {
			jsonResult = new JSONObject(message);
			/* current time used for device status widget */
			long timeNow = System.currentTimeMillis();
			/* Device ID from UDP broadcast message */
			String broadCastDevice = jsonResult.getString("de");
			switch (broadCastDevice) {
				case "spm":
					lastSPM = message;
					lastSpmMsg = timeNow;
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update solar panel widgets");

					/* Value of solar production */
					float solarPower = jsonResult.getInt("s");
					/* Value of house consumption */
					float consPower = jsonResult.getInt("c");

					// Activate/deactivate alarm sound and update widget
					solarAlarmAndWidgetUpdate(solarPower, consPower, context);
					break;
				case "fd1":
					lastAC1 = message;
					lastAc1Msg = timeNow;
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update office aircon widgets");

					/* Flag for alarm switched on or off */
					boolean timerOn = (jsonResult.getInt("ti") == 1);
					/* Timer time */
					int timerTime = jsonResult.getInt("ot");
					/* Timer end time */
					String timerEnd = jsonResult.getString("ts");

					// Update widget
					airconWidgetUpdate(timerOn, timerTime, timerEnd, context);
					break;
				case "ca1":
					lastAC2 = message;
					lastAc2Msg = timeNow;
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update office aircon widgets");
					// TODO add aircon 2 widget */
					break;
				case "am1":
					lastAC3 = message;
					lastAc3Msg = timeNow;
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update office aircon widgets");
					// TODO add aircon 3 widget */
					break;
				case "sf1":
					lastSF1 = message;
					lastSf1Msg = timeNow;
					// Broadcast from front security
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update security widgets");

					/* Flag for alarm on or off */
					boolean alarmIsActive = (jsonResult.getInt("ao") == 1);
					boolean alarmIsOn = (jsonResult.getInt("al") == 1);

					// Activate/deactivate alarm sound and update widget
					securityAlarmAndWidgetUpdate(alarmIsActive, alarmIsOn, broadCastDevice, context);
					break;
				case "sb1":
					lastSB1 = message;
					lastSb1Msg = timeNow;
					// Broadcast from front security
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update security widgets");

					/* Flag for alarm on or off */
					alarmIsActive = (jsonResult.getInt("ao") == 1);
					alarmIsOn = (jsonResult.getInt("al") == 1);

					// Activate/deactivate alarm sound and update widget
					securityAlarmAndWidgetUpdate(alarmIsActive, alarmIsOn, broadCastDevice, context);
					break;
				case "lb1":
					lastLB1 = message;
					lastLb1Msg = timeNow;
					// Broadcast from bedroom lights
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update light control widgets");

					/* Flag for alarm on or off */
					int brightnessValue = 0;
					if (jsonResult.has("br")) {
						brightnessValue = jsonResult.getInt("br");
						if (brightnessValue == 140) {
							brightnessValue = 1; // Bulbs are full on
						} else if (brightnessValue > 222) {
							brightnessValue = 3; // Bulbs are full off
						} else {
							brightnessValue = 2; // Bulbs are dimmed on
						}
					}
					// Update widget
					lightControlWidgetUpdate(brightnessValue, broadCastDevice, context);
					break;
				case "ly1":
					lastLY1 = message;
					lastLy1Msg = timeNow;
					// Broadcast from bedroom lights
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update light control widgets");

					int lightStatus = 0;
					if (jsonResult.has("lo")) {
						lightStatus = jsonResult.getInt("lo");
					}
					// Update widget
					lightControlWidgetUpdate(lightStatus, broadCastDevice, context);
					break;
				case "weo":
					lastWEO = message;
					break;
				case "wei":
					lastWEI = message;
					break;
				case "cm1":
					lastCM1 = message;
					lastCm1Msg = timeNow;
					break;
				case "mhc":
					lastMHC = message;
					lastMhcMsg = timeNow;
					break;
				case "db1":
					int ringDetect = jsonResult.getInt("rd");
					if ((ringDetect == 1) && !doorBellActive) {
						doorBellActive = true; // To avoid multiple starts of the notification
						/* String for notification */
						String notifText;
						/* Icon for notification */
						int notifIcon;
						/* Background color for notification icon in SDK Lollipop and newer */
						int notifColor;

						notifIcon = R.mipmap.ic_doorbell;
						notifText = "Visitor at main gate!";

						notifColor = context.getResources().getColor(android.R.color.holo_red_light);

						/* Pointer to notification builder for alarm message */
						NotificationCompat.Builder myNotifBuilder;
						long[] pattern = {500,500,500,500,500,500,500,500,500};
						myNotifBuilder = new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
								.setContentTitle(context.getString(R.string.app_name))
								.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MyHomeControl.class), 0))
								.setAutoCancel(false)
								.setPriority(NotificationCompat.PRIORITY_DEFAULT)
								.setVibrate(pattern)
								.setWhen(System.currentTimeMillis());
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
							myNotifBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
						}
						/* Pointer to notification manager for alarm message */
						NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

						myNotifBuilder.setSound(Uri.parse("android.resource://"
								+ this.getPackageName() + "/"
								+ R.raw.doorbell));

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
						alarmNotification.flags |= Notification.FLAG_INSISTENT;
						if (notificationManager != null) {
							notificationManager.notify(3, alarmNotification);
						}
					} else {
						doorBellActive = false;
					}
			}
		} catch (JSONException e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Create JSONObject from String <"+ message +"> failed " + e.getMessage());
		}

		boolean[] hasConnection = Utilities.connectionAvailable(this);
		if (hasConnection[2] && am == null) { // Home WiFi connection available?
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Home WiFi, but device status update was off");
			/* Pending intent to update device status widget every 3 minutes */
			devStatpi = PendingIntent.getService(this, 5003,
					new Intent(this, DeviceStatus.class), PendingIntent.FLAG_UPDATE_CURRENT);
			/* Alarm manager to update device status widget every 3 minutes */
			am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
			am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 120000,
					180000, devStatpi);
		}
	}

	/**
	 * Connect to MQTT broker and subscribe to topics
	 */
	@SuppressLint("StaticFieldLeak")
	private class sendLastMsgs extends AsyncTask<String, Void, Void> {

		protected Void doInBackground(String... params) {
			if (!lastSPM.equalsIgnoreCase("")) {
				sendMyBroadcast(lastSPM, "LAST");
				handleMsgs(lastSPM);
			}
			if (!lastAC1.equalsIgnoreCase("")) {
				sendMyBroadcast(lastAC1, "LAST");
				handleMsgs(lastAC1);
			}
			if (!lastAC2.equalsIgnoreCase("")) sendMyBroadcast(lastAC2, "LAST");
			if (!lastAC3.equalsIgnoreCase("")) sendMyBroadcast(lastAC3, "LAST");
			if (!lastSF1.equalsIgnoreCase("")) {
				sendMyBroadcast(lastSF1, "LAST");
				handleMsgs(lastSF1);
			}
			if (!lastSB1.equalsIgnoreCase("")) {
				sendMyBroadcast(lastSB1, "LAST");
				handleMsgs(lastSB1);
			}
			if (!lastLB1.equalsIgnoreCase("")) {
				sendMyBroadcast(lastLB1, "LAST");
				handleMsgs(lastLB1);
			}
			if (!lastLY1.equalsIgnoreCase("")) {
				sendMyBroadcast(lastLY1, "LAST");
				handleMsgs(lastLY1);
			}
			if (!lastWEO.equalsIgnoreCase("")) sendMyBroadcast(lastWEO, "LAST");
			if (!lastWEI.equalsIgnoreCase("")) sendMyBroadcast(lastWEI, "LAST");
			if (!lastCM1.equalsIgnoreCase("")) sendMyBroadcast(lastCM1, "LAST");
			if (!lastMHC.equalsIgnoreCase("")) sendMyBroadcast(lastMHC, "LAST");
			return null;
		}
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
			/* String for notification */
			String notifText;
			/* Icon for notification */
			int notifIcon;
			/* Background color for notification icon in SDK Lollipop and newer */
			int notifColor;

			notifIcon = R.drawable.ic_detection;
			notifText = "Intruder! in " + context.getResources().getString(R.string.sec_front_device);
			if (device.equalsIgnoreCase("sb1")) {
				notifText = "Intruder! in " + context.getResources().getString(R.string.sec_back_device);
			}

			notifColor = context.getResources().getColor(android.R.color.holo_red_light);

			/* Pointer to notification builder for alarm message */
			NotificationCompat.Builder myNotifBuilder;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				myNotifBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
						.setContentTitle(context.getString(R.string.app_name))
						.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MyHomeControl.class), 0))
						.setAutoCancel(false)
						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
						.setVisibility(Notification.VISIBILITY_PUBLIC)
						.setWhen(System.currentTimeMillis());
			} else {
				myNotifBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
						.setContentTitle(context.getString(R.string.app_name))
						.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MyHomeControl.class), 0))
						.setAutoCancel(false)
						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
						.setWhen(System.currentTimeMillis());
			}

			/* Pointer to notification manager for alarm message */
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

			/* Access to shared preferences of app widget */
			String selUri = context.getSharedPreferences(MyHomeControl.sharedPrefName, 0)
					.getString(MyHomeControl.prefsSecurityAlarm, "");
			/* Uri of selected alarm */
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
			alarmNotification.flags |= Notification.FLAG_INSISTENT | FLAG_AUTO_CANCEL;
			if (notificationManager != null) {
				notificationManager.notify(3, alarmNotification);
			}
		}

		// Update security widget
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update Security Widget");
		/* App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/* Component name of this widget */
		ComponentName thisAppWidget;
		if (device.equalsIgnoreCase("sf1")) {
			thisAppWidget = new ComponentName(context.getPackageName(),
					SecAlarmWidget.class.getName());
			/* List of all active widgets */
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

			for (int appWidgetId : appWidgetIds) {
				SecAlarmWidget.updateAppWidget(context,appWidgetManager,appWidgetId, alarmIsActive);
			}
//		} else {
//			// TODO missing widget for back yard security
		}
	}

	/**
	 * Update light widgets
	 *
	 * @param lightStatus
	 *            Status of Bulbs
	 * @param device
	 *            Name of light device
	 * @param context
	 *            Application context
	 */
	private static void lightControlWidgetUpdate(int lightStatus,
												 String device,
												 Context context) {
		// Update light control widget
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update lights control widget");

		/* App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/* Component name of this widget */
		ComponentName thisAppWidget;
		if (device.equalsIgnoreCase("lb1")) {
			thisAppWidget = new ComponentName(context.getPackageName(),
					BedRoomLightWidget.class.getName());
			/* List of all active widgets */
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

			for (int appWidgetId : appWidgetIds) {
				BedRoomLightWidget.updateAppWidget(context, appWidgetManager, appWidgetId, lightStatus);
			}
		} else if (device.equalsIgnoreCase("ly1")) {
			thisAppWidget = new ComponentName(context.getPackageName(),
					BackYardLightWidget.class.getName());
			/* List of all active widgets */
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

			for (int appWidgetId : appWidgetIds) {
				BackYardLightWidget.updateAppWidget(context, appWidgetManager, appWidgetId, lightStatus);
			}
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
	private static void solarAlarmAndWidgetUpdate(float solarPower, float consPower, Context context) {
		/*--------------------------------------*/
		/* Update notification                  */
		/*--------------------------------------*/
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update Solar Notification");
		/* Icon for notification */
		int notifIcon;
		/* Background color for notification icon in SDK Lollipop and newer */
		int notifColor;
		/* Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences(MyHomeControl.sharedPrefName, 0);

		// Average power consumption
		if (avgConsIndex < 10) { // Still filling the array?
			avgConsIndex++;
			avgConsumption.add(consPower);
			if (BuildConfig.DEBUG)
				Log.d(DEBUG_LOG_TAG,
						"Building up avg. consumption: i=" +
								avgConsIndex +
								" Array = " + avgConsumption.size());
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

		if (consPower <= 0.0d) {
			notifColor = context.getResources()
					.getColor(android.R.color.holo_green_light);
			// Instance of notification manager to cancel the existing notification */
			NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (newAvgConsumption < -300.0d) {
				/* Uri of selected alarm */
				String selUri = mPrefs.getString(MyHomeControl.prefsSolarWarning,"");
				if (!selUri.equalsIgnoreCase("")) {
					@SuppressLint("InlinedApi") NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
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
							.setColor(notifColor)
							.setSmallIcon(notifIcon);

					nMgr.notify(2, builder.build());
				}
			} else {
				if (nMgr != null) {
					nMgr.cancel(2);
				}
			}
		} else {
			notifColor = context.getResources()
					.getColor(android.R.color.holo_red_light);
		}
		String notifText = String.format("%.0f", Math.abs(consPower)) + "W";
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

			NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
			notificationBuilder.setOngoing(true)
					.setSmallIcon(notifIcon)
					.setContentText(notifText)
					.setTicker(notifText)
					.setColor(notifColor)
					.build();

			manager.notify(2, notificationBuilder.build());
//			chan1.notify();
		} else {

			/* Pointer to notification builder for export/import arrow */
			@SuppressLint("InlinedApi") NotificationCompat.Builder builder1 = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
					.setContentTitle(context.getString(R.string.app_name))
					.setContentIntent(PendingIntent.getActivity(context,
							0,
							new Intent(context,MyHomeControl.class),
							0))
					.setAutoCancel(false)
					.setVisibility(Notification.VISIBILITY_PUBLIC)
					.setSound(null)
					.setWhen(System.currentTimeMillis());
			/* Pointer to notification manager for export/import arrow */
			NotificationManager notificationManager1 = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

			builder1.setSmallIcon(notifIcon);
			builder1.setContentText(notifText);
			builder1.setTicker(String.format("%.0f", Math.abs(consPower)) + "W");
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				builder1.setColor(notifColor);
			}

			builder1.setPriority(NotificationCompat.PRIORITY_DEFAULT);

			/* Pointer to notification for export/import arrow */
			Notification notification1 = builder1.build();
			if (notificationManager1 != null) {
				notificationManager1.notify(1, notification1);
			}
		}
		// Update solar panel widgets if any
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update Solar Widget");
		/* App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/* Component name of this widget */
		ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
				SolarPanelWidget.class.getName());
		/* List of all active widgets */
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

		for (int appWidgetId : appWidgetIds) {
			SolarPanelWidget.updateAppWidget(context,appWidgetManager,appWidgetId, solarPower, consPower);
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
	private static void airconWidgetUpdate(boolean timerOn, int timerTime, String stopTime, Context context) {
		// Update aircon panel widgets if any
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update Aircon Widget");
		/* App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/* Component name of this widget */
		ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
				AirconWidget.class.getName());
		/* List of all active widgets */
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

		for (int appWidgetId : appWidgetIds) {
			AirconWidget.updateAppWidget(context, appWidgetManager,
					appWidgetId, timerTime, stopTime, timerOn);
		}
	}

	/**
	 * Update device status widgets
	 *
	 * @param context
	 *            Application context
	 * @param updateType
	 *            True - Full update (devices and connection)
	 *            False - Only connection update
	 */
	public static void deviceStatusWidgetUpdate(Context context, boolean updateType) {
		/* App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/* Component name of this widget */
		ComponentName thisAppWidget;

		thisAppWidget = new ComponentName(context.getPackageName(),
				DeviceStatusWidget.class.getName());
		/* List of all active widgets */
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

		for (int appWidgetId : appWidgetIds) {
			DeviceStatusWidget.updateAppWidget(context, appWidgetManager, appWidgetId, updateType);
		}
	}

	/**
	 * AsyncTask requestNewConnection
	 * Enables requested WiFi AP and requests reconnect
	 */
	@SuppressLint("StaticFieldLeak")
	private class requestNewConnAsync extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String reqSSID = params[0];
			List<WifiConfiguration> wifiNetworks = wifiMgr.getConfiguredNetworks();
			if (wifiNetworks != null) {
				for (int index = 0; index < wifiNetworks.size(); index++) {
					if (wifiNetworks.get(index).SSID.equalsIgnoreCase(reqSSID)) {
						wifiMgr.enableNetwork(wifiNetworks.get(index).networkId, true);
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG_SW, "Enabled: " + wifiNetworks.get(index).SSID);
						break;
					}
					wifiMgr.updateNetwork(wifiNetworks.get(index));
				}
				wifiMgr.reconnect();
				toastMsg = "MHC - Switching to " + reqSSID;
			} else {
				toastMsg = "MHC - Failed to request WiFi AP switch";
			}
			Handler handler = new Handler(Looper.getMainLooper());
			handler.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(),
							toastMsg,
							Toast.LENGTH_SHORT).show();
				}
			});
			return null;
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
		/* String for notification */
		String notifText;
		/* Icon for notification */
		int notifIcon;
		/* Background color for notification icon in SDK Lollipop and newer */
		int notifColor;

		// Prepare notification for foreground service
		notifIcon = R.drawable.ic_no_detection;
		notifText = context.getResources().getString(R.string.msg_listener);
		notifColor = context.getResources()
				.getColor(android.R.color.holo_green_light);

		/* Pointer to notification builder for export/import arrow */
		NotificationCompat.Builder myNotifBuilder;
		myNotifBuilder = new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
				.setContentTitle(context.getString(R.string.app_name))
				.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MyHomeControl.class), 0))
				.setAutoCancel(false)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setWhen(System.currentTimeMillis());

		myNotifBuilder.setSmallIcon(notifIcon)
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

	@RequiresApi(Build.VERSION_CODES.O)
	private void startMyOwnForeground(Context context){
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		String notifText = context.getResources().getString(R.string.msg_listener);
		chan1 = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_NONE);
		chan1.setLightColor(Color.BLUE);
		chan1.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
		manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager != null;
		manager.createNotificationChannel(chan1);

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
		Notification notification = notificationBuilder.setOngoing(true)
				.setSmallIcon(R.drawable.ic_no_detection)
				.setContentTitle(context.getString(R.string.app_name))
				.setContentText(notifText)
				.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MyHomeControl.class), 0))
				.setPriority(NotificationManager.IMPORTANCE_NONE)
				.setCategory(Notification.CATEGORY_SERVICE)
				.setStyle(new NotificationCompat.BigTextStyle().bigText(notifText))
				.setTicker(notifText)
				.setAutoCancel(false)
				.setOnlyAlertOnce(true)
				.build();
		startForeground(2, notification);
	}}
