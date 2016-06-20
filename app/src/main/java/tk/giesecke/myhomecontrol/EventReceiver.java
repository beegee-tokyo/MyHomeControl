package tk.giesecke.myhomecontrol;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EventReceiver extends BroadcastReceiver {

	private static final String DEBUG_LOG_TAG = "MHC-EVE";

	public EventReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// Screen on/off
		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Screen on");
			// Start MQTT services if not running already
			if (isMyServiceNotRunning(MQTTService.class, context)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start MQTT listener");
				context.startService(new Intent(context, MQTTService.class));
			}
			// Start UDP services if not running already
			if (isMyServiceNotRunning(UDPlistener.class, context)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start UDP listener");
				context.startService(new Intent(context, UDPlistener.class));
			}
		}
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Screen on");
			// Start MQTT services if not running already
			if (isMyServiceNotRunning(MQTTService.class, context)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start MQTT listener");
				context.startService(new Intent(context, MQTTService.class));
			}
			// Start UDP services if not running already
			if (isMyServiceNotRunning(UDPlistener.class, context)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start UDP listener");
				context.startService(new Intent(context, UDPlistener.class));
			}
		}
	}

	/**
	 * Check if UDP receiver service is running
	 *
	 * @param serviceClass
	 *              Service class we want to check if it is running
	 * @return <code>boolean</code>
	 *              True if service is running
	 *              False if service is not running
	 */
	private boolean isMyServiceNotRunning(Class<?> serviceClass, Context context) {
		/** Activity manager for services */
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return false;
			}
		}
		return true;
	}
}
