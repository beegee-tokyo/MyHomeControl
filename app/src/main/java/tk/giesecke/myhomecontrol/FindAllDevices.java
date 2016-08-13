package tk.giesecke.myhomecontrol;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

class FindAllDevices extends AsyncTask<String, String, Void> {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-SEARCH";
	/** Application context */
	private final Context appContext;

	public FindAllDevices (Context context){
		appContext = context;
	}
	@Override
	protected Void doInBackground(String... params) {

		/** Array list to hold found IP addresses */
		ArrayList<String> hosts = new ArrayList<>();

		/** String with subnet to search */
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
			if (hosts.get(i).equalsIgnoreCase(MyHomeControl.SOLAR_URL.substring(7))
					&& !MyHomeControl.deviceIsOn[MyHomeControl.spMonitorIndex]) {
				MyHomeControl.deviceIsOn[MyHomeControl.spMonitorIndex] = true;
				sendBC("spm");
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.SECURITY_URL_FRONT_1.substring(7))
					&& !MyHomeControl.deviceIsOn[MyHomeControl.secFrontIndex]) {
				MyHomeControl.deviceIsOn[MyHomeControl.secFrontIndex] = true;
				sendBC("sf1");
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.SECURITY_URL_BACK_1.substring(7))
					&& !MyHomeControl.deviceIsOn[MyHomeControl.secBackIndex]) {
				MyHomeControl.deviceIsOn[MyHomeControl.secBackIndex] = true;
				sendBC("sb1");
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.AIRCON_URL_1.substring(7))
					&& !MyHomeControl.deviceIsOn[MyHomeControl.aircon1Index]) {
				MyHomeControl.deviceIsOn[MyHomeControl.aircon1Index] = true;
				MyHomeControl.espIP[0] = MyHomeControl.AIRCON_URL_1.substring(7);
				sendBC("fd1");
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.AIRCON_URL_2.substring(7))
					&& !MyHomeControl.deviceIsOn[MyHomeControl.aircon2Index]) {
				MyHomeControl.deviceIsOn[MyHomeControl.aircon2Index] = true;
				MyHomeControl.espIP[1] = MyHomeControl.AIRCON_URL_2.substring(7);
				sendBC("ca1");
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.AIRCON_URL_3.substring(7))
					&& !MyHomeControl.deviceIsOn[MyHomeControl.aircon3Index]) {
				MyHomeControl.deviceIsOn[MyHomeControl.aircon3Index] = true;
				MyHomeControl.espIP[2] = MyHomeControl.AIRCON_URL_3.substring(7);
				sendBC("xy1");
			}
		}
		return null;
	}
	/**
	 * Send broadcast to main thread to start initialization of device
	 */
	private void sendBC(final String deviceFound) {
		/** Intent for activity internal broadcast message */
		Intent broadCastIntent = new Intent();
		broadCastIntent.setAction(MessageListener.BROADCAST_RECEIVED);
		broadCastIntent.putExtra("from", "search");
		broadCastIntent.putExtra("message", deviceFound);
		appContext.sendBroadcast(broadCastIntent);
	}
}
