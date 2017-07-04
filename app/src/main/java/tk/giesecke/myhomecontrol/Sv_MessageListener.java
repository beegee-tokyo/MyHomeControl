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
import android.os.Looper;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
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
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import tk.giesecke.myhomecontrol.aircon.Wi_Aircon;
import tk.giesecke.myhomecontrol.lights.Wi_ByLightCtrl;
import tk.giesecke.myhomecontrol.lights.Wi_LightCtrl;
import tk.giesecke.myhomecontrol.security.Wi_SecAlarm;
import tk.giesecke.myhomecontrol.solar.Wi_SolarPanel;

public class Sv_MessageListener extends Service {

	/** Tag for debug messages */
	private static final String DEBUG_LOG_TAG = "MHC-LISTENER";

	/** TCP client port to send commands */
	public static final int TCP_CLIENT_PORT = 9998; // was 6000
	/** UDP server port where we receive the UDP broadcasts */
	private static final int UDP_SERVER_PORT = 9997; // was 5000
	/** TCP server port where we receive the TCP debug messages */
	private static final int TCP_SERVER_PORT = 9999;
	/** Action for broadcast message to main activity */
	public static final String BROADCAST_RECEIVED = "BC_RECEIVED";

	/** Flag if UDP/TCP listener is restarted after a broadcast was received */
	static Boolean shouldRestartSocketListen=true;
	/** Socket for broadcast datagram */
	static DatagramSocket socket;
	/** Socket for TCP messages */
	static ServerSocket serverSocket;

	/** Multicast wifiWakeLock to keep WiFi awake until broadcast is received */
	private WifiManager.MulticastLock wifiWakeLock = null;

	/** MQTT client */
	public static volatile IMqttAsyncClient mqttClient = null;
	/** Flag if MQTT listener is restarted after a broadcast was received */
	static Boolean shouldRestartMQTTListen=true;
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

	/** Last message from SPM */
	private String lastSPM = "";
	/** Last message from AC1 */
	private String lastAC1= "";
	/** Last message from AC2 */
	private String lastAC2 = "";
	/** Last message from SF1 */
	private String lastSF1 = "";
	/** Last message from SB1 */
	private String lastSB1 = "";
	/** Last message from LB1 */
	private String lastLB1 = "";
	/** Last message from LY1 */
	private String lastLY1 = "";
	/** Last message from WEO */
	private String lastWEO = "";
	/** Last message from WEI */
	private String lastWEI = "";

