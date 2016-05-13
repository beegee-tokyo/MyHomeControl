package tk.giesecke.myhomecontrol;

import android.app.IntentService;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class GCMIntentService extends IntentService {

	@SuppressWarnings("FieldCanBeLocal")
	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-GCM";

	public GCMIntentService() {
		super(GCMIntentService.class.getName());
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		/** Bundle with extras received when Intent is called */
		Bundle extras = intent.getExtras();
		if (!extras.isEmpty()) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Got GCM = " + extras.toString());

			// Check if screen is locked
			/** Keyguard manager instance */
			KeyguardManager myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
			/** Flag for locked phone */
			boolean phoneIsLocked = myKM.inKeyguardRestrictedInputMode();
			// Check if screen is off
			/** Instance of power manager */
			PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
			/** Flag for screen off */
			boolean screenIsOff = true;
			if (Build.VERSION.SDK_INT >= 20) {
				if (powerManager.isInteractive()) {
					screenIsOff = false;
				}
			}
			else {
				//noinspection deprecation
				if(powerManager.isScreenOn()){
					screenIsOff = false;
				}
			}
			// Check if we are connected to the home WiFi
			/** Flag for local Wifi */
			boolean notOnHomeWifi = !Utilities.isHomeWiFi(getApplicationContext());

			// If we are not on home Wifi or screen is off or locked => process the message
			if (notOnHomeWifi || phoneIsLocked || screenIsOff) {
				if (extras.containsKey("message")) {
					// read extras as sent from server
					/** Message received as GCM push notification  */
					String message = extras.getString("message");
					// Check if response is a JSON array
					if (Utilities.isJSONValid(message)) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Got JSON: " + message);
						/** JSON object to hold the received message */
						JSONObject jsonResult;
						try {
							jsonResult = new JSONObject(message);
							try {
								/** Device ID to check who sent the push notification */
								String broadCastDevice = jsonResult.getString("device");
								if (broadCastDevice.startsWith("sf")
										|| broadCastDevice.startsWith("sb")) { // Broadcast from security device
									// Show notification
									UDPlistener.alarmNotif(jsonResult, getApplicationContext());
								} else {
									if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Got message "+ jsonResult);
								}
								// Broadcast message inside the Android device
								sendGCMBroadcast(message);
							} catch (Exception ignore) {
							}
						} catch (JSONException e) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
						}
					} else { // Test message from
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Got GCM message " + message);
					}
				}
			}
		} else {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Got GCM without extras");
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GCMBroadcastReceiver.completeWakefulIntent(intent);
	}

	//send broadcast from activity to all receivers listening to the action "ACTION_STRING_ACTIVITY"
	private void sendGCMBroadcast(String msgReceived) {
		/** Intent for broadcast */
		Intent broadCastIntent = new Intent();
		broadCastIntent.setAction(UDPlistener.BROADCAST_RECEIVED);
		broadCastIntent.putExtra("message", msgReceived);
		sendBroadcast(broadCastIntent);
	}
}
