package tk.giesecke.myhomecontrol;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class PanicButtonClick extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-LIGHT-W";

	public PanicButtonClick() {
		super("PanicButtonClick");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			// If we are not on home WiFi, send command to MQTT broker
			if (!Utilities.isHomeWiFi(getApplicationContext())) {
				String mqttTopic = "{\"ip\":\"sf1\","; // Device IP address
				mqttTopic += "\"cm\":\"p\"}"; // The command

				AirconWidgetClick.doPublish(mqttTopic, getApplicationContext());
				mqttTopic = "{\"ip\":\"sb1\","; // Device IP address
				mqttTopic += "\"cm\":\"p\"}"; // The command

				AirconWidgetClick.doPublish(mqttTopic, getApplicationContext());
			} else {
				String urlFront = getApplicationContext().getResources().getString(R.string.SECURITY_URL_FRONT_1); // = "http://192.168.xxx.xx1";
				String urlBack = this.getResources().getString(R.string.SECURITY_URL_BACK_1); // = "http://192.168.xxx.xx4";
				try {
					InetAddress tcpServer = InetAddress.getByName(urlFront);
					Socket tcpSocket = new Socket(tcpServer, 6000);

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
					Socket tcpSocket = new Socket(tcpServer, 6000);

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
