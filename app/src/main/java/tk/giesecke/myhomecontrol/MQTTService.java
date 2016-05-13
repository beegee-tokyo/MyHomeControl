package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
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

import java.util.Arrays;

public class MQTTService extends Service {

	private static final String DEBUG_LOG_TAG = "MQTTService";
	private static boolean hasWifi = false;
	private static boolean hasMmobile = false;
//	private Thread thread;
	private ConnectivityManager mConnMan;
	public static volatile IMqttAsyncClient mqttClient;
	public static String deviceId;

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

//	public class MQTTBinder extends Binder {
//		public MQTTService getService(){
//			return MQTTService.this;
//		}
//	}

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
		options.setUserName("remote_device");
		options.setPassword("750ce5e999e9c78b".toCharArray());
		try {
			mqttClient = new MqttAsyncClient("tcp://broker.shiftr.io:1883", deviceId, new MemoryPersistence());
			token = mqttClient.connect(options);
			token.waitForCompletion(3500);
			mqttClient.setCallback(new MqttEventCallback());
			token = mqttClient.subscribe("SPM", 0);
			token.waitForCompletion(5000);
			token = mqttClient.subscribe("AC1", 0);
			token.waitForCompletion(5000);
			token = mqttClient.subscribe("WEA", 0);
			token.waitForCompletion(5000);
			token = mqttClient.subscribe("SEC", 0);
			token.waitForCompletion(5000);
			token = mqttClient.subscribe("TEST", 0);
			token.waitForCompletion(5000);
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
//					Intent launchA = new Intent(MQTTService.this, FullscreenActivityTest.class);
//					launchA.putExtra("message", msg.getPayload());
//					//TODO write somethinkg that has some sense
//					if(Build.VERSION.SDK_INT >= 11){
//						launchA.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_NO_ANIMATION);
//					} /*else {
//        		        launchA.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        		    }*/
//					startActivity(launchA);

//					byte payLoad[] = msg.getPayload();
//					String receivedMessage = Utilities.cryptMessage(payLoad, topic);

					String payLoad = new String(msg.getPayload());
					String receivedMessage = new String(msg.getPayload());

//					Toast.makeText(getApplicationContext(), "MQTT Message:\n" + topic + ": " + receivedMessage, Toast.LENGTH_SHORT).show();
					Log.d(DEBUG_LOG_TAG, "Payload is " + receivedMessage);
				}
			});
		}
	}

//	public String getThread(){
//		return Long.valueOf(thread.getId()).toString();
//	}

	@Override
	public IBinder onBind(Intent intent) {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "onBind called");
		return null;
	}
}