package tk.giesecke.myhomecontrol.aircon;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import tk.giesecke.myhomecontrol.MyHomeControl;
import tk.giesecke.myhomecontrol.R;

/**
 * Implementation of App Widget functionality.
 */
public class AirconWidget extends AppWidgetProvider {

	public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
	                                   int appWidgetId, int timerTime, String timerEnd, boolean isRunning) {

		// Construct the RemoteViews object
		RemoteViews rvACview = new RemoteViews(context.getPackageName(), R.layout.wi_aircon);

		if (isRunning) {
			rvACview.setInt(R.id.bt_ac_wid_timer, "setBackgroundResource", R.drawable.orange_round_button);
			rvACview.setTextViewText(R.id.bt_ac_wid_timer, timerEnd);
		} else {
			rvACview.setInt(R.id.bt_ac_wid_timer, "setBackgroundResource", R.drawable.green_round_button);
			String newButtonText = timerTime + " " + context.getResources().getString(R.string.bt_txt_hour);
			rvACview.setTextViewText(R.id.bt_ac_wid_timer, newButtonText);
		}

		/* Intent to start method to increase timer time on PLUS button push */
		Intent serviceIntent = new Intent(context, AirconWidgetHelper.class);
		serviceIntent.setAction("p");
		/* Pending intent to start method when plus button is clicked */
		PendingIntent pendingServiceIntent =
				PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Attach a click listener to the button
		rvACview.setOnClickPendingIntent(R.id.bt_ac_wid_plus, pendingServiceIntent);

		/* Intent to start method to decrease timer time on MINUS button push */
		serviceIntent = new Intent(context, AirconWidgetHelper.class);
		serviceIntent.setAction("m");
		/* Pending intent to start method when minus button is clicked */
		pendingServiceIntent =
				PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		// Attach a click listener to the button
		rvACview.setOnClickPendingIntent(R.id.bt_ac_wid_minus, pendingServiceIntent);

		/* Intent to start method to start aircon in timer mode */
		serviceIntent = new Intent(context, AirconWidgetHelper.class);
		serviceIntent.setAction("s");
		/* Pending intent to start method when widget is clicked */
		pendingServiceIntent =
				PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		// Attach a click listener to the button
		rvACview.setOnClickPendingIntent(R.id.bt_ac_wid_timer, pendingServiceIntent);

//		/** Intent to start app if widget is pushed */
//		Intent appIntent = new Intent(context, MyHomeControl.class);
//		appIntent.putExtra("view", MyHomeControl.view_aircon_id);
//		/** Pending intent to start app if widget is pushed */
//		PendingIntent pendingAppIntent = PendingIntent.getActivity(context, 2,
//				appIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		/* Intent to start method to start aircon in timer mode */
		serviceIntent = new Intent(context, AirconWidgetHelper.class);
		serviceIntent.setAction("a");
		/* Pending intent to start method when widget is clicked */
		pendingServiceIntent =
				PendingIntent.getService(context, 0, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		//  Attach an on-click listener to the aircon icon
//		rvACview.setOnClickPendingIntent(R.id.iv_ac_wid_icon, pendingAppIntent);
		rvACview.setOnClickPendingIntent(R.id.iv_ac_wid_icon, pendingServiceIntent);

		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, rvACview);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		/* Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences(MyHomeControl.sharedPrefName, 0);
		int timerTime = mPrefs.getInt("acTimerTime", 1);
		boolean isRunning = mPrefs.getBoolean("acTimerOn", false);
		String timerEnd = mPrefs.getString("acTimerEnd", "??:??");

		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId, timerTime, timerEnd, isRunning);
		}
	}

	@Override
	public void onEnabled(Context context) {
		// Construct the RemoteViews object
		RemoteViews rvACwidget = new RemoteViews(context.getPackageName(), R.layout.wi_aircon);
		/* Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences(MyHomeControl.sharedPrefName, 0);
		String newButtonText = mPrefs.getInt("acTimerTime",1) + " " + context.getResources().getString(R.string.bt_txt_hour);
		rvACwidget.setTextViewText(R.id.bt_ac_wid_timer, newButtonText);
	}
}

