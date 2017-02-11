package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class LightCtrlWidgetClick extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-LB-W";

	public LightCtrlWidgetClick() {
		super("LightCtrlWidgetClick");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			/** Access to shared preferences of app widget */
			SharedPreferences mPrefs = this.getSharedPreferences(MyHomeControl.sharedPrefName, 0);
			int dimBrightness = mPrefs.getInt(MyHomeControl.prefsLightBedDim, 200);
			final String action = intent.getAction();
			String cmd;
			switch (action) {
				case "1":
					cmd = "b=140";
					break;
				case "2":
					cmd = "b=" + Integer.toString(dimBrightness);
					break;
				case "3":
					cmd = "b=255";
					break;
				default:
					return;
			}
			lightCtrlSend(cmd);
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Send light control: " + cmd);
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

		String url = MyHomeControl.urlLB1;
		try {
			InetAddress tcpServer = InetAddress.getByName(url);
			Socket tcpSocket = new Socket(tcpServer, 6000);

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
