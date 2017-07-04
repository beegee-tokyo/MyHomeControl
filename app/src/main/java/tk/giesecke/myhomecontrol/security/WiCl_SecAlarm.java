package tk.giesecke.myhomecontrol.security;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.aircon.WiCl_Aircon;

import static tk.giesecke.myhomecontrol.Cl_MyHomeControl.sharedPrefName;
import static tk.giesecke.myhomecontrol.Sv_MessageListener.TCP_CLIENT_PORT;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class WiCl_SecAlarm extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-SEC-W";

	public WiCl_SecAlarm() {
		super("WiCl_SecAlarm");
	}

	@SuppressLint("CommitPrefEdits")
	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				/* App widget id */
				int mAppWidgetId = extras.getInt(
						AppWidgetManager.EXTRA_APPWIDGET_ID,
						AppWidgetManager.INVALID_APPWIDGET_ID);

				boolean alarmIsActive = extras.getBoolean("AlarmStatus");

				// If they gave us an intent without the widget id, just bail.
				if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
					return;
				}

				/** Command string */
				String cmd;
				if (alarmIsActive) {
					cmd = "a=0"; // Disable alarm
				} else {
					cmd = "a=1"; // Enable alarm
				}

				// If we are not on home WiFi, send command to MQTT broker
				if (!tk.giesecke.myhomecontrol.Cl_Utilities.isHomeWiFi(getApplicationContext())) {
					String mqttTopic = "{\"ip\":\"sf1\","; // Device IP address
					mqttTopic += "\"cm\":\"" + cmd + "\"}"; // The command

					WiCl_Aircon.doPublish(mqttTopic, getApplicationContext());
					mqttTopic = "{\"ip\":\"sb1\","; // Device IP address
					mqttTopic += "\"cm\":\"" + cmd + "\"}"; // The command

					WiCl_Aircon.doPublish(mqttTopic, getApplicationContext());
				} else {
					SharedPreferences mPrefs = getSharedPreferences(sharedPrefName,0);
					String urlFront = mPrefs.getString( tk.giesecke.myhomecontrol.Cl_MyHomeControl.deviceNames[tk.giesecke.myhomecontrol.Cl_MyHomeControl.secFrontIndex],"NA");
					String urlBack = mPrefs.getString( tk.giesecke.myhomecontrol.Cl_MyHomeControl.deviceNames[tk.giesecke.myhomecontrol.Cl_MyHomeControl.secBackIndex],"NA");
					try {
						InetAddress tcpServer = InetAddress.getByName(urlFront);
						Socket tcpSocket = new Socket(tcpServer, TCP_CLIENT_PORT);

						tcpSocket.setSoTimeout(1000);
						PrintWriter out = new PrintWriter(new BufferedWriter(
								new OutputStreamWriter(tcpSocket.getOutputStream())), true);
						out.println(cmd);
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
						out.println(cmd);
						tcpSocket.close();
					} catch (Exception e) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
								+ " " + urlBack);
					}
				}
			}
		}
	}
}
