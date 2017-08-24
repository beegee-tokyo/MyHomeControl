package tk.giesecke.myhomecontrol;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;

import tk.giesecke.myhomecontrol.devices.DeviceStatus;

import static tk.giesecke.myhomecontrol.MessageListener.am;
import static tk.giesecke.myhomecontrol.MessageListener.connStatusChanged;
import static tk.giesecke.myhomecontrol.MessageListener.devStatpi;
import static tk.giesecke.myhomecontrol.MessageListener.mqttClient;
import static tk.giesecke.myhomecontrol.MessageListener.serverSocket;
import static tk.giesecke.myhomecontrol.MessageListener.shouldRestartSocketListen;
import static tk.giesecke.myhomecontrol.MessageListener.shouldRestartMQTTListen;
import static tk.giesecke.myhomecontrol.MessageListener.socket;

public class Events extends BroadcastReceiver {

	private static final String DEBUG_LOG_TAG = "MHC-EVE";

	static boolean eventReceiverRegistered = false;

	public Events() {
		eventReceiverRegistered = true;
	}

	@Override
	public void onReceive(final Context context, Intent intent) {
		// Screen on/off
		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)
				|| intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Screen on Event");
			}
			if (intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Connection Change Event");
			}
			final boolean bHasConnection[] = Utilities.connectionAvailable(context);
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "connStatusChanged = " + connStatusChanged);
			if (connStatusChanged) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Connection Changed");
				if (bHasConnection[2]) { // Home WiFi connection available?
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Home WiFi connection");
					// Start/Restart service to listen to UDP/MQTT broadcast messages
					shouldRestartSocketListen = true;
					shouldRestartMQTTListen = true;
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart Device Status Updates");
					/** Pending intent to update device status widget every 5 minutes */
					devStatpi = PendingIntent.getService(context, 5003,
							new Intent(context, DeviceStatus.class), PendingIntent.FLAG_UPDATE_CURRENT);
					/** Alarm manager to update device status widget every 5 minutes */
					am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
					am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 180000,
							180000, devStatpi);

				} else if (bHasConnection[1] || bHasConnection[0]) { // Mobile or WiFi connection available?
					if (bHasConnection[1]) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Mobile connection");
					}
					if (bHasConnection[0]) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "WiFi connection");
					}
					// Start/Restart service to listen to MQTT broadcast messages
					shouldRestartSocketListen = false;
					shouldRestartMQTTListen = true;
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
					// Stop Device Status Update
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop Device Status Updates");
					if (devStatpi != null && am != null) {
						am.cancel(devStatpi);
					}
				} else { // No connection available
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop Message listener because no connection");
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
					// Stop Device Status Update
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop Device Status Updates");
					if (devStatpi != null && am != null) {
						am.cancel(devStatpi);
					}
				}
				if (shouldRestartSocketListen || shouldRestartMQTTListen) { // Restart service only if connection available
					if (serviceIsRunning(MessageListener.class, context)) {
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG, "UDP/TCP/MQTT listener still running");
						// Sockets are closed or MQTT connection is closed ???
						if (socket == null || serverSocket == null || mqttClient == null) {
							Log.d(DEBUG_LOG_TAG, "Stopping UDP/TCP/MQTT listener because sockets were closed");
							// Stop service (it will restart by itself)
							context.stopService(new Intent(context,MessageListener.class));
						}
					} else {
						// Restart service with a delay of 5 seconds to let connection settle down
						final Handler handler = new Handler();
						handler.postDelayed(new Runnable() {
							@Override
							public void run() {
								if (BuildConfig.DEBUG)
									Log.d(DEBUG_LOG_TAG, "Start/Restart UDP/TCP/MQTT listener");
								context.startService(new Intent(context, MessageListener.class));
							}
						}, 5000);
					}
				}
			}
			return;
		}

		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop Device Status Updates");
			if (devStatpi != null && am != null) {
				am.cancel(devStatpi);
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
