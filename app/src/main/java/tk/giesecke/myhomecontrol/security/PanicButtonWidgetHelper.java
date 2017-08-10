package tk.giesecke.myhomecontrol.security;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.MyHomeControl;
import tk.giesecke.myhomecontrol.Utilities;
import tk.giesecke.myhomecontrol.aircon.AirconWidgetHelper;

import static tk.giesecke.myhomecontrol.MyHomeControl.sharedPrefName;
import static tk.giesecke.myhomecontrol.MessageListener.TCP_CLIENT_PORT;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class PanicButtonWidgetHelper extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-LIGHT-W";

	public PanicButtonWidgetHelper() {
		super("PanicButtonWidgetHelper");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			// If we are not on home WiFi, send command to MQTT broker
			if (!Utilities.isHomeWiFi(getApplicationContext())) {
				String mqttTopic = "{\"ip\":\"sf1\","; // Device IP address
				mqttTopic += "\"cm\":\"p\"}"; // The command
				AirconWidgetHelper.doPublish(mqttTopic, getApplicationContext());

				mqttTopic = "{\"ip\":\"sb1\","; // Device IP address
				mqttTopic += "\"cm\":\"p\"}"; // The command
				AirconWidgetHelper.doPublish(mqttTopic, getApplicationContext());

				mqttTopic = "{\"ip\":\"ly1\","; // Device IP address
				mqttTopic += "\"cm\":\"p\"}"; // The command
				AirconWidgetHelper.doPublish(mqttTopic, getApplicationContext());
			} else {
				SharedPreferences mPrefs = getSharedPreferences(sharedPrefName,0);
				String urlFront = mPrefs.getString(
						MyHomeControl.deviceNames[MyHomeControl.secFrontIndex]
						,"NA");
				String urlBack = mPrefs.getString(
						MyHomeControl.deviceNames[MyHomeControl.secBackIndex]
						,"NA");
				String urlLights = mPrefs.getString(
						MyHomeControl.deviceNames[MyHomeControl.ly1Index]
						,"NA");
				try {
					InetAddress tcpServer = InetAddress.getByName(urlFront);
					Socket tcpSocket = new Socket(tcpServer, TCP_CLIENT_PORT);

					tcpSocket.setSoTimeout(1000);
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(tcpSocket.getOutputStream())), true);
					out.println("p");
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
					out.println("p");
					tcpSocket.close();
				} catch (Exception e) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
							+ " " + urlBack);
				}
				try {
					InetAddress tcpServer = InetAddress.getByName(urlLights);
					Socket tcpSocket = new Socket(tcpServer, TCP_CLIENT_PORT);

					tcpSocket.setSoTimeout(1000);
					PrintWriter out = new PrintWriter(new BufferedWriter(
							new OutputStreamWriter(tcpSocket.getOutputStream())), true);
					out.println("p");
					tcpSocket.close();
				} catch (Exception e) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
							+ " " + urlBack);
				}
			}
		}
	}
}
