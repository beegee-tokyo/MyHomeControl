package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class SecurityWidgetClick extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-SEC-W";

	public SecurityWidgetClick() {
		super("SecurityWidgetClick");
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
				if (!Utilities.isHomeWiFi(getApplicationContext())) {
					String mqttTopic = "{\"ip\":\"sf1\","; // Device IP address
					mqttTopic += "\"cm\":\"" + cmd + "\"}"; // The command

					AirconWidgetClick.doPublish(mqttTopic, getApplicationContext());
					mqttTopic = "{\"ip\":\"sb1\","; // Device IP address
					mqttTopic += "\"cm\":\"" + cmd + "\"}"; // The command

					AirconWidgetClick.doPublish(mqttTopic, getApplicationContext());
				} else {
					try {
						InetAddress tcpServer = InetAddress.getByName(MyHomeControl.SECURITY_URL_FRONT_1.substring(7));
						Socket tcpSocket = new Socket(tcpServer, 6000);

						tcpSocket.setSoTimeout(1000);
						PrintWriter out = new PrintWriter(new BufferedWriter(
								new OutputStreamWriter(tcpSocket.getOutputStream())), true);
						out.println(cmd);
						tcpSocket.close();
					} catch (Exception e) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
								+ " " + MyHomeControl.SECURITY_URL_FRONT_1.substring(7));
					}
					try {
						InetAddress tcpServer = InetAddress.getByName(MyHomeControl.SECURITY_URL_BACK_1.substring(7));
						Socket tcpSocket = new Socket(tcpServer, 6000);

						tcpSocket.setSoTimeout(1000);
						PrintWriter out = new PrintWriter(new BufferedWriter(
								new OutputStreamWriter(tcpSocket.getOutputStream())), true);
						out.println(cmd);
						tcpSocket.close();
					} catch (Exception e) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
								+ " " + MyHomeControl.SECURITY_URL_BACK_1.substring(7));
					}
				}
			}
		}
	}
}
