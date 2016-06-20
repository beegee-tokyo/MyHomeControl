package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SecurityWidgetClick extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-SEC-W";

	public SecurityWidgetClick() {
		super("SecurityWidgetClick");
	}

	@SuppressLint("CommitPrefEdits")
	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				/* App widget id */
				int mAppWidgetId = extras.getInt(
						AppWidgetManager.EXTRA_APPWIDGET_ID,
						AppWidgetManager.INVALID_APPWIDGET_ID);

				boolean alarmIsActive = extras.getBoolean("AlarmStatus");

				// If they gave us an intent without the widget id, just bail.
				if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
					return;
				}

				/** Command string */
				String cmd;
				if (alarmIsActive) {
					cmd = "/?a=0"; // Disable alarm
				} else {
					cmd = "/?a=1"; // Enable alarm
				}

				// TODO if we are not on home wifi send command over MQTT instead of WiFi

				/** A HTTP client to access the ESP device */
				// Set timeout to 300ms/10ms
				OkHttpClient client = new OkHttpClient.Builder()
						.connectTimeout(300, TimeUnit.SECONDS)
						.writeTimeout(10, TimeUnit.SECONDS)
						.readTimeout(300, TimeUnit.SECONDS)
						.build();

				/** URL to be called */
				String urlString = MyHomeControl.SECURITY_URL_FRONT_1 + cmd; // URL to call

				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callESP = " + urlString);

				/** Request to ESP device */
				Request request = new Request.Builder()
						.url(urlString)
						.build();

				if (request != null) {
					try {
						/** Send request to ESP device */
						Response response = client.newCall(request).execute();
						if (response != null) {
							int status = response.code();
							if (status == 200) {
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Switched alarm");
							}
						}
					} catch (IOException e) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callESP failed = " + e.getMessage());
					}
				}
			}
		}
	}
}
