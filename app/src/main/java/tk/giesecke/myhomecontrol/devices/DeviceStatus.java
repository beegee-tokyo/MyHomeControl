package tk.giesecke.myhomecontrol.devices;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import tk.giesecke.myhomecontrol.BuildConfig;


public class DeviceStatus extends IntentService {

	/** Tag for debug messages */
	private static final String DEBUG_LOG_TAG = "MHC-DEVSTAT";

	public DeviceStatus() {
		super("DeviceStatus");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			Context intentContext = getApplicationContext();
			// Update light control widget
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Update device status widget");

			/** App widget manager for all widgets of this app */
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(intentContext);
			/** Component name of this widget */
			ComponentName thisAppWidget;

			thisAppWidget = new ComponentName(intentContext.getPackageName(),
					DeviceStatusWidget.class.getName());
			/** List of all active widgets */
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

			for (int appWidgetId : appWidgetIds) {
				DeviceStatusWidget.updateAppWidget(intentContext, appWidgetManager, appWidgetId);
			}
		}
	}
}
