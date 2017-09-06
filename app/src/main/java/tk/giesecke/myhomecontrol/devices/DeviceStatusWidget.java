package tk.giesecke.myhomecontrol.devices;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.R;

/**
 * Implementation of App Widget functionality.
 */
public class DeviceStatusWidget extends AppWidgetProvider {

	/** Tag for debug messages */
	private static final String DEBUG_LOG_TAG = "MHC-DEVSTAT";

	@SuppressWarnings("deprecation")
	static public void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
	                            int appWidgetId, boolean checkAll) {

		// Construct the RemoteViews object
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wi_device_status);
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "updateAppWidget status widget");

		/** Time now to check when last device status message arrived */
		long timeNow = System.currentTimeMillis();
		/** Timeout for device status update (set to 6 minutes) */
		long timeOut = 360000;
		if (checkAll) {
			if (MessageListener.lastSpmMsg == 0) {
				views.setTextColor(R.id.tv_spm, context.getResources()
						.getColor(android.R.color.darker_gray));
			} else if (timeNow - MessageListener.lastSpmMsg < timeOut) {
				views.setTextColor(R.id.tv_spm, context.getResources()
						.getColor(android.R.color.holo_green_light));
			} else {
				views.setTextColor(R.id.tv_spm, context.getResources()
						.getColor(android.R.color.holo_red_light));
			}
			if (MessageListener.lastSf1Msg == 0) {
				views.setTextColor(R.id.tv_sf1, context.getResources()
						.getColor(android.R.color.darker_gray));
			} else if (timeNow - MessageListener.lastSf1Msg < timeOut) {
				views.setTextColor(R.id.tv_sf1, context.getResources()
						.getColor(android.R.color.holo_green_light));
			} else {
				views.setTextColor(R.id.tv_sf1, context.getResources()
						.getColor(android.R.color.holo_red_light));
			}
			if (MessageListener.lastSb1Msg == 0) {
				views.setTextColor(R.id.tv_sb1, context.getResources()
						.getColor(android.R.color.darker_gray));
			} else if (timeNow - MessageListener.lastSb1Msg < timeOut) {
				views.setTextColor(R.id.tv_sb1, context.getResources()
						.getColor(android.R.color.holo_green_light));
			} else {
				views.setTextColor(R.id.tv_sb1, context.getResources()
						.getColor(android.R.color.holo_red_light));
			}
			if (MessageListener.lastCm1Msg == 0) {
				views.setTextColor(R.id.tv_cm1, context.getResources()
						.getColor(android.R.color.darker_gray));
			} else if (timeNow - MessageListener.lastCm1Msg < timeOut) {
				views.setTextColor(R.id.tv_cm1, context.getResources()
						.getColor(android.R.color.holo_green_light));
			} else {
				views.setTextColor(R.id.tv_cm1, context.getResources()
						.getColor(android.R.color.holo_red_light));
			}
			if (MessageListener.lastMhcMsg == 0) {
				views.setTextColor(R.id.tv_mhc, context.getResources()
						.getColor(android.R.color.darker_gray));
			} else if (timeNow - MessageListener.lastMhcMsg < timeOut) {
				views.setTextColor(R.id.tv_mhc, context.getResources()
						.getColor(android.R.color.holo_green_light));
			} else {
				views.setTextColor(R.id.tv_mhc, context.getResources()
						.getColor(android.R.color.holo_red_light));
			}
			if (MessageListener.lastLy1Msg == 0) {
				views.setTextColor(R.id.tv_ly1, context.getResources()
						.getColor(android.R.color.darker_gray));
			} else if (timeNow - MessageListener.lastLy1Msg < timeOut) {
				views.setTextColor(R.id.tv_ly1, context.getResources()
						.getColor(android.R.color.holo_green_light));
			} else {
				views.setTextColor(R.id.tv_ly1, context.getResources()
						.getColor(android.R.color.holo_red_light));
			}
			if (MessageListener.lastLb1Msg == 0) {
				views.setTextColor(R.id.tv_lb1, context.getResources()
						.getColor(android.R.color.darker_gray));
			} else if (timeNow - MessageListener.lastLb1Msg < timeOut) {
				views.setTextColor(R.id.tv_lb1, context.getResources()
						.getColor(android.R.color.holo_green_light));
			} else {
				views.setTextColor(R.id.tv_lb1, context.getResources()
						.getColor(android.R.color.holo_red_light));
			}
			if (MessageListener.lastAc1Msg == 0) {
				views.setTextColor(R.id.tv_ac1, context.getResources()
						.getColor(android.R.color.darker_gray));
			} else if (timeNow - MessageListener.lastAc1Msg < timeOut) {
				views.setTextColor(R.id.tv_ac1, context.getResources()
						.getColor(android.R.color.holo_green_light));
			} else {
				views.setTextColor(R.id.tv_ac1, context.getResources()
						.getColor(android.R.color.holo_red_light));
			}
			if (MessageListener.lastAc2Msg == 0) {
				views.setTextColor(R.id.tv_ac2, context.getResources()
						.getColor(android.R.color.darker_gray));
			} else if (timeNow - MessageListener.lastAc2Msg < timeOut) {
				views.setTextColor(R.id.tv_ac2, context.getResources()
						.getColor(android.R.color.holo_green_light));
			} else {
				views.setTextColor(R.id.tv_ac2, context.getResources()
						.getColor(android.R.color.holo_red_light));
			}
			if (MessageListener.lastAc3Msg == 0) {
				views.setTextColor(R.id.tv_ac3, context.getResources()
						.getColor(android.R.color.darker_gray));
			} else if (timeNow - MessageListener.lastAc3Msg < timeOut) {
				views.setTextColor(R.id.tv_ac3, context.getResources()
						.getColor(android.R.color.holo_green_light));
			} else {
				views.setTextColor(R.id.tv_ac3, context.getResources()
						.getColor(android.R.color.holo_red_light));
			}
		}
		if (MessageListener.udpSocket != null) {
			views.setTextColor(R.id.tv_udp, context.getResources()
					.getColor(android.R.color.holo_green_light));
		} else {
			views.setTextColor(R.id.tv_udp, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}
		if (MessageListener.tcpSocket != null) {
			views.setTextColor(R.id.tv_tcp, context.getResources()
					.getColor(android.R.color.holo_green_light));
		} else {
			views.setTextColor(R.id.tv_tcp, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}
		if (MessageListener.mqttClient != null && MessageListener.mqttClient.isConnected()) {
			views.setTextColor(R.id.tv_mqtt, context.getResources()
					.getColor(android.R.color.holo_green_light));
		} else {
			views.setTextColor(R.id.tv_mqtt, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}

		/** Intent to start app if widget is pushed */
		Intent appIntent = new Intent(context, DeviceStatusWidgetHelper.class);
		/** Pending intent to start app if widget is pushed */
		PendingIntent pendingAppIntent = PendingIntent.getService(context, 0,
				appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		//  Attach an on-click listener to the widget
		views.setOnClickPendingIntent(R.id.ll_dev_status, pendingAppIntent);
		views.setOnClickPendingIntent(R.id.tv_spm, pendingAppIntent);
		views.setOnClickPendingIntent(R.id.tv_tcp, pendingAppIntent);
		views.setOnClickPendingIntent(R.id.tv_lb1, pendingAppIntent);

		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "onUpdate status widget");
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId, false);
		}
	}

	@Override
	public void onEnabled(Context context) {
		// Enter relevant functionality for when the first widget is created
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Enabled status widget");
	}

	@Override
	public void onDisabled(Context context) {
		// Enter relevant functionality for when the last widget is disabled
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Disabled status widget");
	}
}

