package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class AirconWidgetClick extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-AC-W";

	public AirconWidgetClick() {
		super("AirconWidgetClick");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			/** Access to shared preferences of app widget */
			SharedPreferences mPrefs = this.getSharedPreferences(MyHomeControl.sharedPrefName, 0);
			int timerTime = mPrefs.getInt("acTimerTime", 1);
			boolean isRunning = mPrefs.getBoolean("acTimerOn", false);
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Widget click: " + intent.getAction());

			if (intent.getAction().equals("m")){
				if (timerTime > 1) {
					timerTime--;
					espComm("/?t=" + Integer.toString(timerTime));
				}
			}
			if (intent.getAction().equals("p")){
				if (timerTime < 9) {
					timerTime++;
					espComm("/?t=" + Integer.toString(timerTime));
				}
			}
			if (intent.getAction().equals("s")){
				if (!isRunning) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Starting timer");
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "First command = " + "/?t=" + Integer.toString(timerTime));
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Second command = " + "/?c="  + MyHomeControl.CMD_OTHER_TIMER);
					if (espComm("/?t=" + Integer.toString(timerTime))) {
						if (espComm("/?c=" + MyHomeControl.CMD_OTHER_TIMER)) {
							isRunning = true;
							mPrefs.edit().putBoolean("acTimerOn", true).apply();
						}
					}
				} else {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stopping timer");
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "First command = " + "/?c="  + MyHomeControl.CMD_OTHER_TIMER);
					if (espComm("/?c=" + MyHomeControl.CMD_OTHER_TIMER)) {
						isRunning = false;
						mPrefs.edit().putBoolean("acTimerOn", false).apply();
					}
				}
			}
			mPrefs.edit().putInt("acTimerTime", timerTime).apply();

			/** App widget manager for all widgets of this app */
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
			/** Component name of this widget */
			ComponentName thisAppWidget = new ComponentName(this.getPackageName(),
					AirconWidget.class.getName());
			/** List of all active widgets */
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

			for (int appWidgetId : appWidgetIds) {
				AirconWidget.updateAppWidget(this,appWidgetManager,appWidgetId, timerTime, isRunning);
			}
		}
	}

	@SuppressLint("CommitPrefEdits")
	private boolean espComm(String cmd) {
		/** A HTTP client to access the ESP device */
		// Set timeout to 5 minutes in case we have a lot of data to load
		OkHttpClient client = new OkHttpClient.Builder()
				.connectTimeout(300, TimeUnit.SECONDS)
				.writeTimeout(10, TimeUnit.SECONDS)
				.readTimeout(300, TimeUnit.SECONDS)
				.build();

		/** URL to be called */
		String urlString = MyHomeControl.AIRCON_URL_1 + cmd; // URL to call

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
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callESP response = " + Integer.toString(status));
						return true;
					} else {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callESP response = " + Integer.toString(status));
						return false;
					}
				}
			} catch (IOException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callESP failed = " + e.getMessage());
				return false;
			}
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callESP failed with invalid request");
		return false;
	}
}
