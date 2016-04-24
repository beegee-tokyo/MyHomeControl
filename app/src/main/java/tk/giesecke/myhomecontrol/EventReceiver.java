package tk.giesecke.myhomecontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
			securityUpdate(context);
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

	/**
	 * Update status of security widget
	 */
	private void securityUpdate(Context context) {
		// Check if we are on home WiFi network
		if (!Utilities.isHomeWiFi(context)) {
			// if not on home Wifi just quit
			return;
		}
		/** String with the URL to get the data */
		String urlString = context.getResources().getString(R.string.SECURITY_URL_FRONT_1) + "/?s"; // URL to call

		/** Response from the spMonitor device or error message */
		String resultFromCom = "";

		/** A HTTP client to access the spMonitor device */
		// Set timeout to 5 minutes in case we have a lot of data to load
		OkHttpClient client = new OkHttpClient.Builder()
				.connectTimeout(300, TimeUnit.SECONDS)
				.writeTimeout(10, TimeUnit.SECONDS)
				.readTimeout(300, TimeUnit.SECONDS)
				.build();

		/** Request to spMonitor device */
		Request request = new Request.Builder()
				.url(urlString)
				.build();

		if (request != null) {
			try {
				/** Response from spMonitor device */
				Response response = client.newCall(request).execute();
				if (response != null) {
					resultFromCom = response.body().string();
				}
			} catch (IOException e) {
				return;
			}
		}

		if (!resultFromCom.equalsIgnoreCase("")) {
			// decode JSON
			if (Utilities.isJSONValid(resultFromCom)) {
				/** Json object for received data */
				JSONObject jsonResult;
				try {
					jsonResult = new JSONObject(resultFromCom);
					UDPlistener.alarmNotif(jsonResult, context);
				} catch (JSONException e) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
				}
			}
		}
	}

}
