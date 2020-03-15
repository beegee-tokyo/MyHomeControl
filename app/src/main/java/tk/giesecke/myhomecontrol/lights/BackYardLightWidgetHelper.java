package tk.giesecke.myhomecontrol.lights;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.R;
import tk.giesecke.myhomecontrol.Utilities;

import static tk.giesecke.myhomecontrol.MyHomeControl.deviceIPs;
import static tk.giesecke.myhomecontrol.MyHomeControl.ly1Index;
import static tk.giesecke.myhomecontrol.devices.MessageListener.TCP_CLIENT_PORT;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class BackYardLightWidgetHelper extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-LIGHT-B";

	public BackYardLightWidgetHelper() {
		super("BackYardLightWidgetHelper");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();
			if (action != null) {
				String cmd;
				switch (action) {
					case "0":
						cmd = "l=0";
						break;
					case "1":
						cmd = "l=1";
						break;
					default:
						return;
				}
				lightCtrlSend(cmd);
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Send light control: " + cmd);
			}
		}
	}

	/**
	 * Send command to ESP
	 *
	 * @param cmd
	 * 		Command to be sent to ESP
	 */
	@SuppressLint("CommitPrefEdits")
	private void lightCtrlSend(String cmd) {
		// If we are not on home WiFi, return immediately
		if (!Utilities.isHomeWiFi(getApplicationContext())) {
			return;
		}

//		String url = getString(R.string.LIGHTS_BACKYARD);
		String url = deviceIPs[ly1Index];
		try {
			InetAddress tcpServer = InetAddress.getByName(url);
			Socket tcpSocket = new Socket(tcpServer, TCP_CLIENT_PORT);

			tcpSocket.setSoTimeout(1000);
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(tcpSocket.getOutputStream())), true);
			out.println(cmd);
			tcpSocket.close();
		} catch (Exception e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
					+ " " + url);
		}
	}
}
