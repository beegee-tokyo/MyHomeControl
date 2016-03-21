package tk.giesecke.myhomecontrol;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
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

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SolarSyncDBService extends IntentService {

	/** Debug tag */
	static final String DEBUG_LOG_TAG = "MHC-SYN";

	public SolarSyncDBService() {
		super("SolarSyncDBService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			/** Context of application */
			Context intentContext = getApplicationContext();

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Background sync of database started");

			/** Access to shared preferences of app widget */
			SharedPreferences mPrefs = intentContext.getSharedPreferences(MyHomeControl.sharedPrefName, 0);

			// Try to sync only if we have connection and are on same WiFi as the spMonitor device
			if (Utilities.isHomeWiFi(intentContext)) {
				if (android.os.Build.VERSION.SDK_INT > 9) {
					StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
					StrictMode.setThreadPolicy(policy);
				}

				// Get today's day for the online database name
				String[] dbNamesList = Utilities.getDateStrings();

				/** Instance of DataBaseHelper */
				DataBaseHelper dbHelper;
				/** Instance of data base */
				SQLiteDatabase dataBase;

				String syncedMonth = mPrefs.getString(MyHomeControl.prefsSolarSynced, "");
				// Check if the month changed
				if (!syncedMonth.equalsIgnoreCase(dbNamesList[0])) {
					// If month changed, force sync of both databases
					deleteDatabase(DataBaseHelper.DATABASE_NAME);
					deleteDatabase(DataBaseHelper.DATABASE_NAME_LAST);
					mPrefs.edit().putString(MyHomeControl.prefsSolarSynced, dbNamesList[0]).apply();
					/** Instance of DataBaseHelper */
					dbHelper = new DataBaseHelper(this, DataBaseHelper.DATABASE_NAME);
					/** Instance of data base */
					dataBase = dbHelper.getReadableDatabase();
					dataBase.close();
					dbHelper.close();
					/** Instance of DataBaseHelper */
					dbHelper = new DataBaseHelper(this, DataBaseHelper.DATABASE_NAME_LAST);
					/** Instance of data base */
					dataBase = dbHelper.getReadableDatabase();
					dataBase.close();
					dbHelper.close();
				}

				// Sync this months database
				if (!syncDB(DataBaseHelper.DATABASE_NAME, dbNamesList[0], intentContext)) {
					return;
				}

				// Check if we have already synced the last month
				/** Instance of DataBaseHelper */
				dbHelper = new DataBaseHelper(intentContext, DataBaseHelper.DATABASE_NAME_LAST);
				/** Instance of data base */
				dataBase = dbHelper.getReadableDatabase();
				/** Cursor with data from database */
				Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
				if (dbCursor != null) {
					if (dbCursor.getCount() == 0) { // local database is empty, need to sync all data
						// Sync this months database
						if (!syncDB(DataBaseHelper.DATABASE_NAME_LAST, dbNamesList[1], intentContext)) {
							dataBase.close();
							dbHelper.close();
							return;
						}
					}
				}
				if (dbCursor != null) {
					dbCursor.close();
				}
				dataBase.close();
				dbHelper.close();
			}
		}
	}

	/**
	 * Sync local database with the spMonitor database
	 *
	 * @param dbName
	 *        Name of database to be synced
	 * @param syncMonth
	 *        Month to be synced
	 * @param intentContext
	 *        Context of this intent
	 */
	private boolean syncDB(String dbName, String syncMonth, Context intentContext) {
		/** String with data received from spMonitor device */
		String resultData = "";
		/** A HTTP client to access the ESP device */
		// Set timeout to 5 minutes in case we have a lot of data to load
		OkHttpClient client = new OkHttpClient.Builder()
				.connectTimeout(300, TimeUnit.SECONDS)
				.writeTimeout(10, TimeUnit.SECONDS)
				.readTimeout(300, TimeUnit.SECONDS)
				.build();

		/** URL to be called */
		String urlString = MyHomeControl.SOLAR_URL + "/sd/spMonitor/query2.php"; // URL to call

		// Check for last entry in the local database
		/** Instance of DataBaseHelper */
		DataBaseHelper dbHelper = new DataBaseHelper(intentContext, dbName);
		/** Instance of data base */
		SQLiteDatabase dataBase = dbHelper.getReadableDatabase();
		dataBase.beginTransaction();
		/** Cursor with data from database */
		Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
		/** Flag for database access type */
		boolean splitAccess = false;
		if (dbCursor != null) {
			if (dbCursor.getCount() != 0) { // local database not empty, need to sync only missing
				dbCursor.moveToFirst();

				int lastMinute =  dbCursor.getInt(4);
				int lastHour = dbCursor.getInt(3);
				int lastDay = dbCursor.getInt(2);

				urlString += "?date=" + dbCursor.getString(0); // add year
				urlString += "-" + ("00" +
						dbCursor.getString(1)).substring(dbCursor.getString(1).length()); // add month
				urlString += "-" + ("00" +
						String.valueOf(lastDay))
						.substring(String.valueOf(lastDay).length()); // add day
				urlString += "-" + ("00" +
						String.valueOf(lastHour))
						.substring(String.valueOf(lastHour).length()); // add hour
				urlString += ":" + ("00" +
						String.valueOf(lastMinute))
						.substring(String.valueOf(lastMinute).length()); // add minute
				urlString += "&get=all";
			} else { // local database is empty, need to sync all data
				//urlString += "?date=" + syncMonth + "&get=all"; // get all of this month
				splitAccess = true;
				urlString += "?date=" + syncMonth;
			}
			dbCursor.close();
		} else { // something went wrong with the database access
			dataBase.endTransaction();
			dataBase.close();
			dbHelper.close();
		}
		dataBase.endTransaction();
		dataBase.close();
		dbHelper.close();

		/** Repeat counter used when full database needs to be synced */
		int loopCnt = 0;
		/** URL used for access */
		String thisURL = urlString;
		if (splitAccess) {
			loopCnt = 3;
		}

		for (int loop = 0; loop <= loopCnt; loop++) {
			if (splitAccess) {
				urlString = thisURL + "-" + String.valueOf(loop);
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "URL = " + urlString);
			}
			/** Request to spMonitor device */
			Request request = new Request.Builder()
					.url(urlString)
					.build();

			if (request != null) {
				try {
					/** Response from spMonitor device */
					Response response = client.newCall(request).execute();
					if (response != null) {
						resultData = response.body().string();
					}
				} catch (IOException ignore) {
				}
				try {
					/** JSON array with the data received from spMonitor device */
					JSONArray jsonFromDevice = new JSONArray(resultData);
					/** Instance of DataBaseHelper */
					dbHelper = new DataBaseHelper(intentContext, dbName);
					/** Instance of data base */

					dataBase = dbHelper.getWritableDatabase();
					if (dataBase == null) {
						dbHelper.close();
						return false;
					}
					// Is database in use?
					if (dataBase.inTransaction()) {
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Database is in use");
						dataBase.close();
						dbHelper.close();
						return false;
					}
					// Get received data into local database
					try {
						dataBase.beginTransaction();
					} catch (SQLiteDatabaseLockedException e) {
						dataBase.close();
						dbHelper.close();
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Database locked"+e);
						return false;
					}
					for (int i=0; i<jsonFromDevice.length(); i++) {
						// skip first data record from device if we are just updating the database
						if (i == 0 && !splitAccess) i++;
						/** JSONObject with a single record */
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
				} catch (JSONException e) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"Sync JSON error for " + dbName + resultData);
					dataBase.endTransaction();
					dataBase.close();
					dbHelper.close();
					return false;
				}
				dataBase.close();
				dbHelper.close();
			}
		}
		return true;
	}
}
