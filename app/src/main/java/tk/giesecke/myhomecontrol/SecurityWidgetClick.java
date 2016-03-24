package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

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

				// If they gave us an intent without the widget id, just bail.
				if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
					return;
				}

				/** App widget manager for all widgets of this app */
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

				SecurityWidget.updateAppWidget(this,appWidgetManager, mAppWidgetId, true);

				/** Pointer to shared preferences */
				SharedPreferences mPrefs = getSharedPreferences(MyHomeControl.sharedPrefName, 0);
				/** Command string */
				String cmd;
				if (mPrefs.getBoolean(MyHomeControl.prefsSecurityAlarmOn, false)) {
					cmd = "/?a=0";
				} else {
					cmd = "/?a=1";
				}

				/** A HTTP client to access the ESP device */
				OkHttpClient client = new OkHttpClient();

				/** URL to be called */
				String urlString = MyHomeControl.SECURITY_URL_FRONT_1 + cmd; // URL to call

				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callESP = " + urlString);

				/** Request to ESP device */
				Request request = new Request.Builder()
						.url(urlString)
						.build();

				if (request != null) {
					try {
						/** Response from ESP device */
						Response response = client.newCall(request).execute();
						if (response != null) {
							int status = response.code();
							if (status == 200) {
								// Request success
								if (cmd.equalsIgnoreCase("/?a=0")) {
									mPrefs.edit().putBoolean(MyHomeControl.prefsSecurityAlarmOn, false).commit();
								} else {
									mPrefs.edit().putBoolean(MyHomeControl.prefsSecurityAlarmOn, true).commit();
								}
								SecurityWidget.updateAppWidget(this,appWidgetManager, mAppWidgetId, false);
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
