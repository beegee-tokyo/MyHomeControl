package tk.giesecke.myhomecontrol.solar;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.os.StrictMode;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tk.giesecke.myhomecontrol.BuildConfig;
import tk.giesecke.myhomecontrol.MyHomeControl;
import tk.giesecke.myhomecontrol.Utilities;

import static tk.giesecke.myhomecontrol.MyHomeControl.sharedPrefName;
import static tk.giesecke.myhomecontrol.devices.MessageListener.BROADCAST_RECEIVED;

/*
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class SolarSyncDataBase extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-SYN";

	public SolarSyncDataBase() {
		super("SolarSyncDataBase");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			/* Context of application */
			Context intentContext = getApplicationContext();

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Background sync of database started");

			/* Access to shared preferences of app widget */
			SharedPreferences mPrefs = intentContext.getSharedPreferences(sharedPrefName, 0);

			// Try to sync only if we have connection and are on same WiFi as the spMonitor device
//			if (tk.giesecke.myhomecontrol.Utilities.isHomeWiFi(intentContext) && (!tk.giesecke.myhomecontrol.MyHomeControl.dataBaseIsEmpty)) {
			if (Utilities.isHomeWiFi(intentContext)) {
				StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
				StrictMode.setThreadPolicy(policy);

				// Get today's day for the online database name
				String[] dbNamesList = Utilities.getDateStrings();

				/* Instance of DataBaseHelper */
				DataBaseHelper dbHelper;
				/* Instance of data base */
				SQLiteDatabase dataBase;

				/* Flag for full sync */
				boolean syncAll = false;

				String syncedMonth = mPrefs.getString(MyHomeControl.prefsSolarSynced, "");

				// Check if the month changed
				if (!syncedMonth.equalsIgnoreCase(dbNamesList[0])) {
					// If month changed, force sync of both databases
					syncAll = true;
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Month change detected");
					deleteDatabase(DataBaseHelper.DATABASE_NAME);
					deleteDatabase(DataBaseHelper.DATABASE_NAME_LAST);
					mPrefs.edit().putString(MyHomeControl.prefsSolarSynced, dbNamesList[0]).apply();
					/* Instance of DataBaseHelper */
					dbHelper = new DataBaseHelper(this, DataBaseHelper.DATABASE_NAME);
					/* Instance of data base */
					dataBase = dbHelper.getReadableDatabase();
					dataBase.close();
					dbHelper.close();
					/* Instance of DataBaseHelper */
					dbHelper = new DataBaseHelper(this, DataBaseHelper.DATABASE_NAME_LAST);
					/* Instance of data base */
					dataBase = dbHelper.getReadableDatabase();
					dataBase.close();
					dbHelper.close();
				}

				// Sync this months database
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Sync this month: " + dbNamesList[0]);
				if (!syncDB(DataBaseHelper.DATABASE_NAME, dbNamesList[0], intentContext, syncAll)) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Sync this month failed");
					return;
				} else {
					sendMyBroadcast(dbNamesList[0]);
				}

				// Check if we have already synced the last month
				if (syncAll) {
					// Sync last months database
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Sync last month");
					if (!syncDB(DataBaseHelper.DATABASE_NAME_LAST, dbNamesList[1], intentContext, true)) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Sync last month failed");
						return;
//					} else {
//						sendMyBroadcast(dbNamesList[1]);
					}
				}
				// Update applications database list
				sendMyBroadcast(dbNamesList[1]);
			} else {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Sync stopped, wrong WiFi network");
			}
		}
	}

	/*
	 * Sync local database with the spMonitor database
	 *
	 * @param dbName
	 *        Name of database to be synced
	 * @param syncMonth
	 *        Month to be synced
	 * @param intentContext
	 *        Context of this intent
	 */
	private boolean syncDB(String dbName, String syncMonth, Context intentContext, boolean syncAll) {
		/* URL to be called */
		SharedPreferences mPrefs = getSharedPreferences(sharedPrefName,0);
		String solarURL = mPrefs.getString(
				MyHomeControl.deviceNames
						[MyHomeControl.spMonitorIndex],
				"NA");
		String urlString = "http://" + solarURL + "/sd/spMonitor/query2.php"; // URL to call
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"syncDB: " + MyHomeControl.deviceNames
				[MyHomeControl.spMonitorIndex]);
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"syncDB: " + mPrefs.getString(
				MyHomeControl.deviceNames
						[MyHomeControl.spMonitorIndex],
				"NA"));
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"syncDB: " + urlString);

		/* Instance of DataBaseHelper */
		DataBaseHelper dbHelper;
		/* Instance of data base */
		SQLiteDatabase dataBase;

		// Check if we need to sync all
		if (syncAll) {
			urlString += "?date=" + syncMonth;
		} else {
			/* Instance of DataBaseHelper */
			dbHelper = new DataBaseHelper(intentContext, dbName);
			try {
				/* Instance of data base */
				dataBase = dbHelper.getReadableDatabase();// Check for last entry in the local database
				dataBase.beginTransaction();
				/* Cursor with data from database */
				Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
				if (dbCursor != null) {
					if (dbCursor.getCount() != 0) { // local database not empty, need to sync only missing
						dbCursor.moveToFirst();
						dbCursor.moveToLast();

						int lastMinute = dbCursor.getInt(4);
						int lastHour = dbCursor.getInt(3);
						int lastDay = dbCursor.getInt(2);

						urlString += "?date=" + dbCursor.getString(0); // add year
						urlString += "-" + ("00" +
								dbCursor.getString(1)).substring(dbCursor.getString(1).length()); // add month
						urlString += "-" + ("00" +
								lastDay)
								.substring(String.valueOf(lastDay).length()); // add day
						urlString += "-" + ("00" +
								lastHour)
								.substring(String.valueOf(lastHour).length()); // add hour
						urlString += ":" + ("00" +
								lastMinute)
								.substring(String.valueOf(lastMinute).length()); // add minute
						urlString += "&get=all";
					} else { // local database is empty, need to sync all data
						//urlString += "?date=" + syncMonth + "&get=all"; // get all of this month
						syncAll = true;
						urlString += "?date=" + syncMonth;
					}
					dbCursor.close();
				} else { // something went wrong with the database access
					dataBase.endTransaction();
					dataBase.close();
					dbHelper.close();
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Database error");
					return false;
				}
				dataBase.endTransaction();
				dataBase.close();
				dbHelper.close();
			} catch (Exception e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Database error: " + e.getMessage());
				return true;
			}
		}

		/* Repeat counter used when full database needs to be synced */
		int loopCnt = 0;
		/* URL used for access */
		String thisURL = urlString;
		if (syncAll) {
			loopCnt = 3;
		}

		for (int loop = 0; loop <= loopCnt; loop++) {
			if (syncAll) {
				urlString = thisURL + "-" + loop;
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "URL = " + urlString);
			}
			getSPMData(urlString, intentContext, dbName);
		}
		return true;
	}

	/* Call SPM device to get requested data
	 */
	private void getSPMData(String urlString, Context intentContext, String dbName) {
		/* Request to spMonitor device */
		Request request = new Request.Builder()
				.url(urlString)
				.build();

		if (request != null) {
			/* String with data received from spMonitor device */
			String resultData = "";
			/* Instance of DataBaseHelper */
			DataBaseHelper dbHelper;
			/* Instance of data base */
			SQLiteDatabase dataBase;

			/* A HTTP client to access the SPM device */
			// Set timeout to 5 minutes in case we have a lot of data to load
			OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(300, TimeUnit.SECONDS)
					.writeTimeout(10, TimeUnit.SECONDS)
					.readTimeout(300, TimeUnit.SECONDS)
					.build();
			try {
				/* Response from spMonitor device */
				Response response = client.newCall(request).execute();
				if (response != null) {
					resultData = response.body().string();
				}
				try {
					if (!resultData.equalsIgnoreCase("")) {
						if (resultData.startsWith("[")) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Found valid JSON start");
							/* JSON array with the data received from spMonitor device */
							JSONArray jsonFromDevice = new JSONArray(resultData);
							/* Instance of DataBaseHelper */
							dbHelper = new DataBaseHelper(intentContext, dbName);
							/* Instance of data base */
							dataBase = dbHelper.getWritableDatabase();
							if (dataBase == null) {
								dbHelper.close();
							} else {
								// Is database in use?
								if (dataBase.inTransaction()) {
									if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Database is in use");
								} else {
									// Get received data into local database
									try {
										dataBase.beginTransaction();
										for (int i=0; i<jsonFromDevice.length(); i++) {
											// skip first data record from device if we are just updating the database
											if (i == 0) i++;
											/* JSONObject with a single record */
											JSONObject jsonRecord = jsonFromDevice.getJSONObject(i);
											String record = jsonRecord.getString("d");
											record = record.replace("-",",");
											record += ","+jsonRecord.getString("l");
											record += ","+jsonRecord.getString("s");
											record += ","+jsonRecord.getString("c");
											DataBaseHelper.addDay(dataBase, record);
										}
										dataBase.setTransactionSuccessful();
										dataBase.endTransaction();
										if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Sync successful for " + dbName);
									} catch (SQLiteDatabaseLockedException e) {
										if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Database locked"+e);
									}
								}
								dataBase.close();
								dbHelper.close();
							}
						} else {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Error returned: " + resultData);
						}
					}
				} catch (JSONException e) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Sync JSON error for " + dbName + resultData);
				}
			} catch (IOException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "IOException: "+e);
			}
		}
	}

	/*
	 * Send received message to all listing threads
	 *
	 * @param dataBase
	 *            Name of synced database
	 */
	private void sendMyBroadcast(String dataBase) {
		/* Intent for activity internal broadcast message */
		Intent broadCastIntent = new Intent();
		broadCastIntent.setAction(BROADCAST_RECEIVED);
		broadCastIntent.putExtra("from", "SPSYNC");
		broadCastIntent.putExtra("message", dataBase);
		sendBroadcast(broadCastIntent);
	}
}
