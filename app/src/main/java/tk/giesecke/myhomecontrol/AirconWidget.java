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
public class AirconWidget extends AppWidgetProvider {

	@SuppressWarnings("deprecation")
	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
								int appWidgetId, int timerTime, boolean isRunning) {

		// Construct the RemoteViews object
		RemoteViews rvACview = new RemoteViews(context.getPackageName(), R.layout.aircon_widget);

		if (isRunning) {
			rvACview.setInt(R.id.bt_ac_wid_timer, "setBackgroundResource", R.drawable.orange_round_button);
			rvACview.setTextViewText(R.id.bt_ac_wid_timer, context.getResources().getString(R.string.timer_on));
		} else {
			rvACview.setInt(R.id.bt_ac_wid_timer, "setBackgroundResource", R.drawable.green_round_button);
			String newButtonText = Integer.toString(timerTime) + " " + context.getResources().getString(R.string.bt_txt_hour);
			rvACview.setTextViewText(R.id.bt_ac_wid_timer, newButtonText);
		}

		// Create an intent to launch the service on PLUS button push
		/** Intent to start method to increase timer time */
		Intent serviceIntent = new Intent(context, AirconWidgetClick.class);
		serviceIntent.setAction("p");
		// PendingIntent is required for the onClickPendingIntent that actually
		// starts the service from a button click
		/** Pending intent to start method when widget is clicked */
		PendingIntent pendingServiceIntent =
				PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Attach a click listener to the button
		rvACview.setOnClickPendingIntent(R.id.bt_ac_wid_plus, pendingServiceIntent);

		// Create an intent to launch the service on MINUS button push
		/** Intent to start method to decrease timer time */
		serviceIntent = new Intent(context, AirconWidgetClick.class);
		serviceIntent.setAction("m");
		/** Pending intent to start method when widget is clicked */
		pendingServiceIntent =
				PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		// Attach a click listener to the button
		rvACview.setOnClickPendingIntent(R.id.bt_ac_wid_minus, pendingServiceIntent);

		// Create an intent to launch the service on MINUS button push
		/** Intent to start method to start aircon in timer mode */
		serviceIntent = new Intent(context, AirconWidgetClick.class);
		serviceIntent.setAction("s");
		/** Pending intent to start method when widget is clicked */
		pendingServiceIntent =
				PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		// Attach a click listener to the button
		rvACview.setOnClickPendingIntent(R.id.bt_ac_wid_timer, pendingServiceIntent);

		// Create an Intent to launch MainActivity
		/** Intent to start app if widget is pushed */
		Intent appIntent = new Intent(context, MyHomeControl.class);
		appIntent.putExtra("view", 2);
		// Creating a pending intent, which will be invoked when the user
		// clicks on the widget
		/** Pending intent to start app if widget is pushed */
		PendingIntent pendingAppIntent = PendingIntent.getActivity(context, 2,
				appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		//  Attach an on-click listener to the battery icon
		rvACview.setOnClickPendingIntent(R.id.iv_ac_wid_icon, pendingAppIntent);

		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, rvACview);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		/** Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences(MyHomeControl.sharedPrefName, 0);
		int timerTime = mPrefs.getInt("acTimerTime", 1);
		boolean isRunning = mPrefs.getBoolean("acTimerOn", false);

		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId, timerTime, isRunning);
		}
	}

	@Override
	public void onEnabled(Context context) {
		// Construct the RemoteViews object
		RemoteViews rvACwidget = new RemoteViews(context.getPackageName(), R.layout.aircon_widget);
		/** Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences(MyHomeControl.sharedPrefName, 0);
		String newButtonText = Integer.toString(mPrefs.getInt("acTimerTime",1)) + " " + context.getResources().getString(R.string.bt_txt_hour);
		rvACwidget.setTextViewText(R.id.bt_ac_wid_timer, newButtonText);
	}
}

