package tk.giesecke.myhomecontrol;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class Utilities extends MyHomeControl {

	/**
	 * Check WiFi connection and return SSID
	 *
	 * @param context
	 *            application context
	 * @return <code>String</code>
	 *            SSID name or empty string if not connected
	 */
	@SuppressWarnings("deprecation")
	public static Boolean isHomeWiFi(Context context) {
		/** Access to connectivity manager */
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		/** WiFi connection information  */
		android.net.NetworkInfo wifiOn = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (!wifiOn.isConnected()) {
			return false;
		} else {
			/** WiFi manager to check current connection */
			final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			/** Info of current connection */
			final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
			if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
				String currentSSID = connectionInfo.getSSID();
				currentSSID = currentSSID.substring(1,currentSSID.length()-1);
				String MY_LOCAL_SSID = context.getResources().getString(R.string.MY_LOCAL_SSID);
				return currentSSID.equalsIgnoreCase(MY_LOCAL_SSID);
			}
		}
		return false;
	}

	/**
	 * Scan the local subnet and update the list of devices
	 */
	public static void findDevicesOnLAN(Context appContext){
		/** Array list to hold found IP addresses */
		ArrayList<String> hosts = new ArrayList<>();

		String subnet = appContext.getResources().getString(R.string.MY_LOCAL_SUB);
		for(int i=140; i<150; i++){ // Home automation devices IPs are from ...140 to ...149
			try {
				/** IP address under test */
				InetAddress inetAddress = InetAddress.getByName(subnet + String.valueOf(i));
				if(inetAddress.isReachable(500)){
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
				MyHomeControl.deviceIsOn[spMonitorIndex] = true;
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.SECURITY_URL_FRONT_1.substring(7))) {
				MyHomeControl.deviceIsOn[secFrontIndex] = true;
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.SECURITY_URL_BACK_1.substring(7))) {
				MyHomeControl.deviceIsOn[secRearIndex] = true;
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.AIRCON_URL_1.substring(7))) {
				MyHomeControl.deviceIsOn[aircon1Index] = true;
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.AIRCON_URL_2.substring(7))) {
				MyHomeControl.deviceIsOn[aircon2Index] = true;
			} else if (hosts.get(i).equalsIgnoreCase(MyHomeControl.AIRCON_URL_3.substring(7))) {
				MyHomeControl.deviceIsOn[aircon3Index] = true;
			}
		}
	}

	/**
	 * Start or stop timer for updates
	 * If connection is same LAN as spMonitor device then update is every 60 seconds
	 * else the update is every 5 minutes
	 *
	 * @param context
	 *            application context
	 * @param isStart
	 *            flag if timer should be started or stopped
	 */
	public static void startStopUpdates(Context context, boolean isStart) {

		/** Pending intent for broadcast message to update notifications */
		PendingIntent pendingIntent;
		/** Alarm manager for scheduled updates */
		AlarmManager alarmManager;

		/** Update interval 1 minute */
		int alarmTime = 60000;

		// Stop the update
		pendingIntent = PendingIntent.getService(context, 5001,
				new Intent(context, SolarUpdateService.class),PendingIntent.FLAG_CANCEL_CURRENT);

		/** Alarm manager for scheduled updates */
		alarmManager = (AlarmManager) context.getSystemService
				(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);

		if (isStart) {
			if (!isHomeWiFi(context)) {
				/** Change update interval to 5 minutes if we are not on Wifi */
				alarmTime = 300000;
			}
			/** Pending intent for notification updates */
			pendingIntent = PendingIntent.getService(context, 5001,
					new Intent(context, SolarUpdateService.class),PendingIntent.FLAG_UPDATE_CURRENT);
			/** Alarm manager for daily sync */
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			am.setRepeating(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis() + 10000,
					alarmTime, pendingIntent);
		}
	}

	/**
	 * Get list of all available notification tones
	 *
	 * @param jsonResult
	 *              JSON object with the status received from the ESP8266
	 * @return <code>String message</code>
	 *              String with the status in viewable format
	 */
	@SuppressLint("CommitPrefEdits")
	@SuppressWarnings("deprecation")
	public static String getDeviceStatus(JSONObject jsonResult) {
		/** Device ID */
		String deviceIDString;
		/** String with the device related status */
		String message = "";

		try {
			deviceIDString = jsonResult.getString("device");
		} catch (JSONException e) {
			deviceIDString = "unknown";
		}

		/** ImageView to show status of alarm enabled */
		ImageView ivAlarmStatusThis = ivAlarmStatus;
		/** ImageView to show status of light enabled */
		ImageView ivLightStatusThis = ivLightStatus;
		/** ImageView to show active alarm */
		ImageView ivAlarmOnThis = ivAlarmOn;
		/** Animator to make blinking active alarm ImageView */
		ValueAnimator animatorThis = animator;
		/** Flag for front or rear sensor */
		boolean isFrontSensor = true;

		if (deviceIDString.equalsIgnoreCase("sb1")) {
			ivAlarmStatusThis = ivAlarmStatusBack;
			ivLightStatusThis = ivLightStatusBack;
			ivAlarmOnThis = ivAlarmOnBack;
			animatorThis = animatorBack;
			isFrontSensor = false;
		}

		try {
			if (jsonResult.getInt("alarm") == 1) {
				Log.d(DEBUG_LOG_TAG, "Alarm on from " + deviceIDString);
				Log.d(DEBUG_LOG_TAG, "JSON = " + jsonResult);
				message = "Intruder! from " + deviceIDString + "\n";
				animatorThis.start();
			} else {
				Log.d(DEBUG_LOG_TAG, "Alarm off from " + deviceIDString);
				Log.d(DEBUG_LOG_TAG, "JSON = " + jsonResult);
				message = "No detection at " + deviceIDString + "\n";
				animatorThis.end();
				ivAlarmOnThis.setAlpha(0f);
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.getInt("alarm_on") == 1) {
				message += "Alarm active\n";
				ivAlarmStatusThis.setImageDrawable(appContext.getResources().getDrawable(R.mipmap.ic_sec_widget_on));
				if (isFrontSensor) {
					hasAlarmOnFront = true;
				} else {
					hasAlarmOnBack = true;
				}
				mPrefs.edit().putBoolean(MyHomeControl.prefsSecurityAlarmOn, true).commit();
			} else {
				message += "Alarm not active\n";
				ivAlarmStatusThis.setImageDrawable(appContext.getResources().getDrawable(R.mipmap.ic_sec_widget_off));
				if (isFrontSensor) {
					hasAlarmOnFront = false;
				} else {
					hasAlarmOnBack = false;
				}
				mPrefs.edit().putBoolean(MyHomeControl.prefsSecurityAlarmOn, false).commit();
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.has("auto_on")) {
				secAutoOnStored = jsonResult.getInt("auto_on");
				if (secAutoOnStored < 12) {
					secAutoOn = Integer.toString(secAutoOnStored) + "am";
				} else {
					if (secAutoOnStored == 12) {
						secAutoOn = Integer.toString(secAutoOnStored) + "pm";
					} else {
						secAutoOn = Integer.toString(secAutoOnStored-12) + "pm";
					}
				}
			} else {
				secAutoOn = "10pm";
				secAutoOnStored = 22;
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.has("auto_off")) {
				secAutoOffStored = jsonResult.getInt("auto_off");
				if (secAutoOffStored < 12) {
					secAutoOff = Integer.toString(secAutoOffStored) + "am";
				} else {
					if (secAutoOffStored == 12) {
						secAutoOff = Integer.toString(secAutoOffStored) + "pm";
					} else {
						secAutoOff = Integer.toString(secAutoOffStored-12) + "pm";
					}
				}
			} else {
				secAutoOff = "8am";
				secAutoOffStored = 8;
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.getInt("auto") == 1) {
				secAutoAlarm.setChecked(true);
				secAutoAlarm.setText(appContext.getResources().getString(R.string.sec_auto_alarm_on,
						secAutoOn, secAutoOff));
				message += appContext.getResources().getString(R.string.sec_auto_alarm_on,
						secAutoOn, secAutoOff) + "\n";
				secChangeAlarm.setVisibility(View.VISIBLE);
			} else {
				message += "Alarm auto activation off\n";
				secAutoAlarm.setChecked(false);
				secAutoAlarm.setText(appContext.getResources().getString(R.string.sec_auto_alarm_off));
				message += appContext.getResources().getString(R.string.sec_auto_alarm_off) + "\n";
				secChangeAlarm.setVisibility(View.INVISIBLE);
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.getInt("light_on") == 1) {
				message += "Light active\n";
				ivLightStatusThis.setImageDrawable(appContext.getResources().getDrawable(R.mipmap.ic_light_on));
			} else {
				message += "Light not active\n";
				ivLightStatusThis.setImageDrawable(appContext.getResources().getDrawable(R.mipmap.ic_light_off));
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.getInt("boot") != 0) {
				message += "Device restarted!\n";
			}
		} catch (JSONException ignore) {
		}
		try {
			message += "Signal = " + jsonResult.getInt("rssi") + " dB\n";
		} catch (JSONException ignore) {
		}
		try {
			message += "Debug: " + jsonResult.getString("reboot") + "\n";
		} catch (JSONException ignore) {
		}

		return message;
	}

	/**
	 * Get light related status from the JSON object received from the ESP8266
	 *
	 * @param jsonResult
	 *              JSON object with the status received from the ESP8266
	 * @return <code>String message</code>
	 *              String with the status in viewable format
	 */
	public static String getLightStatus(JSONObject jsonResult) {
		/** Light value measured by TSL2561 connected to the ESP8266 */
		long lightValueLong;
		/** Light value measured by the LDR connected to the ESP8266 */
		int ldrValueInt;
		try {
			lightValueLong = jsonResult.getLong("light_val");
		} catch (JSONException ignore) {
			lightValueLong = 0;
		}
		try {
			ldrValueInt = jsonResult.getInt("ldr_val");
		} catch (JSONException ignore) {
			ldrValueInt = 0;
		}
		/** String with the light related status */
		String message = "";
		if (lightValueLong != 0) {
			message += "Light = " + lightValueLong + " lux\n";
		}
		if (ldrValueInt != 0) {
			message += "LDR = " + ldrValueInt + "\n";
		}
		return message;
	}

	/**
	 * Get list of all available notification tones
	 *
	 * @param context
	 *              application context
	 * @param notifNames
	 *              array list to store the name of the tones
	 * @param notifUri
	 *              array list to store the paths of the tones
	 * @return <code>int uriIndex</code>
	 *              URI of user selected alarm sound
	 */
	public static int getNotifSounds(Context context, ArrayList<String> notifNames, ArrayList<String> notifUri, boolean isSelAlarm) {
		/** Instance of the ringtone manager */
		RingtoneManager manager = new RingtoneManager(context);
		manager.setType(RingtoneManager.TYPE_NOTIFICATION);
		/** Cursor with the notification tones */
		Cursor cursor = manager.getCursor();
		/** Access to shared preferences of application*/
		SharedPreferences mPrefs = context.getSharedPreferences(sharedPrefName, 0);
		/** Last user selected alarm tone */
		String lastUri;
		if (isSelAlarm) {
			lastUri = mPrefs.getString(MyHomeControl.prefsSecurityAlarm,"");
		} else {
			lastUri = mPrefs.getString(MyHomeControl.prefsSolarWarning,"");
		}
		/** Index of lastUri in the list */
		int uriIndex = -1;

		while (cursor.moveToNext()) {
			notifNames.add(cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX));
			notifUri.add(cursor.getString(RingtoneManager.URI_COLUMN_INDEX) + "/" +
					cursor.getString(RingtoneManager.ID_COLUMN_INDEX));
			if (lastUri.equalsIgnoreCase(cursor.getString(RingtoneManager.URI_COLUMN_INDEX) + "/" +
					cursor.getString(RingtoneManager.ID_COLUMN_INDEX))) {
				uriIndex = cursor.getPosition();
			}
		}
		return uriIndex;
	}

	/**
	 * Get current time as string
	 *
	 * @return <code>String</code>
	 *            Time as string HH:mm
	 */
	public static String getCurrentTime() {
		/** Calendar to get current time and date */
		Calendar cal = Calendar.getInstance();
		/** Time format */
		@SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("HH:mm");
		return df.format(cal.getTime());
	}

	/**
	 * Get current date as integer
	 *
	 * @return <code>int[]</code>
	 *            Date as integer values
	 *            int[0] = year
	 *            int[1] = month
	 *            int[2] = day
	 */
	public static int[] getCurrentDate() {
		/** Integer array for return values */
		int[] currTime = new int[3];
		/** Calendar to get current time and date */
		Calendar cal = Calendar.getInstance();

		/** Today's month */
		currTime[1] = cal.get(Calendar.MONTH) + 1;

		/** Today's year */
		currTime[0] = cal.get(Calendar.YEAR);

		/** Today's day */
		currTime[2] = cal.get(Calendar.DATE);

		return currTime;
	}

	/**
	 * Get current month and last month as string
	 *
	 * @return <code>String[]</code>
	 *            [0] current month as string yy-mm
	 *            [1] last month as string yy-mm
	 */
	public static String[] getDateStrings() {
		/** Array with strings for this and last month date */
		String[] dateStrings = new String[2];
		/** Calendar to get current time and date */
		Calendar cal = Calendar.getInstance();
		/** Time format */
		@SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yy-MM");
		dateStrings[0] = df.format(cal.getTime());
		cal.add(Calendar.MONTH,-1);
		dateStrings[1] = df.format(cal.getTime());

		return dateStrings;
	}

	/**
	 * Check if JSON object is valid
	 *
	 * @param test
	 *            String with JSON object or array
	 * @return boolean
	 *            true if "test" us a JSON object or array
	 *            false if no JSON object or array
	 */
	public static boolean isJSONValid(String test) {
		try {
			new JSONObject(test);
		} catch (JSONException ex) {
			try {
				new JSONArray(test);
			} catch (JSONException ex1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return notification icon ID as int
	 * @param currPower
	 *          power value as float
	 * @return <code>int</code>
	 *          ID of matching icon
	 */
	public static int getNotifIcon(float currPower) {
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			if (currPower > 0.0d) {
				return R.drawable.arrow_red_down_small;
			} else {
				return R.drawable.arrow_green_up_small;
			}
		}

		if (currPower < -400) {
			return R.drawable.m400;
		} else if (currPower < -350) {
			return R.drawable.m350;
		} else if (currPower < -300) {
			return R.drawable.m300;
		} else if (currPower < -250) {
			return R.drawable.m250;
		} else if (currPower < -200) {
			return R.drawable.m200;
		} else if (currPower < -150) {
			return R.drawable.m150;
		} else if (currPower < -100) {
			return R.drawable.m100;
		} else if (currPower < -50) {
			return R.drawable.m50;
		} else if (currPower < 0){
			return R.drawable.m0;
		} else if (currPower < 50) {
			return R.drawable.p0;
		} else if (currPower < 100) {
			return R.drawable.p50;
		} else if (currPower < 150) {
			return R.drawable.p100;
		} else if (currPower < 200) {
			return R.drawable.p150;
		} else if (currPower < 250) {
			return R.drawable.p200;
		} else if (currPower < 300) {
			return R.drawable.p250;
		} else if (currPower < 350) {
			return R.drawable.p300;
		} else if (currPower < 400) {
			return R.drawable.p350;
		} else {
			return R.drawable.p400;
		}
	}

	/**
	 * Highlight selected icon in dialog to set device name and icon
	 * all other icons will be shown normal
	 *
	 * @param selIconID
	 *            index of icon that will be highlighted
	 */
	@SuppressWarnings("deprecation")
	public static void highlightDlgIcon(int selIconID, View locationsView) {
		// deselect all buttons
		/** Image button in device change dialog used to deselect and highlight */
		ImageButton changeButton = (ImageButton) locationsView.findViewById(R.id.im_bath);
		changeButton.setBackgroundColor(appContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_bed);
		changeButton.setBackgroundColor(appContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_dining);
		changeButton.setBackgroundColor(appContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_entertain);
		changeButton.setBackgroundColor(appContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_kids);
		changeButton.setBackgroundColor(appContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_kitchen);
		changeButton.setBackgroundColor(appContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_living);
		changeButton.setBackgroundColor(appContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_office);
		changeButton.setBackgroundColor(appContext.getResources().getColor(R.color.colorPrimary));
		switch (selIconID) {
			case R.id.im_bath:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_bath);
				changeButton.setBackgroundColor(appContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_bed:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_bed);
				changeButton.setBackgroundColor(appContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_dining:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_dining);
				changeButton.setBackgroundColor(appContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_entertain:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_entertain);
				changeButton.setBackgroundColor(appContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_kids:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_kids);
				changeButton.setBackgroundColor(appContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_kitchen:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_kitchen);
				changeButton.setBackgroundColor(appContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_living:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_living);
				changeButton.setBackgroundColor(appContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_office:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_office);
				changeButton.setBackgroundColor(appContext.getResources().getColor(android.R.color.holo_green_light));
				break;
		}
	}
}
