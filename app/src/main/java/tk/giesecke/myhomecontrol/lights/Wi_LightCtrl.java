package tk.giesecke.myhomecontrol.lights;

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
public class Wi_LightCtrl extends AppWidgetProvider {

	/**
	 * Updates a widgets
	 *
	 * @param context
	 *            Context of this application
	 * @param appWidgetManager
	 *            Instance of the appWidgetManager
	 * @param appWidgetId
	 *            ID of the widget to be updated
	 * @param lightStatus
	 *            Status of lights
	 */
	public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
	                            int appWidgetId, int lightStatus) {

		// Construct the RemoteViews object
		RemoteViews rvLightCtrlWidget = new RemoteViews(context.getPackageName(), R.layout.wi_light_ctrl);

		String intentAction;
		switch (lightStatus) {
			case 1:
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_ctrl_widget, R.mipmap.ic_bulb_on);
				intentAction = "2";
				break;
			case 2:
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_ctrl_widget, R.mipmap.ic_bulb_dim);
				intentAction = "3";
				break;
			case 3:
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_ctrl_widget, R.mipmap.ic_bulb_off);
				intentAction = "1";
				break;
			default:
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_ctrl_widget, R.mipmap.ic_bulb_unavail);
				intentAction = "0";
				break;
		}

		// Create an intent to activate the alarm on widget push
		Intent serviceIntent = new Intent(context, WiCl_LightCtrl.class);
		serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		serviceIntent.setAction(intentAction);
		// PendingIntent is required for the onClickPendingIntent that actually
		// starts the service from a button click
		PendingIntent pendingServiceIntent =
				PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		// Attach a click listener to the button
		rvLightCtrlWidget.setOnClickPendingIntent(R.id.ib_light_ctrl_widget, pendingServiceIntent);

		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, rvLightCtrlWidget);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId, 0);
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

