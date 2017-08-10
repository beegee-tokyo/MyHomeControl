package tk.giesecke.myhomecontrol.security;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.MyHomeControl;
import tk.giesecke.myhomecontrol.Utilities;
import tk.giesecke.myhomecontrol.aircon.AirconWidgetHelper;

import static tk.giesecke.myhomecontrol.MyHomeControl.sharedPrefName;
import static tk.giesecke.myhomecontrol.MessageListener.TCP_CLIENT_PORT;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SecLightWidgetHelper extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-LIGHT-W";

	public SecLightWidgetHelper() {
		super("SecLightWidgetHelper");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			/** Bundle received when service is called */
			Bundle extras = intent.getExtras();
			if (extras != null) {
				/** App widget id */
				int mAppWidgetId = extras.getInt(
						AppWidgetManager.EXTRA_APPWIDGET_ID,
						AppWidgetManager.INVALID_APPWIDGET_ID);

				// If they gave us an intent without the widget id, just bail.
				if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
					return;
				}

				/** App widget manager for all widgets of this app */
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

				// If we are not on home WiFi, send command to MQTT broker
				if (!Utilities.isHomeWiFi(getApplicationContext())) {
					String mqttTopic = "{\"ip\":\"sf1\","; // Device IP address
					mqttTopic += "\"cm\":\"b\"}"; // The command

					AirconWidgetHelper.doPublish(mqttTopic, getApplicationContext());

					mqttTopic = "{\"ip\":\"sb1\","; // Device IP address
					mqttTopic += "\"cm\":\"b\"}"; // The command

					AirconWidgetHelper.doPublish(mqttTopic, getApplicationContext());
				} else {
					SharedPreferences mPrefs = getSharedPreferences(sharedPrefName,0);
					String urlFront = mPrefs.getString( MyHomeControl.deviceNames[MyHomeControl.secFrontIndex],"NA");
					String urlBack = mPrefs.getString( MyHomeControl.deviceNames[MyHomeControl.secBackIndex],"NA");
					try {
						InetAddress tcpServer = InetAddress.getByName(urlFront);
						Socket tcpSocket = new Socket(tcpServer, TCP_CLIENT_PORT);

						tcpSocket.setSoTimeout(1000);
						PrintWriter out = new PrintWriter(new BufferedWriter(
								new OutputStreamWriter(tcpSocket.getOutputStream())), true);
						out.println("b");
						tcpSocket.close();
					} catch (Exception e) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
								+ " " + urlFront);
					}
					try {
						InetAddress tcpServer = InetAddress.getByName(urlBack);
						Socket tcpSocket = new Socket(tcpServer, TCP_CLIENT_PORT);

						tcpSocket.setSoTimeout(1000);
						PrintWriter out = new PrintWriter(new BufferedWriter(
								new OutputStreamWriter(tcpSocket.getOutputStream())), true);
						out.println("b");
						tcpSocket.close();
					} catch (Exception e) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
								+ " " + urlBack);
					}
				}

				SecLightWidget.updateAppWidget(this, appWidgetManager, mAppWidgetId, true);
				/** Timer to change back widget icon */
				Timer timer = new Timer();
				timer.schedule(new changeBackWidget(this, appWidgetManager, mAppWidgetId), 120000);
			}
		}
	}

	/**
	 * Class to switch back the widget icon after 2 minutes
	 */
	private class changeBackWidget extends TimerTask
	{
		/** Application context */
		final Context context;
		/** Instance of app widget manager */
		final AppWidgetManager appWidgetManager;
		/** ID of widget */
		final int appWidgetId;

		changeBackWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
			this.context = context;
			this.appWidgetManager = appWidgetManager;
			this.appWidgetId = appWidgetId;
		}

		@Override
		public void run() {
			SecLightWidget.updateAppWidget(context, appWidgetManager, appWidgetId, false);
		}
	}
}
