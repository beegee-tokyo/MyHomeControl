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
			if (!serviceIsRunning(MessageListener.class, context)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start Message listener");
				context.startService(new Intent(context, MessageListener.class));
			}
		}
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Screen off");
			// Stop MessageListener service if running
			MessageListener.shouldRestartSocketListen = false;
			if (MessageListener.socket != null) {
				MessageListener.socket.disconnect();
				MessageListener.socket.close();
			}
			if (serviceIsRunning(MessageListener.class, context)) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop Message listener");
				context.stopService(new Intent(context, MessageListener.class));
			}
		}

		// CONNECTIVITY CHANGE
		if (intent.getAction().equals
				(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Connection Change (EventReceiver)");
			final boolean bHasConnection[] = Utilities.connectionAvailable(context);
			if (bHasConnection[0] || bHasConnection[1]) { // Connection available?
				// Start/Restart service to listen to UDP/MQTT broadcast messages
				if (serviceIsRunning(MessageListener.class, context)) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "UDP/MQTT listener WiFi still running");
				} else {
					// Start service with a delay of 5 seconds to let connection settle down
					final Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start/Restart UDP/MQTT listener = " + bHasConnection[0]
									+ " MOBILE = " + bHasConnection[1]);
							context.startService(new Intent(context, MessageListener.class));
						}
					}, 5000);
				}
			} else {
				MessageListener.shouldRestartSocketListen = false;
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop Message listener");
				context.stopService(new Intent(context, MessageListener.class));
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
	private boolean serviceIsRunning(Class<?> serviceClass, Context context) {
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
