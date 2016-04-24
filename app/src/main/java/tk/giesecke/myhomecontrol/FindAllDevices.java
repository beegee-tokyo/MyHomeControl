package tk.giesecke.myhomecontrol;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

public class FindAllDevices extends AsyncTask<String, String, Void> {

	/** Debug tag */
	static final String DEBUG_LOG_TAG = "MHC-SEARCH";
	/** Application context */
	Context appContext;

	public FindAllDevices (Context context){
		appContext = context;
	}
	@Override
	protected Void doInBackground(String... params) {

		/** Array list to hold found IP addresses */
		ArrayList<String> hosts = new ArrayList<>();

		String subnet = appContext.getResources().getString(R.string.MY_LOCAL_SUB);
		for(int i=140; i<150; i++){ // Home automation devices IPs are from ...140 to ...149
			try {
				/** IP address under test */
				InetAddress inetAddress = InetAddress.getByName(subnet + String.valueOf(i));
				if(inetAddress.isReachable(300000)){
					hosts.add(inetAddress.getHostName());
					Log.d(DEBUG_LOG_TAG, inetAddress.getHostName());
				}
			} catch (IOException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Exception " + e);
			}
		}
		for (int i=0; i<hosts.size(); i++) {
			// SPmonitor, Security front, Aircon 1, Aircon 2, Aircon 3, Security back
			if (hosts.get(i).equalsIgnoreCase(MyHomeControl.SOLAR_URL.substring(7))) {
				MyHomeControl.deviceIsOn[MyHomeControl.spMonitorIndex] = true;
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.SECURITY_URL_FRONT_1.substring(7))) {
				MyHomeControl.deviceIsOn[MyHomeControl.secFrontIndex] = true;
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.SECURITY_URL_BACK_1.substring(7))) {
				MyHomeControl.deviceIsOn[MyHomeControl.secRearIndex] = true;
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.AIRCON_URL_1.substring(7))) {
				MyHomeControl.deviceIsOn[MyHomeControl.aircon1Index] = true;
				MyHomeControl.espIP[0] = MyHomeControl.AIRCON_URL_1.substring(7);
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.AIRCON_URL_2.substring(7))) {
				MyHomeControl.deviceIsOn[MyHomeControl.aircon2Index] = true;
				MyHomeControl.espIP[1] = MyHomeControl.AIRCON_URL_2.substring(7);
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.AIRCON_URL_3.substring(7))) {
				MyHomeControl.deviceIsOn[MyHomeControl.aircon3Index] = true;
				MyHomeControl.espIP[2] = MyHomeControl.AIRCON_URL_3.substring(7);
			}
		}
		return null;
	}
}
