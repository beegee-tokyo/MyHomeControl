package tk.giesecke.myhomecontrol;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class LightWidget extends AppWidgetProvider {

	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
								int appWidgetId, boolean lightIsOn) {

		// Construct the RemoteViews object
		/** Instance of the widget view */
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.light_widget);

		if (lightIsOn) {
			views.setImageViewResource(R.id.iv_light_widget, R.mipmap.ic_light_on);
		} else {
			views.setImageViewResource(R.id.iv_light_widget, R.mipmap.ic_light_off);
		}

		/** Intent to start method to switch on light when widget is clicked */
		Intent serviceIntent = new Intent(context, LightWidgetClick.class);
		serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		/** Pending intent to start method when widget is clicked */
		PendingIntent pendingServiceIntent =
				PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the layout for the App Widget and attach a click listener to the
		// button
		views.setOnClickPendingIntent(R.id.light_widget, pendingServiceIntent);

		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId, false);
		}
	}
}

