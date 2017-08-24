package tk.giesecke.myhomecontrol.devices;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;

import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.MessageListener;
import tk.giesecke.myhomecontrol.R;

/**
 * Implementation of App Widget functionality.
 */
public class DeviceStatusWidget extends AppWidgetProvider {

	/** Tag for debug messages */
	private static final String DEBUG_LOG_TAG = "MHC-DEVSTAT";

	@SuppressWarnings("deprecation")
	static public void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
	                            int appWidgetId) {

		// Construct the RemoteViews object
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wi_device_status);
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "updateAppWidget status widget");

		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "spmOK:" + MessageListener.spmOK);
		if (MessageListener.spmOK) {
			views.setTextColor(R.id.tv_spm, context.getResources()
					.getColor(android.R.color.holo_green_dark));
		} else {
			views.setTextColor(R.id.tv_spm, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "sf1OK:" + MessageListener.sf1OK);
		if (MessageListener.sf1OK) {
			views.setTextColor(R.id.tv_sf1, context.getResources()
					.getColor(android.R.color.holo_green_dark));
		} else {
			views.setTextColor(R.id.tv_sf1, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "sb1OK:" + MessageListener.sb1OK);
		if (MessageListener.sb1OK) {
			views.setTextColor(R.id.tv_sb1, context.getResources()
					.getColor(android.R.color.holo_green_dark));
		} else {
			views.setTextColor(R.id.tv_sb1, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "cm1OK:" + MessageListener.cm1OK);
		if (MessageListener.cm1OK) {
			views.setTextColor(R.id.tv_cm1, context.getResources()
					.getColor(android.R.color.holo_green_dark));
		} else {
			views.setTextColor(R.id.tv_cm1, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "vc1OK:" + MessageListener.vc1OK);
		if (MessageListener.vc1OK) {
			views.setTextColor(R.id.tv_vc1, context.getResources()
					.getColor(android.R.color.holo_green_dark));
		} else {
			views.setTextColor(R.id.tv_vc1, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "ly1OK:" + MessageListener.ly1OK);
		if (MessageListener.ly1OK) {
			views.setTextColor(R.id.tv_ly1, context.getResources()
					.getColor(android.R.color.holo_green_dark));
		} else {
			views.setTextColor(R.id.tv_ly1, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "lb1OK:" + MessageListener.lb1OK);
		if (MessageListener.lb1OK) {
			views.setTextColor(R.id.tv_lb1, context.getResources()
					.getColor(android.R.color.holo_green_dark));
		} else {
			views.setTextColor(R.id.tv_lb1, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "ac1OK:" + MessageListener.ac1OK);
		if (MessageListener.ac1OK) {
			views.setTextColor(R.id.tv_ac1, context.getResources()
					.getColor(android.R.color.holo_green_dark));
		} else {
			views.setTextColor(R.id.tv_ac1, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "ac2OK:" + MessageListener.ac2OK);
		if (MessageListener.ac2OK) {
			views.setTextColor(R.id.tv_ac2, context.getResources()
					.getColor(android.R.color.holo_green_dark));
		} else {
			views.setTextColor(R.id.tv_ac2, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "ac3OK:" + MessageListener.ac3OK);
		if (MessageListener.ac3OK) {
			views.setTextColor(R.id.tv_ac3, context.getResources()
					.getColor(android.R.color.holo_green_dark));
		} else {
			views.setTextColor(R.id.tv_ac3, context.getResources()
					.getColor(android.R.color.holo_red_light));
		}

		MessageListener.spmOK = false;
		MessageListener.sf1OK = false;
		MessageListener.sb1OK = false;
		MessageListener.cm1OK = false;
		MessageListener.vc1OK = false;
		MessageListener.ly1OK = false;
		MessageListener.lb1OK = false;
		MessageListener.ac1OK = false;
		MessageListener.ac2OK = false;
		MessageListener.ac3OK = false;

		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "onUpdate status widget");
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId);
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

