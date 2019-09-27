package tk.giesecke.myhomecontrol.lights;

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

import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.Utilities;

import static tk.giesecke.myhomecontrol.MyHomeControl.deviceIPs;
import static tk.giesecke.myhomecontrol.MyHomeControl.lb1Index;
import static tk.giesecke.myhomecontrol.MyHomeControl.prefsLightBedDim;
import static tk.giesecke.myhomecontrol.MyHomeControl.sharedPrefName;
import static tk.giesecke.myhomecontrol.devices.MessageListener.TCP_CLIENT_PORT;

public class BedRoomLightWidgetDim extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-LB-W";

	public BedRoomLightWidgetDim() {
		super("BedRoomLightWidgetDim");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			/* Access to shared preferences of app widget */
			SharedPreferences mPrefs = this.getSharedPreferences(sharedPrefName, 0);
			int dimBrightness = mPrefs.getInt(prefsLightBedDim, 200);
			String cmd = "b=" + dimBrightness; // DIM
			lightCtrlSend(cmd);
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

//		String url = getString(R.string.LIGHTS_BEDROOM);
		String url = deviceIPs[lb1Index];
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
