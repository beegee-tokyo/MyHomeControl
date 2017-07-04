package tk.giesecke.myhomecontrol.devices;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.Sv_MessageListener;

import static tk.giesecke.myhomecontrol.Cl_MyHomeControl.deviceNames;

public class Sv_CheckAvailDevices extends Service {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG  = "MHC-NSD";
	/** Action for broadcast message to service */
	public static final String NSD_RESOLVED = "NSD_RESOLVED";
	/** Context of this application */
	private Context serviceContext;

	/** Helper to discover Home Control devices with mDNS/NSD */
	private Cl_NsdHelper mNsdHelper;
	/** Number of next mDNS/NDS service to resolve */
	private int nextNsdService;
	/** Access to activities shared preferences */
	private static SharedPreferences mPrefs;
	/* Name of shared preferences */
	private static final String sharedPrefName = "Cl_MyHomeControl";


//	public Sv_CheckAvailDevices() {
//	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {

		// Get context of the application to be reused in Async Tasks
		serviceContext = this;

		// Check if we are in the home WiFi
		if (!tk.giesecke.myhomecontrol.Cl_Utilities.isHomeWiFi(this)) {
			sendMyBroadcast(false); // Report failure
			stopSelf(); // Not on home WiFi, stop service
		}

		// Get pointer to shared preferences
		mPrefs = getSharedPreferences(sharedPrefName,0);

		// Register the receiver for messages from mDNS/NSD resolving
		// Create an intent filter to listen to the broadcast sent with the action "NSD_RESOLVED"
		/** Intent filter for app internal broadcast receiver */
		IntentFilter intentFilter = new IntentFilter(NSD_RESOLVED);
		//Map the intent filter to the receiver
		registerReceiver(nsdReceiver, intentFilter);

		/** Start discovery of mDNS/NSD services available */
		mNsdHelper = new Cl_NsdHelper(this);
		mNsdHelper.initializeDiscoveryListener();
		mNsdHelper.discoverServices();

		// Wait 10 seconds to finish discovery, then start to resolve mDNS/NSD devices
		final Handler resolveDevicesHandler = new Handler();
		resolveDevicesHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (Cl_NsdHelper.foundServices != 0) { // Found any mDNS/NSD devices?
					nextNsdService = 0;
					new resolveDevices().execute(nextNsdService); // Start resolving mDNS/NSD devices
				}
			}
		}, 10000);
	}

	/**
	 * Broadcast receiver for notifications received over UDP or MQTT or GCM
	 */
	private final BroadcastReceiver nsdReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			/** Message received over mDNS/NSD */
			String sender = intent.getStringExtra("from");
			Boolean result = intent.getBooleanExtra("resolved", false);
			if (sender.equalsIgnoreCase("NSD")) {
				int serviceNum = intent.getIntExtra("service",0);
				if (result) { // Resolved a mDNS/NSD device
					if (BuildConfig.DEBUG)
						Log.d(DEBUG_LOG_TAG, "Resolved " + Cl_NsdHelper.mServicesNames[serviceNum]
								+ " IP= " + Cl_NsdHelper.mServicesHosts[serviceNum].toString().substring(1)
								+ ":" + Cl_NsdHelper.mServicesPort[serviceNum]);
					for (String validDevice : deviceNames) {
						if (Cl_NsdHelper.mServicesNames[serviceNum].equalsIgnoreCase(validDevice)) {
							// Save IP's in the apps preferences
							mPrefs.edit().putString(Cl_NsdHelper.mServicesNames[serviceNum],
									Cl_NsdHelper.mServicesHosts[serviceNum].toString().substring(1)).apply();
							break;
						}
					}
					nextNsdService++;
					if (nextNsdService < Cl_NsdHelper.foundServices) {
						new resolveDevices().execute(nextNsdService);
					} else {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Resolving finished");
						mNsdHelper.stopDiscovery();
						unregisterReceiver(nsdReceiver);
						sendMyBroadcast(true);
						stopSelf();
					}
				} else {// else resolving stopped with an error. Further investigations why is needed!
					if (BuildConfig.DEBUG)
						Log.d(DEBUG_LOG_TAG, "Resolve failed " + Cl_NsdHelper.mServicesNames[serviceNum]);
					nextNsdService++;
					if (nextNsdService < Cl_NsdHelper.foundServices) {
						new resolveDevices().execute(nextNsdService);
					} else {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Resolving finished");
						mNsdHelper.stopDiscovery();
						unregisterReceiver(nsdReceiver);
						sendMyBroadcast(true);
						stopSelf();
					}
				}
			}
		}
	};

	/**
	 * Resolve the found device service to update the available devices list
	 */
	private class resolveDevices extends AsyncTask<Integer, Void, Void> {

		@Override
		protected Void doInBackground(Integer ... params) {

			int serviceNum = params[0];
			mNsdHelper.resolveService(Cl_NsdHelper.mServices.get(serviceNum));
			return null;
		}
	}

	/**
	 * Inform UI (if listening) that mDNS/NSD discovery is finished
	 *
	 */
	private void sendMyBroadcast(boolean resolveSuccess) {
		/** Intent for activity internal broadcast message */
		Intent broadCastIntent = new Intent();
		broadCastIntent.setAction(Sv_MessageListener.BROADCAST_RECEIVED);
		broadCastIntent.putExtra("from", "NSD");
		broadCastIntent.putExtra("resolved", resolveSuccess);
		serviceContext.sendBroadcast(broadCastIntent);
	}
}
