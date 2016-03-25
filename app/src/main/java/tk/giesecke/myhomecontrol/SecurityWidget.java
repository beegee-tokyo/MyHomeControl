package tk.giesecke.myhomecontrol;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class SecurityWidget extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId, false);
		}
	}

	@Override
	public void onEnabled(Context context) {
		// Enter relevant functionality for when the first widget is created
	}

	@Override
	public void onDisabled(Context context) {
		// Enter relevant functionality for when the last widget is disabled
	}

	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
								int appWidgetId, boolean isChanging) {

		// Construct the RemoteViews object
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.security_widget);

		/** Pointer to shared preferences */
		SharedPreferences mPrefs = context.getSharedPreferences(MyHomeControl.sharedPrefName, 0);
		if (isChanging) {
			views.setImageViewResource(R.id.iv_sec_widget, R.mipmap.ic_sec_temp);
		} else {
			if (mPrefs.getBoolean(MyHomeControl.prefsSecurityAlarmOn, false)) {
				views.setImageViewResource(R.id.iv_sec_widget, R.mipmap.ic_sec_widget_on);
			} else {
				views.setImageViewResource(R.id.iv_sec_widget, R.mipmap.ic_sec_widget_off);
			}
		}

		// Create an intent to launch the service on widget push
		Intent serviceIntent = new Intent(context, SecurityWidgetClick.class);
		serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		// PendingIntent is required for the onClickPendingIntent that actually
		// starts the service from a button click
		PendingIntent pendingServiceIntent =
				PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the layout for the App Widget and attach a click listener to the
		// button
		views.setOnClickPendingIntent(R.id.sec_widget, pendingServiceIntent);

		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
}

