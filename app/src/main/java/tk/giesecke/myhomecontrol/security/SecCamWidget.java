package tk.giesecke.myhomecontrol.security;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import tk.giesecke.myhomecontrol.R;

/**
 * Implementation of App Widget functionality.
 */
public class SecCamWidget extends AppWidgetProvider {

	private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
	                                    int appWidgetId) {

		// Construct the RemoteViews object
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wi_sec_cam);

		/* Intent to start app if widget is pushed */
		Intent secCamIntent = new Intent(context, SecCamViewer.class);
		/* Pending intent to start app if widget is pushed */
		PendingIntent pendingIntent1 = PendingIntent.getActivity(context, 0,
				secCamIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		//  Attach an on-click listener to the icon
		views.setOnClickPendingIntent(R.id.rlSecCamwidget, pendingIntent1);

		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId);
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
}

