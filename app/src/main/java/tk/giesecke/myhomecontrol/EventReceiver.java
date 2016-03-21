package tk.giesecke.myhomecontrol;

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
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Screen off");
			Utilities.startStopUpdates(context, false);
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Screen on");
			Utilities.startStopUpdates(context, true);
		} if (intent.getAction().equals
				(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
			// CONNECTIVITY CHANGE
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Connection Change");
			if (!Utilities.isHomeWiFi(context)) { // No WiFi connection
				// Stop service to listen to UDP broadcast messages
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stop UDP listener");
				context.stopService(new Intent(context, UDPlistener.class));
				Utilities.startStopUpdates(context, false);
			} else {
				// Start service to listen to UDP broadcast messages
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Start UDP listener");
				context.startService(new Intent(context, UDPlistener.class));
				Utilities.startStopUpdates(context, true);
			}
		}
	}
}
