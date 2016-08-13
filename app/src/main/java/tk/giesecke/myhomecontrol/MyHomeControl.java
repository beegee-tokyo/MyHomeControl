package tk.giesecke.myhomecontrol;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyHomeControl extends AppCompatActivity implements View.OnClickListener
		, AdapterView.OnItemClickListener {

	/** Debug tag */
	static final String DEBUG_LOG_TAG = "MHC-MAIN";

	/** Access to activities shared preferences */
	static SharedPreferences mPrefs;
	/* Name of shared preferences */
	public static final String sharedPrefName = "MyHomeControl";
	/** Context of this application */
	static Context appContext;
	/** Id of menu, needed to set user selected icons and device names */
	private Menu abMenu;
	/** Visible view 0 = security, 1 = solar panel, 2 = aircon */
	private int visibleView = 0;
	/** Flag for debug output */
	private boolean showDebug = false;
	/** The view of the main UI */
	static View appView;

	/** Shared preferences value for last shown view */
	private final String prefsLastView = "lastView";
	/** Shared preferences value for show debug messages flag */
	private final String prefsShowDebug = "showDebug";

	/** Shared preferences value security alarm sound */
	public static final String prefsSecurityAlarm = "secAlarm";
	/** Shared preferences value security alarm sound */
	static final String prefsSecurityAlarmOn = "secAlarmOn";

	/** Shared preferences value for solar alarm sound */
	public static final String prefsSolarWarning = "solarAlarm";
	/** Shared preferences value for last synced month */
	public static final String prefsSolarSynced = "solarLastSynced";
	/** Shared preferences value for number solar widgets placed */
	public static final String prefsSolarWidgetNum = "solarWidgetNum";
	/** Shared preferences value for large widget */
	public static final String prefsSolarWidgetSize = "solarWidgetSizeLarge";

	/** Shared preferences value for last selected device */
	private final String prefsSelDevice = "airconSelDevice";
	/** Shared preferences value for show debug messages flag */
	private final String prefsLocationName = "airconLocation";
	/** Shared preferences value for show debug messages flag */
	private final String prefsDeviceIcon = "airconIcon";

	/** View for selecting device to change icon and device name */
	private View locationSettingsView;
	/** View of aircon device name and icon change dialog */
	private View airconDialogView;
	/** Button ids from location selection dialog */
	private final int[] buttonIds = {
			R.id.dia_sel_device0,
			R.id.dia_sel_device1,
			R.id.dia_sel_device2};
	/** Resource ids of drawables for the icons */
	private final int[] iconIDs = {R.drawable.ic_bathroom,
			R.drawable.ic_bedroom,
			R.drawable.ic_dining,
			R.drawable.ic_entertainment,
			R.drawable.ic_kids,
			R.drawable.ic_kitchen,
			R.drawable.ic_livingroom,
			R.drawable.ic_office};
	/* Resource ids of the icon buttons */
	private final int[] iconButtons = {R.id.im_bath,
			R.id.im_bed,
			R.id.im_dining,
			R.id.im_entertain,
			R.id.im_kids,
			R.id.im_kitchen,
			R.id.im_living,
			R.id.im_office};
	/** Index of device handled in dialog box */
	private int dlgDeviceIndex;
	/** R.id of selected icon for a device */
	private int dlgIconIndex;

	/** Flag for sound selector (true = security alarm, false = solar panel warning) */
	private boolean isSelAlarm = true;

	/** List of potential control device availability */
	// SPmonitor, Security front, Aircon 1, Aircon 2, Aircon 3, Security back
	static final boolean[] deviceIsOn = {false, false, false, false, false, false};
	/** deviceIsOn index for SPmonitor */
	static final int spMonitorIndex = 0;
	/** deviceIsOn index for Security front */
	static final int secFrontIndex = 1;
	/** deviceIsOn index for Aircon 1 */
	static final int aircon1Index = 2;
	/** deviceIsOn index for Aircon 2 */
	public static final int aircon2Index = 3;
	/** deviceIsOn index for Aircon 3 */
	public static final int aircon3Index = 4;
	/** deviceIsOn index for Security back */
	public static final int secBackIndex = 5;

	/** URL to spMonitor ESP8266*/
	static String SOLAR_URL;
	/** URL to Front Security ESP8266*/
	public static String SECURITY_URL_FRONT_1;
	/** URL to Back Security ESP8266*/
	public static String SECURITY_URL_BACK_1;
	/** URL to FujiDenzo aircon ESP8266*/
	public static String AIRCON_URL_1;
	/** URL to Carrier aircon ESP8266*/
	public static String AIRCON_URL_2;
	/** URL to another aircon ESP8266*/
	public static String AIRCON_URL_3;

	// Security view related
	/** Security view */
	private RelativeLayout secView = null;
	/** TextView for status message display in security view */
	private static TextView secStatus;
	/** ImageView to show status of alarm enabled front sensor */
	static ImageView ivAlarmStatus;
	/** ImageView to show status of light enabled front sensor */
	static ImageView ivLightStatus;
	/** ImageView to show active alarm front sensor */
	private static ImageView ivAlarmOn;
	/** Animator to make blinking active alarm ImageView front sensor */
	private static ValueAnimator animator;
	/** TableLayout to show status of back alarm system */
	static TableLayout secBackView;
	/** ImageView to show status of alarm enabled back sensor */
	static ImageView ivAlarmStatusBack;
	/** ImageView to show status of light enabled back sensor */
	static ImageView ivLightStatusBack;
	/** ImageView to show active alarm back sensor */
	private static ImageView ivAlarmOnBack;
	/** Animator to make blinking active alarm ImageView back sensor */
	private static ValueAnimator animatorBack;
	/** Check box for auto activation of alarm */
	static CheckBox secAutoAlarm;
	/** Clickable text view to change activation times */
	static TextView secChangeAlarm;

	/** Status flag for alarm front sensor */
	static boolean hasAlarmOnFront = true;
	/** Status flag for alarm back sensor */
	static boolean hasAlarmOnBack = true;

	/** Auto activation on time as string */
	static String secAutoOn;
	/** Auto activation off time as string */
	static String secAutoOff;
	/** Auto activation on time as integer */
	static int secAutoOnStored;
	/** Auto activation off time as integer */
	static int secAutoOffStored;

	/** Array list with available alarm names */
	private ArrayList<String> notifNames = new ArrayList<>();
	/** Array list with available alarm uri's */
	private ArrayList<String> notifUri = new ArrayList<>();
	/** Selected alarm name */
	private String notifNameSel = "";
	/** Selected alarm uri */
	private String notifUriSel = "";

	// Solar monitor view related
	/** Solar panel view */
	private RelativeLayout solView = null;
	/** TextView for status message display in solar panel view */
	private static TextView solStatus;
	/** Array with existing log dates on the Arduino */
	private static final List<String> logDates = new ArrayList<>();
	/** Pointer to current displayed log in logDates array */
	private static int logDatesIndex = 0;
	/** Array with existing log dates on the Arduino */
	private static final List<String> lastLogDates = new ArrayList<>();
	/** Pointer to current displayed log in logDates array */
	private static int lastLogDatesIndex = 0;
	/** Flag for showing last month */
	private static boolean showingLast = false;
	/** Flag for showing a log */
	static boolean showingLog = false;
	// Flag for database empty */
	static boolean dataBaseIsEmpty = true;

	/** Instance of DataBaseHelper for this month*/
	private DataBaseHelper dbHelperNow;
	/** Instance of DataBaseHelper for last month*/
	private DataBaseHelper dbHelperLast;

	/** Today's year-month database name */
	private static String[] dbNamesList = new String[2];
	/** Flag for last month update request */
	private static boolean needLastMonth = false;

	/** AsyncTask for updating current month database */
	private static AsyncTask atNow;
	/** AsyncTask for updating last month database */
	private static AsyncTask atLast;

	// Aircon view related
	/** Aircon control view */
	private RelativeLayout airView = null;
	/** FujiDenzo control view */
	private RelativeLayout airFDView = null;
	/** Carrier control view */
	private RelativeLayout airCAView = null;
	/** TextView for status message display in aircon view */
	private static TextView airStatus;
	/** Light of button to switch consumption control for FujiDenzo layout */
	private static View btAutoLightFD;
	/** Light of button to switch consumption control for Carrier layout */
	private static View btAutoLightCA;
	/** Light of button to switch on/off for FujiDenzo layout */
	private static View btOnOffLightFD;
	/** Light of button to switch on/off for Carrier layout */
	private static View btOnOffLightCA;
	/** Light of button to switch fan to high speed for FujiDenzo layout */
	private static View btFanHighLightFD;
	/** Light of button to switch fan to medium speed for FujiDenzo layout */
	private static View btFanMedLightFD;
	/** Light of button to switch fan to low speed for FujiDenzo layout */
	private static View btFanLowLightFD;
	/** Light of button to switch to cool mode for FujiDenzo layout */
	private static View btCoolLightFD;
	/** Light of button to switch to cool mode for Carrier layout */
	private static View btCoolLightCA;
	/** Light of button to switch to dry mode for FujiDenzo layout */
	private static View btDryLightFD;
	/** Light of button to switch to dry mode for Carrier layout */
	private static View btDryLightCA;
	/** Light of button to switch to fan mode for FujiDenzo layout */
	private static View btFanLightFD;
	/** Light of button to switch to fan mode for Carrier layout */
	private static View btFanLightCA;
	/** Light of button to switch on sweep for Carrier layout */
	private static View btSweepLightCA;
	/** Light of button to switch on turbo mode for Carrier layout */
	private static View btTurboLightCA;
	/** Light of button to switch on ion mode for Carrier layout */
	private static View btIonLightCA;
	/** Light of button to switch on auto temp function for Carrier layout */
	private static View btAutomLightCA;
	/** Light of button to switch to timer function for FujiDenzo layout */
	/* Timer function is not supported atm
	View static btTimerLightFD;
	*/
	/** Light of button to switch to timer function for Carrier layout */
	/* Timer function is not supported atm
	View static btTimerLightCA;
	*/
	/** Button to switch fan speed for Carrier layout */
	private static Button btFanCA;
	/** Consumption value display for FujiDenzo layout */
	private static TextView txtConsValFD;
	/** Temperature value display for FujiDenzo layout */
	private static TextView txtTempValFD;
	/** Status value display for FujiDenzo layout */
	private static TextView txtAutoStatusValFD;
	/** Consumption value display for Carrier layout */
	private static TextView txtConsValCA;
	/** Temperature value display for Carrier layout */
	private static TextView txtTempValCA;
	/** Status value display for Carrier layout */
	private static TextView txtAutoStatusValCA;

	/** Timer button for FujiDenzo layout */
	private static Button btTimerFD;
	/** Timer button for Carrier layout */
	private static Button btTimerCA;

	/** Color for activated button */
	private static int colorRed;
	/** Color for deactivated button */
	private static int colorGrey;
	/** Color for deactivated timer button */
	private static int colorGreen;
	/** Color for activated timer button */
	private static int colorOrange;

	/** ID of the selected device */
	private static int selDevice = 0;
	/** IP address of the selected device */
	public static final String[] espIP = {"192.168.0.142",
			"192.168.0.143",
			"192.168.0.144",
			"192.168.0.145",
			"192.168.0.146",
			"192.168.0.147",
			"192.168.0.148",
			"192.168.0.149"};
	/** Name of the device */
	private static final String[] deviceName = {"", "", "", "", "", "", "", ""};
	/** Layout version for the device */
	private static final int[] deviceType = {99, 99, 99, 99, 99, 99, 99, 99};
	/** Valid device type ids */
	private static final int FUJIDENZO = 0;
	private static final int CARRIER = 1;
	private static final int OTHER_AIRCON = 2;
	/** Location of the device */
	private final String[] locationName = {"Office", "Living", "Bedroom", "", "", "", "", "Test"};
	/** Icon for the device */
	private final int[] deviceIcon = {7, 6, 1, 0, 0, 0, 0, 3};
	/** Fan speed of device */
	private static final int[] fanStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Mode status of device */
	private static final int[] modeStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Power status of device */
	private static final int[] powerStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Cooling temperature of device */
	private static final int[] coolStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Timer setting of device */
	private static final int[] deviceTimer = {1, 1, 1, 1, 1, 1, 1, 1};
	/** Consumption status of device (only from master device */
	private static double consStatus = 0;
	/** Auto power status of device (only from master device */
	private static int autoStatus = 0;
	/** Auto power enabled status of device */
	private static final int[] autoOnStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Sweep enabled status of device */
	private static final int[] sweepStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Turbo enabled status of device */
	private static final int[] turboStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Ion enabled status of device */
	private static final int[] ionStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Timer on status of device */
	private static final int[] timerStatus = {0, 0, 0, 0, 0, 0, 0, 0};

	private static final String CMD_ON_OFF = "00";

	private static final String CMD_MODE_AUTO = "10";
	private static final String CMD_MODE_COOL = "11";
	private static final String CMD_MODE_DRY = "12";
	private static final String CMD_MODE_FAN = "13";

	private static final String CMD_FAN_HIGH = "20";
	private static final String CMD_FAN_MED = "21";
	private static final String CMD_FAN_LOW = "22";
	private static final String CMD_FAN_SPEED = "23";

	private static final String CMD_TEMP_PLUS = "30";
	private static final String CMD_TEMP_MINUS = "31";

	public static final String CMD_OTHER_TIMER = "40";
	private static final String CMD_OTHER_SWEEP = "41";
	private static final String CMD_OTHER_TURBO = "42";
	private static final String CMD_OTHER_ION = "43";

	private static final String CMD_AUTO_ON = "98";
	private static final String CMD_AUTO_OFF = "99";

	private static String mqttUser;
	private static String mqttPw;


	@Override
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_home_control);
		/** Instance of the tool bar */
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// Get pointer to shared preferences
		mPrefs = getSharedPreferences(sharedPrefName,0);

		// Get context of the application to be reused in Async Tasks
		appContext = this;

		// Enable access to internet
		if (Build.VERSION.SDK_INT > 9) {
			/** ThreadPolicy to get permission to access internet */
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		// Initialize variables for buttons, layouts, views, ...
		setGlobalVar();

		// In case the database is not yet existing, open it once
		// Open databases
		dbHelperNow = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
		dbHelperLast = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);

		/** Instance of database */
		SQLiteDatabase dataBase = dbHelperNow.getReadableDatabase();
		dataBase.beginTransaction();
		dataBase.endTransaction();
		dataBase.close();
		/** Instance of data base */
		dataBase = dbHelperLast.getReadableDatabase();
		dataBase.beginTransaction();
		dataBase.endTransaction();
		dataBase.close();

		if (Utilities.isHomeWiFi(this)) {
			secStatus.setText(getResources().getString(R.string.at_home));
			airStatus.setText(getResources().getString(R.string.at_home));
			solStatus.setText(getResources().getString(R.string.at_home));
		} else {
			secStatus.setText(getResources().getString(R.string.not_home));
			airStatus.setText(getResources().getString(R.string.not_home));
			solStatus.setText(getResources().getString(R.string.not_home));
		}

		// Register the receiver for messages from UDP & GCM listener
		// Create an intent filter to listen to the broadcast sent with the action "BROADCAST_RECEIVED"
		/** Intent filter for app internal broadcast receiver */
		IntentFilter intentFilter = new IntentFilter(MessageListener.BROADCAST_RECEIVED);
		//Map the intent filter to the receiver
		registerReceiver(activityReceiver, intentFilter);
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);

		// set the intent passed from the service to the original intent
		setIntent(intent);
	}

	/**
	 * Called when activity is getting visible
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();

		Intent i = getIntent();
		Bundle b = i.getExtras();
		if (b != null) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart with extra: " + b.getInt("view", 9));
			visibleView = b.getInt("view", visibleView);
		}

		// Get pointer to shared preferences
		mPrefs = getSharedPreferences(sharedPrefName,0);

		// Scan for available devices
		new checkMainDevices().execute();

		// Get the layouts of all three possible views
		secView = (RelativeLayout) findViewById(R.id.view_security);
		solView = (RelativeLayout) findViewById(R.id.view_solar);
		airView = (RelativeLayout) findViewById(R.id.view_aircon);

		// Setup views
		switch (visibleView) {
			case 0: // Security
				switchUI(0);
				break;
			case 1: // Solar panel
				switchUI(1);
				break;
			case 2: // Aircon
				switchUI(2);
				break;
		}

		// Setup aircon views
		switch (selDevice) {
			case 0:
				airCAView.setVisibility(View.INVISIBLE);
				airFDView.setVisibility(View.VISIBLE);
				break;
			case 1:
				airFDView.setVisibility(View.INVISIBLE);
				airCAView.setVisibility(View.VISIBLE);
				break;
			case 2:
				airCAView.setVisibility(View.INVISIBLE);
				airFDView.setVisibility(View.VISIBLE);
				break;
		}

		// Open databases
		dbHelperNow = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
		dbHelperLast = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);

		if (!isMyServiceRunning(MessageListener.class)) {
			// Start background services
			startService(new Intent(this, StartBackgroundServices.class));
		}

		if (Utilities.isHomeWiFi(this)) {
			// Start searching for other devices with a delay of 30 seconds
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					new FindAllDevices(getApplicationContext()).execute();
				}
			}, 30000);
		}
	}

	/**
	 * Called when activity is getting invisible
	 */
	@Override
	protected void onPause() {
		super.onPause();

		// Check if async tasks with database access are still running
		if(atNow != null && atNow.getStatus() == AsyncTask.Status.RUNNING)
			atNow.cancel(false);
		if(atLast != null && atLast.getStatus() == AsyncTask.Status.RUNNING)
			atLast.cancel(false);

		// Close databases
		dbHelperNow.close();
		dbHelperLast.close();

		animator.end();
		ivAlarmOn.setAlpha(0f);
		animatorBack.end();
		ivAlarmOnBack.setAlpha(0f);
	}

	/**
	 * Called when activity is getting destroyed
	 * Handles security fragment specific tasks
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unregister the receiver for messages from UDP listener
		unregisterReceiver(activityReceiver);
		activityReceiver = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_my_home_control, menu);
		abMenu = menu;

		MenuItem menuItem = abMenu.getItem(8); // Debug menu entry
		if (showDebug) {
			menuItem.setTitle(R.string.action_debug_off);
		} else {
			menuItem.setTitle(R.string.action_debug);
		}
		return true;
	}

	@SuppressLint("InflateParams")
	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/** Menu item pointer */
		MenuItem menuItem;

		switch (item.getItemId()) {
			case R.id.action_close:
				finish();
				break;
			case R.id.action_selWarning:
			case R.id.action_selAlarm:
				isSelAlarm = item.getItemId() != R.id.action_selWarning;
				notifNames = new ArrayList<>();
				notifUri = new ArrayList<>();
				notifNames.add(getString(R.string.no_alarm_sel));
				notifUri.add("");
				notifNames.add(getString(R.string.snd_alarm));
				notifUri.add("android.resource://"
						+ this.getPackageName() + "/"
						+ R.raw.alarm);
				notifNames.add(getString(R.string.snd_alert));
				notifUri.add("android.resource://"
						+ this.getPackageName() + "/"
						+ R.raw.alert);
				notifNames.add(getString(R.string.snd_dog));
				notifUri.add("android.resource://"
						+ this.getPackageName() + "/"
						+ R.raw.dog);
				/** Index of last user selected alarm tone */
				int uriIndex = Utilities.getNotifSounds(this, notifNames, notifUri, isSelAlarm) + 2;

				// get sound_selector.xml view
				/** Layout inflater for sound selection dialog */
				LayoutInflater alarmDialogInflater = LayoutInflater.from(this);
				/** View for sound selection dialog */
				@SuppressLint("InflateParams")
				View alarmSettingsView = alarmDialogInflater.inflate(R.layout.sound_selector, null);
				/** Alert dialog builder for device selection dialog */
				AlertDialog.Builder alarmDialogBuilder = new AlertDialog.Builder(this);

				// set sound_selector.xml to alert dialog builder
				alarmDialogBuilder.setView(alarmSettingsView);

				// set dialog message
				alarmDialogBuilder
						.setTitle(getResources().getString(R.string.action_selAlarm))
						.setCancelable(false)
						.setNegativeButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										if (!notifNameSel.equalsIgnoreCase("")) {
											if (isSelAlarm) {
												mPrefs.edit().putString(prefsSecurityAlarm, notifUriSel).apply();
											} else {
												mPrefs.edit().putString(prefsSolarWarning, notifUriSel).apply();
											}
										}
										dialog.cancel();
									}
								});

				// create alert dialog
				/** Alert dialog  for device selection */
				AlertDialog alarmDialog = alarmDialogBuilder.create();

				// show it
				alarmDialog.show();

				/** Pointer to list view with the alarms */
				ListView lvAlarmList = (ListView) alarmSettingsView.findViewById(R.id.lv_AlarmList);
				/** Array adapter for the ListView */
				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
						appContext,
						R.layout.my_list_item,
						notifNames );
				lvAlarmList.setAdapter(arrayAdapter);
				// Use long click listener to play the alarm sound
				lvAlarmList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
					public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					                               int pos, long id) {
						/** Instance of media player */
						MediaPlayer mMediaPlayer = new MediaPlayer();
						try {
							mMediaPlayer.setDataSource(appContext, Uri.parse(notifUri.get(pos)));
							/** Audio manager to play the sound */
							final AudioManager audioManager = (AudioManager) appContext
									.getSystemService(Context.AUDIO_SERVICE);
							if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
								mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
								mMediaPlayer.prepare();
								mMediaPlayer.start();
							}
						} catch (IOException e) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Cannot play alarm");
						}
						return true;
					}
				});
				lvAlarmList.setOnItemClickListener(this);
				lvAlarmList.setItemChecked(uriIndex, true);
				lvAlarmList.setSelection(uriIndex);
				break;
			case R.id.action_security:
				// Show security UI
				switchUI(0);
				break;
			case R.id.action_solar:
				// Show solar panel UI
				switchUI(1);
				break;
			case R.id.action_aircon:
				// Show aircon UI
				switchUI(2);
				break;
			case R.id.action_refresh:
				if (Utilities.isHomeWiFi(this)) {
					new checkMainDevices().execute();
				} else {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_home), Toast.LENGTH_LONG).show();
				}
				break;
			case R.id.action_debug:
				showDebug = !showDebug;
				mPrefs.edit().putBoolean(prefsShowDebug,showDebug).apply();
				menuItem = abMenu.getItem(8); // Debug menu entry
				if (showDebug) {
					menuItem.setTitle(R.string.action_debug_off);
				} else {
					menuItem.setTitle(R.string.action_debug);
				}
				break;
			case R.id.action_locations:
				// get location_selector.xml view
				/** Layout inflater for device selection dialog */
				LayoutInflater locationDialogInflater = LayoutInflater.from(this);
				locationSettingsView = locationDialogInflater.inflate(R.layout.location_selector, null);
				/** Alert dialog builder for device selection dialog */
				AlertDialog.Builder locationDialogBuilder = new AlertDialog.Builder(this);

				// set location_selector.xml to alert dialog builder
				locationDialogBuilder.setView(locationSettingsView);

				/** Pointer to button, used to set OnClickListener for buttons in the dialog */
				Button btToSetOnClickListener;
				/** Pointer to button text, used to give each button in the dialog a specific name */
				String buttonTxt;

				for (int i = 0; i < 3; i++) {
					if (deviceIsOn[i+2]) {
						btToSetOnClickListener = (Button) locationSettingsView.findViewById(buttonIds[i]);
						btToSetOnClickListener.setVisibility(View.VISIBLE);
						if (locationName[i].equalsIgnoreCase("")) {
							btToSetOnClickListener.setText(deviceName[i]);
						} else {
							buttonTxt = locationName[i];
							btToSetOnClickListener.setText(buttonTxt);
						}
						btToSetOnClickListener.setOnClickListener(this);
					}
				}

				// set dialog message
				locationDialogBuilder
						.setTitle(getResources().getString(R.string.dialog_selector_title))
						.setCancelable(false)
						.setNegativeButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										for (int i = 0; i < 3; i++) {
											mPrefs.edit().putString(
													prefsLocationName + Integer.toString(i),
													locationName[i]).apply();
											mPrefs.edit().putInt(
													prefsDeviceIcon + Integer.toString(i),
													deviceIcon[i]).apply();
										}
										dialog.cancel();
									}
								});

				// create alert dialog
				/** Alert dialog  for device selection */
				AlertDialog alertDialog = locationDialogBuilder.create();

				// show it
				alertDialog.show();

				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Listener for clicks in ListView for alarm sound selection
	 *
	 * @param parent
	 *              AdapterView of alert dialog
	 * @param view
	 *              View of ListView
	 * @param position
	 *              Position in ListView that has been clicked
	 * @param id
	 *              ID of item in ListView that has been clicked
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		notifNameSel = notifNames.get(position);
		notifUriSel = notifUri.get(position);
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v
	 * 		The view that was clicked.
	 */
	@SuppressLint("InflateParams")
	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {

		if (!handleSPMbuttons(v)) { // Check if it was a solar panel view button and handle it
			if (!handleSecurityButtons(v)) { // Check if it was a security view button and handle it
				if (!handleAirconButtons(v)) { // Check if it was a aircon view button and handle it
					// Handle other buttons right here
					switch (v.getId()) {
						case R.id.dia_sel_device0:
						case R.id.dia_sel_device1:
						case R.id.dia_sel_device2:
							switch (v.getId()) {
								case R.id.dia_sel_device0:
									dlgDeviceIndex = FUJIDENZO;
									break;
								case R.id.dia_sel_device1:
									dlgDeviceIndex = CARRIER;
									break;
								default:
									dlgDeviceIndex = OTHER_AIRCON;
									break;
							}
							// get location_selector.xml view
							/** Layout inflater for dialog to change device name and icon */
							LayoutInflater airconDialogInflater = LayoutInflater.from(this);
							/** View of aircon device name and icon change dialog */
							airconDialogView = airconDialogInflater.inflate(R.layout.locations, null);
							/** Alert dialog builder for dialog to change device name and icon */
							AlertDialog.Builder airconDialogBuilder = new AlertDialog.Builder(this);
							// set location_selector.xml to alert dialog builder
							airconDialogBuilder.setView(airconDialogView);

							/** Button to set onClickListener for icon buttons in the dialog */
							ImageButton btOnlyClickListener;

							for (int i = 0; i < 8; i++) {
								btOnlyClickListener = (ImageButton) airconDialogView.findViewById(iconButtons[i]);
								btOnlyClickListener.setOnClickListener(this);
							}

							dlgIconIndex = deviceIcon[dlgDeviceIndex];
							Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);

							/** Edit text field for the user selected device name */
							final EditText userInput = (EditText) airconDialogView.findViewById(R.id.dia_et_location);
							userInput.setText(locationName[dlgDeviceIndex]);

							// set dialog message
							airconDialogBuilder
									.setTitle(getResources().getString(R.string.dialog_change_title))
									.setCancelable(false)
									.setPositiveButton("OK",
											new DialogInterface.OnClickListener() {
												@SuppressWarnings({"deprecation", "ConstantConditions"})
												public void onClick(DialogInterface dialog, int id) {
													locationName[dlgDeviceIndex] = userInput.getText().toString();
													deviceIcon[dlgDeviceIndex] = dlgIconIndex;
													// Update underlying dialog box with new device name
													/** Button of selection dialog that we are processing */
													Button btToChangeName = (Button) locationSettingsView.findViewById(buttonIds[dlgDeviceIndex]);
													btToChangeName.setText(locationName[dlgDeviceIndex]);
													locationSettingsView.invalidate();
													// Update UI
													/** Text view to show location name */
													TextView locationText;
													/** Image view to show location icon */
													ImageView locationIcon;
													if (dlgDeviceIndex == FUJIDENZO) {
														locationText = (TextView) findViewById(R.id.txt_device_fd);
														locationText.setText(locationName[dlgDeviceIndex]);
														locationIcon = (ImageView) findViewById(R.id.im_icon_fd);
														locationIcon.setImageDrawable(getResources().getDrawable(iconIDs[deviceIcon[dlgDeviceIndex]]));
													} else if (dlgDeviceIndex == CARRIER) {
														locationText = (TextView) findViewById(R.id.txt_device_ca);
														locationText.setText(locationName[dlgDeviceIndex]);
														locationIcon = (ImageView) findViewById(R.id.im_icon_ca);
														locationIcon.setImageDrawable(getResources().getDrawable(iconIDs[deviceIcon[dlgDeviceIndex]]));
													} else if (dlgDeviceIndex == OTHER_AIRCON) {
														// TODO add another aircon layout
														if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Selected another aircon");
													}
													// TODO add other aircon control layouts here
												}
											})
									.setNegativeButton("Cancel",
											new DialogInterface.OnClickListener() {
												public void onClick(DialogInterface dialog, int id) {
													dialog.cancel();
												}
											});

							// create alert dialog
							AlertDialog alertDialog = airconDialogBuilder.create();

							// show it
							alertDialog.show();
							break;
						case R.id.im_bath:
							dlgIconIndex = 0;
							Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
							break;
						case R.id.im_bed:
							dlgIconIndex = 1;
							Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
							break;
						case R.id.im_dining:
							dlgIconIndex = 2;
							Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
							break;
						case R.id.im_entertain:
							dlgIconIndex = 3;
							Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
							break;
						case R.id.im_kids:
							dlgIconIndex = 4;
							Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
							break;
						case R.id.im_kitchen:
							dlgIconIndex = 5;
							Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
							break;
						case R.id.im_living:
							dlgIconIndex = 6;
							Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
							break;
						case R.id.im_office:
							dlgIconIndex = 7;
							Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
							break;
					}
				}
			}
		}
	}

	/**
	 * Broadcast receiver for notifications received over UDP or MQTT or GCM
	 */
	private BroadcastReceiver activityReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			/** Message received over UDP or GCM or from */
			String message = intent.getStringExtra("message");
			String sender = intent.getStringExtra("from");

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received broadcast from " + sender);

			if (sender.equalsIgnoreCase("search")) {
				new Initialize().execute(message);
				return;
			}
			/** Return values for onPostExecute */
			CommResultWrapper result = new CommResultWrapper();

			// Check if response is a JSON array
			if (Utilities.isJSONValid(message)) {
				result.comResult = message;
				JSONObject jsonResult;
				try {
					jsonResult = new JSONObject(message);
					String broadCastDevice;
					if (sender.equalsIgnoreCase("MQTT")) {
						broadCastDevice = jsonResult.getString("de");
					} else if (sender.equalsIgnoreCase("UDP")) {
						broadCastDevice = jsonResult.getString("device");
					} else { // Must be GCM which we will ignore in the future
						return;
					}
					if (broadCastDevice.startsWith("sf")) { // Broadcast from security device
						if (sender.equalsIgnoreCase("MQTT")) {
							// TODO standardize JSON between status, UDP and MQTT to avoid the below conversion
							// {"device":"sf1","alarm":0,"alarm_on":0,"auto":1,"auto_on":22,"auto_off":8,"light_on":0}
							jsonResult.put("device", jsonResult.get("de"));
							jsonResult.put("alarm", jsonResult.get("al"));
							jsonResult.put("alarm_on", jsonResult.get("ao"));
							jsonResult.put("auto", jsonResult.get("au"));
							jsonResult.put("auto_on", jsonResult.get("an"));
							jsonResult.put("auto_off", jsonResult.get("af"));
							jsonResult.put("light_on", jsonResult.get("lo"));
							jsonResult.remove("de");
							jsonResult.remove("al");
							jsonResult.remove("ao");
							jsonResult.remove("au");
							jsonResult.remove("an");
							jsonResult.remove("af");
							jsonResult.remove("lo");
							result.comResult = jsonResult.toString();
						}
						result.comCmd = "/?s";
						securityViewUpdate(result);
					} else if (broadCastDevice.startsWith("sb")) { // Broadcast from security device
						if (sender.equalsIgnoreCase("MQTT")) {
							// TODO standardize JSON between status, UDP and MQTT to avoid the below conversion
							// {"device":"sf1","alarm":0,"alarm_on":0,"auto":1,"auto_on":22,"auto_off":8,"light_on":0}
							jsonResult.put("device",jsonResult.get("de"));
							jsonResult.put("alarm",jsonResult.get("al"));
							jsonResult.put("alarm_on",jsonResult.get("ao"));
							jsonResult.put("auto",jsonResult.get("au"));
							jsonResult.put("auto_on",jsonResult.get("an"));
							jsonResult.put("auto_off",jsonResult.get("af"));
							jsonResult.put("light_on",jsonResult.get("lo"));
							jsonResult.remove("de");
							jsonResult.remove("al");
							jsonResult.remove("ao");
							jsonResult.remove("au");
							jsonResult.remove("an");
							jsonResult.remove("af");
							jsonResult.remove("lo");
							result.comResult = jsonResult.toString();
						}
						result.comCmd = "/?s";
						securityViewUpdate(result);
					} else if (broadCastDevice.startsWith("fd")) { // Broadcast from aircon device 0
						if (sender.equalsIgnoreCase("MQTT")) {
							// TODO standardize JSON between status, UDP and MQTT to avoid the below conversion
							jsonResult.put("device",jsonResult.get("de"));
							jsonResult.put("power",jsonResult.get("po"));
							jsonResult.put("mode",jsonResult.get("mo"));
							jsonResult.put("speed",jsonResult.get("sp"));
							jsonResult.put("temp",jsonResult.get("te"));
							jsonResult.put("cons",jsonResult.get("co"));
							jsonResult.put("status",jsonResult.get("st"));
							jsonResult.put("auto",jsonResult.get("au"));
							jsonResult.put("timer",jsonResult.get("ti"));
							jsonResult.put("onTime",jsonResult.get("to"));
							// TODO check if we really need the field "result"
							jsonResult.put("result","success");
							jsonResult.remove("de");
							jsonResult.remove("po");
							jsonResult.remove("mo");
							jsonResult.remove("sp");
							jsonResult.remove("te");
							jsonResult.remove("co");
							jsonResult.remove("st");
							jsonResult.remove("au");
							jsonResult.remove("ti");
							jsonResult.remove("to");
							result.comResult = jsonResult.toString();
						}
						result.comCmd = "/?s";
						result.deviceIndex = 0;
						airconViewUpdate(result);
					} else if (broadCastDevice.startsWith("ca")) { // Broadcast from aircon device 1
						if (sender.equalsIgnoreCase("MQTT")) {
							// TODO standardize JSON between status, UDP and MQTT to avoid the below conversion
							jsonResult.put("device",jsonResult.get("de"));
							jsonResult.put("power",jsonResult.get("po"));
							jsonResult.put("mode",jsonResult.get("mo"));
							jsonResult.put("speed",jsonResult.get("sp"));
							jsonResult.put("temp",jsonResult.get("te"));
							jsonResult.put("cons",jsonResult.get("co"));
							jsonResult.put("status",jsonResult.get("st"));
							jsonResult.put("auto",jsonResult.get("au"));
							jsonResult.put("timer",jsonResult.get("ti"));
							jsonResult.put("onTime",jsonResult.get("to"));
							// TODO check if we really need the field "result"
							jsonResult.put("result","success");
							jsonResult.remove("de");
							jsonResult.remove("po");
							jsonResult.remove("mo");
							jsonResult.remove("sp");
							jsonResult.remove("te");
							jsonResult.remove("co");
							jsonResult.remove("st");
							jsonResult.remove("au");
							jsonResult.remove("ti");
							jsonResult.remove("to");
							result.comResult = jsonResult.toString();
						}
						result.comCmd = "/?s";
						result.deviceIndex = 1;
						airconViewUpdate(result);
					} else if (broadCastDevice.startsWith("sp")) { // Broadcast from solar panel
						if (sender.equalsIgnoreCase("MQTT")) {
							// TODO standardize JSON between status, UDP and MQTT to avoid the below conversion
							// {"device":"sf1","alarm":0,"alarm_on":0,"auto":1,"auto_on":22,"auto_off":8,"light_on":0}
							jsonResult.put("device",jsonResult.get("de"));
							jsonResult.remove("de");
							result.comResult = jsonResult.toString();
						}
						result.comCmd = "/?s";
						result.deviceIndex = 1;
						solarViewUpdate(message, true);
					}
				} catch (JSONException e) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
				}
			}
		}
	};

	/**
	 * Communication in Async Task between Android and ESP8266 or Arduino Yun
	 */
	private class ESPcommunication extends AsyncTask<String, String, CommResultWrapper> {

		/**
		 * Background process of communication
		 *
		 * @param params
		 * 		params[0] = URL
		 * 		params[1] = command to be sent to ESP or Arduino
		 * 		params[2] = result of communication
		 * 		params[3] = ID of requester
		 * 			spm = solar panel monitor view
		 * 			air = aircon control view
		 * 			sec = security control view
		 * 	@return <code>CommResultWrapper</code>
		 * 			Requester ID and result of communication
		 */
		@Override
		protected CommResultWrapper doInBackground(String... params) {

			/** Return values for onPostExecute */
			CommResultWrapper result = new CommResultWrapper();

			result.httpURL = params[0];
			result.comCmd = params[1];
			result.comResult = params[2];
			result.callID = params[3];
			result.deviceIndex = Integer.parseInt(params[4]);

			/** A HTTP client to access the ESP device */
			// Set timeout to 5 minutes in case we have a lot of data to load
			OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(300, TimeUnit.SECONDS)
					.writeTimeout(10, TimeUnit.SECONDS)
					.readTimeout(300, TimeUnit.SECONDS)
					.build();

			if (result.callID.equalsIgnoreCase("spm")) {
				if (!Utilities.isHomeWiFi(getApplicationContext())) {
					// For solar panel monitor get data from web site if we are not home
					result.httpURL = "http://www.spmonitor.giesecke.tk/l.php";
					result.comCmd = "/l.php";
				}
			}
			/** URL to be called */
			String urlString = result.httpURL + result.comCmd; // URL to call

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callESP = " + urlString);

			/** Request to ESP device */
			Request request = new Request.Builder()
					.url(urlString)
					.build();

			if (request != null) {
				try {
					/** Response from ESP device */
					Response response = client.newCall(request).execute();
					if (response != null) {
						result.comResult = response.body().string();
					}
				} catch (IOException e) {
					result.comResult = e.getMessage();
					try {
						if (result.comResult.contains("EHOSTUNREACH")) {
							result.comResult = MyHomeControl.appContext.getString(R.string.err_esp);
						}
						if (result.comResult.equalsIgnoreCase("")) {
							result.comResult = MyHomeControl.appContext.getString(R.string.err_esp);
						}
						return result;
					} catch (NullPointerException en) {
						result.comResult = MyHomeControl.appContext.getString(R.string.err_no_esp);
						return result;
					}
				}
			}

			if (result.comResult.equalsIgnoreCase("")) {
				result.comResult = MyHomeControl.appContext.getString(R.string.err_esp);
			}
			return result;
		}

		/**
		 * Called when AsyncTask background process is finished
		 *
		 * @param result
		 * 		CommResultWrapper with requester ID and result of communication
		 */
		protected void onPostExecute(CommResultWrapper result) {
			switch (result.callID) {
				case "sec": // Caller is security view
					securityViewUpdate(result);
					break;
				case "air": // Caller is aircon view
					airconViewUpdate(result);
					break;
				case "spm": // Caller is solar monitor view
					if (!dataBaseIsEmpty) {
						solarViewUpdate(result.comResult, false);
					}
					break;
			}
		}
	}

	/**
	 * Send topic to MQTT broker
	 *
	 * @param payload
	 * 		Topic message
	 */
	private void doPublish(String payload){
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT topic publish: " + payload);
		if (MessageListener.mqttClient == null) { // If service is not (yet) active, don't publish
			return;
		}
		IMqttToken token;
		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setUserName(mqttUser);
		options.setPassword(mqttPw.toCharArray());
		try {
			byte[] encodedPayload;
			encodedPayload = payload.getBytes("UTF-8");
			MqttMessage message = new MqttMessage(encodedPayload);
			token = MessageListener.mqttClient.publish("/CMD", message);
			token.waitForCompletion(5000);
		} catch (MqttSecurityException | UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			switch (e.getReasonCode()) {
				case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "BROKER_UNAVAILABLE " +e.getMessage());
					e.printStackTrace();
					break;
				case MqttException.REASON_CODE_CLIENT_TIMEOUT:
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "CLIENT_TIMEOUT " +e.getMessage());
					e.printStackTrace();
					break;
				case MqttException.REASON_CODE_CONNECTION_LOST:
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "CONNECTION_LOST " +e.getMessage());
					e.printStackTrace();
					break;
				case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "SERVER_CONNECT_ERROR " +e.getMessage());
					e.printStackTrace();
					break;
				case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "FAILED_AUTHENTICATION "+ e.getMessage());
					break;
				default:
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT unknown error " + e.getMessage());
					break;
			}
		}
	}

	/**
	 * Communication in Async Task between Android and ESP8266 over TCP
	 */
	class ESPbyTCP implements Runnable {

		final String targetAddress;
		final String targetMessage;
		final String targetDevice;

		ESPbyTCP(String ipAddress, String tcpMessage, String deviceID) {
			this.targetAddress = ipAddress;
			this.targetMessage = tcpMessage;
			this.targetDevice = deviceID;
			run();
		}
		public void run() {
			// If we are not on home WiFi, send command to MQTT broker
			if (!Utilities.isHomeWiFi(getApplicationContext())) {
					String mqttTopic = "{\"ip\":\"" + targetDevice + "\","; // Device IP address
					mqttTopic += "\"cm\":\"" + targetMessage + "\"}"; // The command

					doPublish(mqttTopic);
					return;
			}

			try {
				InetAddress tcpServer = InetAddress.getByName(targetAddress);
				Socket tcpSocket = new Socket(tcpServer, 6000);

				tcpSocket.setSoTimeout(1000);
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sending " + targetMessage
						+ " to " + targetAddress);
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(tcpSocket.getOutputStream())), true);
				out.println(targetMessage);
				tcpSocket.close();
			} catch (Exception e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
				+ " " + targetAddress);
			}
		}
	}

	/**
	 * Update UI with values received from ESP device
	 *
	 * @param result
	 * 		result sent by onPostExecute
	 */
	private void securityViewUpdate(final CommResultWrapper result) {
		runOnUiThread(new Runnable() {
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				/** String used for temporary conversions */
				String tempString;
				if (Utilities.isJSONValid(result.comResult)) {
					/** JSON object to hold the result received from the ESP8266 */
					JSONObject jsonResult;
					try {
						jsonResult = new JSONObject(result.comResult);
						if (result.comCmd.equalsIgnoreCase("/?s")) { // Status request or UDP broadcast
							/** String to hold complete status in viewable form */
							String message;
							// Get device status and light status and add it to viewable status
							message = Utilities.getDeviceStatus(jsonResult) + Utilities.getLightStatus(jsonResult);
							try {
								tempString = jsonResult.getString("ssid");
								message += "SSID: " + tempString + "\n";
							} catch (JSONException ignore) {
							}
							try {
								tempString = jsonResult.getString("ip");
								message += "IP: " + tempString + "\n";
							} catch (JSONException ignore) {
							}
							try {
								tempString = jsonResult.getString("mac");
								message += "MAC: " + tempString + "\n";
							} catch (JSONException ignore) {
							}
							try {
								tempString = jsonResult.getString("sketch");
								message += "Sketch size: " + tempString + "\n";
							} catch (JSONException ignore) {
							}
							try {
								tempString = jsonResult.getString("freemem");
								message += "Free Memory: " + tempString + "\n";
							} catch (JSONException ignore) {
							}

							if (showDebug) {
								secStatus.setText(message);
							} else {
								secStatus.setText("");
							}
						} else { // Change of alarm status
							try {
								if (showDebug) {
									tempString = jsonResult.getString("result");
									secStatus.setText(tempString);
								} else {
									secStatus.setText("");
								}
							} catch (JSONException e) {
								secStatus.setText(getString(R.string.err_unknown));
							}
						}
					} catch (JSONException e) {
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
					}
				}
			}
		});
	}

	/**
	 * Parse JSON and show received status in UI
	 *
	 * @param result
	 *            CommResultWrapper
	 *               isSearchDevice = flag that device search is active
	 *               deviceIndex = index of device that is investigated
	 *               reqCmd = command to be sent to the ESP device
	 *               comResult = return string as JSON from the ESP device
	 */
	private void airconViewUpdate(CommResultWrapper result) {

		try {
			/** JSON object with the result from the ESP device */
			JSONObject deviceResult = new JSONObject(result.comResult);
			if (deviceResult.has("result")) {
				if (deviceResult.getString("result").equalsIgnoreCase("success")) {
					if (deviceResult.has("device")) {
						deviceName[result.deviceIndex] = deviceResult.getString("device");
						if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("fd")) {
							deviceType[result.deviceIndex] = FUJIDENZO;
						}
						if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("fb")) {
							deviceType[result.deviceIndex] = FUJIDENZO;
						}
						if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("ca")) {
							deviceType[result.deviceIndex] = CARRIER;
						}
						// TODO here is the place to add more layout versions for air cons
					}
					if (deviceResult.has("power")) {
						powerStatus[result.deviceIndex] = deviceResult.getInt("power");
					}
					if (deviceResult.has("mode")) {
						modeStatus[result.deviceIndex] = deviceResult.getInt("mode");
					}
					if (deviceResult.has("speed")) {
						fanStatus[result.deviceIndex] = deviceResult.getInt("speed");
					}
					if (deviceResult.has("temp")) {
						coolStatus[result.deviceIndex] = deviceResult.getInt("temp");
					}
					if (deviceResult.has("cons")) {
						consStatus = deviceResult.getDouble("cons");
					}
					if (deviceResult.has("status")) {
						autoStatus = deviceResult.getInt("status");
					}
					if (deviceResult.has("auto")) {
						autoOnStatus[result.deviceIndex] = deviceResult.getInt("auto");
					}
					if (deviceResult.has("sweep")) {
						sweepStatus[result.deviceIndex] = deviceResult.getInt("sweep");
					}
					if (deviceResult.has("turbo")) {
						turboStatus[result.deviceIndex] = deviceResult.getInt("turbo");
					}
					if (deviceResult.has("ion")) {
						ionStatus[result.deviceIndex] = deviceResult.getInt("ion");
					}
					if (deviceResult.has("timer")) {
						timerStatus[result.deviceIndex] = deviceResult.getInt("timer");
					}
					if (deviceResult.has("onTime")) {
						deviceTimer[result.deviceIndex] = deviceResult.getInt("onTime");
					}
					// TODO here is the place to add more status for other air cons

					// Update UI
					updateAirStatus(result.deviceIndex);
				} else {
					if (BuildConfig.DEBUG)
						Log.d(DEBUG_LOG_TAG, "Communication result = " + result.comResult);
				}
			}
		} catch (JSONException e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received invalid JSON = " + result.comResult);
		}
		if (showDebug) {
			airStatus.setText(result.comResult);
		} else {
			airStatus.setText("");
		}
	}

	/**
	 * Update UI fields with the latest status of a device
	 *
	 * @param deviceIndex
	 *            Index of the device to be updated
	 */
	private static void updateAirStatus(int deviceIndex) {
		/** String for the average consumption value */
		@SuppressLint("DefaultLocale") String consText = String.format("%.0f", consStatus) + "W";
		/** String for the temperature setting value */
		String tempText = Integer.toString(coolStatus[deviceIndex]) + "C";
		/** String for the auto on/off status */
		String statusText = Integer.toString(autoStatus);
		/** String with timer duration */
		String timerTime;

		switch (deviceType[deviceIndex]) {
			case FUJIDENZO:
				btOnOffLightFD.setBackgroundColor(
						(powerStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				switch (modeStatus[deviceIndex]) {
					case 0: // Fan mode
						btCoolLightFD.setBackgroundColor(colorGrey);
						btDryLightFD.setBackgroundColor(colorGrey);
						btFanLightFD.setBackgroundColor(colorRed);
						break;
					case 1: // Dry mode
						btCoolLightFD.setBackgroundColor(colorGrey);
						btDryLightFD.setBackgroundColor(colorRed);
						btFanLightFD.setBackgroundColor(colorGrey);
						break;
					case 2: // Cool mode
						btCoolLightFD.setBackgroundColor(colorRed);
						btDryLightFD.setBackgroundColor(colorGrey);
						btFanLightFD.setBackgroundColor(colorGrey);
						break;
				}
				switch (fanStatus[deviceIndex]) {
					case 0: // Fan low mode
						btFanHighLightFD.setBackgroundColor(colorGrey);
						btFanMedLightFD.setBackgroundColor(colorGrey);
						btFanLowLightFD.setBackgroundColor(colorRed);
						break;
					case 1: // Fan medium mode
						btFanHighLightFD.setBackgroundColor(colorGrey);
						btFanMedLightFD.setBackgroundColor(colorRed);
						btFanLowLightFD.setBackgroundColor(colorGrey);
						break;
					case 2: // Fan high mode
						btFanHighLightFD.setBackgroundColor(colorRed);
						btFanMedLightFD.setBackgroundColor(colorGrey);
						btFanLowLightFD.setBackgroundColor(colorGrey);
						break;
				}
				if (timerStatus[deviceIndex] == 0) {
					btTimerFD.setBackgroundColor(colorGreen);
					timerTime = Integer.toString(deviceTimer[selDevice]) +
							" " +
							appContext.getResources().getString(R.string.bt_txt_hour);
				} else {
					btTimerFD.setBackgroundColor(colorOrange);
					timerTime = appContext.getResources().getString(R.string.timer_on);
				}
				btTimerFD.setText(timerTime);

				txtConsValFD.setText(consText);
				txtTempValFD.setText(tempText);
				txtAutoStatusValFD.setText(statusText);
				btAutoLightFD.setBackgroundColor(
						(autoOnStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				break;
			case CARRIER:
				btOnOffLightCA.setBackgroundColor(
						(powerStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				switch (modeStatus[deviceIndex]) {
					case 0: // Fan mode
						btAutomLightCA.setBackgroundColor(colorGrey);
						btCoolLightCA.setBackgroundColor(colorGrey);
						btDryLightCA.setBackgroundColor(colorGrey);
						btFanLightCA.setBackgroundColor(colorRed);
						break;
					case 1: // Dry mode
						btAutomLightCA.setBackgroundColor(colorGrey);
						btCoolLightCA.setBackgroundColor(colorGrey);
						btDryLightCA.setBackgroundColor(colorRed);
						btFanLightCA.setBackgroundColor(colorGrey);
						break;
					case 2: // Cool mode
						btAutomLightCA.setBackgroundColor(colorGrey);
						btCoolLightCA.setBackgroundColor(colorRed);
						btDryLightCA.setBackgroundColor(colorGrey);
						btFanLightCA.setBackgroundColor(colorGrey);
						break;
					case 3: // Auto mode
						btAutomLightCA.setBackgroundColor(colorRed);
						btCoolLightCA.setBackgroundColor(colorGrey);
						btDryLightCA.setBackgroundColor(colorGrey);
						btFanLightCA.setBackgroundColor(colorGrey);
						break;
				}
				switch (fanStatus[deviceIndex]) {
					case 0: // Fan low mode
						btFanCA.setText(R.string.bt_txt_fan_low);
						break;
					case 1: // Fan medium mode
						btFanCA.setText(R.string.bt_txt_fan_med);
						break;
					case 2: // Fan high mode
						btFanCA.setText(R.string.bt_txt_fan_high);
						break;
				}
				if (timerStatus[deviceIndex] == 0) {
					btTimerCA.setBackgroundColor(colorGreen);
					timerTime = Integer.toString(deviceTimer[selDevice]) +
							" " +
							appContext.getResources().getString(R.string.bt_txt_hour);
				} else {
					btTimerCA.setBackgroundColor(colorOrange);
					timerTime = appContext.getResources().getString(R.string.timer_on);
				}
				btTimerCA.setText(timerTime);
				btSweepLightCA.setBackgroundColor(
						(sweepStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				btTurboLightCA.setBackgroundColor(
						(turboStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				btIonLightCA.setBackgroundColor(
						(ionStatus[deviceIndex] == 1) ? colorRed : colorGrey);

				txtConsValCA.setText(consText);
				txtTempValCA.setText(tempText);
				txtAutoStatusValCA.setText(statusText);
				btAutoLightCA.setBackgroundColor(
						(autoOnStatus[deviceIndex] == 1) ? colorRed : colorGrey);

				break;
			// TODO here is the place to add more layouts for other air cons
		}
	}

	/**
	 * Async task class to contact Linino part of the spMonitor device
	 * and sync spMonitor database with local Android database
	 */
	private class syncSolarDB extends AsyncTask<String, String, SolarCommResultWrapper> {

		@Override
		protected SolarCommResultWrapper doInBackground(String... params) {

			/** Return values for onPostExecute */
			SolarCommResultWrapper result = new SolarCommResultWrapper();

			/** A HTTP client to access the ESP device */
			// Set timeout to 5 minutes in case we have a lot of data to load
			OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(300, TimeUnit.SECONDS)
					.writeTimeout(10, TimeUnit.SECONDS)
					.readTimeout(300, TimeUnit.SECONDS)
					.build();

			/** Which month to sync */
			result.syncMonth = params[0];

			/** Response from the spMonitor device or error message */
			result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail);

			/** URL to be called */
			String urlString = SOLAR_URL + "/sd/spMonitor/query2.php"; // URL to call

			// Check for last entry in the local database
			/** Instance of data base */
			SQLiteDatabase dataBase;
			if (result.syncMonth.equalsIgnoreCase(dbNamesList[0])) {
				dataBase = dbHelperNow.getReadableDatabase();
			} else {
				dataBase = dbHelperLast.getReadableDatabase();
			}
			// Is database in use?
			while (dataBase.inTransaction()) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "syncSolarDB Database is in use");
			}

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
					splitAccess = true;
					urlString += "?date=" + result.syncMonth;
				}
			} else { // something went wrong with the database access
				result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail);
				dataBase.endTransaction();
				dataBase.close();
				return result;
			}
			dbCursor.close();
			dataBase.endTransaction();
			dataBase.close();

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
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"URL = " + urlString);
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
							result.taskResult = response.body().string();
						}
					} catch (IOException e) {
						e.printStackTrace();
						result.taskResult = e.getMessage();
						try {
							if (result.taskResult.contains("EHOSTUNREACH")) {
								result.taskResult = getApplicationContext().getString(R.string.err_arduino);
							}
							if (result.taskResult.equalsIgnoreCase("")) {
								result.taskResult = getApplicationContext().getString(R.string.err_arduino);
							}
							return result;
						} catch (NullPointerException en) {
							result.taskResult = getResources().getString(R.string.err_no_device);
							return result;
						}
					}

					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"JSON size = " + result.taskResult.length());

					try {
						/** JSON array with the data received from spMonitor device */
						JSONArray jsonFromDevice = new JSONArray(result.taskResult);
						if (result.syncMonth.equalsIgnoreCase(dbNamesList[0])) {
							dataBase = dbHelperNow.getWritableDatabase();
						} else {
							dataBase = dbHelperLast.getWritableDatabase();
						}
						// Get received data into local database
						/** Data string for insert into database */
						String record = "";
						try {
							dataBase.beginTransaction();
							for (int i=0; i<jsonFromDevice.length(); i++) {
								// skip first data record from device if we are just updating the database
								if (i == 0 && !splitAccess) i++;
								/** JSONObject with a single record */
								JSONObject jsonRecord = jsonFromDevice.getJSONObject(i);
								record = jsonRecord.getString("d");
								record = record.replace("-",",");
								record += ","+jsonRecord.getString("l");
								record += ","+jsonRecord.getString("s");
								record += ","+jsonRecord.getString("c");
								if (BuildConfig.DEBUG && i <= 1) Log.d(DEBUG_LOG_TAG,"DB insert: " + record);
								DataBaseHelper.addDay(dataBase, record);
							}
							dataBase.setTransactionSuccessful();
							dataBase.endTransaction();
							dataBase.close();
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG,"DB insert: " + record);
							result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSynced);
						} catch (SQLiteDatabaseLockedException e) {
							result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail1);
							dataBase.endTransaction();
							dataBase.close();
						}
					} catch (JSONException e) {
						result.taskResult = result.syncMonth + " " + getResources().getString(R.string.filesSyncFail);
						dataBase.endTransaction();
						dataBase.close();
					}
				}
			}
			dataBaseIsEmpty = false;
			return result;
		}

		protected void onPostExecute(SolarCommResultWrapper result) {
			updateSynced(result.taskResult, result.syncMonth);
			if (needLastMonth) {
				atLast = new syncSolarDB().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dbNamesList[1]);
				needLastMonth = false;
			}
		}
	}

	/**
	 * Update UI with values received from spMonitor device (Arduino part)
	 *
	 * @param value
	 *        result sent by spMonitor
	 */
	private void solarViewUpdate(final String value, final boolean isBroadCast) {
		runOnUiThread(new Runnable() {
			@SuppressLint("DefaultLocale")
			@SuppressWarnings({"deprecation", "ConstantConditions"})
			@Override
			public void run() {
				/** Pointer to text views to be updated */
				TextView valueFields;
				/* String with results received from spMonitor device */
				String result;

				if (value.length() != 0) {
					// decode JSON
					if (Utilities.isJSONValid(value)) {
						/** Flag for data from external server */
						boolean isFromLocal;
						/** JSON object containing the values */
						JSONObject jsonValues = null;
						try {
							jsonValues = new JSONObject(value.substring(1,value.length()-1));
							isFromLocal = false;
						} catch (JSONException ignore) {
							isFromLocal = true;
						}
						if (isBroadCast) {
							isFromLocal = false;
							try {
								jsonValues = new JSONObject(value);
							} catch (JSONException ignore) {
								return;
							}
						}
						try {
							if (isFromLocal) {
								/** JSON object containing result from server */
								JSONObject jsonResult = new JSONObject(value);
								/** JSON object containing the values */
								jsonValues = jsonResult.getJSONObject("value");
							}

							try {
								ChartHelper.solarPowerMin = isFromLocal?
										Float.parseFloat(jsonValues.getString("S")):
										Float.parseFloat(jsonValues.getString("s"));
								ChartHelper.lastSolarPowerMin = ChartHelper.solarPowerMin;
							} catch (Exception excError) {
								ChartHelper.solarPowerMin = ChartHelper.lastSolarPowerMin;
							}
							try {
								ChartHelper.consPowerMin = isFromLocal?
										Float.parseFloat(jsonValues.getString("C")):
										Float.parseFloat(jsonValues.getString("c"));
								ChartHelper.lastConsPowerMin = ChartHelper.consPowerMin;
							} catch (Exception excError) {
								ChartHelper.consPowerMin = ChartHelper.lastConsPowerMin;
							}

							result = "S=" + String.valueOf(ChartHelper.solarPowerMin) + "W ";
							result += "s=";
							try {
								result += jsonValues.getString("s");
							} catch (Exception ignore) {
								result += "---";
							}
							if (jsonValues.has("sv")) {
								result += "A sv=";
								try {
									result += jsonValues.getString("sv");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("sr")) {
								result += "V sr=";
								try {
									result += jsonValues.getString("sr");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("sa")) {
								result += "W sa=";
								try {
									result += jsonValues.getString("sa");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("sp")) {
								result += "W sp=";
								try {
									result += jsonValues.getString("sp");
								} catch (Exception excError) {
									result += "---";
								}
							}
							result += "\nC=" + String.valueOf(ChartHelper.consPowerMin) + "W c=";
							try {
								result += jsonValues.getString("c");
							} catch (Exception excError) {
								result += "---";
							}
							if (jsonValues.has("cv")) {
								result += "A cv=";
								try {
									result += jsonValues.getString("cv");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("cr")) {
								result += "V cr=";
								try {
									result += jsonValues.getString("cr");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("ca")) {
								result += "W ca=";
								try {
									result += jsonValues.getString("ca");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("cp")) {
								result += "W cp=";
								try {
									result += jsonValues.getString("cp");
								} catch (Exception excError) {
									result += "---";
								}
								result += " ";
							} else {
								result += " ";
							}

							/** Double for the result of solar current and consumption used at 1min updates */
							double resultPowerMin = ChartHelper.solarPowerMin + ChartHelper.consPowerMin;

							valueFields = (TextView) findViewById(R.id.tv_solar_val);
							/** String for display */
							String displayTxt;
							displayTxt = String.format("%.0f", ChartHelper.solarPowerMin) + "W";
							valueFields.setText(displayTxt);
							valueFields = (TextView) findViewById(R.id.tv_cons_val);
							displayTxt = String.format("%.0f", resultPowerMin) + "W";
							valueFields.setText(displayTxt);
							solStatus.setText(result);

							valueFields = (TextView) findViewById(R.id.tv_result_txt);
							if (ChartHelper.consPowerMin > 0.0d) {
								valueFields.setText(getString(R.string.tv_result_txt_im));
								valueFields = (TextView) findViewById(R.id.tv_result_val);
								valueFields.setTextColor(getResources()
										.getColor(android.R.color.holo_red_light));
							} else {
								valueFields.setText(getString(R.string.tv_result_txt_ex));
								valueFields = (TextView) findViewById(R.id.tv_result_val);
								valueFields.setTextColor(getResources()
										.getColor(android.R.color.holo_green_light));
							}
							displayTxt = String.format("%.0f", Math.abs(ChartHelper.consPowerMin)) + "W";
							valueFields.setText(displayTxt);

							if (ChartHelper.autoRefreshOn) {
								if (ChartHelper.plotData != null) {
									/** Current time as string */
									String nowTime = Utilities.getCurrentTime();
									ChartHelper.plotData.addXValue(nowTime);
									ChartHelper.timeStampsCont.add(nowTime);
									ChartHelper.solarSeries.add
											(new Entry(ChartHelper.solarPowerMin, ChartHelper.solarSeries.size()));
									ChartHelper.solarPowerCont.add(ChartHelper.solarPowerMin);
									if (ChartHelper.consPowerMin < 0.0) {
										ChartHelper.consPSeries.add
												(new Entry(ChartHelper.consPowerMin, ChartHelper.consPSeries.size()));
										ChartHelper.consumPPowerCont.add(ChartHelper.consPowerMin);
										ChartHelper.consMSeries.add(new Entry(0, ChartHelper.consMSeries.size()));
										ChartHelper.consumMPowerCont.add(0.0f);
									} else {
										ChartHelper.consMSeries.add
												(new Entry(ChartHelper.consPowerMin, ChartHelper.consMSeries.size()));
										ChartHelper.consumMPowerCont.add(ChartHelper.consPowerMin);
										ChartHelper.consPSeries.add(new Entry(0, ChartHelper.consPSeries.size()));
										ChartHelper.consumPPowerCont.add(0.0f);
									}
									/** Text view to show min and max poser values */
									TextView maxPowerText = (TextView) findViewById(R.id.tv_cons_max);
									displayTxt = "(" + String.format("%.0f",
											Collections.max(ChartHelper.consumMPowerCont)) + "W)";
									maxPowerText.setText(displayTxt);
									maxPowerText = (TextView) findViewById(R.id.tv_solar_max);
									displayTxt = "(" + String.format("%.0f",
											Collections.max(ChartHelper.solarPowerCont)) + "W)";
									maxPowerText.setText(displayTxt);

									// let the chart know it's data has changed
									ChartHelper.lineChart.notifyDataSetChanged();
									ChartHelper.lineChart.invalidate();
								}
							}
						} catch (JSONException e) {
							e.printStackTrace();
							solStatus.setText(e.getMessage());
							return;
						}
						result += Utilities.getCurrentTime();
						solStatus.setText(result);
					}
				} else {
					solStatus.setText(value);
				}
			}
		});
	}

	/**
	 * Update UI with values received from spMonitor device (Linino part)
	 *
	 * @param result
	 *        result sent by spMonitor
	 * @param syncMonth
	 *        Month that got synced
	 */
	private void updateSynced(final String result, final String syncMonth) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				solStatus.setText(result);

				if (!showingLog) {
					/** Today split into 3 integers for the database query */
					int[] todayDate = Utilities.getCurrentDate();
					/** Array with existing log dates on the Arduino */
					List<String> thisLogDates;

					/** Instance of data base */
					SQLiteDatabase dataBase;
					if (syncMonth.equalsIgnoreCase(dbNamesList[0])) {
						dataBase = dbHelperNow.getReadableDatabase();
						thisLogDates = logDates;
					} else {
						dataBase = dbHelperLast.getReadableDatabase();
						thisLogDates = lastLogDates;
					}

					dataBase.beginTransaction();

					/** Cursor with new data from the database */
					Cursor newDataSet = DataBaseHelper.getDay(dataBase, todayDate[2],
							todayDate[1], todayDate[0] - 2000);
					ChartHelper.fillSeries(newDataSet);
					newDataSet.close();
					thisLogDates.clear();
					/** List with years in the database */
					ArrayList<Integer> yearsAvail = DataBaseHelper.getEntries(dataBase, "year", 0, 0);
					for (int year = 0; year < yearsAvail.size(); year++) {
						/** List with months of year in the database */
						ArrayList<Integer> monthsAvail = DataBaseHelper.getEntries(dataBase, "month",
								0, yearsAvail.get(year));
						for (int month = 0; month < monthsAvail.size(); month++) {
							/** List with days of month of year in the database */
							ArrayList<Integer> daysAvail = DataBaseHelper.getEntries(dataBase, "day",
									monthsAvail.get(month),
									yearsAvail.get(year));
							for (int day = 0; day < daysAvail.size(); day++) {
								thisLogDates.add(("00" + String.valueOf(yearsAvail.get(year)))
										.substring(String.valueOf(yearsAvail.get(year)).length()) +
										"-" + ("00" + String.valueOf(monthsAvail.get(month)))
										.substring(String.valueOf(monthsAvail.get(month)).length()) +
										"-" + ("00" + String.valueOf(daysAvail.get(day)))
										.substring(String.valueOf(daysAvail.get(day)).length()));
							}
						}
					}

					dataBase.endTransaction();
					dataBase.close();

					if (syncMonth.equalsIgnoreCase(dbNamesList[0])) {
						logDatesIndex = thisLogDates.size() - 1;
						ChartHelper.initChart(true);
					} else {
						lastLogDatesIndex = thisLogDates.size() - 1;
					}
				}
				// Get latest value and update UI
				new ESPcommunication().execute(SOLAR_URL, "/data/get", "", "spm", Integer.toString(selDevice));
			}
		});
	}

	/**
	 * Check if UDP receiver service is running
	 *
	 * @param serviceClass
	 *              Service class we want to check if it is running
	 * @return <code>boolean</code>
	 *              True if service is running
	 *              False if service is not running
	 */
	private boolean isMyServiceRunning(Class<?> serviceClass) {
		/** Activity manager for services */
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Set all global variables used
	 */
	@SuppressWarnings({"deprecation", "ConstantConditions"})
	private void setGlobalVar() {
		// Get project ID and device URLs
		SOLAR_URL = getApplicationContext().getResources().getString(R.string.SOLAR_URL); // = "http://192.168.xxx.xx0";
		SECURITY_URL_FRONT_1 = getApplicationContext().getResources().getString(R.string.SECURITY_URL_FRONT_1); // = "http://192.168.xxx.xx1";
		SECURITY_URL_BACK_1 = this.getResources().getString(R.string.SECURITY_URL_BACK_1); // = "http://192.168.xxx.xx4";
		AIRCON_URL_1 = getApplicationContext().getResources().getString(R.string.AIRCON_URL_1); // = "http://192.168.xxx.xx2";
		AIRCON_URL_2 = getApplicationContext().getResources().getString(R.string.AIRCON_URL_2); // = "http://192.168.xxx.xx3";
		AIRCON_URL_3 = getApplicationContext().getResources().getString(R.string.AIRCON_URL_3); // = "http://192.168.xxx.xx3";
		mqttUser = getResources().getString(R.string.MQTT_USER);
		mqttPw = getResources().getString(R.string.MQTT_PW);

		// For security view:
		secStatus = (TextView) findViewById(R.id.security_status);
		ivAlarmStatus = (ImageView) findViewById(R.id.dot_alarm_status);
		ivLightStatus = (ImageView) findViewById(R.id.dot_light);
		ivAlarmOn = (ImageView) findViewById(R.id.dot_alarm_on);
		secAutoAlarm = (CheckBox) findViewById(R.id.cb_sec_auto_alarm);
		secChangeAlarm = (TextView) findViewById(R.id.tv_change_alarm);
		secBackView = (TableLayout) findViewById(R.id.tl_alarm_back);
		ivAlarmStatusBack = (ImageView) findViewById(R.id.dot_alarm_status_back);
		ivLightStatusBack = (ImageView) findViewById(R.id.dot_light_back);
		ivAlarmOnBack = (ImageView) findViewById(R.id.dot_alarm_on_back);

		animator = ValueAnimator.ofFloat(0f, 1f);
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				ivAlarmOn.setAlpha((Float) animation.getAnimatedValue());
			}
		});

		animator.setDuration(1500);
		animator.setRepeatMode(ValueAnimator.REVERSE);
		animator.setRepeatCount(-1);

		animatorBack = ValueAnimator.ofFloat(0f, 1f);
		animatorBack.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				ivAlarmOnBack.setAlpha((Float) animation.getAnimatedValue());
			}
		});

		animatorBack.setDuration(1500);
		animatorBack.setRepeatMode(ValueAnimator.REVERSE);
		animatorBack.setRepeatCount(-1);

		// For solar view:
		solStatus = (TextView) findViewById(R.id.solar_status);
		appView = getWindow().getDecorView().findViewById(android.R.id.content);
		ChartHelper.lineChart = (LineChart) findViewById(R.id.graph);
		ChartHelper.chartTitle = (TextView) findViewById(R.id.tv_plotTitle);

		// For aircon view:
		airFDView = (RelativeLayout) findViewById(R.id.fuji_denzo);
		airCAView = (RelativeLayout) findViewById(R.id.carrier);

		airStatus = (TextView) findViewById(R.id.aircon_status);
		btAutoLightFD = findViewById(R.id.bt_auto_hl_fd);
		btAutoLightCA = findViewById(R.id.bt_auto_hl_ca);
		btOnOffLightFD = findViewById(R.id.bt_on_off_hl_fd);
		btOnOffLightCA = findViewById(R.id.bt_on_off_hl_ca);
		btFanHighLightFD = findViewById(R.id.bt_fan_high_hl_fd);
		btFanMedLightFD = findViewById(R.id.bt_fan_med_hl_fd);
		btFanLowLightFD = findViewById(R.id.bt_fan_low_hl_fd);
		btCoolLightFD = findViewById(R.id.bt_cool_hl_fd);
		btCoolLightCA = findViewById(R.id.bt_cool_hl_ca);
		btDryLightFD = findViewById(R.id.bt_dry_hl_fd);
		btDryLightCA = findViewById(R.id.bt_dry_hl_ca);
		btFanLightFD = findViewById(R.id.bt_fan_hl_fd);
		btFanLightCA = findViewById(R.id.bt_fan_hl_ca);
		btSweepLightCA = findViewById(R.id.bt_sweep_hl_ca);
		btTurboLightCA = findViewById(R.id.bt_turbo_hl_ca);
		btIonLightCA = findViewById(R.id.bt_ion_hl_ca);
		btAutomLightCA = findViewById(R.id.bt_autom_hl_ca);

		btTimerFD = (Button) findViewById(R.id.bt_timer_fd);
		btTimerCA = (Button) findViewById(R.id.bt_timer_ca);

		btFanCA = (Button) findViewById(R.id.bt_fanspeed_ca);

		txtConsValFD = (TextView) findViewById(R.id.txt_cons_val_fd);
		txtTempValFD = (TextView) findViewById(R.id.txt_temp_val_fd);
		txtAutoStatusValFD = (TextView) findViewById(R.id.txt_auto_status_val_fd);
		txtConsValCA = (TextView) findViewById(R.id.txt_cons_val_ca);
		txtTempValCA = (TextView) findViewById(R.id.txt_temp_val_ca);
		txtAutoStatusValCA = (TextView) findViewById(R.id.txt_auto_status_val_ca);

		//noinspection deprecation
		colorRed = getResources().getColor(android.R.color.holo_red_light);
		//noinspection deprecation
		colorGrey = getResources().getColor(android.R.color.darker_gray);
		//noinspection deprecation
		colorOrange = getResources().getColor(android.R.color.holo_orange_light);
		//noinspection deprecation
		colorGreen = getResources().getColor(android.R.color.holo_green_light);

		/** Pointer to text views showing the consumed / produced energy */
		TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
		energyText.setVisibility(View.INVISIBLE);
		energyText = (TextView) findViewById(R.id.tv_solar_energy);
		energyText.setVisibility(View.INVISIBLE);

		/** Button to stop/start continuous UI refresh */
		Button btStop = (Button) findViewById(R.id.bt_stop);
		if (showingLog) {
			//noinspection deprecation
			btStop.setTextColor(getResources().getColor(android.R.color.holo_green_light));
			btStop.setText(getResources().getString(R.string.start));
		}

		// Get index of last selected device */
		selDevice = mPrefs.getInt(prefsSelDevice, 0);

		// Set visible view flag to security
		visibleView = mPrefs.getInt(prefsLastView, 0);
		// Set flag for debug output
		showDebug = mPrefs.getBoolean(prefsShowDebug, false);
	}

	/**
	 * Scan the local subnet and update the list of devices
	 */
	private class checkMainDevices extends AsyncTask<String, Void, Void> {

		@SuppressLint("CommitPrefEdits")
		@Override
		protected Void doInBackground(String... params) {
			try {
				// Check solar panel monitor device
				if (InetAddress.getByName("192.168.0.140").isReachable(1000)) {
					MyHomeControl.deviceIsOn[spMonitorIndex] = true;
					sendBC("spm");
				}
			} catch (IOException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Check SPM Exception " + e);
			}
			try {
				// Check front yard security device
				if (InetAddress.getByName("192.168.0.141").isReachable(1000)) {
					MyHomeControl.deviceIsOn[secFrontIndex] = true;
					sendBC("sf1");
				}
			} catch (IOException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Check SEC Front Exception " + e);
			}
			try {
				// Check back yard security device
				if (InetAddress.getByName("192.168.0.144").isReachable(1000)) {
					MyHomeControl.deviceIsOn[secBackIndex] = true;
					sendBC("sb1");
				}
			} catch (IOException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Check SEC Front Exception " + e);
			}
			try {
				// Check office aircon control device
				if (InetAddress.getByName("192.168.0.142").isReachable(1000)) {
					MyHomeControl.deviceIsOn[aircon1Index] = true;
					sendBC("fd1");
				}
			} catch (IOException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Check AC Control Exception " + e);
			}
			return null;
		}
	}

	/**
	 * Send broadcast to main thread to start initialization of device
	 */
	private void sendBC(final String deviceFound) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/** Intent for activity internal broadcast message */
				Intent broadCastIntent = new Intent();
				broadCastIntent.setAction(MessageListener.BROADCAST_RECEIVED);
				broadCastIntent.putExtra("from", "search");
				broadCastIntent.putExtra("message", deviceFound);
				sendBroadcast(broadCastIntent);
			}
		});
	}

	/**
	 * Initializing method
	 * - Find all available devices
	 * - Check if Google Cloud Messaging is registered
	 * - Call initializing methods for all devices
	 */
	private class Initialize extends AsyncTask<String, Void, Void> {

		@SuppressLint("CommitPrefEdits")
		@Override
		protected Void doInBackground(String... params) {

			String foundDevice = params[0];

			if (foundDevice.equalsIgnoreCase("spm")) {
				initSPM();
				return null;
			}
			if (foundDevice.equalsIgnoreCase("sf1") || foundDevice.equalsIgnoreCase("sb1")) {
				initSecurity(foundDevice);
				return null;
			}

			if (foundDevice.equalsIgnoreCase("fd1") || foundDevice.equalsIgnoreCase("ca1")) {
				initAircons(foundDevice);
				return null;
			}

			return null;
		}
	}

	/**
	 * Initializing method for aircon control
	 * Send status update request
	 *
	 * @param foundDevice
	 *          id of the found device
	 */
	private void initAircons(String foundDevice) {
		if (foundDevice.equalsIgnoreCase("fd1")) { // Aircon 1 - Office
			// Get initial status from Aircon 1
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of Aircon 1");
			// Update aircon status
			handleTasks(8, "/?s", AIRCON_URL_1, "fd1", "0", null, null);
			if (mPrefs.contains(prefsLocationName + "0")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "0", "");
			}
			// Update aircon 1 location name
			handleTasks(4, locationName[0], "1", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "0")) {
				deviceIcon[0] = mPrefs.getInt(prefsDeviceIcon + "0", 99);
			}
			// Update aircon 1 icon
			//noinspection deprecation
			handleTasks(7, "1", "", "", "",
					(ImageView) findViewById(R.id.im_icon_fd),
					getResources().getDrawable(iconIDs[deviceIcon[0]]));
		}
		if (foundDevice.equalsIgnoreCase("ca1")) { // Aircon 2 - Living room
			// Get initial status from Aircon 2
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of Aircon 2");
			// Update aircon 2 status
			handleTasks(8, "/?s", AIRCON_URL_2, "ca1", "1", null, null);
			if (mPrefs.contains(prefsLocationName + "1")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "1", "");
			}
			// Update aircon 2 location name
			handleTasks(5, locationName[1], "", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "1")) {
				deviceIcon[1] = mPrefs.getInt(prefsDeviceIcon + "0", 99);
			}
			// Update aircon 2 icon
			//noinspection deprecation
			handleTasks(7, "", "", "", "",
					(ImageView) findViewById(R.id.im_icon_ca),
					getResources().getDrawable(iconIDs[deviceIcon[1]]));
		}
		// TODO add third aircon if ever available
		if (foundDevice.equalsIgnoreCase("xy1")) { // Aircon 2 - Living room
			// Get initial status from Aircon 3
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of Aircon 3");
			handleTasks(8, "/?s", AIRCON_URL_3, "xy1", "2", null, null);
			if (mPrefs.contains(prefsLocationName + "2")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "2", "");
			}
			// Update aircon 3 location name
			handleTasks(6, locationName[2], "", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "2")) {
				deviceIcon[0] = mPrefs.getInt(prefsDeviceIcon + "2", 99);
			}
			// Update aircon 3 icon
			//noinspection deprecation
			handleTasks(7, "", "", "", "",
					(ImageView) findViewById(R.id.im_icon_fd),
					getResources().getDrawable(iconIDs[deviceIcon[2]]));
		}
		if (!deviceIsOn[aircon1Index] && !deviceIsOn[aircon2Index] && !deviceIsOn[aircon3Index]) {
			// Show message no aircons found
			handleTasks(2, getResources().getString(R.string.err_aircon),"","","", null, null);
		}
	}

	/**
	 * Initializing method for security control
	 * Send status update request
	 *
	 * @param foundDevice
	 *          id of the found device
	 */
	private void initSecurity(String foundDevice) {
		if (foundDevice.equalsIgnoreCase("sf1")) { // Security front
			// Get initial status from Security
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of front Security");
			// Update security status front sensor
			handleTasks(8, "/?s", SECURITY_URL_FRONT_1, "sf1", Integer.toString(selDevice), null, null);
		}
		if (foundDevice.equalsIgnoreCase("sb1")) { // Security back
			// Get initial status from Security
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of back Security");
			// Update security status back sensor
			handleTasks(8, "/?s", SECURITY_URL_BACK_1, "sb1", Integer.toString(selDevice), null, null);
			handleTasks(10, "", "", "", "", null, null);
		}
	}

	/**
	 * Initializing method for solar panel monitor
	 * Check local databases and request update if necessary
	 * Send status update request
	 */
	private void initSPM() {
		if (deviceIsOn[spMonitorIndex]) { // spMonitor
			// Get initial status from spMonitor device
			// Get today's day for the online database name
			dbNamesList = Utilities.getDateStrings();

			// In case the database is not yet existing, open it once
			/** Instance of data base */
			SQLiteDatabase dataBase = dbHelperNow.getReadableDatabase();
			dataBase.beginTransaction();
			dataBase.endTransaction();
			dataBase.close();
			/** Instance of data base */
			dataBase = dbHelperLast.getReadableDatabase();
			dataBase.beginTransaction();
			dataBase.endTransaction();
			dataBase.close();

			// Check if database is empty. If yes, sync only the database for this month
			dataBase = dbHelperNow.getReadableDatabase();
			/** Cursor with data from database */
			Cursor chCursor = DataBaseHelper.getLastRow(dataBase);
			if (chCursor != null) {
				if (chCursor.getCount() != 0) { // local database is not empty, no need can sync all data
					dataBaseIsEmpty = false;
				} else { // local database is empty, need to sync all data including last month
					needLastMonth = true;
				}
			}
			if (chCursor != null) {
				chCursor.close();
			}
			dataBase.close();

			// Start background sync of the database
			handleTasks(9, dbNamesList[0], "", "", "", null, null);

			if (!dataBaseIsEmpty) { // Sync second database only if first one is not empty
				// Check if we have already synced the last month
				/** Instance of data base */
				dataBase = dbHelperLast.getReadableDatabase();
				/** Cursor with data from database */
				Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
				if (dbCursor != null) {
					if (dbCursor.getCount() == 0) { // local database is empty, need to sync all data
						needLastMonth = true;
					} else { // fill last log file array
						lastLogDates.clear();
						/** List with years in the database */
						ArrayList<Integer> yearsAvail = DataBaseHelper.getEntries(dataBase, "year", 0, 0);
						for (int year = 0; year < yearsAvail.size(); year++) {
							/** List with months of year in the database */
							ArrayList<Integer> monthsAvail = DataBaseHelper.getEntries(dataBase, "month",
									0, yearsAvail.get(year));
							for (int month = 0; month < monthsAvail.size(); month++) {
								/** List with days of month of year in the database */
								ArrayList<Integer> daysAvail = DataBaseHelper.getEntries(dataBase, "day",
										monthsAvail.get(month),
										yearsAvail.get(year));
								for (int day = 0; day < daysAvail.size(); day++) {
									lastLogDates.add(("00" + String.valueOf(yearsAvail.get(year)))
											.substring(String.valueOf(yearsAvail.get(year)).length()) +
											"-" + ("00" + String.valueOf(monthsAvail.get(month)))
											.substring(String.valueOf(monthsAvail.get(month)).length()) +
											"-" + ("00" + String.valueOf(daysAvail.get(day)))
											.substring(String.valueOf(daysAvail.get(day)).length()));
								}
							}
						}
						lastLogDatesIndex = lastLogDates.size() - 1;
					}
				}
				if (dbCursor != null) {
					dbCursor.close();
				}
				dataBase.close();
			}

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of spMonitor");
		} else {
			dataBaseIsEmpty = false;
			// Show message not on home WiFi
			handleTasks(1, getResources().getString(R.string.err_spMonitor), "", "", "", null, null);
			// Update of solar panel values
			handleTasks(3, "/data/get", SOLAR_URL, "spm", Integer.toString(selDevice), null, null);
		}
	}

	/**
	 * Handle UI tasks of async task initialize()
	 *
	 * @param task
	 * 		task to take care of
	 * 	        0: Security screen status update
	 * 	        1: spMonitor screen status update
	 * 	        2: Aircon screen status update
	 * 	        3: start communication with Arduino Yun
	 * 	        4: Aircon 1 location & timer button text
	 * 	        5: Aircon 2 location & timer button text
	 * 	        6: Aircon 3 location & timer button text
	 * 	        7: Aircon location icon
	 * 	        8: start communication with ESP8266
	 * 	        9: Start background sync of database
	 * 	        10: Show backyard alarm status
	 * @param message
	 *      Update string for task 0, 1 and 2
	 *      Location text for task 4, 5 and 6
	 *      Command used in task 3
	 *      Database name in task 9
	 * @param url
	 *      URL to be used in task 3
	 * @param deviceID
	 *      Device ID to be used in task 3 and 8
	 * @param airconID
	 *      Aircon ID to be used in task 3
	 * @param iconImage
	 *      Image view of icon for task 7
	 * @param iconDrawable
	 *      Drawable of icon for task 7
	 */
	private void handleTasks(final int task,
	                         final String message,
	                         final String url,
	                         final String deviceID,
	                         final String airconID,
	                         final ImageView iconImage,
	                         final Drawable iconDrawable) {
		runOnUiThread(new Runnable() {
			@SuppressWarnings({"deprecation", "ConstantConditions"})
			@Override
			public void run() {
				/** Text view to show location name */
				TextView locationText;
				/** Timer button */
				Button btTimer;
				/** Text for timer button */
				String timerTime;
				switch (task) {
					case 0: // Security screen status update
						if (showDebug) {
							secStatus.setText(message);
						} else {
							secStatus.setText("");
						}
						break;
					case 1: // spMonitor screen status update
						solStatus.setText(message);
						break;
					case 2: // Aircon screen status update
						if (showDebug) {
							airStatus.setText(message);
						} else {
							airStatus.setText("");
						}
						break;
					case 3: // start communication with Arduino Yun
						new ESPcommunication().execute(url, message, "", deviceID, airconID);
						break;
					case 4: // Aircon 1 location & timer button text
						locationText = (TextView) findViewById(R.id.txt_device_fd);
						locationText.setText(message);
						btTimer = (Button) findViewById(R.id.bt_timer_fd);
						timerTime = Integer.toString(deviceTimer[0]) +
								" " +
								getString(R.string.bt_txt_hour);
						btTimer.setText(timerTime);
						break;
					case 5: // Aircon 2 location & timer button text
						locationText = (TextView) findViewById(R.id.txt_device_ca);
						locationText.setText(message);
						btTimer = (Button) findViewById(R.id.bt_timer_ca);
						timerTime = Integer.toString(deviceTimer[1]) +
								" " +
								getString(R.string.bt_txt_hour);
						btTimer.setText(timerTime);
						break;
					case 6: // Aircon 3 location & timer button text
						locationText = (TextView) findViewById(R.id.txt_device_fd);
						locationText.setText(message);
						btTimer = (Button) findViewById(R.id.bt_timer_fd);
						timerTime = Integer.toString(deviceTimer[2]) +
								" " +
								getString(R.string.bt_txt_hour);
						btTimer.setText(timerTime);
						break;
					case 7: // Aircon location icon
						iconImage.setImageDrawable(iconDrawable);
						break;
					case 8: // start communication with ESP8266
						new ESPbyTCP(url.substring(7), message.substring(2), deviceID);
						break;
					case 9: // Start background sync of database
						atNow = new syncSolarDB().execute(message);
						break;
					case 10:
						TableLayout backYardDots = (TableLayout) findViewById(R.id.tl_alarm_back);
						backYardDots.setVisibility(View.VISIBLE);
						break;
					case 11:
				}
			}
		});
	}

	/**
	 * Switch to requested UI
	 *
	 * @param uiSelected
	 *            0 = Security UI
	 *            1 = Solar panel UI
	 *            2 = Aircon control UI
	 */
	@SuppressWarnings({"deprecation", "ConstantConditions"})
	private void switchUI(int uiSelected) {

		/** Pointer to action bar */
		Toolbar actionBar = (Toolbar) findViewById(R.id.toolbar);
		/** Color of toolbar background */
		Drawable toolBarDrawable;
		/** Menu item pointer */
		MenuItem menuItem;
		/** Color for status bar */
		int statusBarColor;
		/** Color for action bar */
		int actionBarColor;

		switch (uiSelected) {
			case 0: // Security UI
				statusBarColor = getResources().getColor(R.color.colorPrimaryDark);
				actionBarColor = getResources().getColor(R.color.colorPrimary);
				if (abMenu != null) {
					// Make security menu items visible
					menuItem = abMenu.getItem(4); // Alarm sound menu entry
					menuItem.setVisible(true);
					menuItem = abMenu.getItem(5); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(6); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				// Make security view visible
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				secView.setVisibility(View.VISIBLE);
				visibleView = 0;
				break;
			case 1: // Solar panel UI
				statusBarColor = getResources().getColor(android.R.color.holo_green_dark);
				actionBarColor = getResources().getColor(android.R.color.holo_green_light);
				if (abMenu != null) {
					// Make solar panel menu items visible
					menuItem = abMenu.getItem(4); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(5); // Solar alarm sound menu entry
					menuItem.setVisible(true);
					menuItem = abMenu.getItem(6); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				secView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.VISIBLE);
				visibleView = 1;
				break;
			case 2: // Aircon control UI
			default: // Whatever
				statusBarColor = getResources().getColor(android.R.color.holo_blue_dark);
				actionBarColor = getResources().getColor(android.R.color.holo_blue_light);
				if (abMenu != null) {
					// Make aircon menu items visible
					menuItem = abMenu.getItem(4); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(5); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(6); // Aircon location menu entry
					menuItem.setVisible(true);
				}
				secView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.VISIBLE);
				visibleView = 2;
				break;
		}
		if (android.os.Build.VERSION.SDK_INT >= 21) {
			getWindow().setStatusBarColor(statusBarColor);
		}
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			toolBarDrawable = new ColorDrawable(actionBarColor);
			actionBar.setBackground(toolBarDrawable);
		}
		mPrefs.edit().putInt(prefsLastView,visibleView).apply();
	}

	/**
	 * Handle Security view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from security view
	 */
	@SuppressWarnings("deprecation")
	private boolean handleSecurityButtons(View v) {
		/** Flag if button was handled */
		boolean wasSecButton = true;
		/** URL for communication with ESP */
		String url = "";
		/** URL for communication with ESP in case we need to second security device as well */
		String url2 = "";
		/** Command for ESP */
		String cmd = "";
		/** DeviceID used for MQTT */
		String deviceID = "";
		/** DeviceID used for MQTT in case we need to second security device as well */
		String deviceID2 = "";
		switch (v.getId()) {
			case R.id.dot_alarm_status:
				if (hasAlarmOnFront) {
					ivAlarmStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_sec_widget_off));
					url = SECURITY_URL_FRONT_1;
					cmd = "/?a=0";
				} else {
					ivAlarmStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_sec_widget_on));
					url = SECURITY_URL_FRONT_1;
					cmd = "/?a=1";
				}
				deviceID = "sf1";
				break;
			case R.id.dot_alarm_status_back:
				if (hasAlarmOnBack) {
					ivAlarmStatusBack.setImageDrawable(getResources().getDrawable(R.mipmap.ic_sec_widget_off));
					url = SECURITY_URL_BACK_1;
					cmd = "/?a=0";
				} else {
					ivAlarmStatusBack.setImageDrawable(getResources().getDrawable(R.mipmap.ic_sec_widget_on));
					url = SECURITY_URL_BACK_1;
					cmd = "/?a=1";
				}
				deviceID = "sb1";
				break;
			case R.id.dot_light:
				ivLightStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_light_on));
				url = SECURITY_URL_FRONT_1;
				cmd = "/?b";
				deviceID = "sf1";
				break;
			case R.id.dot_light_back:
				ivLightStatusBack.setImageDrawable(getResources().getDrawable(R.mipmap.ic_light_on));
				url = SECURITY_URL_BACK_1;
				cmd = "/?b";
				deviceID = "sb1";
				break;
			case R.id.cb_sec_auto_alarm:
				if (secAutoAlarm.isChecked()) {
					String onTime = Integer.toString(secAutoOnStored);
					if (secAutoOnStored < 10) {
						onTime = "0" + onTime;
					}
					String offTime = Integer.toString(secAutoOffStored);
					if (secAutoOffStored < 10) {
						offTime = "0" + offTime;
					}
					url = SECURITY_URL_FRONT_1;
					url2 = SECURITY_URL_BACK_1;
					cmd = "/?a=2," + onTime + "," + offTime;
					secAutoAlarm.setText(getResources().getString(R.string.sec_auto_alarm_on,secAutoOn,secAutoOff));
					secChangeAlarm.setVisibility(View.VISIBLE);
				} else {
					url = SECURITY_URL_FRONT_1;
					url2 = SECURITY_URL_BACK_1;
					cmd = "/?a=3";
					secAutoAlarm.setText(getResources().getString(R.string.sec_auto_alarm_off));
					secChangeAlarm.setVisibility(View.INVISIBLE);
				}
				deviceID = "sf1";
				deviceID2 = "sb1";
				break;
			case R.id.tv_change_alarm:
				final Dialog alarmDlg = new Dialog(MyHomeControl.this);
				final int orgOnTime = secAutoOnStored;
				final int orgOffTime = secAutoOffStored;
				alarmDlg.setTitle("NumberPicker");
				alarmDlg.setContentView(R.layout.alarm_settings);
				Button cancelButton = (Button) alarmDlg.findViewById(R.id.bt_sec_cancel);
				Button okButton = (Button) alarmDlg.findViewById(R.id.bt_sec_ok);
				final NumberPicker npOnTime = (NumberPicker) alarmDlg.findViewById(R.id.np_Alarm_on);
				npOnTime.setMaxValue(23);
				npOnTime.setMinValue(0);
				npOnTime.setValue(secAutoOnStored);
				npOnTime.setWrapSelectorWheel(false);
				npOnTime.setOnValueChangedListener( new NumberPicker.OnValueChangeListener() {
					@Override
					public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
						secAutoOnStored = newVal;
					}
				});
				final NumberPicker npOffTime = (NumberPicker) alarmDlg.findViewById(R.id.np_Alarm_off);
				npOffTime.setMaxValue(23);
				npOffTime.setMinValue(0);
				npOffTime.setValue(secAutoOffStored);
				npOffTime.setWrapSelectorWheel(false);
				npOffTime.setOnValueChangedListener( new NumberPicker.OnValueChangeListener() {
					@Override
					public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
						secAutoOffStored = newVal;
					}
				});
				cancelButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// Reset changed values
						secAutoOnStored = orgOnTime;
						secAutoOffStored = orgOffTime;
						alarmDlg.dismiss();
					}
				});
				okButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String onTime = Integer.toString(secAutoOnStored);
						if (secAutoOnStored < 10) {
							onTime = "0" + onTime;
						}
						String offTime = Integer.toString(secAutoOffStored);
						if (secAutoOffStored < 10) {
							offTime = "0" + offTime;
						}
						new ESPbyTCP(MyHomeControl.SECURITY_URL_FRONT_1.substring(7),
								"a=2," + onTime + "," + offTime, "sf1");
						new ESPbyTCP(MyHomeControl.SECURITY_URL_BACK_1.substring(7),
								"a=2," + onTime + "," + offTime, "sb1");

						alarmDlg.dismiss();
					}
				});
				alarmDlg.show();
				break;
			default:
				wasSecButton = false;
				break;
		}
		if (!cmd.equalsIgnoreCase("")) {
			new ESPbyTCP(url.substring(7),cmd.substring(2), deviceID);
			if (!url2.equalsIgnoreCase("")) {
				new ESPbyTCP(url2.substring(7),cmd.substring(2), deviceID2);
			}
		}
		return wasSecButton;
	}

	/**
	 * Handle Solar panel view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from security view
	 */
	@SuppressWarnings({"deprecation", "ConstantConditions"})
	private boolean handleSPMbuttons(View v) {
		/** Flag if button was handled */
		boolean wasSPMbutton = true;
		/** Button to go to previous  log */
		Button prevButton  = (Button) findViewById(R.id.bt_prevLog);
		/** Button to go to next log */
		Button nextButton  = (Button) findViewById(R.id.bt_nextLog);

		switch (v.getId()) {
			case R.id.bt_prevLog:
				if (logDatesIndex == 0) {
					if ((lastLogDatesIndex == lastLogDates.size() - 1) && !showingLast) {
						lastLogDatesIndex++;
					}
					showingLast = true;
				} else {
					showingLast = false;
				}
				if (!showingLast) { // use this months database
					if (logDatesIndex > 0) {
						logDatesIndex--;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = logDates.get(logDatesIndex).substring(0, 8).split("-");
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelperNow.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						ChartHelper.fillSeries(newDataSet);
						ChartHelper.initChart(false);
						newDataSet.close();
						dataBase.close();

						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
				} else { // use last months database
					if (lastLogDatesIndex > 0) {
						lastLogDatesIndex--;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = lastLogDates.get(lastLogDatesIndex).substring(0, 8).split("-");
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelperLast.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						ChartHelper.fillSeries(newDataSet);
						ChartHelper.initChart(false);
						newDataSet.close();
						dataBase.close();

						if (lastLogDatesIndex == 0) {
							prevButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
						} else {
							prevButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
						}
						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
				}
				break;
			case R.id.bt_nextLog:
				if (lastLogDatesIndex == lastLogDates.size() - 1) {
					if ((logDatesIndex == 0) && showingLast) {
						logDatesIndex--;
					}
					showingLast = false;
				} else {
					showingLast = true;
				}
				if (!showingLast) { // use this months database
					if (logDatesIndex < logDates.size() - 1) {
						logDatesIndex++;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = logDates.get(logDatesIndex).substring(0, 8).split("-");
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelperNow.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						ChartHelper.fillSeries(newDataSet);
						ChartHelper.initChart(false);
						newDataSet.close();
						dataBase.close();

						if (logDatesIndex == logDates.size() - 1) {
							nextButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
						} else {
							nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
						}
						prevButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
				} else { // use last months database
					if (lastLogDatesIndex < lastLogDates.size() - 1) {
						lastLogDatesIndex++;
						/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = (Button) findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/** String list with requested date info */
						String[] requestedDate = lastLogDates.get(lastLogDatesIndex).substring(0, 8).split("-");
						/** Instance of data base */
						SQLiteDatabase dataBase = dbHelperLast.getReadableDatabase();

						/** Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						ChartHelper.fillSeries(newDataSet);
						ChartHelper.initChart(false);
						newDataSet.close();
						dataBase.close();

						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
				}
				break;
			case R.id.bt_stop:
				if (ChartHelper.autoRefreshOn) {
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
					stopButton.setText(getResources().getString(R.string.start));
					ChartHelper.autoRefreshOn = false;
				} else {
					if (showingLog) {
						showingLog = false;
						if (Utilities.isHomeWiFi(this)) {
							atNow = new syncSolarDB().execute(dbNamesList[0]);
						}

						/** Pointer to text views showing the consumed / produced energy */
						TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
						energyText.setVisibility(View.INVISIBLE);
						energyText = (TextView) findViewById(R.id.tv_solar_energy);
						energyText.setVisibility(View.INVISIBLE);

						logDatesIndex = logDates.size() - 1;
						nextButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					}
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					stopButton.setText(getResources().getString(R.string.stop));
					ChartHelper.autoRefreshOn = true;
				}
				break;
			default:
				wasSPMbutton = false;
				break;
		}
		return wasSPMbutton;
	}

	/**
	 * Handle Security view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from security view
	 */
	@SuppressWarnings("ConstantConditions")
	private boolean handleAirconButtons(View v) {
		/** Flag if button was handled */
		boolean wasAirconButton = true;
		/** URL for communication with ESP */
		String url = "http://" + espIP[selDevice];
		/** DeviceID used for MQTT */
		String deviceID = deviceName[selDevice];
		/** Command for ESP */
		String cmd = "";
		/** Timer button */
		Button btTimer;
		/** Text for timer button */
		String timerTime;

		switch (v.getId()) {
			case R.id.bt_auto_fd:
			case R.id.bt_auto_ca:
				if (autoOnStatus[selDevice] == 1) {
					cmd = "/?c=" + CMD_AUTO_OFF;
				} else {
					cmd = "/?c=" + CMD_AUTO_ON;
				}
				break;
			case R.id.bt_on_off_fd:
			case R.id.bt_on_off_ca:
				cmd = "/?c=" + CMD_ON_OFF;
				break;
			case R.id.bt_fan_high_fd:
				if (fanStatus[selDevice] != 2) {
					cmd = "/?c=" + CMD_FAN_HIGH;
				}
				break;
			case R.id.bt_fan_med_fd:
				if (fanStatus[selDevice] != 1) {
					cmd = "/?c=" + CMD_FAN_MED;
				}
				break;
			case R.id.bt_fan_low_fd:
				if (fanStatus[selDevice] != 0) {
					cmd = "/?c=" + CMD_FAN_LOW;
				}
				break;
			case R.id.bt_autom_ca:
				if (modeStatus[selDevice] != 3) {
					cmd = "/?c=" + CMD_MODE_AUTO;
				}
				break;
			case R.id.bt_cool_fd:
			case R.id.bt_cool_ca:
				if (modeStatus[selDevice] != 2) {
					cmd = "/?c=" + CMD_MODE_COOL;
				}
				break;
			case R.id.bt_dry_fd:
			case R.id.bt_dry_ca:
				if (modeStatus[selDevice] != 1) {
					cmd = "/?c=" + CMD_MODE_DRY;
				}
				break;
			case R.id.bt_fan_fd:
			case R.id.bt_fan_ca:
				if (modeStatus[selDevice] != 0) {
					cmd = "/?c=" + CMD_MODE_FAN;
				}
				break;
			case R.id.bt_sweep_ca:
				cmd = "/?c=" + CMD_OTHER_SWEEP;
				break;
			case R.id.bt_turbo_ca:
				cmd = "/?c=" + CMD_OTHER_TURBO;
				break;
			case R.id.bt_ion_ca:
				cmd = "/?c=" + CMD_OTHER_ION;
				break;
			case R.id.bt_plus_fd:
			case R.id.bt_plus_ca:
				cmd = "/?c=" + CMD_TEMP_PLUS;
				break;
			case R.id.bt_minus_fd:
			case R.id.bt_minus_ca:
				cmd = "/?c=" + CMD_TEMP_MINUS;
				break;
			case R.id.bt_fanspeed_ca:
				cmd = "/?c=" + CMD_FAN_SPEED;
				break;
			case R.id.im_icon_fd:
				if (deviceIsOn[aircon2Index]) { // Is Carrier aircon online?
					airFDView.setVisibility(View.INVISIBLE);
					airCAView.setVisibility(View.VISIBLE);
					selDevice = 1;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				} else if (deviceIsOn[aircon3Index]) { // Is other aircon online?
					// TODO switch to third aircon view
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Trying to switch to aircon 3");
					selDevice = 2;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				}
				break;
			case R.id.im_icon_ca:
				if (deviceIsOn[aircon3Index]) { // Is other aircon online?
					// TODO switch to third aircon view
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Trying to switch to aircon 3");
					selDevice = 2;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				} else if (deviceIsOn[aircon1Index]) {
					airCAView.setVisibility(View.INVISIBLE);
					airFDView.setVisibility(View.VISIBLE);
					selDevice = 0;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				}
				break;
			case R.id.bt_timer_fd:
			case R.id.bt_timer_ca:
				if (selDevice < 2) { // Only Aircon 1 and 2 support timer for now
					// TODO
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Setting timer to " + deviceTimer[selDevice]);
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "First command = " + "/?t=" + Integer.toString(deviceTimer[selDevice]));
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Second command = " + "/?c="  + CMD_OTHER_TIMER);
					new ESPbyTCP(url.substring(7), "t=" + Integer.toString(deviceTimer[selDevice]), deviceID);
					cmd = "/?c=" + CMD_OTHER_TIMER;
				}
				break;
			case R.id.bt_timer_minus_fd:
			case R.id.bt_timer_minus_ca:
				if (timerStatus[selDevice] == 0) {
					if (deviceTimer[selDevice] > 1) {
						deviceTimer[selDevice]--;
						cmd = "/?t=" + Integer.toString(deviceTimer[selDevice]);
					}
					btTimer = (Button) findViewById(R.id.bt_timer_fd);
					timerTime = Integer.toString(deviceTimer[selDevice]) +
							" " +
							getString(R.string.bt_txt_hour);
					btTimer.setText(timerTime);
				}
				break;
			case R.id.bt_timer_plus_fd:
			case R.id.bt_timer_plus_ca:
				if (timerStatus[selDevice] == 0) {
					if (deviceTimer[selDevice] < 9) {
						deviceTimer[selDevice]++;
						cmd = "/?t=" + Integer.toString(deviceTimer[selDevice]);
					}
					btTimer = (Button) findViewById(R.id.bt_timer_fd);
					timerTime = Integer.toString(deviceTimer[selDevice]) +
							" " +
							getString(R.string.bt_txt_hour);
					btTimer.setText(timerTime);
				}
				break;
			default: // End here if it was not an aircon view button
				wasAirconButton = false;
		}
		if (!cmd.equalsIgnoreCase("")) {
			new ESPbyTCP(url.substring(7), cmd.substring(2), deviceID);
		}
		return wasAirconButton;
	}
}

