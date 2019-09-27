package tk.giesecke.myhomecontrol.solar;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.widget.RemoteViews;

import tk.giesecke.myhomecontrol.MyHomeControl;
import tk.giesecke.myhomecontrol.R;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link SolarPanelWidgetConfig SolarPanelWidgetConfig}
 */
public class SolarPanelWidget extends AppWidgetProvider {

	@Override
	public void onDisabled(Context context) {
		/* Instance of the shared preferences */
		SharedPreferences mPrefs = context.getSharedPreferences(MyHomeControl.sharedPrefName,0);
		mPrefs.edit().putInt(MyHomeControl.prefsSolarWidgetNum,0).apply();
	}

	/**
	 * Updates a widgets
	 *
	 * @param context
	 *            Context of this application
	 * @param appWidgetManager
	 *            Instance of the appWidgetManager
	 * @param appWidgetId
	 *            ID of the widget to be updated
	 * @param solarPowerMin
	 *            Power produced by the solar panels
	 * @param consPowerMin
	 *            Power imported/exported by the house
	 */
	@SuppressLint({"InlinedApi", "DefaultLocale"})
	public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
	                                   int appWidgetId,
	                                   Float solarPowerMin,
	                                   Float consPowerMin) {

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		/* RemoteViews object */
		RemoteViews views;
		/* Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences(MyHomeControl.sharedPrefName, 0);

		if (mPrefs.getBoolean(MyHomeControl.prefsSolarWidgetSize,true)) {
			views = new RemoteViews(context.getPackageName(), R.layout.wi_solar_panel_l);
		} else {
			views = new RemoteViews(context.getPackageName(), R.layout.wi_solar_panel_s);
		}

		/* Intent to start app if widget is pushed */
		Intent intent1 = new Intent(context, SolarPanelWidgetHelper.class);
		/* Pending intent to start app if widget is pushed */
		PendingIntent pendingIntent1 = PendingIntent.getService(context, 0,
				intent1, PendingIntent.FLAG_UPDATE_CURRENT);
		//  Attach an on-click listener to the battery icon
		views.setOnClickPendingIntent(R.id.rlSPwidget, pendingIntent1);

		/* Double for the result of solar current and consumption used at 1min updates */
		double resultPowerMin = solarPowerMin + consPowerMin;

		views.setTextViewText(R.id.tv_widgetRow1Value, String.format("%.0f", resultPowerMin) + "W");
		views.setTextViewText(R.id.tv_widgetRow2Value, String.format("%.0f", Math.abs(consPowerMin)) + "W");
		views.setTextViewText(R.id.tv_widgetRow3Value, String.format("%.0f", solarPowerMin) + "W");

		if (consPowerMin > 0.0d) {
			views.setTextColor(R.id.tv_widgetRow2Value, context.getResources()
					.getColor(android.R.color.holo_red_light));
		} else {
			views.setTextColor(R.id.tv_widgetRow2Value, context.getResources()
					.getColor(android.R.color.holo_green_light));
		}
		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
}

