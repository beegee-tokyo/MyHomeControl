package tk.giesecke.myhomecontrol;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;

import static tk.giesecke.myhomecontrol.Sv_MessageListener.mqttClient;
import static tk.giesecke.myhomecontrol.Sv_MessageListener.serverSocket;
import static tk.giesecke.myhomecontrol.Sv_MessageListener.shouldRestartSocketListen;
import static tk.giesecke.myhomecontrol.Sv_MessageListener.shouldRestartMQTTListen;
import static tk.giesecke.myhomecontrol.Sv_MessageListener.socket;

public class BR_Events extends BroadcastReceiver {

	private static final String DEBUG_LOG_TAG = "MHC-EVE";

	static boolean eventReceiverRegistered = false;

	public BR_Events() {
		eventReceiverRegistered = true;
	}

	@Override
	public void onReceive(final Context context, Intent intent) {
		// Screen on/off
		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)
				|| intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Screen on or Connection Change (BR_Events)");
			final boolean bHasConnection[] = tk.giesecke.myhomecontrol.Cl_Utilities.connectionAvailable(context);
			if (bHasConnection[0] && tk.giesecke.myhomecontrol.Cl_Utilities.isHomeWiFi(context)) { // Home WiFi connection available?
				// Start/Restart service to listen to UDP/MQTT broadcast messages
				shouldRestartSocketListen = true;
				shouldRestartMQTTListen = true;
			} else if (bHasConnection[1] || bHasConnection[0]) { // Mobile or WiFi connection available?
				// Start/Restart service to listen to MQTT broadcast messages
				shouldRestartSocketListen = false;
				shouldRestartMQTTListen = true;
			} else { // No connection available
				shouldRestartSocketListen = false;
				shouldRestartMQTTListen = false;
				// Disconnect all listener
				if (mqttClient != null) { // mqttClient initialized?
					try {
						mqttClient.disconnect();
					} catch (MqttException ignore) {
					}
				}
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
			if (shouldRestartSocketListen || shouldRestartMQTTListen) { // Restart service only if connection available
				if (serviceIsRunning(Sv_MessageListener.class, context)) {
					if (BuildConfig.DEBUG)
						Log.d(DEBUG_LOG_TAG, "UDP/TCP/MQTT listener still running");
				} else {
					// Start service with a delay of 5 seconds to let connection settle down
					final Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if (BuildConfig.DEBUG)
								Log.d(DEBUG_LOG_TAG, "Start/Restart UDP/TCP/MQTT listener");
							context.startService(new Intent(context, Sv_MessageListener.class));
						}
					}, 5000);
				}
			}
			return;
		}

		// CONNECTIVITY CHANGE
		if (intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Connection Change (BR_Events)");
			final boolean bHasConnection[] = tk.giesecke.myhomecontrol.Cl_Utilities.connectionAvailable(context);
			if (bHasConnection[0] && tk.giesecke.myhomecontrol.Cl_Utilities.isHomeWiFi(context)) { // WiFi connection available?
				// Start/Restart service to listen to UDP/MQTT broadcast messages
				shouldRestartSocketListen = true;
				shouldRestartMQTTListen = true;
				if (serviceIsRunning(Sv_MessageListener.class, context)) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "UDP/TCP/MQTT listener WiFi still running");
				} else {
					// Start service with a delay of 5 seconds to let connection settle down
					final Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start/Restart UDP/TCP/MQTT WiFi listener");
							context.startService(new Intent(context, Sv_MessageListener.class));
						}
					}, 5000);
				}
			} else if (bHasConnection[1] || bHasConnection[0]) { // Mobile or WiFi connection available?
				// Start/Restart service to listen to UDP/MQTT broadcast messages
				shouldRestartSocketListen = false;
				shouldRestartMQTTListen = true;
				if (serviceIsRunning(Sv_MessageListener.class, context)) {
					if (BuildConfig.DEBUG)
						Log.d(DEBUG_LOG_TAG, "UDP/TCP/MQTT listener Mobile still running");
				} else {
					// Start service with a delay of 5 seconds to let connection settle down
					final Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if (BuildConfig.DEBUG)
								Log.d(DEBUG_LOG_TAG, "Start/Restart UDP/TCP/MQTT Mobile listener");
							context.startService(new Intent(context, Sv_MessageListener.class));
						}
					}, 5000);
				}
			} else { // No connection available
				shouldRestartSocketListen = false;
				shouldRestartMQTTListen = false;
				if (mqttClient != null) { // mqttClient initialized?
					try {
						mqttClient.disconnect();
					} catch (MqttException ignore) {
					}
				}
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
		}
	}

	/**
	 * Check if a service is running
	 *
	 * @param serviceClass
	 *              Service class we want to check if it is running
	 * @return <code>boolean</code>
	 *              True if service is running
	 *              False if service is not running
	 */
	private static boolean serviceIsRunning(Class<?> serviceClass, Context context) {
		/** Activity manager for services */
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
