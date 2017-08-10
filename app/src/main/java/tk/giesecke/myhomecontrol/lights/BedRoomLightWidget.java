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
public class BedRoomLightWidget extends AppWidgetProvider {

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

		switch (lightStatus) {
			case 1:
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_on, R.mipmap.ic_bulb_on_on);
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_off, R.mipmap.ic_bulb_off);
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_dim, R.mipmap.ic_bulb_dim);
				break;
			case 2:
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_on, R.mipmap.ic_bulb_on);
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_off, R.mipmap.ic_bulb_off);
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_dim, R.mipmap.ic_bulb_dim_on);
				break;
			case 3:
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_on, R.mipmap.ic_bulb_on);
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_off, R.mipmap.ic_bulb_off_on);
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_dim, R.mipmap.ic_bulb_dim);
				break;
			default:
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_on, R.mipmap.ic_bulb_unavail);
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_off, R.mipmap.ic_bulb_unavail);
				rvLightCtrlWidget.setImageViewResource(R.id.ib_light_dim, R.mipmap.ic_bulb_unavail);
				break;
		}

		// Create an intent to switch on the lights
		Intent serviceIntentOn = new Intent(context, BedRoomLightWidgetOn.class);
		serviceIntentOn.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		// PendingIntent is required for the onClickPendingIntent that actually
		// starts the service from a button click
		PendingIntent pendingServiceIntentOn =
				PendingIntent.getService(context, 0, serviceIntentOn, PendingIntent.FLAG_UPDATE_CURRENT);
		// Attach a click listener to the button
		rvLightCtrlWidget.setOnClickPendingIntent(R.id.ib_light_on, pendingServiceIntentOn);

		// Create an intent to switch off the lights
		Intent serviceIntentOff = new Intent(context, BedRoomLightWidgetOff.class);
		serviceIntentOff.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		// PendingIntent is required for the onClickPendingIntent that actually
		// starts the service from a button click
		PendingIntent pendingServiceIntentOff =
				PendingIntent.getService(context, 0, serviceIntentOff, PendingIntent.FLAG_UPDATE_CURRENT);
		// Attach a click listener to the button
		rvLightCtrlWidget.setOnClickPendingIntent(R.id.ib_light_off, pendingServiceIntentOff);

		// Create an intent to switch off the lights
		Intent serviceIntentDim = new Intent(context, BedRoomLightWidgetDim.class);
		serviceIntentDim.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		// PendingIntent is required for the onClickPendingIntent that actually
		// starts the service from a button click
		PendingIntent pendingServiceIntentDim =
				PendingIntent.getService(context, 0, serviceIntentDim, PendingIntent.FLAG_UPDATE_CURRENT);
		// Attach a click listener to the button
		rvLightCtrlWidget.setOnClickPendingIntent(R.id.ib_light_dim, pendingServiceIntentDim);

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