	/** Broker status structure */
	public static int clientsConn = 0; // $SYS/broker/clients/connected
	public static Double bytesLoadRcvd = 0.0; // $SYS/broker/load/bytes/received/15min
	public static Double bytesLoadSend = 0.0; // $SYS/broker/load/bytes/sent/15min
	public static Double bytesMsgsRcvd = 0.0; // $SYS/broker/load/publish/received/15min
	public static Double bytesMsgsSend = 0.0; // $SYS/broker/load/publish/sent/15min
	public static final ArrayList<String> mqttClientList = new ArrayList<>();

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// Enable access to internet
		/** ThreadPolicy to get permission to access internet */
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Get the MulticastLock to be able to receive multicast UDP messages
		/** Wifi manager to check wifi status */
		WifiManager wifi = (WifiManager) getApplicationContext().getSystemService( Context.WIFI_SERVICE );
		if(wifi != null){
			if (wifiWakeLock != null) { // In case we restart after receiver problem
				wifiWakeLock = wifi.createMulticastLock("Cl_MyHomeControl");
				wifiWakeLock.acquire();
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// Start in foreground (to avoid being killed)
		startForeground(1, ServiceNotification(this));

		boolean hasConnection[] = tk.giesecke.myhomecontrol.Cl_Utilities.connectionAvailable(this);
		if (hasConnection[0] && tk.giesecke.myhomecontrol.Cl_Utilities.isHomeWiFi(this)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start UDP listener");
			shouldRestartSocketListen = true;
			try {
				// Start listener for UDP broadcast messages
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
		} else if (hasConnection[1] || hasConnection[0]) { // Mobile or WiFi connection available?
			// Connect to MQTT broker
			if ((mqttClient == null) || (!mqttClient.isConnected())) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start MQTT listener");
				if (!mqttIsConnecting) { // Check if already trying to connect?
					new doConnect().execute();
				}
			}
			shouldRestartMQTTListen = true;
		} else {
			shouldRestartSocketListen = false;
			shouldRestartMQTTListen = false;
		}

		/** IntentFilter to receive screen on/off & connectivity broadcast msgs */
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
		/** Receiver for screen on/off & connectivity broadcast msgs */
		BroadcastReceiver mReceiver = new BR_Events();
		registerReceiver(mReceiver, filter);

		IntentFilter intentf = new IntentFilter();
		intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(connChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		//this service should run until we stop it
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
		if (socket != null) {
			socket.disconnect();
			socket.close();
		}
		if (mqttClient != null) {
			try {
				mqttClient.disconnect();
			} catch (MqttException ignore) {
			}
		}
		// Restart service with a delay of 5 seconds
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start/Restart UDP/TCP/MQTT listener");
				getApplicationContext().startService(new Intent(getApplicationContext(), Sv_MessageListener.class));
			}
		}, 5000);
	}

	private BroadcastReceiver connChangeReceiver = new BroadcastReceiver() {
		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(final Context context, Intent intent) {

			// Connectivity has changed
			if (intent.getAction().equals (android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Connection Change (Sv_MessageListener)");

				boolean bHasConnection[] = tk.giesecke.myhomecontrol.Cl_Utilities.connectionAvailable(context);
				if (bHasConnection[0] && tk.giesecke.myhomecontrol.Cl_Utilities.isHomeWiFi(context)) { // WiFi connection available?
					// Start/Restart service to listen to UDP/MQTT broadcast messages
					shouldRestartSocketListen = true;
					shouldRestartMQTTListen = true;
					// Reconnect listeners
					// MQTT
					if (mqttClient == null || !mqttClient.isConnected()) {
						if (!mqttIsConnecting) { // Check if already trying to connect?
							new doConnect().execute();
						}
					}
					// UDP
					if (socket != null) { // Socket still open?
						socket.disconnect();
						socket.close();
						socket = null;
					}
					startListenForUDPBroadcast();
					// TCP
					if (serverSocket != null) { // Socket still open?
						try {
							serverSocket.close();
							serverSocket = null;
						} catch (IOException ignore) {
						}
					}
					startListenForTCPMessage();
				} else if (bHasConnection[1] || bHasConnection[0]) { // Mobile or WiFi connection available?
					// Start/Restart service to listen to UDP/MQTT broadcast messages
					shouldRestartSocketListen = false;
					shouldRestartMQTTListen = true;
					if (mqttClient == null || !mqttClient.isConnected()) {
						if (!mqttIsConnecting) { // Check if already trying to connect?
							new doConnect().execute();
						}
					}
				} else { // No connection available
					shouldRestartSocketListen = false;
					shouldRestartMQTTListen = false;
					if (socket != null) { // Socket still open?
						socket.disconnect();
						socket.close();
						socket = null;
					}
					if (serverSocket != null) { // Socket still open?
						try {
							serverSocket.close();
							serverSocket = null;
						} catch (IOException ignore) {
						}
					}
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop Message listener because no connection");
				}
			} else if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				if (mqttClient == null || !mqttClient.isConnected()) {
					if (!mqttIsConnecting) { // Check if already trying to connect?
						new doConnect().execute();
					}
					Toast.makeText(getApplicationContext(),
							"MQTT connection was lost!",
							Toast.LENGTH_LONG).show();
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT connection was lost!");
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
					InetAddress broadcastIP = InetAddress.getByName(getString(R.string.BROADCAST_IP)); //172.16.238.42 //192.168.1.255
					/** Port from where we expect the UDP broadcasts */
					Integer port = UDP_SERVER_PORT;
					boolean runForEver = true;
					while (runForEver) {
						if (shouldRestartSocketListen) { // Should we listen to UDP broadcasts???
							listenUDPBroadCast(broadcastIP, port);
							// Check if MQTT is still connected
							if (mqttClient != null && !mqttClient.isConnected() && !mqttIsConnecting) {
								if (!mqttIsConnecting) { // Check if already trying to connect?
									if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Reconnect to MQTT");
									// Reconnect to MQTT
									final Handler handler = new Handler();
									handler.post(new Runnable() {
										@Override
										public void run() {
											if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Reconnect MQTT listener");
											mqttIsConnecting = true;
											new doConnect().execute();
										}
									});
								}
							}
							// Check if UI started
							if (uiStarted) {
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Send last status to UI");
								uiStarted = false;
								new sendLastMsgs().execute();
							}
						} else {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "UDP shouldRestartSocketListen = false");
							runForEver = false;
						}
					}
					if (!shouldRestartSocketListen) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop UDP listener");
						if (socket != null) {
							socket.disconnect();
							socket.close();
							socket = null;
						}
						if (serverSocket != null) {
							try {
								serverSocket.close();
								serverSocket = null;
							} catch (IOException ignore) {
							}
						}
					}
				} catch (Exception e) {
					if (shouldRestartSocketListen) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart UDP listener after error " + e.getMessage());
						startListenForUDPBroadcast();
					} else {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop UDP listener");
						if (socket != null) {
							socket.disconnect();
							socket.close();
							socket = null;
						}
						if (serverSocket != null) {
							try {
								serverSocket.close();
								serverSocket = null;
							} catch (IOException ignore) {
							}
						}
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
		/** Byte buffer for incoming data */
		byte[] recvBuf = new byte[1000];
		if (socket == null || socket.isClosed()) {
			try {
				socket = new DatagramSocket(port, broadcastIP);
			} catch (SocketException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Cannot open socket " + e.getMessage());
				shouldRestartSocketListen = false;
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

		// Check MQTT connection
		if (mqttClient == null || !mqttClient.isConnected()) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "UDP listener detected MQTT disconnect");
			if (!mqttIsConnecting) { // Check if already trying to connect?
				// Reconnect to MQTT
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Reconnect MQTT listener");
				mqttIsConnecting = true;
				new doConnect().execute();
			}
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Got UDP broadcast from " + senderIP + ", message: " + message);

		// Check if response is a JSON array
		if (tk.giesecke.myhomecontrol.Cl_Utilities.isJSONValid(message)) {
			handleMsgs(message);
			// Send broadcast to listening activities
			sendMyBroadcast(message, "UDP");

			SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(tk.giesecke.myhomecontrol.Cl_MyHomeControl.sharedPrefName,0);
			if (mPrefs.getBoolean(tk.giesecke.myhomecontrol.Cl_MyHomeControl.prefsShowDebug, false)) {
				toastMsg = message;
				Handler handler = new Handler(Looper.getMainLooper());
				handler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								"Received UDP data: " + toastMsg,
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}

	/**
	 * Start listener for TCP messages
	 */
	private static String toastMsg;

	private void startListenForTCPMessage() {
		Thread TCPMessageThread = new Thread(new Runnable() {
			@SuppressWarnings("InfiniteLoopStatement")
			public void run() {
				try {
					boolean runForEver = true;
					while (runForEver) {
						if (shouldRestartSocketListen) {
							if (serverSocket == null) {
								serverSocket = new ServerSocket();
								serverSocket.setReuseAddress(true);
								serverSocket.bind(new InetSocketAddress(TCP_SERVER_PORT));
							}
							// LISTEN FOR INCOMING CLIENTS
							Socket client = serverSocket.accept();
							try {
								BufferedReader in = new BufferedReader(
										new InputStreamReader(client.getInputStream()));
								String inMsg;
								while ((inMsg = in.readLine()) != null) {
									// Send broadcast to listening activities
									sendMyBroadcast(inMsg, "DEBUG");
//									// Check MQTT connection
//									if (mqttClient == null || !mqttClient.isConnected()) {
//										if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP listener detected MQTT disconnect");
//										// Reconnect to MQTT
//										if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Reconnect MQTT listener");
//										new doConnect().execute();
//									}
									if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received TCP data from: "
											+ client.getInetAddress().toString().substring(1) + " :" + inMsg);
									SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(tk.giesecke.myhomecontrol.Cl_MyHomeControl.sharedPrefName,0);
									if (mPrefs.getBoolean(tk.giesecke.myhomecontrol.Cl_MyHomeControl.prefsShowDebug, false)) {
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
							if (serverSocket != null) {
								try {
									serverSocket.close();
								} catch (IOException ignore) {
								}
								serverSocket = null;
								runForEver = false;
							}
						}
					}
				} catch (Exception e) {
					if (serverSocket != null) {
						try {
							serverSocket.close();
						} catch (IOException | NullPointerException ignore) {
						}
						serverSocket = null;
					}
					if (shouldRestartSocketListen) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart TCP listener after error " + e.getMessage());
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
				String deviceId = tk.giesecke.myhomecontrol.Cl_Utilities.getDeviceName();
				String subscriberID = "/DEV/" + deviceId.toUpperCase();
				String mqttIP = getResources().getString(R.string.MQTT_IP);
				SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(tk.giesecke.myhomecontrol.Cl_MyHomeControl.sharedPrefName,0);
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
				try {
					lastWill = "Dead".getBytes("UTF-8");
					options.setWill(subscriberID, lastWill, 2, true);
				} catch (UnsupportedEncodingException ignore) {
				}

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
									stopSelf();
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
				if (!mqttClientList.contains(tk.giesecke.myhomecontrol.Cl_Utilities.getDeviceName().toUpperCase())){
					mqttClientList.add(tk.giesecke.myhomecontrol.Cl_Utilities.getDeviceName().toUpperCase());
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
				mqttIsConnecting = false;
				IMqttToken token;
				SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(tk.giesecke.myhomecontrol.Cl_MyHomeControl.sharedPrefName,0);
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
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to /DEV/#");
					token = mqttClient.subscribe("/DEV/#", 0);
					token.waitForCompletion(10000);

					// Send one time topic to announce device as subscriber
					String deviceId = tk.giesecke.myhomecontrol.Cl_Utilities.getDeviceName();
					deviceId = "/DEV/" + deviceId.toUpperCase();
					byte[] encodedPayload;
					encodedPayload = mqttUser.getBytes("UTF-8");
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
									stopSelf();
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
				} catch (UnsupportedEncodingException ignore) {
				}
			} else { // retry in 30 seconds
				boolean bHasConnection[] = tk.giesecke.myhomecontrol.Cl_Utilities.connectionAvailable(getApplicationContext());
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
			if (mqttClientList.contains(tk.giesecke.myhomecontrol.Cl_Utilities.getDeviceName().toUpperCase())){
				mqttClientList.remove(tk.giesecke.myhomecontrol.Cl_Utilities.getDeviceName().toUpperCase());
			}
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken arg0) {

		}

		@Override
		@SuppressLint("NewApi")
		public void messageArrived(final String topic, final MqttMessage msg) throws Exception {
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
						if (topic.contains("load/bytes/received/1min")) {
							bytesLoadRcvd = Double.parseDouble(receivedMessage);
						} else if (topic.contains("load/bytes/sent/1min")) {
							bytesLoadSend = Double.parseDouble(receivedMessage);
						} else if (topic.contains("messages/received/1min")) {
							bytesMsgsRcvd = Double.parseDouble(receivedMessage);
						} else if (topic.contains("messages/sent/1min")) {
							bytesMsgsSend = Double.parseDouble(receivedMessage);
						} else if (topic.contains("clients/connected")) {
							clientsConn = Integer.parseInt(receivedMessage);
						}
						sendMyBroadcast("BrokerStatus", "BROKER");
						return;
					}
					// Check if topic is a device registration
					String deviceId = tk.giesecke.myhomecontrol.Cl_Utilities.getDeviceName();
					String subscriberID = "/DEV/" + deviceId.toUpperCase();
					if (topic.startsWith("/DEV/")) {
						if (!topic.equalsIgnoreCase(subscriberID)) { // check if it is our own registration
							if (!receivedMessage.equalsIgnoreCase("Dead")) { // Client is disconnected
								SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(tk.giesecke.myhomecontrol.Cl_MyHomeControl.sharedPrefName,0);
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
							if (mqttClientList.contains(topic.substring(5).toUpperCase())){
								mqttClientList.remove(topic.substring(5).toUpperCase());
							}
						} else { // Client is connected
							if (!mqttClientList.contains(topic.substring(5).toUpperCase())){
								mqttClientList.add(topic.substring(5).toUpperCase());
							}
						}
						sendMyBroadcast("BrokerStatus", "BROKER");
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
					boolean notOnHomeWifi = !tk.giesecke.myhomecontrol.Cl_Utilities.isHomeWiFi(getApplicationContext());

					// If we are not on home Wifi or screen is off or locked => process the message
					if (notOnHomeWifi || phoneIsLocked || screenIsOff || uiStarted) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Not home or phone is locked or screen is off");

						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Payload is " + receivedMessage);

						if (receivedMessage.length() == 0) { // Empty message
							return;
						}

						// Update widgets
						handleMsgs(receivedMessage);

//						// Forward to all local listeners
						sendMyBroadcast(receivedMessage, "MQTT");

						// Show toast on screen if debug is enabled
						SharedPreferences mPrefs = getApplicationContext().getSharedPreferences(tk.giesecke.myhomecontrol.Cl_MyHomeControl.sharedPrefName,0);
						if (mPrefs.getBoolean(tk.giesecke.myhomecontrol.Cl_MyHomeControl.prefsShowDebug, false)) {
							toastMsg = receivedMessage;
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
				}
			});
		}
	}

	public static void subscribeBrokerStatus() {
		if (mqttClient != null) {
			// Clear old status
			clientsConn = 0; // $SYS/broker/clients/connected
			bytesLoadRcvd = 0.0; // $SYS/broker/load/bytes/received/15min
			bytesLoadSend = 0.0; // $SYS/broker/load/bytes/sent/15min
			bytesMsgsRcvd = 0.0; // $SYS/broker/load/publish/received/15min
			bytesMsgsSend = 0.0; // $SYS/broker/load/publish/sent/15min
			mqttClientList.clear();

			IMqttToken token;
			try {
				token = mqttClient.unsubscribe("/DEV/#");
				token.waitForCompletion(10000);
				token = mqttClient.subscribe("$SYS/broker/load/bytes/received/1min", 0);
				token.waitForCompletion(10000);
				token = mqttClient.subscribe("$SYS/broker/load/bytes/sent/1min", 0);
				token.waitForCompletion(10000);
				token = mqttClient.subscribe("$SYS/broker/load/messages/received/1min", 0);
				token.waitForCompletion(10000);
				token = mqttClient.subscribe("$SYS/broker/load/messages/sent/1min", 0);
				token.waitForCompletion(10000);
				token = mqttClient.subscribe("$SYS/broker/clients/connected", 0);
				token.waitForCompletion(10000);
				token = mqttClient.subscribe("/DEV/#", 0);
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

	public static void unSubscribeBrokerStatus() {
		if (mqttClient != null) {
			IMqttToken token;
			try {
//				token = mqttClient.unsubscribe("");
//				token.waitForCompletion(10000);

				token = mqttClient.unsubscribe("$SYS/broker/clients/connected");
				token.waitForCompletion(10000);
				token = mqttClient.unsubscribe("$SYS/broker/load/bytes/received/1min");
				token.waitForCompletion(10000);
				token = mqttClient.unsubscribe("$SYS/broker/load/bytes/sent/1min");
				token.waitForCompletion(10000);
				token = mqttClient.unsubscribe("$SYS/broker/load/messages/received/1min");
				token.waitForCompletion(10000);
				token = mqttClient.unsubscribe("$SYS/broker/load/messages/sent/1min");
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

	/**
	 * Send received message to all listing threads
	 *
	 * @param msgReceived
	 *            Flag if alarm is active
	 * @param fromSender
	 *            Flag if alarm is on
	 */
	private void sendMyBroadcast(String msgReceived, String fromSender) {
		/** Intent for activity internal broadcast message */
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
		/** Json object for received data */
		JSONObject jsonResult;
		try {
			jsonResult = new JSONObject(message);
			/** Device ID from UDP broadcast message */
			String broadCastDevice = jsonResult.getString("de");
			switch (broadCastDevice) {
				case "spm":
					lastSPM = message;
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update solar panel widgets");

					/** Value of solar production */
					float solarPower = jsonResult.getInt("s");
					/** Value of house consumption */
					float consPower = jsonResult.getInt("c");

					// Activate/deactivate alarm sound and update widget
					solarAlarmAndWidgetUpdate(solarPower, consPower, context);
					break;
				case "fd1":
					lastAC1 = message;
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update office aircon widgets");

					/** Flag for alarm switched on or off */
					boolean timerOn = (jsonResult.getInt("ti") == 1);
					/** Timer time */
					int timerTime = jsonResult.getInt("ot");
					/** Timer end time */
					String timerEnd = jsonResult.getString("ts");

					// Update widget
					airconWidgetUpdate(timerOn, timerTime, timerEnd, context);
					break;
				case "ca1":
					lastAC2 = message;
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update office aircon widgets");
					// TODO add aircon 2 widget */
					break;
				case "sf1":
					lastSF1 = message;
					// Broadcast from front security
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update security widgets");									/** Flag for alarm on or off */

					boolean alarmIsActive = (jsonResult.getInt("ao") == 1);
					boolean alarmIsOn = (jsonResult.getInt("al") == 1);

					// Activate/deactivate alarm sound and update widget
					securityAlarmAndWidgetUpdate(alarmIsActive, alarmIsOn, broadCastDevice, context);
					break;
				case "sb1":
					lastSB1 = message;
					// Broadcast from front security
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update security widgets");									/** Flag for alarm on or off */

					alarmIsActive = (jsonResult.getInt("ao") == 1);
					alarmIsOn = (jsonResult.getInt("al") == 1);

					// Activate/deactivate alarm sound and update widget
					securityAlarmAndWidgetUpdate(alarmIsActive, alarmIsOn, broadCastDevice, context);
					break;
				case "lb1":
					lastLB1 = message;
					// Broadcast from bedroom lights
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update light control widgets");									/** Flag for alarm on or off */

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
					// Broadcast from bedroom lights
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update light control widgets");									/** Flag for alarm on or off */

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
			}
		} catch (JSONException e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Create JSONObject from String <"+ message +"> failed " + e.getMessage());
		}
	}

	/**
	 * Connect to MQTT broker and subscribe to topics
	 */
	private class sendLastMsgs extends AsyncTask<String, Void, Void> {

		protected Void doInBackground(String... params) {
			sendMyBroadcast(lastSPM, "LAST");
			handleMsgs(lastSPM);
			sendMyBroadcast(lastAC1, "LAST");
			handleMsgs(lastAC1);
			sendMyBroadcast(lastAC2, "LAST");
			sendMyBroadcast(lastSF1, "LAST");
			handleMsgs(lastSF1);
			sendMyBroadcast(lastSB1, "LAST");
			handleMsgs(lastSB1);
			sendMyBroadcast(lastLB1, "LAST");
			handleMsgs(lastLB1);
			sendMyBroadcast(lastLY1, "LAST");
			handleMsgs(lastLY1);
			sendMyBroadcast(lastWEO, "LAST");
			sendMyBroadcast(lastWEI, "LAST");
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
			/** String for notification */
			String notifText;
			/** Icon for notification */
			int notifIcon;
			/** Background color for notification icon in SDK Lollipop and newer */
			int notifColor;

			notifIcon = R.drawable.ic_detection;
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
						.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, Cl_MyHomeControl.class), 0))
						.setAutoCancel(false)
						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
						.setVisibility(Notification.VISIBILITY_PUBLIC)
						.setWhen(System.currentTimeMillis());
			} else {
				myNotifBuilder = new NotificationCompat.Builder(context)
						.setContentTitle(context.getString(R.string.app_name))
						.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, Cl_MyHomeControl.class), 0))
						.setAutoCancel(false)
						.setPriority(NotificationCompat.PRIORITY_DEFAULT)
						.setWhen(System.currentTimeMillis());
			}

			/** Pointer to notification manager for export/import arrow */
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

			/** Access to shared preferences of app widget */
			String selUri = context.getSharedPreferences(tk.giesecke.myhomecontrol.Cl_MyHomeControl.sharedPrefName, 0)
					.getString(tk.giesecke.myhomecontrol.Cl_MyHomeControl.prefsSecurityAlarm, "");/** Uri of selected alarm */
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
					Wi_SecAlarm.class.getName());
			/** List of all active widgets */
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

			for (int appWidgetId : appWidgetIds) {
				Wi_SecAlarm.updateAppWidget(context,appWidgetManager,appWidgetId, alarmIsActive);
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

		/** App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/** Component name of this widget */
		ComponentName thisAppWidget;
		if (device.equalsIgnoreCase("lb1")) {
			thisAppWidget = new ComponentName(context.getPackageName(),
					Wi_LightCtrl.class.getName());
			/** List of all active widgets */
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

			for (int appWidgetId : appWidgetIds) {
				Wi_LightCtrl.updateAppWidget(context, appWidgetManager, appWidgetId, lightStatus);
			}
		} else if (device.equalsIgnoreCase("ly1")) {
			thisAppWidget = new ComponentName(context.getPackageName(),
					Wi_ByLightCtrl.class.getName());
			/** List of all active widgets */
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

			for (int appWidgetId : appWidgetIds) {
				Wi_ByLightCtrl.updateAppWidget(context, appWidgetManager, appWidgetId, lightStatus);
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
		SharedPreferences mPrefs = context.getSharedPreferences(tk.giesecke.myhomecontrol.Cl_MyHomeControl.sharedPrefName, 0);

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

		notifIcon = tk.giesecke.myhomecontrol.Cl_Utilities.getNotifIcon(consPower);

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
				String selUri = mPrefs.getString(tk.giesecke.myhomecontrol.Cl_MyHomeControl.prefsSolarWarning,"");
				if (!selUri.equalsIgnoreCase("")) {
					@SuppressLint("InlinedApi") NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
							.setContentTitle(context.getString(R.string.app_name))
							.setContentIntent(PendingIntent.getActivity(context, 0,
									new Intent(context, Cl_MyHomeControl.class), 0))
							.setContentText(context.getString(R.string.notif_export,
									String.format("%.0f", Math.abs(consPower)),
									tk.giesecke.myhomecontrol.Cl_Utilities.getCurrentTime()))
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
						new Intent(context,Cl_MyHomeControl.class),
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
				Wi_SolarPanel.class.getName());
		/** List of all active widgets */
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

		for (int appWidgetId : appWidgetIds) {
			Wi_SolarPanel.updateAppWidget(context,appWidgetManager,appWidgetId, solarPower, consPower);
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
		/** App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/** Component name of this widget */
		ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
				Wi_Aircon.class.getName());
		/** List of all active widgets */
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

		for (int appWidgetId : appWidgetIds) {
			Wi_Aircon.updateAppWidget(context, appWidgetManager,
					appWidgetId, timerTime, stopTime, timerOn);
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
		notifIcon = R.drawable.ic_no_detection;
		notifText = context.getResources().getString(R.string.msg_listener);
		//noinspection deprecation
		notifColor = context.getResources()
				.getColor(android.R.color.holo_green_light);

		/** Pointer to notification builder for export/import arrow */
		NotificationCompat.Builder myNotifBuilder;
		myNotifBuilder = new NotificationCompat.Builder(context)
				.setContentTitle(context.getString(R.string.app_name))
				.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, Cl_MyHomeControl.class), 0))
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
