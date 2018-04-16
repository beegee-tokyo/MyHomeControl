package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import tk.giesecke.myhomecontrol.devices.MessageListener;

import static tk.giesecke.myhomecontrol.devices.MessageListener.currentSSID;

public class Utilities extends MyHomeControl {

	/**
	 * Check WiFi connection and return SSID
	 *
	 * @param thisAppContext
	 * 		application context
	 * @return <code>String</code>
	 * SSID name or empty string if not connected
	 */
	@SuppressWarnings("deprecation")
	public static Boolean isHomeWiFi(Context thisAppContext) {
		/** Access to connectivity manager */
		ConnectivityManager cm = (ConnectivityManager) thisAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		/** WiFi connection information  */
		NetworkInfo wifiOn;
		if (cm != null) {
			wifiOn = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (!wifiOn.isConnected()) {
				return false;
			} else {
				/** WiFi manager to check current connection */
				@SuppressLint("WifiManagerPotentialLeak")
				final WifiManager wifiManager = (WifiManager) thisAppContext.getSystemService(Context.WIFI_SERVICE);
				/** Info of current connection */
				final WifiInfo connectionInfo;
				if (wifiManager != null) {
					connectionInfo = wifiManager.getConnectionInfo();
					if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
						currentSSID = connectionInfo.getSSID();
						currentSSID = currentSSID.substring(1, currentSSID.length() - 1);
						String primLocalSSID = thisAppContext.getResources().getString(R.string.LOCAL_SSID);
						String altLocalSSID = thisAppContext.getResources().getString(R.string.ALT_LOCAL_SSID);

						int newIP = connectionInfo.getIpAddress();
						MessageListener.broadCastIP = Formatter.formatIpAddress(newIP);
						String[] splitIP = MessageListener.broadCastIP.split("\\.");
						if (splitIP.length == 4) {
							MessageListener.broadCastIP = splitIP[0]
											+ "." + splitIP[1]
											+ "." + splitIP[2]
											+ "." + "255";
						} else {
							MessageListener.broadCastIP = "192.168.0.255";
						}
						return ((currentSSID.equalsIgnoreCase(primLocalSSID)) || (currentSSID.equalsIgnoreCase(altLocalSSID)));
//				return currentSSID.equalsIgnoreCase(MY_LOCAL_SSID);
					}
				}
			}
		}

