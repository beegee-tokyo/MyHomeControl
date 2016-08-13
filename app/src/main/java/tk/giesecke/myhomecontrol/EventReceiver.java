package tk.giesecke.myhomecontrol;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public class EventReceiver extends BroadcastReceiver {

	private static final String DEBUG_LOG_TAG = "MHC-EVE";

	public EventReceiver() {
	}

	@Override
	public void onReceive(final Context context, Intent intent) {
		// Screen on/off
		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Screen on");
			// Start MessageListener service if not running already
			if (isMyServiceNotRunning(MessageListener.class, context)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start Message listener");
				context.startService(new Intent(context, MessageListener.class));
			}
		}

		if (intent.getAction().equals
				(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
			// CONNECTIVITY CHANGE
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Connection Change (EventReceiver)");
			boolean bHasConnection[] = Utilities.connectionAvailable(context);
			if (bHasConnection[0] || bHasConnection[1]) { // Connection available?
				// Start service to listen to UDP/MQTT broadcast messages
				if (isMyServiceNotRunning(MessageListener.class, context)) {

					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start UDP/MQTT listener WiFi = " + bHasConnection[0]
							+ " MOBILE = " + bHasConnection[1]);
					// Start service with a delay of 30 seconds to let connection settle down
					final Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							context.startService(new Intent(context, MessageListener.class));
						}
					}, 30000);
				}
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
