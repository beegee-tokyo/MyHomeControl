package tk.giesecke.myhomecontrol.devices;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import tk.giesecke.myhomecontrol.BuildConfig;

import static tk.giesecke.myhomecontrol.devices.MessageListener.connStatus;
import static tk.giesecke.myhomecontrol.devices.MessageListener.mqttClient;
import static tk.giesecke.myhomecontrol.devices.MessageListener.tcpListenerActive;
import static tk.giesecke.myhomecontrol.devices.MessageListener.udpListenerActive;


public class DeviceStatus extends IntentService {

	/** Tag for debug messages */
	private static final String DEBUG_LOG_TAG = "MHC-DEVSTAT";

	public DeviceStatus() {
		super("DeviceStatus");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			final Context intentContext = getApplicationContext();
			// Update device status widget
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update device status widget");
			MessageListener.deviceStatusWidgetUpdate(intentContext, true);

			if (serviceIsRunning(MessageListener.class, intentContext)) {
				if (BuildConfig.DEBUG)
					Log.d(DEBUG_LOG_TAG, "MessageListener still running");
				// Sockets are closed or MQTT connection is closed ???
				if (connStatus == MessageListener.HOME_WIFI && (!udpListenerActive || !tcpListenerActive)) {
					Log.d(DEBUG_LOG_TAG, "Restart MessageListener because sockets were closed");
					// Restart MessageListener Service
					intentContext.startService(new Intent(intentContext, MessageListener.class));
				} else if (connStatus == MessageListener.WIFI_MOBILE && (mqttClient == null || !mqttClient.isConnected())) {
					Log.d(DEBUG_LOG_TAG, "Restart MessageListener because MQTT is disconnected");
					// Restart MessageListener Service
					intentContext.startService(new Intent(intentContext, MessageListener.class));
				}
			} else {
				// Restart MessageListener Service
				intentContext.startService(new Intent(intentContext, MessageListener.class));
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
		if (manager != null) {
			for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
				if (serviceClass.getName().equals(service.service.getClassName())) {
					return true;
				}
			}
		}
		return false;
	}
}