		return false;
	}

	/**
	 * Check if an internet connection is available
	 *
	 * @param thisAppContext
	 * 		Context of application
	 * @return <code>boolean[]</code>
	 * [0] True if we have WiFi connection
	 * [1] True if we have Mobile connection
	 * [2] True if we have Home WiFi connection
	 * [3] False if we do not have connection
	 */
	@SuppressWarnings("deprecation")
	public static boolean[] connectionAvailable(Context thisAppContext) {
		/** Flags for connections */
		boolean[] bConnFlags = {false, false, false, false};

		/** Access to connectivity manager */
		ConnectivityManager cm = (ConnectivityManager) thisAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);

		// Check if there is any network connected
		NetworkInfo activeNetwork;
		if (cm != null) {
			activeNetwork = cm.getActiveNetworkInfo();
			if (null != activeNetwork) {
				switch (activeNetwork.getType()) {
					case ConnectivityManager.TYPE_WIFI:
						// Active network is WiFi
						if (isHomeWiFi(thisAppContext)) {
							bConnFlags[2] = true;
						} else {
							bConnFlags[0] = true;
						}
						bConnFlags[3] = true;
						break;
					case ConnectivityManager.TYPE_MOBILE:
						// Active network is Mobile
						bConnFlags[1] = true;
						bConnFlags[3] = true;
						break;
					default:
						// No Active network found
						bConnFlags[3] = false;
						break;
				}
			} else {
				// No Active network found
				bConnFlags[3] = false;
			}
		}
		return bConnFlags;
	}

	/**
	 * Get data from security device status update
	 *
	 * @param jsonResult
	 * 		JSON object with the status received from the ESP8266
	 * @return <code>String message</code>
	 * String with the status in viewable format
	 */
	@SuppressLint("CommitPrefEdits")
	@SuppressWarnings("deprecation")
	public static String getDeviceStatus(JSONObject jsonResult,
	                                     Context thisAppContext,
	                                     ImageView ivAlarmStatusThis,
	                                     ImageView ivLightStatusThis,
	                                     TableLayout secBackViewThis,
	                                     CheckBox secAutoAlarmThis,
	                                     TextView secChangeAlarmThis) {
		/** Device ID */
		String deviceIDString;
		/** String with the device related status */
		String message = "";

		try {
			deviceIDString = jsonResult.getString("de");
		} catch (JSONException e) {
			deviceIDString = "unknown";
		}

		/** Flag for front or back sensor */
		boolean isFrontSensor = true;

		if (deviceIDString.equalsIgnoreCase("sb1")) {
			isFrontSensor = false;
			secBackViewThis.setVisibility(View.VISIBLE);		}

		try {
			if (jsonResult.getInt("al") == 1) {
				Log.d(DEBUG_LOG_TAG, "Alarm on from " + deviceIDString);
				Log.d(DEBUG_LOG_TAG, "JSON = " + jsonResult);
				message = "Intruder! from " + deviceIDString + "\n";
			} else {
				Log.d(DEBUG_LOG_TAG, "Alarm off from " + deviceIDString);
				Log.d(DEBUG_LOG_TAG, "JSON = " + jsonResult);
				message = "No detection at " + deviceIDString + "\n";
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.getInt("au") == 1) {
				if (isFrontSensor) {
					secAutoAlarmThis.setChecked(true);
					secAutoAlarmThis.setText(thisAppContext.getResources().getString(R.string.sec_auto_alarm_on,
							secAutoOn, secAutoOff));
					message += thisAppContext.getResources().getString(R.string.sec_auto_alarm_on,
							secAutoOn, secAutoOff) + "\n";
					hasAutoOnFront = true;
				} else {
					secAutoAlarmThis.setChecked(true);
					secAutoAlarmThis.setText(thisAppContext.getResources().getString(R.string.sec_auto_alarm_on,
							secAutoOn, secAutoOff));
					message += thisAppContext.getResources().getString(R.string.sec_auto_alarm_on,
							secAutoOn, secAutoOff) + "\n";
					hasAutoOnBack = true;
				}
			} else {
				if (isFrontSensor) {
					message += "Alarm auto activation off\n";
					secAutoAlarmThis.setChecked(false);
					secAutoAlarmThis.setText(thisAppContext.getResources().getString(R.string.sec_auto_alarm_off));
					message += thisAppContext.getResources().getString(R.string.sec_auto_alarm_off) + "\n";
					hasAutoOnFront = false;
				} else {
					message += "Alarm auto activation off\n";
					secAutoAlarmThis.setChecked(false);
					secAutoAlarmThis.setText(thisAppContext.getResources().getString(R.string.sec_auto_alarm_off));
					message += thisAppContext.getResources().getString(R.string.sec_auto_alarm_off) + "\n";
					hasAutoOnBack = false;
				}
			}
			if (secAutoAlarmThis.isChecked()) {
				secChangeAlarmThis.setVisibility(View.VISIBLE);
			} else {
				secChangeAlarmThis.setVisibility(View.INVISIBLE);
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.getInt("ao") == 1) {
				message += "Alarm active\n";
				ivAlarmStatusThis.setImageDrawable(thisAppContext.getResources().getDrawable(R.mipmap.ic_alarm_on));
				if (isFrontSensor) {
					hasAlarmOnFront = true;
				} else {
					hasAlarmOnBack = true;
				}
			} else {
				message += "Alarm not active\n";
				secAutoAlarmThis.isChecked();
				if (isFrontSensor) {
					hasAlarmOnFront = false;
					if (hasAutoOnFront) {
						ivAlarmStatusThis.setImageDrawable(thisAppContext.getResources().getDrawable(R.mipmap.ic_alarm_autooff));
					} else {
						ivAlarmStatusThis.setImageDrawable(thisAppContext.getResources().getDrawable(R.mipmap.ic_alarm_off));
					}
				} else {
					hasAlarmOnBack = false;
					if (hasAutoOnBack) {
						ivAlarmStatusThis.setImageDrawable(thisAppContext.getResources().getDrawable(R.mipmap.ic_alarm_autooff));
					} else {
						ivAlarmStatusThis.setImageDrawable(thisAppContext.getResources().getDrawable(R.mipmap.ic_alarm_off));
					}
				}
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.has("an")) {
				secAutoOnStored = jsonResult.getInt("an");
				if (secAutoOnStored < 12) {
					secAutoOn = Integer.toString(secAutoOnStored) + "am";
				} else {
					if (secAutoOnStored == 12) {
						secAutoOn = Integer.toString(secAutoOnStored) + "pm";
					} else {
						secAutoOn = Integer.toString(secAutoOnStored - 12) + "pm";
					}
				}
			} else {
				secAutoOn = "10pm";
				secAutoOnStored = 22;
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.has("af")) {
				secAutoOffStored = jsonResult.getInt("af");
				if (secAutoOffStored < 12) {
					secAutoOff = Integer.toString(secAutoOffStored) + "am";
				} else {
					if (secAutoOffStored == 12) {
						secAutoOff = Integer.toString(secAutoOffStored) + "pm";
					} else {
						secAutoOff = Integer.toString(secAutoOffStored - 12) + "pm";
					}
				}
			} else {
				secAutoOff = "8am";
				secAutoOffStored = 8;
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.getInt("lo") == 1) {
				message += "Light active\n";
				ivLightStatusThis.setImageDrawable(thisAppContext.getResources().getDrawable(R.mipmap.ic_light_on));
			} else {
				message += "Light not active\n";
				ivLightStatusThis.setImageDrawable(thisAppContext.getResources().getDrawable(R.mipmap.ic_light_autooff));
			}
		} catch (JSONException ignore) {
		}
		try {
			if (jsonResult.getInt("bo") != 0) {
				message += "Device restarted!\n";
			}
		} catch (JSONException ignore) {
		}
		try {
			message += "Signal = " + jsonResult.getInt("rs") + " dB\n";
		} catch (JSONException ignore) {
		}
		try {
			message += "Debug: " + jsonResult.getString("re") + "\n";
		} catch (JSONException ignore) {
		}

		return message;
	}

	/**
	 * Get light related status from the JSON object received from the ESP8266
	 *
	 * @param jsonResult
	 * 		JSON object with the status received from the ESP8266
	 * @return <code>String message</code>
	 * String with the status in viewable format
	 */
	public static String getLightStatus(JSONObject jsonResult) {
		/** Light value measured by TSL2561 connected to the ESP8266 */
		long lightValueLong;
		/** Light value measured by the LDR connected to the ESP8266 */
		int ldrValueInt;
		try {
			lightValueLong = jsonResult.getLong("lv");
		} catch (JSONException ignore) {
			lightValueLong = 0;
		}
		try {
			ldrValueInt = jsonResult.getInt("ld");
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
	 * @param thisAppContext
	 * 		application context
	 * @param notifNames
	 * 		array list to store the name of the tones
	 * @param notifUri
	 * 		array list to store the paths of the tones
	 * @return <code>int uriIndex</code>
	 * URI of user selected alarm sound
	 */
	public static int getNotifSounds(Context thisAppContext, ArrayList<String> notifNames, ArrayList<String> notifUri, boolean isSelAlarm) {
		/** Instance of the ringtone manager */
		RingtoneManager manager = new RingtoneManager(thisAppContext);
		manager.setType(RingtoneManager.TYPE_NOTIFICATION);
		/** Cursor with the notification tones */
		Cursor cursor = manager.getCursor();
		/** Access to shared preferences of application*/
		SharedPreferences mPrefs = thisAppContext.getSharedPreferences(sharedPrefName, 0);
		/** Last user selected alarm tone */
		String lastUri;
		if (isSelAlarm) {
			lastUri = mPrefs.getString(MyHomeControl.prefsSecurityAlarm, "");
		} else {
			lastUri = mPrefs.getString(MyHomeControl.prefsSolarWarning, "");
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
	 * Time as string HH:mm
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
	 * Date as integer values
	 * int[0] = year
	 * int[1] = month
	 * int[2] = day
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
	 * [0] current month as string yy-mm
	 * [1] last month as string yy-mm
	 */
	public static String[] getDateStrings() {
		/** Array with strings for this and last month date */
		String[] dateStrings = new String[2];
		/** Calendar to get current time and date */
		Calendar cal = Calendar.getInstance();
		/** Time format */
		@SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yy-MM");
		dateStrings[0] = df.format(cal.getTime());
		cal.add(Calendar.MONTH, -1);
		dateStrings[1] = df.format(cal.getTime());

		return dateStrings;
	}

	/**
	 * Check if JSON object is valid
	 *
	 * @param test
	 * 		String with JSON object or array
	 * @return boolean
	 * true if "test" us a JSON object or array
	 * false if no JSON object or array
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
	 *
	 * @param currPower
	 * 		power value as float
	 * @return <code>int</code>
	 * ID of matching icon
	 */
	public static int getNotifIcon(float currPower) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			if (currPower > 0.0d) {
				return R.drawable.tb_arrow_red_down_small;
			} else {
				return R.drawable.tb_arrow_green_up_small;
			}
		}

		if (currPower < -400) {
			return R.drawable.tb_m400;
		} else if (currPower < -350) {
			return R.drawable.tb_m350;
		} else if (currPower < -300) {
			return R.drawable.tb_m300;
		} else if (currPower < -250) {
			return R.drawable.tb_m250;
		} else if (currPower < -200) {
			return R.drawable.tb_m200;
		} else if (currPower < -150) {
			return R.drawable.tb_m150;
		} else if (currPower < -100) {
			return R.drawable.tb_m100;
		} else if (currPower < -50) {
			return R.drawable.tb_m050;
		} else if (currPower < 0) {
			return R.drawable.tb_m000;
		} else if (currPower < 50) {
			return R.drawable.tb_p000;
		} else if (currPower < 100) {
			return R.drawable.tb_p050;
		} else if (currPower < 150) {
			return R.drawable.tb_p100;
		} else if (currPower < 200) {
			return R.drawable.tb_p150;
		} else if (currPower < 250) {
			return R.drawable.tb_p200;
		} else if (currPower < 300) {
			return R.drawable.tb_p250;
		} else if (currPower < 350) {
			return R.drawable.tb_p300;
		} else if (currPower < 400) {
			return R.drawable.tb_p350;
		} else {
			return R.drawable.tb_p400;
		}
	}

	/**
	 * Highlight selected icon in dialog to set device name and icon
	 * all other icons will be shown normal
	 *
	 * @param selIconID
	 * 		index of icon that will be highlighted
	 */
	@SuppressWarnings("deprecation")
	public static void highlightDlgIcon(int selIconID, View locationsView, Context thisAppContext) {

		// deselect all buttons
		/** Image button in device change dialog used to deselect and highlight */
		ImageButton changeButton = (ImageButton) locationsView.findViewById(R.id.im_bath);
		changeButton.setBackgroundColor(thisAppContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_bed);
		changeButton.setBackgroundColor(thisAppContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_dining);
		changeButton.setBackgroundColor(thisAppContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_entertain);
		changeButton.setBackgroundColor(thisAppContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_kids);
		changeButton.setBackgroundColor(thisAppContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_kitchen);
		changeButton.setBackgroundColor(thisAppContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_living);
		changeButton.setBackgroundColor(thisAppContext.getResources().getColor(R.color.colorPrimary));
		changeButton = (ImageButton) locationsView.findViewById(R.id.im_office);
		changeButton.setBackgroundColor(thisAppContext.getResources().getColor(R.color.colorPrimary));
		switch (selIconID) {
			case R.id.im_bath:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_bath);
				changeButton.setBackgroundColor(thisAppContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_bed:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_bed);
				changeButton.setBackgroundColor(thisAppContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_dining:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_dining);
				changeButton.setBackgroundColor(thisAppContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_entertain:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_entertain);
				changeButton.setBackgroundColor(thisAppContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_kids:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_kids);
				changeButton.setBackgroundColor(thisAppContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_kitchen:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_kitchen);
				changeButton.setBackgroundColor(thisAppContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_living:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_living);
				changeButton.setBackgroundColor(thisAppContext.getResources().getColor(android.R.color.holo_green_light));
				break;
			case R.id.im_office:
				changeButton = (ImageButton) locationsView.findViewById(R.id.im_office);
				changeButton.setBackgroundColor(thisAppContext.getResources().getColor(android.R.color.holo_green_light));
				break;
		}
	}

	/**
	 * Returns the consumer friendly device name
	 */
	public static String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return capitalize(model);
		}
		return capitalize(manufacturer) + " " + model;
	}

	private static String capitalize(String str) {
		if (TextUtils.isEmpty(str)) {
			return str;
		}
		char[] arr = str.toCharArray();
		boolean capitalizeNext = true;
		StringBuilder phrase = new StringBuilder();
		for (char c : arr) {
			if (capitalizeNext && Character.isLetter(c)) {
				phrase.append(Character.toUpperCase(c));
				capitalizeNext = false;
				continue;
			} else if (Character.isWhitespace(c)) {
				capitalizeNext = true;
			}
			phrase.append(c);
		}
		return phrase.toString();
	}
}
