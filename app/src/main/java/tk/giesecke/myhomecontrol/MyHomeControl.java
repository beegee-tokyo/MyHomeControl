package tk.giesecke.myhomecontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteReadOnlyDatabaseException;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.druk.dnssd.DNSSD;
import com.github.druk.dnssd.DNSSDBindable;
import com.github.druk.dnssd.DNSSDException;
import com.github.druk.dnssd.DNSSDRegistration;
import com.github.druk.dnssd.DNSSDService;
import com.github.druk.dnssd.RegisterListener;
import com.github.druk.dnssd.TXTRecord;
import com.github.mikephil.charting.data.Entry;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import tk.giesecke.myhomecontrol.devices.CheckAvailDevices;
import tk.giesecke.myhomecontrol.devices.MessageListener;
import tk.giesecke.myhomecontrol.security.CCTVfootages;
import tk.giesecke.myhomecontrol.security.LoadImage;
import tk.giesecke.myhomecontrol.security.SecCamViewer;
import uk.co.senab.photoview.PhotoViewAttacher;

import tk.giesecke.myhomecontrol.solar.ChartHelper;
import tk.giesecke.myhomecontrol.solar.DataBaseHelper;
import tk.giesecke.myhomecontrol.solar.SolarSyncDataBase;

import static com.github.druk.dnssd.DNSSD.NO_AUTO_RENAME;
import static java.lang.Thread.sleep;
import static tk.giesecke.myhomecontrol.devices.CheckAvailDevices.dsURL;


public class MyHomeControl extends AppCompatActivity implements View.OnClickListener
		, AdapterView.OnItemClickListener, LoadImage.Listener, SeekBar.OnSeekBarChangeListener {

	/*
	 * Debug tag
	 */
	static final String DEBUG_LOG_TAG = "MHC-MAIN";

	/*
	 * Access to activities shared preferences
	 */
	private static SharedPreferences mPrefs;
	/* Name of shared preferences */
	public static final String sharedPrefName = "MyHomeControl";
	/*
	 * Context of this application
	 */
	@SuppressLint("StaticFieldLeak")
	private Context appContext;
	/*
	 * Id of menu, needed to set user selected icons and device names
	 */
	private Menu abMenu;
	/*
	 * Id's of menu items
	 */
//	int action_lightControl_id = 0;
//	int action_security_id = 1;
//	int action_solar_id = 2;
//	int action_aircon_id = 3;
//	int action_close_id = 4;
	@SuppressWarnings("FieldCanBeLocal")
	private final int action_selAlarm_id = 5;
	@SuppressWarnings("FieldCanBeLocal")
	private final int action_selWarning_id = 6;
	@SuppressWarnings("FieldCanBeLocal")
	private final int action_locations_id = 7;
	//	int action_refresh_id = 8;
	private final int action_debug_id = 9;
	@SuppressWarnings("FieldCanBeLocal")
	private final int action_devDebug_id = 10;
	/*
	 * Id's of views
	 */
	private final int view_security_id = 0;
	public final static int view_solar_id = 1;
	public final static int view_aircon_id = 2;
	public final static int view_devDebug_id = 3;
	private final int view_seccam_id = 4;
	private final int view_lights_id = 5;
	private final static int view_secVideo_id = 6;
	/*
	 * Visible view 0 = security, 1 = solar panel, 2 = aircon
	 */
	private int visibleView = 1;
	/*
	 * Flag for debug output
	 */
	private boolean showDebug = false;
	/*
	 * The view of the main UI
	 */
	private View appView;
	/*
	 * Text view for the date
	 */
	private TextView chartTitle;

	/*
	 * Shared preferences value for last shown view
	 */
	private final String prefsLastView = "lastView";
	/*
	 * Shared preferences value for show debug messages flag
	 */
	public static final String prefsShowDebug = "showDebug";

	/*
	 * Shared preferences value security alarm sound
	 */
	public static final String prefsSecurityAlarm = "secAlarm";

	/*
	 * Shared preferences value for solar alarm sound
	 */
	public static final String prefsSolarWarning = "solarAlarm";
	/*
	 * Shared preferences value for last synced month
	 */
	public static final String prefsSolarSynced = "solarLastSynced";
	/*
	 * Shared preferences value for number solar widgets placed
	 */
	public static final String prefsSolarWidgetNum = "solarWidgetNum";
	/*
	 * Shared preferences value for large widget
	 */
	public static final String prefsSolarWidgetSize = "solarWidgetSizeLarge";

	/*
	 * Shared preferences value for last selected device
	 */
	private final String prefsSelDevice = "airconSelDevice";
	/*
	 * Shared preferences value for show debug messages flag
	 */
	private final String prefsLocationName = "airconLocation";
	/*
	 * Shared preferences value for show debug messages flag
	 */
	private final String prefsDeviceIcon = "airconIcon";

	/*
	 * Shared preferences value for dimmed brightness values
	 */
	public static final String prefsLightBedDim = "lightBedDimVal";

	/*
	 * View for selecting device to change icon and device name
	 */
	private View locationSettingsView;
	/*
	 * View of aircon device name and icon change dialog
	 */
	private View airconDialogView;
	/*
	 * Button ids from location selection dialog
	 */
	private final int[] buttonIds = {
			R.id.dia_sel_device0,
			R.id.dia_sel_device1,
			R.id.dia_sel_device2};
	/*
	 * Resource ids of drawables for the icons
	 */
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
	/*
	 * Index of device handled in dialog box
	 */
	private int dlgDeviceIndex;
	/*
	 * R.id of selected icon for a device
	 */
	private int dlgIconIndex;

	/*
	 * Flag for sound selector (true = security alarm, false = solar panel warning)
	 */
	private boolean isSelAlarm = true;

	// SPmonitor, Security front, Security back, Aircon 1, Aircon 2, Aircon 3, Monitor, Camera front
	/*
	 * List of device names
	 */
	public static final String[] deviceNames = {"spm", "sf1", "sb1", "fd1", "ca1", "am1", "moni", "cm1", "lb1", "ly1", "mhc", "MHControl"};
	//	public static final String[] deviceNames = {"spMonitor", "sf1", "sb1", "fd1", "ca1", "am1", "moni", "cm1", "lb1", "ly1", "mhc", "MHControl"};
	/*
	 * List of potential control device availability
	 */
	public static final boolean[] deviceIsOn = {false, false, false, false, false, false, false, false, false, false, false, false};
	/*
	 * List of IP addresses of found devices
	 */
	public static final String[] deviceIPs = {"", "", "", "", "", "", "", "", "", "", "", ""};
	/*
	 * deviceIsOn index for SPmonitor
	 */
	public static final int spMonitorIndex = 0;
	/*
	 * deviceIsOn index for Security front
	 */
	public static final int secFrontIndex = 1;
	/*
	 * deviceIsOn index for Security back
	 */
	public static final int secBackIndex = 2;
	/*
	 * deviceIsOn index for Aircon 1
	 */
	public static final int aircon1Index = 3;
	/*
	 * deviceIsOn index for Aircon 2
	 */
	public static final int aircon2Index = 4;
	/*
	 * deviceIsOn index for Aircon 3
	 */
	public static final int aircon3Index = 5;
	/*
	 * deviceIsOn index for Monitoring display
	 */
	private static final int moniIndex = 6;
	/*
	 * deviceIsOn index for front yard camera
	 */
	public static final int cam1Index = 7;
	/*
	 * deviceIsOn index for bedroom lights
	 */
	public static final int lb1Index = 8;
	/*
	 * deviceIsOn index for backyard lights
	 */
	public static final int ly1Index = 9;
	/*
	 * deviceIsOn index for gateway
	 */
	public static final int mhcIndex = 10;

	// Security view related
	/*
	 * Security view
	 */
	private RelativeLayout secView = null;
	/*
	 * TextView for status message display in security view
	 */
	private TextView secStatus;
	/*
	 * ImageView to show status of alarm enabled front sensor
	 */
	private ImageView ivAlarmStatus;
	/*
	 * ImageView to show status of light enabled front sensor
	 */
	private ImageView ivLightStatus;
	/*
	 * TableLayout to show status of back alarm system
	 */
	private TableLayout secBackView;
	/*
	 * ImageView to show status of alarm enabled back sensor
	 */
	private ImageView ivAlarmStatusBack;
	/*
	 * ImageView to show status of light enabled back sensor
	 */
	private ImageView ivLightStatusBack;
	/*
	 * Check box for auto activation of alarm
	 */
	private CheckBox secAutoAlarmFront;
	/*
	 * Check box for auto activation of alarm
	 */
	private CheckBox secAutoAlarmBack;
	/*
	 * Clickable text view to change activation times
	 */
	private TextView secChangeAlarm;

	/*
	 * Status flag for alarm front sensor
	 */
	static boolean hasAlarmOnFront = true;
	/*
	 * Status flag for alarm back sensor
	 */
	static boolean hasAlarmOnBack = true;
	/*
	 * Status flag for auto alarm front sensor
	 */
	static boolean hasAutoOnFront = true;
	/*
	 * Status flag for auto alarm back sensor
	 */
	static boolean hasAutoOnBack = true;

	/*
	 * Auto activation on time as string
	 */
	static String secAutoOn;
	/*
	 * Auto activation off time as string
	 */
	static String secAutoOff;
	/*
	 * Auto activation on time as integer
	 */
	static int secAutoOnStored;
	/*
	 * Auto activation off time as integer
	 */
	static int secAutoOffStored;

	/*
	 * Array list with available alarm names
	 */
	private ArrayList<String> notifNames = new ArrayList<>();
	/*
	 * Array list with available alarm uri's
	 */
	private ArrayList<String> notifUri = new ArrayList<>();
	/*
	 * Selected alarm name
	 */
	private String notifNameSel = "";
	/*
	 * Selected alarm uri
	 */
	private String notifUriSel = "";

	// Solar monitor view related
	/*
	 * Solar panel view
	 */
	private RelativeLayout solView = null;
	/*
	 * TextView for status message display in solar panel view
	 */
	private TextView solStatus;
	/*
	 * Array with existing log dates on the Arduino
	 */
	private static final List<String> logDates = new ArrayList<>();
	/*
	 * Pointer to current displayed log in logDates array
	 */
	private static int logDatesIndex = 0;
	/*
	 * Array with existing log dates on the Arduino
	 */
	private static final List<String> lastLogDates = new ArrayList<>();
	/*
	 * Pointer to current displayed log in logDates array
	 */
	private static int lastLogDatesIndex = 0;
	/*
	 * Flag for showing last month
	 */
	private static boolean showingLast = false;
	/*
	 * Flag for showing a log
	 */
	protected static boolean showingLog = false;
	// Flag for database empty */
	private static boolean dataBaseIsEmpty = true;

	/*
	 * Instance of DataBaseHelper for this month
	 */
	private DataBaseHelper dbHelperNow;
	/*
	 * Instance of DataBaseHelper for last month
	 */
	private DataBaseHelper dbHelperLast;

	/*
	 * Today's year-month database name
	 */
	private static String[] dbNamesList = new String[2];

	// Aircon view related
	/*
	 * Aircon control view
	 */
	private RelativeLayout airView = null;
	/*
	 * FujiDenzo control view
	 */
	private RelativeLayout airFDView = null;
	/*
	 * Carrier control view
	 */
	private RelativeLayout airCAView = null;
	/*
	 * American Home control view
	 */
	private RelativeLayout airAMView = null;
	/*
	 * TextView for status message display in aircon view
	 */
	private TextView airStatus;
	/*
	 * Light of button to switch consumption control for FujiDenzo layout
	 */
	private View btAutoLightFD;
	/*
	 * Light of button to switch consumption control for Carrier layout
	 */
	private View btAutoLightCA;
	/*
	 * Light of button to switch consumption control for American Home layout
	 */
	private View btAutoLightAM;
	/*
	 * Light of button to switch on/off for FujiDenzo layout
	 */
	private View btOnOffLightFD;
	/*
	 * Light of button to switch on/off for Carrier layout
	 */
	private View btOnOffLightCA;
	/*
	 * Light of button to switch on/off for American Home layout
	 */
	private View btOnOffLightAM;
	/*
	 * Light of button to switch fan to high speed for FujiDenzo layout
	 */
	private View btFanHighLightFD;
	/*
	 * Light of button to switch fan to medium speed for FujiDenzo layout
	 */
	private View btFanMedLightFD;
	/*
	 * Light of button to switch fan to low speed for FujiDenzo layout
	 */
	private View btFanLowLightFD;
	/*
	 * Light of button to switch to cool mode for FujiDenzo layout
	 */
	private View btCoolLightFD;
	/*
	 * Light of button to switch to cool mode for Carrier layout
	 */
	private View btCoolLightCA;
	/*
	 * Light of button to switch to cool mode for American Home layout
	 */
	private View btCoolLightAM;
	/*
	 * Light of button to switch to dry mode for FujiDenzo layout
	 */
	private View btDryLightFD;
	/*
	 * Light of button to switch to dry mode for Carrier layout
	 */
	private View btDryLightCA;
	/*
	 * Light of button to switch to dry mode for American Home layout
	 */
	private View btDryLightAM;
	/*
	 * Light of button to switch to fan mode for FujiDenzo layout
	 */
	private View btFanLightFD;
	/*
	 * Light of button to switch to fan mode for Carrier layout
	 */
	private View btFanLightCA;
	/*
	 * Light of button to switch to fan mode for American Home layout
	 */
	private View btFanLightAM;
	/*
	 * Light of button to switch on sweep for Carrier layout
	 */
	private View btSweepLightCA;
	/*
	 * Light of button to switch on sweep for American Home layout
	 */
	private View btSweepLightAM;
	/*
	 * Light of button to switch on turbo mode for Carrier layout
	 */
	private View btTurboLightCA;
	/*
	 * Light of button to switch on turbo mode for American Home layout
	 */
	private View btSleepLightAM;
	/*
	 * Light of button to switch on ion mode for Carrier layout
	 */
	private View btIonLightCA;
	/*
	 * Light of button to switch on auto temp function for Carrier layout
	 */
	private View btAutomLightCA;
	/*
	 * Light of button to switch on auto temp function for American Home layout
	 */
	private View btAutomLightAM;
	/*
	 * Button to switch fan speed for Carrier layout
	 */
	private Button btFanCA;
	/*
	 * Button to switch fan speed for American Home layout
	 */
	private Button btFanAM;
	/*
	 * Consumption value display for FujiDenzo layout
	 */
	private TextView txtConsValFD;
	/*
	 * Temperature value display for FujiDenzo layout
	 */
	private TextView txtTempValFD;
	/*
	 * Status value display for FujiDenzo layout
	 */
	private TextView txtAutoStatusValFD;
	/*
	 * Consumption value display for Carrier layout
	 */
	private TextView txtConsValCA;
	/*
	 * Temperature value display for Carrier layout
	 */
	private TextView txtTempValCA;
	/*
	 * Status value display for Carrier layout
	 */
	private TextView txtAutoStatusValCA;
	/*
	 * Consumption value display for American Home layout
	 */
	private TextView txtConsValAM;
	/*
	 * Temperature value display for American Home layout
	 */
	private TextView txtTempValAM;
	/*
	 * Status value display for American Home layout
	 */
	private TextView txtAutoStatusValAM;

	/*
	 * Timer button for FujiDenzo layout
	 */
	private Button btTimerFD;
	/*
	 * Timer button for Carrier layout
	 */
	private Button btTimerCA;
	/*
	 * Timer button for American Home layout
	 */
	private Button btTimerAM;

	/*
	 * Color for activated button
	 */
	private static int colorRed;
	/*
	 * Color for deactivated button
	 */
	private static int colorGrey;
	/*
	 * Color for deactivated timer button
	 */
	private static int colorGreen;
	/*
	 * Color for activated timer button
	 */
	private static int colorOrange;

	/*
	 * ID of the selected device
	 */
	private static int selDevice = 0;
	/*
	 * Name of the device
	 */
	private static final String[] deviceName = {"", "", ""};
	/*
	 * Layout version for the device
	 */
	private static final int[] deviceType = {99, 99, 99};
	/*
	 * Valid device type ids
	 */
	private static final int FUJIDENZO = 0;
	private static final int CARRIER = 1;
	private static final int AMERICANHOME = 2;
	/*
	 * Location of the device
	 */
	private final String[] locationName = {"Office", "Living", "Office"};
	/*
	 * Icon for the device
	 */
	private final int[] deviceIcon = {7, 6, 7};
	/*
	 * Fan speed of device
	 */
	private static final int[] fanStatus = {0, 0, 0};
	/*
	 * Mode status of device
	 */
	private static final int[] modeStatus = {0, 0, 0};
	/*
	 * Power status of device
	 */
	private static final int[] powerStatus = {0, 0, 0};
	/*
	 * Cooling temperature of device
	 */
	private static final int[] coolStatus = {0, 0, 0};
	/*
	 * Timer setting of device
	 */
	private static final int[] deviceTimer = {1, 1, 1};
	/*
	 * Off time of timer of device
	 */
	private static final String[] deviceOffTime = {"", "", ""};
	/*
	 * Consumption status of device (only from master device
	 */
	private static double consStatus = 0;
	/*
	 * Auto power status of device (only from master device
	 */
	private static int autoStatus = 0;
	/*
	 * Auto power enabled status of device
	 */
	private static final int[] autoOnStatus = {0, 0, 0};
	/*
	 * Sweep enabled status of device
	 */
	private static final int[] sweepStatus = {0, 0, 0};
	/*
	 * Turbo enabled status of device
	 */
	private static final int[] turboStatus = {0, 0, 0};
	/*
	 * Ion enabled status of device
	 */
	private static final int[] ionStatus = {0, 0, 0};
	/*
	 * Timer on status of device
	 */
	private static final int[] timerStatus = {0, 0, 0};

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

	/*
	 * Debug device view
	 */
	private LinearLayout debugView = null;
	/*
	 * String with received debug messages
	 */
	private String debugMsgs = "";
	/*
	 * String of highlighted text
	 */
	private String highlightText = "";
	/*
	 * List of devices to send debug message to
	 */
	private static final boolean[] cmdDeviceList = {false, false, false, false, false, false, false, false, false, false, false};

	/*
	 * Text of snackbar view
	 */
	private String snackBarText = "";

	/*
	 * Debug device view
	 */
	private RelativeLayout seccamView = null;
	/*
	 * Zoomable image view attacher
	 */
	private PhotoViewAttacher snapShotAttacher = null;
	/*
	 * List with available images on image server
	 */
	private List<String> availImages;

	/*
	 * List with available CCTV footage
	 */
	public static CCTVfootages lists;

	/*
	 * Lights device view
	 */
	private RelativeLayout lightsView = null;
	/*
	 * Current value of bed room lights
	 */
	private int lightsBedRoomVal = 0;
	/*
	 * Light control seek bar
	 */
	private SeekBar bedRoomValSB;
	/*
	 * Textview for current brightness level
	 */
	private TextView bedRoomVal;

	/*
	 * Bonjour service, active as long as this app is active
	 */
	private DNSSDService registeredService = null;

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		intent.putExtra("view", getSharedPreferences(sharedPrefName, 0).getInt(prefsLastView, 0));
		// set the intent passed from the service to the original intent
		setIntent(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_home_control);
		/* Instance of the tool bar */
		Toolbar toolBar = findViewById(R.id.toolbar);
		if (toolBar != null) {
			setSupportActionBar(toolBar);
		}

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
			// Ask for permissions if necessary
			ArrayList<String> arrPerm = new ArrayList<>();

			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				arrPerm.add(Manifest.permission.ACCESS_COARSE_LOCATION);
			}

			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				arrPerm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
			}

			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				arrPerm.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			}

			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED) {
				arrPerm.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
			}

			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
				arrPerm.add(Manifest.permission.INTERNET);
			}
			if (!arrPerm.isEmpty()) {
				String[] permissions = new String[arrPerm.size()];
				permissions = arrPerm.toArray(permissions);
				ActivityCompat.requestPermissions(this, permissions, 0);
			}
		}

		// Enable access to internet
		/* ThreadPolicy to get permission to access internet */
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// Get pointer to shared preferences
		mPrefs = getSharedPreferences(sharedPrefName, 0);

		// Get context of the application to be reused in Async Tasks
		appContext = this;

		// Get list of last known devices and IP addresses from the preferences
		for (int i = 0; i < deviceNames.length; i++) {
			String deviceIP = mPrefs.getString(deviceNames[i], "NA");
			if (!deviceIP.equalsIgnoreCase("NA")) { // device saved?
				deviceIPs[i] = deviceIP;
				deviceIsOn[i] = true;
			} else {
				deviceIsOn[i] = false;
			}
		}

		// Initialize variables for buttons, layouts, views, ...
		setGlobalVar();

		// In case the database is not yet existing, open it once
		// Open databases
		dbHelperNow = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
		dbHelperLast = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);

		// Initiate databases (in case they are not existing yet)
		/* Instance of database */
		SQLiteDatabase dataBase;
		try {
			dataBase = dbHelperNow.getReadableDatabase();
			dataBase.beginTransaction();
			dataBase.endTransaction();
			dataBase.close();
			/* Instance of data base */
			dataBase = dbHelperLast.getReadableDatabase();
			dataBase.beginTransaction();
			dataBase.endTransaction();
			dataBase.close();
		} catch (Exception e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Database error: " + e.getMessage());
		}

		String locationStatus;
		if (Utilities.isHomeWiFi(this)) {
			locationStatus = getResources().getString(R.string.at_home);
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Found home WiFi");
		} else {
			locationStatus = getResources().getString(R.string.not_home);
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Didn't find home WiFi");
		}
		secStatus.setText(locationStatus);
		airStatus.setText(locationStatus);
		solStatus.setText(locationStatus);

		String layout = getString(R.string.layout);
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Selected layout = " + layout);

		if (registeredService == null) {
			// Register a service on this device
			try {
				DNSSD dnssd = new DNSSDBindable(this);

				String makerName = android.os.Build.MANUFACTURER.substring(0, 1).toUpperCase() + android.os.Build.MANUFACTURER.substring(1);
				TXTRecord myTxtRecord = new TXTRecord();
				myTxtRecord.set("type", "Display");
				myTxtRecord.set("id", makerName);
				myTxtRecord.set("board", android.os.Build.MODEL);
				myTxtRecord.set("service", "MHC");
				myTxtRecord.set("loc", android.os.Build.MODEL);
				registeredService = dnssd.register(NO_AUTO_RENAME, 0, "mobi", "_arduino._tcp"
						, null, null, 8266, myTxtRecord,
						new RegisterListener() {

							@Override
							public void serviceRegistered(DNSSDRegistration registration, int flags,
														  String serviceName, String regType, String domain) {
								if (BuildConfig.DEBUG)
									Log.d(DEBUG_LOG_TAG, "Register successfully ");
							}

							@Override
							public void operationFailed(DNSSDService service, int errorCode) {
								if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "error " + errorCode);
							}
						});
			} catch (DNSSDException e) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "error", e);
			}
		}
	}

	/*
	 * Called when activity is getting visible
	 */
	@Override
	protected void onResume() {
		super.onResume();

		if (myServiceIsStopped()) {
			// Start service to listen to TCP, UDP & MQTT broadcast messages
			startService(new Intent(this, MessageListener.class));
		}

		// Start discovery of mDNS/NSD services available if not running already
		startService(new Intent(this, CheckAvailDevices.class));

		// Register the receiver for messages from UDP & MQTT listener
		// Create an intent filter to listen to the broadcast sent with the action "BROADCAST_RECEIVED"
		/* Intent filter for app internal broadcast receiver */
		IntentFilter intentFilter = new IntentFilter(MessageListener.BROADCAST_RECEIVED);
		//Map the intent filter to the receiver
		registerReceiver(activityReceiver, intentFilter);

		// Initialize last known devices
		for (String deviceName1 : deviceNames) {
			String deviceIP = mPrefs.getString(deviceName1, "NA");
			if (!deviceIP.equalsIgnoreCase("NA")) { // device saved?
				new Initialize().execute(deviceName1);
			}
		}

		// Tell MQTT listener that we just started and need last status
		MessageListener.uiStarted = true;

		Intent thisIntent = getIntent();
		Bundle thisBundle = thisIntent.getExtras();
		// Get pointer to shared preferences
		mPrefs = getSharedPreferences(sharedPrefName, 0);

		if (thisBundle != null) {
			if (thisBundle.getInt("vID", 99) != 99) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Restart with view from bundle: "
						+ thisBundle.getInt("vID", 99));
				// Get the requested view of call
				visibleView = thisBundle.getInt("vID", view_security_id);
			}
		} else {
			// Get the last view the user had selected or security view if no preference is found
			visibleView = mPrefs.getInt(prefsLastView, view_security_id);
			if (BuildConfig.DEBUG)
				Log.d(DEBUG_LOG_TAG, "Restart with view from prefs: " + visibleView);
		}

		// Get the layouts of all possible views
		secView = findViewById(R.id.view_security);
		solView = findViewById(R.id.view_solar);
		airView = findViewById(R.id.view_aircon);
		debugView = findViewById(R.id.view_devdebug);
		seccamView = findViewById(R.id.view_seccam);
		lightsView = findViewById(R.id.view_lights);

		// Setup views
		switch (visibleView) {
			case view_security_id: // Security
				switchUI(view_security_id);
				break;
			case view_solar_id: // Solar panel
				switchUI(view_solar_id);
				break;
			case view_aircon_id: // Aircon
				switchUI(view_aircon_id);
				break;
			case view_devDebug_id: // Debug screen
				switchUI(view_devDebug_id);
				break;
			case view_seccam_id: // Security camera view
				switchUI(view_seccam_id);
				break;
			case view_lights_id: // Lights control
				switchUI(view_lights_id);
				break;
			case view_secVideo_id: // Security video view
				switchUI(view_secVideo_id);
				break;
			default:
				switchUI(view_solar_id);
				break;
		}

		// Setup aircon views
		switch (selDevice) {
			case 0:
				airCAView.setVisibility(View.INVISIBLE);
				airFDView.setVisibility(View.VISIBLE);
				airAMView.setVisibility(View.INVISIBLE);
				break;
			case 1:
				airFDView.setVisibility(View.INVISIBLE);
				airCAView.setVisibility(View.VISIBLE);
				airAMView.setVisibility(View.INVISIBLE);
				break;
			case 2:
				airCAView.setVisibility(View.INVISIBLE);
				airFDView.setVisibility(View.INVISIBLE);
				airAMView.setVisibility(View.VISIBLE);
				break;
		}

		// Open databases
		dbHelperNow = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME);
		dbHelperLast = new DataBaseHelper(appContext, DataBaseHelper.DATABASE_NAME_LAST);

		ImageButton ibButtonToChange = findViewById(R.id.ib_light_bed_on);
		ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_unavail));
		ibButtonToChange = findViewById(R.id.ib_light_bed_off);
		ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_unavail));
		ibButtonToChange = findViewById(R.id.ib_light_bed_dim);
		ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_unavail));

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Make sure to call the super method so that the states of our views are saved
		super.onSaveInstanceState(outState);
		// Save our own state now
		outState.putInt("vID", visibleView);
	}

	/*
	 * Called when activity is getting invisible
	 */
	@Override
	protected void onPause() {
		super.onPause();

		// Close databases
		dbHelperNow.close();
		dbHelperLast.close();

		// Unsubscribe from MQTT broker status messages
		MessageListener.unSubscribeBrokerStatus();

	}

	/*
	 * Called when activity is getting destroyed
	 * Handles security fragment specific tasks
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Stop Bonjour service broadcast
		if (registeredService != null) {
			registeredService.stop();
			registeredService = null;
		}
		// Unregister the receiver for messages from MQTT/TCP/UDP listener
		unregisterReceiver(activityReceiver);
		activityReceiver = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_my_home_control, menu);
		abMenu = menu;

		// Enable/Disable device debug view selection in menu
		// Check if we have the IP address 192.168.0.10
//		WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
//		WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
//		int ip = wifiInfo.getIpAddress();
//		@SuppressWarnings("deprecation") String ipAddress = Formatter.formatIpAddress(ip);
		MenuItem menuShowDebugItem = abMenu.getItem(action_debug_id); // Switch to device debug view menu entry
		MenuItem menuDebugViewItem = abMenu.getItem(action_devDebug_id); // Switch to device debug view menu entry
//		if (ipAddress.equalsIgnoreCase(getString(R.string.THIS_TABLET))) {
		menuShowDebugItem.setVisible(true);
		menuDebugViewItem.setVisible(true);
		if (showDebug) {
			menuShowDebugItem.setTitle(R.string.action_debug_off);
		} else {
			menuShowDebugItem.setTitle(R.string.action_debug);
		}

//		} else {
//			menuShowDebugItem.setVisible(false);
//			menuDebugViewItem.setVisible(false);
//		}

		return true;
	}

	@SuppressLint({"InflateParams", "ApplySharedPref"})
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/* Menu item pointer */
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
				/* Index of last user selected alarm tone */
				int uriIndex = Utilities.getNotifSounds(this, notifNames, notifUri, isSelAlarm) + 2;

				// get sound_selector.xml view
				/* Layout inflater for sound selection dialog */
				LayoutInflater alarmDialogInflater = LayoutInflater.from(this);
				/* View for sound selection dialog */
				@SuppressLint("InflateParams")
				View alarmSettingsView = alarmDialogInflater.inflate(R.layout.se_sound, null);
				/* Alert dialog builder for device selection dialog */
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
				/* Alert dialog  for device selection */
				AlertDialog alarmDialog = alarmDialogBuilder.create();

				// show it
				alarmDialog.show();

				/* Pointer to list view with the alarms */
				ListView lvAlarmList = alarmSettingsView.findViewById(R.id.lv_AlarmList);
				/* Array adapter for the ListView */
				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
						appContext,
						R.layout.cu_list_item,
						notifNames);
				lvAlarmList.setAdapter(arrayAdapter);
				// Use long click listener to play the alarm sound
				lvAlarmList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
					public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
												   int pos, long id) {
						/* Instance of media player */
						MediaPlayer mMediaPlayer = new MediaPlayer();
						try {
							mMediaPlayer.setDataSource(appContext, Uri.parse(notifUri.get(pos)));
							/* Audio manager to play the sound */
							final AudioManager audioManager = (AudioManager) appContext
									.getSystemService(Context.AUDIO_SERVICE);
							if (audioManager != null && audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
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
				switchUI(view_security_id);
				break;
			case R.id.action_solar:
				// Show solar panel UI
				switchUI(view_solar_id);
				break;
			case R.id.action_aircon:
				// Show aircon UI
				switchUI(view_aircon_id);
				break;
			case R.id.action_clear_devices:
				mPrefs.edit().remove("am1").apply();
				mPrefs.edit().remove("sb1").apply();
				mPrefs.edit().remove("mhc").apply();
				mPrefs.edit().remove("lb1").apply();
				mPrefs.edit().remove("").apply();
				mPrefs.edit().remove("spm").apply();
				mPrefs.edit().remove("cm1").apply();
				mPrefs.edit().commit();
//				if (Utilities.isHomeWiFi(this)) {
//					// Start discovery of mDNS/NSD services available if not running already
//					if (myServiceIsStopped(CheckAvailDevices.class)) {
//						startService(new Intent(this, CheckAvailDevices.class));
//					}
//				} else {
//					Toast.makeText(getApplicationContext(), getResources().getString(R.string.scan_impossible), Toast.LENGTH_LONG).show();
//				}
				break;
			case R.id.action_debug:
				showDebug = !showDebug;
				mPrefs.edit().putBoolean(prefsShowDebug, showDebug).apply();
				menuItem = abMenu.getItem(action_debug_id); // Debug menu entry
				if (showDebug) {
					menuItem.setTitle(R.string.action_debug_off);
				} else {
					menuItem.setTitle(R.string.action_debug);
				}
				break;
			case R.id.action_syncSolar:
				startService(new Intent(this, SolarSyncDataBase.class));
				break;
			case R.id.action_locations:
				// get location_selector.xml view
				/* Layout inflater for device selection dialog */
				LayoutInflater locationDialogInflater = LayoutInflater.from(this);
				locationSettingsView = locationDialogInflater.inflate(R.layout.se_location, null);
				/* Alert dialog builder for device selection dialog */
				AlertDialog.Builder locationDialogBuilder = new AlertDialog.Builder(this);

				// set location_selector.xml to alert dialog builder
				locationDialogBuilder.setView(locationSettingsView);

				/* Pointer to button, used to set OnClickListener for buttons in the dialog */
				Button btToSetOnClickListener;
				/* Pointer to button text, used to give each button in the dialog a specific name */
				String buttonTxt;

				for (int i = aircon1Index; i < aircon1Index + 3; i++) {
					if (deviceIsOn[i]) {
						btToSetOnClickListener = locationSettingsView.findViewById(buttonIds[i]);
						btToSetOnClickListener.setVisibility(View.VISIBLE);
						if (locationName[i - aircon1Index].equalsIgnoreCase("")) {
							btToSetOnClickListener.setText(deviceName[i - aircon1Index]);
						} else {
							buttonTxt = locationName[i - aircon1Index];
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
									@SuppressWarnings("ConstantConditions")
									public void onClick(DialogInterface dialog, int id) {
										for (int i = -aircon1Index; i < -aircon1Index + 3; i++) {
											mPrefs.edit().putString(
													prefsLocationName + i,
													locationName[i - aircon1Index]).apply();
											mPrefs.edit().putInt(
													prefsDeviceIcon + i,
													deviceIcon[i - aircon1Index]).apply();
										}
										dialog.cancel();
									}
								});

				// create alert dialog
				/* Alert dialog  for device selection */
				AlertDialog alertDialog = locationDialogBuilder.create();

				// show it
				alertDialog.show();

				break;
			case R.id.action_devDebug:
				// Show security UI
				switchUI(view_devDebug_id);
				break;
			case R.id.action_lightControl:
				// Show light control view
				switchUI(view_lights_id);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
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

	/*
	 * Called when a view has been clicked.
	 *
	 * @param v
	 * 		The view that was clicked.
	 */
	@SuppressLint("InflateParams")
	@Override
	public void onClick(View v) {

		if (!handleSPMbuttons(v)) { // Check if it was a solar panel view button and handle it
			if (!handleSecurityButtons(v)) { // Check if it was a security view button and handle it
				if (!handleAirconButtons(v)) { // Check if it was a aircon view button and handle it
					if (!handleDebugButtons(v)) { // Check if it was a debug view button and handle it
						if (!handleLightButtons(v)) { // Check if it was a light control view button and handle it
							switch (v.getId()) { // Handle other buttons right here
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
											dlgDeviceIndex = AMERICANHOME;
											break;
									}
									// get location_selector.xml view
									/* Layout inflater for dialog to change device name and icon */
									LayoutInflater airconDialogInflater = LayoutInflater.from(this);
									/* View of aircon device name and icon change dialog */
									airconDialogView = airconDialogInflater.inflate(R.layout.se_locations, null);
									/* Alert dialog builder for dialog to change device name and icon */
									AlertDialog.Builder airconDialogBuilder = new AlertDialog.Builder(this);
									// set location_selector.xml to alert dialog builder
									airconDialogBuilder.setView(airconDialogView);

									/* Button to set onClickListener for icon buttons in the dialog */
									ImageButton btOnlyClickListener;

									for (int i = 0; i < 8; i++) {
										btOnlyClickListener = airconDialogView.findViewById(iconButtons[i]);
										btOnlyClickListener.setOnClickListener(this);
									}

									dlgIconIndex = deviceIcon[dlgDeviceIndex];
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);

									/* Edit text field for the user selected device name */
									final EditText userInput = airconDialogView.findViewById(R.id.dia_et_location);
									userInput.setText(locationName[dlgDeviceIndex]);

									// set dialog message
									airconDialogBuilder
											.setTitle(getResources().getString(R.string.dialog_change_title))
											.setCancelable(false)
											.setPositiveButton("OK",
													new DialogInterface.OnClickListener() {
														public void onClick(DialogInterface dialog, int id) {
															locationName[dlgDeviceIndex] = userInput.getText().toString();
															deviceIcon[dlgDeviceIndex] = dlgIconIndex;
															// Update underlying dialog box with new device name
															/* Button of selection dialog that we are processing */
															Button btToChangeName = locationSettingsView.findViewById(buttonIds[dlgDeviceIndex]);
															btToChangeName.setText(locationName[dlgDeviceIndex]);
															locationSettingsView.invalidate();
															// Update UI
															/* Text view to show location name */
															TextView locationText;
															/* Image view to show location icon */
															ImageView locationIcon;
															switch (dlgDeviceIndex) {
																case FUJIDENZO:
																	locationText = findViewById(R.id.txt_device_fd);
																	locationText.setText(locationName[dlgDeviceIndex]);
																	locationIcon = findViewById(R.id.im_icon_fd);
																	locationIcon.setImageDrawable(getResources().getDrawable(iconIDs[deviceIcon[dlgDeviceIndex]]));
																	break;
																case CARRIER:
																	locationText = findViewById(R.id.txt_device_ca);
																	locationText.setText(locationName[dlgDeviceIndex]);
																	locationIcon = findViewById(R.id.im_icon_ca);
																	locationIcon.setImageDrawable(getResources().getDrawable(iconIDs[deviceIcon[dlgDeviceIndex]]));
																	break;
																case AMERICANHOME:
																	locationText = findViewById(R.id.txt_device_am);
																	locationText.setText(locationName[dlgDeviceIndex]);
																	locationIcon = findViewById(R.id.im_icon_am);
																	locationIcon.setImageDrawable(getResources().getDrawable(iconIDs[deviceIcon[dlgDeviceIndex]]));
																	break;
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
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_bed:
									dlgIconIndex = 1;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_dining:
									dlgIconIndex = 2;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_entertain:
									dlgIconIndex = 3;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_kids:
									dlgIconIndex = 4;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_kitchen:
									dlgIconIndex = 5;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_living:
									dlgIconIndex = 6;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
								case R.id.im_office:
									dlgIconIndex = 7;
									Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView, appContext);
									break;
							}
						}
					}
				}
			}
		}
	}

	/*
	 * Called when an image has been loaded from the gallery server.
	 *
	 * @param bitmap
	 * 		The bitmap that has been downloaded.
	 */
	@Override
	public void onImageLoaded(Bitmap bitmap) {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received image");
		ImageView mImageView = findViewById(R.id.iv_snapshot);
		mImageView.setImageBitmap(bitmap);
		if (snapShotAttacher == null) {
			snapShotAttacher = new PhotoViewAttacher(mImageView);
		} else {
			snapShotAttacher.update();
		}
		TextView imageName = findViewById(R.id.tv_image_name);
		imageName.setVisibility(View.VISIBLE);
		imageName.setText(availImages.get(0));
		switchUI(view_seccam_id); // Switch to security camera view
	}

	/*
	 * Called when an error occurred while an image was loaded from the gallery server.
	 */
	@Override
	public void onError() {
		Toast.makeText(this, "Error Loading Image !", Toast.LENGTH_SHORT).show();
	}

	/*
	 * Called when brightness value seekbar changed.
	 */
	@Override
	public void onProgressChanged(SeekBar v, int progress, boolean isUser) {
		String lightValue;
		lightsBedRoomVal = 222 - progress;
		if (lightsBedRoomVal == 140) {
			lightValue = getString(R.string.lights_val_on); // Bulbs are full on
		} else if (lightsBedRoomVal > 222) {
			lightValue = getString(R.string.lights_val_off); // Bulbs are full off
		} else {
			lightValue = getString(R.string.lights_val_dim); // Bulbs are dimmed on
		}
		bedRoomVal.setText(lightValue);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		int progress = bedRoomValSB.getProgress();
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "onStopTrackingTouch: " + progress);
		lightsBedRoomVal = 222 - progress;
		switch (progress) {
			case 82:
				bedRoomVal.setText(getString(R.string.lights_val_on)); // Bulbs are full on

				lightsBedRoomVal = 140;
				break;
			case 0:
				bedRoomVal.setText(getString(R.string.lights_val_off)); // Bulbs are full off

				lightsBedRoomVal = 255;
				break;
			default:
				bedRoomVal.setText(getString(R.string.lights_val_dim)); // Bulbs are dimmed on

				lightsBedRoomVal = 222 - progress;
				break;
		}
		String lightValue = "b=" + lightsBedRoomVal;
		String url = deviceIPs[lb1Index];
//		new MyHomeControl.ESPbyTCP(getString(R.string.LIGHTS_BEDROOM),lightValue,"0");
		new MyHomeControl.ESPbyTCP(url, lightValue, "0");
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Send new brightness to lights: " + lightValue);
	}

	/*
	 * Broadcast receiver for notifications received over UDP or MQTT or GCM
	 */
	private BroadcastReceiver activityReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			/* Message origin */
			String sender = intent.getStringExtra("from");

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received broadcast from " + sender);

			switch (sender) {
				case "SPSYNC":
					handleSPMSyncBC(intent);
					break;
				case "NSD":
//					handleNSDResultBC(intent);
					break;
				case "DEBUG":
					handleDebugBC(intent);
					break;
				case "STATUS":
					handleStatusBC(intent);
					break;
				case "BROKER":
					debugViewUpdate();
					break;
				default:
					handleDeviceStatusBC(intent);
					break;
			}
		}
	};

	/* Handle broadcast messages about SPM data sync
	 *
	 * @param bcIntent
	 *            Intent of received broadcast
	 */
	private void handleSPMSyncBC(Intent bcIntent) {
		updateSynced(bcIntent.getStringExtra("message"));
	}

//	/* Handle broadcast messages about device search result
//	 *
//	 * @param bcIntent
//	 *            Intent of received broadcast
//	 */
//	private void handleNSDResultBC(Intent bcIntent) {
//		boolean searchAlternative = bcIntent.getBooleanExtra("alternative",false);
//		boolean nsdScanResult = bcIntent.getBooleanExtra("resolved",false);
//		String message;
//		if (nsdScanResult) { // Scan finished successful
//			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Device scan finished");
//			message = "Device scan successful finished";
//			StringBuilder nsdResults;
//			if (!searchAlternative) {
//				// Clean up and refresh preferences from found devices
//				for (int i=0; i<deviceNames.length; i++) {
//					try {
//						int listIndex = 0;
//						for (int j = 0; j< dnssdFoundServices; j++) {
//							if (deviceNames[i].equalsIgnoreCase(dnssdServicesNames[j])) {
//								// found the device, break the loop
//								listIndex = j;
//								break;
//							}
//						}
//						deviceIPs[i] = dnssdServicesHosts[listIndex].toString().substring(1);
//						deviceIsOn[i] = true;
//						new Initialize().execute(deviceNames[i]);
//					} catch (NullPointerException ignore) {
//						mPrefs.edit().remove(deviceNames[i]).apply();
//						deviceIsOn[i] = false;
//					}
//				}
//				debugViewUpdate();
//				nsdResults = new StringBuilder("Found devices & IP's:\n\n");
//				for (int i=0; i<deviceNames.length; i++) {
//					if (deviceIsOn[i]) {
//						nsdResults.append(deviceIPs[i])
//										.append("\t\t")
//										.append(deviceNames[i])
//										.append("\n");
//					}
//				}
//			} else {
//				nsdResults = new StringBuilder("Found other devices & IP's:\n\n");
//				for (int j = 0; j< dnssdFoundServices; j++) {
//					nsdResults.append(dnssdServicesHosts[j].toString().substring(1)).append("\t\t").append(dnssdServicesNames[j]).append("\n");
//				}
//			}
//			if (showDebug) {
//				Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
//								nsdResults.toString(),
//								Snackbar.LENGTH_INDEFINITE);
//				mySnackbar.setAction("OK", mOnClickListener);
//				mySnackbar.show();
//				View snackbarView = mySnackbar.getView();
//				TextView tv= (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
//				tv.setMaxLines(dnssdServicesNames.length+2);
//				tv.setVerticalScrollBarEnabled(true);
//				tv.setMovementMethod(new ScrollingMovementMethod());
//			}
//		} else { // Scan error
//			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Device scan stopped with error");
//			message = "Device scan stopped with error";
//		}
//		if (BuildConfig.DEBUG) {
//			if (showDebug) {
//				if (!snackBarText.isEmpty()) {
//					snackBarText += "\n" + message;
//				} else {
//					snackBarText = message;
//				}
//				Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
//								snackBarText,
//								Snackbar.LENGTH_INDEFINITE);
//				mySnackbar.setAction("OK", mOnClickListener);
//				mySnackbar.show();
//				View snackbarView = mySnackbar.getView();
//				TextView tv= (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
//				tv.setMaxLines(5);
//				tv.setVerticalScrollBarEnabled(true);
//				tv.setMovementMethod(new ScrollingMovementMethod());
//			}
//		}
//	}

	/* Handle broadcast messages about debug status
	 *
	 * @param bcIntent
	 *            Intent of received broadcast
	 */
	private void handleDebugBC(Intent bcIntent) {
		String message = bcIntent.getStringExtra("message");
//		String deviceName = "";
//		if(message.contains(" ")){
//			deviceName= message.substring(0, message.indexOf(" "));
//		}
//		if (!deviceName.isEmpty()) {
//			Button buttonToChange = null;
//			int listIndex = 0;
//			switch (deviceName) {
//				//"spMonitor", "sf1", "sb1", "fd1", "ca1", "am1", "moni", "cm1", "lb1"
//				//    0           1      2      3      4      5      6      7       8
//				case "moni":
//					buttonToChange = (Button) findViewById(R.id.bt_debug_moni);
//					listIndex = moniIndex;
//					break;
//				case "sf1":
//					buttonToChange = (Button) findViewById(R.id.bt_debug_secf);
//					listIndex = secFrontIndex;
//					break;
//				case "sb1":
//					buttonToChange = (Button) findViewById(R.id.bt_debug_secb);
//					listIndex = secBackIndex;
//					break;
//				case "fd1":
//					buttonToChange = (Button) findViewById(R.id.bt_debug_ac1);
//					listIndex = aircon1Index;
//					break;
//				case "ca1":
//					buttonToChange = (Button) findViewById(R.id.bt_debug_ac2);
//					listIndex = aircon2Index;
//					break;
//				// TODO add button for American Home aircon
////						case "am1":
////							buttonToChange = (Button) findViewById(R.id.bt_debug_ac3);
////							listIndex = aircon3Index;
////							break;
//				case "cm1":
//					listIndex = cam1Index;
//					buttonToChange = (Button) findViewById(R.id.bt_debug_cam1);
//					break;
//			}
//			if (buttonToChange != null) {
//				if (message.contains("TCP is on")) {
//					buttonToChange.setBackgroundColor(colorGreen);
//					debugDeviceList[listIndex] = true;
//				}
//				if (message.contains("TCP is off")) {
//					buttonToChange.setBackgroundColor(colorOrange);
//					debugDeviceList[listIndex] = false;
//				}
//			}
//		} else {
//			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received unknown broadcast: " + message);
//		}
		debugMsgs += message + "\n";

		TextView debugTxtView = findViewById(R.id.tv_sv_debug);
		if (highlightText == null || highlightText.isEmpty()) {
			debugTxtView.append(message + "\n");
		} else {
			if (message.toUpperCase().contains(highlightText.toUpperCase())) {
				debugTxtView.append(message + "\n");
			}
		}
		ScrollView debugTxtScroll = findViewById(R.id.sv_debugview);
		debugTxtScroll.fullScroll(View.FOCUS_DOWN);
	}

	/* Handle broadcast messages about device status
	 *
	 * @param bcIntent
	 *            Intent of received broadcast
	 */
	private void handleStatusBC(Intent bcIntent) {
		if (BuildConfig.DEBUG) {
			String message = bcIntent.getStringExtra("message");
			if (showDebug) {
				TextView debugTxtView = findViewById(R.id.tv_sv_debug);
				if (highlightText == null || highlightText.isEmpty()) {
					debugTxtView.append(message + "\n");
				} else {
					if (message.toUpperCase().contains(highlightText.toUpperCase())) {
						debugTxtView.append(message + "\n");
					}
				}
				ScrollView debugTxtScroll = findViewById(R.id.sv_debugview);
				debugTxtScroll.fullScroll(View.FOCUS_DOWN);
				debugViewUpdate();
			}
		}
	}

	/* Handle broadcast messages about SPM data sync
	 *
	 * @param bcIntent
	 *            Intent of received broadcast
	 */
	private void handleDeviceStatusBC(Intent bcIntent) {
		/* Values for handlers */
		CommResult result = new CommResult();
		String message = bcIntent.getStringExtra("message");
		TextView debugTxtView = findViewById(R.id.tv_sv_debug);

		// Check if response is a JSON array
		if (Utilities.isJSONValid(message)) {
			result.comResult = message;
			JSONObject jsonResult;
			try {
				jsonResult = new JSONObject(message);
				String broadCastDevice;

				// Get device name
				if (jsonResult.has("de")) {
					broadCastDevice = jsonResult.getString("de");
					int dotPos;

					// Check if device is already known
					switch (broadCastDevice) {
						case "spm":
							if (!deviceIsOn[spMonitorIndex]) {
								deviceIsOn[spMonitorIndex] = true;
								if (BuildConfig.DEBUG)
									Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							if (cmdDeviceList[spMonitorIndex]) {
								debugMsgs += message + "\n";
							}
							result.comCmd = "/?s";
							solarViewUpdate(message, true);
							break;
						case "fd1":
							if (!deviceIsOn[aircon1Index]) {
								deviceIsOn[aircon1Index] = true;
								if (BuildConfig.DEBUG)
									Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							if (cmdDeviceList[aircon1Index]) {
								debugMsgs += message + "\n";
							}
							result.comCmd = "/?s";
							result.deviceIndex = 0;
							airconViewUpdate(result);
							break;
						case "ca1":
							if (!deviceIsOn[aircon2Index]) {
								deviceIsOn[aircon2Index] = true;
								if (BuildConfig.DEBUG)
									Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							if (cmdDeviceList[aircon2Index]) {
								debugMsgs += message + "\n";
							}
							result.comCmd = "/?s";
							result.deviceIndex = 1;
							airconViewUpdate(result);
							break;
						case "am1":
							if (!deviceIsOn[aircon3Index]) {
								deviceIsOn[aircon3Index] = true;
								if (BuildConfig.DEBUG)
									Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							if (cmdDeviceList[aircon3Index]) {
								debugMsgs += message + "\n";
							}
							result.comCmd = "/?s";
							result.deviceIndex = 1;
							airconViewUpdate(result);
							break;
						case "sf1":
							if (!deviceIsOn[secFrontIndex]) {
								deviceIsOn[secFrontIndex] = true;
								if (BuildConfig.DEBUG)
									Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							if (cmdDeviceList[secFrontIndex]) {
								debugMsgs += message + "\n";
							}
							result.comCmd = "/?s";
							securityViewUpdate(result);
							break;
						case "sb1":
							if (!deviceIsOn[secBackIndex]) {
								deviceIsOn[secBackIndex] = true;
								if (BuildConfig.DEBUG)
									Log.d(DEBUG_LOG_TAG, "Found " + broadCastDevice);
								new Initialize().execute(broadCastDevice);
							}
							if (cmdDeviceList[secBackIndex]) {
								debugMsgs += message + "\n";
							}
							TextView outsideWeatherTV = findViewById(R.id.tv_weather_out);
							String outsideWeather = "Outside\n";
							String leadDigits;
							if (jsonResult.has("te")) {
								leadDigits = jsonResult.getString("te");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0, dotPos);
								}
								outsideWeather = outsideWeather + leadDigits + "\"C\n";
							}
							if (jsonResult.has("hu")) {
								leadDigits = jsonResult.getString("hu");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0, dotPos);
								}
								outsideWeather = outsideWeather + leadDigits + "%";
							}
							outsideWeatherTV.setText(outsideWeather);
							result.comCmd = "/?s";
							securityViewUpdate(result);
							break;
						case "wei":
							TextView insideWeatherTV = findViewById(R.id.tv_weather_in);
							String insideWeather = "Inside\n";
							if (jsonResult.has("te")) {
								leadDigits = jsonResult.getString("te");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0, dotPos);
								}
								insideWeather = insideWeather + leadDigits + "\"C\n";
							}
							if (jsonResult.has("hu")) {
								leadDigits = jsonResult.getString("hu");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0, dotPos);
								}
								insideWeather = insideWeather + leadDigits + "%";
							}
							insideWeatherTV.setText(insideWeather);
							break;
						case "weo":
							outsideWeatherTV = findViewById(R.id.tv_weather_out);
							outsideWeather = "Outside\n";
							if (jsonResult.has("te")) {
								leadDigits = jsonResult.getString("te");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0, dotPos);
								}
								outsideWeather = outsideWeather + leadDigits + "\"C\n";
							}
							if (jsonResult.has("hu")) {
								leadDigits = jsonResult.getString("hu");
								if ((dotPos = leadDigits.indexOf(".")) != -1) {
									leadDigits = leadDigits.substring(0, dotPos);
								}
								outsideWeather = outsideWeather + leadDigits + "%";
							}
							outsideWeatherTV.setText(outsideWeather);
							break;
						case "cm1":
							if (cmdDeviceList[cam1Index]) {
								debugMsgs += message + "\n";
							}
							result.comCmd = "/?s";
							securityViewUpdate(result);
							break;
						case "lb1":
							String lightValue = getResources().getString(R.string.lights_val_unknown);
							if (jsonResult.has("br")) {
								lightsBedRoomVal = jsonResult.getInt("br");
								if (lightsBedRoomVal == 140) {
									lightValue = getString(R.string.lights_val_on); // Bulbs are full on
								} else if (lightsBedRoomVal > 222) {
									lightValue = getString(R.string.lights_val_off); // Bulbs are full off
								} else {
									lightValue = getString(R.string.lights_val_dim); // Bulbs are dimmed on
								}
							} else {
								lightsBedRoomVal = 0;
							}
							bedRoomVal.setText(lightValue);
							bedRoomValSB.setProgress(222 - lightsBedRoomVal);

							if (jsonResult.has("di")) {
								int newDimLightLevel = jsonResult.getInt("di");
								if (newDimLightLevel != mPrefs.getInt(prefsLightBedDim, 200)) {
									mPrefs.edit().putInt(prefsLightBedDim, newDimLightLevel).apply();
								}
							}
							ImageButton ibButtonToChange = findViewById(R.id.ib_light_bed_on);
							ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_on));
							ibButtonToChange = findViewById(R.id.ib_light_bed_off);
							ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_off));
							ibButtonToChange = findViewById(R.id.ib_light_bed_dim);
							ibButtonToChange.setImageDrawable(getResources().getDrawable(R.mipmap.ic_bulb_dim));
							break;
						case "ly1":
							if (jsonResult.has("lo")) {
								ToggleButton byLightSwitch = findViewById(R.id.tb_byard_light);
								if (jsonResult.getInt("lo") == 0) {
									byLightSwitch.setChecked(false);
								} else {
									byLightSwitch.setChecked(true);
								}
							}
							break;
						case "moni":
							if (cmdDeviceList[moniIndex]) {
								debugMsgs += message + "\n";
							}
							break;
					}
					if (showDebug) {
						if (highlightText == null || highlightText.isEmpty()) {
							debugTxtView.append(message + "\n");
						} else {
							if (message.toUpperCase().contains(highlightText.toUpperCase())) {
								debugTxtView.append(message + "\n");
							}
						}
						ScrollView debugTxtScroll = findViewById(R.id.sv_debugview);
						debugTxtScroll.fullScroll(View.FOCUS_DOWN);
					}
					debugViewUpdate();
				}
			} catch (JSONException e) {
				if (BuildConfig.DEBUG)
					Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
			}
		}
	}

	/*
	 * Communication in Async Task between Android and Arduino Yun
	 */
	@SuppressLint("StaticFieldLeak")
	private class SPMPcommunication extends AsyncTask<String, String, CommResult> {

		/*
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
		 * 	@return <code>CommResult</code>
		 * 			Requester ID and result of communication
		 */
		@Override
		protected CommResult doInBackground(String... params) {

			/* Return values for onPostExecute */
			CommResult result = new CommResult();

			result.httpURL = params[0];
			result.comCmd = params[1];
			result.comResult = params[2];
//			result.deviceIndex = Integer.parseInt(params[4]);
			result.comFailed = false;

			Context thisAppContext = getApplicationContext();

			/* A HTTP client to access the YUN device */
			// Set timeout to 5 minutes in case we have a lot of data to load
			OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(300, TimeUnit.SECONDS)
					.writeTimeout(10, TimeUnit.SECONDS)
					.readTimeout(300, TimeUnit.SECONDS)
					.build();

			if (!Utilities.isHomeWiFi(getApplicationContext())) {
				// For solar panel monitor get data from web site if we are not home
				result.httpURL = "www.spMonitor.giesecke.tk";
//				result.httpURL = "www.spmonitor.giesecke.tk";
				result.comCmd = "/l.php";
			}
			/* URL to be called */
			String urlString = "http://" + result.httpURL + result.comCmd; // URL to call

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callSPM = " + urlString);

			/* Request to ESP device */
			Request request = new Request.Builder()
					.url(urlString)
					.build();

			if (request != null) {
				try {
					/* Response from SPM device */
					Response response = client.newCall(request).execute();
					if (response != null) {
						result.comResult = response.body().string();
					} else {
						result.comFailed = true;
					}
				} catch (IOException e) {
					result.comResult = e.getMessage();
					try {
						String errorMsg = thisAppContext.getString(R.string.err_esp);
						if (result.comResult.contains("EHOSTUNREACH") || result.comResult.equalsIgnoreCase("")) {
							result.comResult = errorMsg;
							if (Utilities.isHomeWiFi(getApplicationContext())) {
								// Set spMonitor device as not available
								deviceIsOn[spMonitorIndex] = false;
								deviceIPs[spMonitorIndex] = "";
								result.comFailed = true;
							}
						}
						return result;
					} catch (NullPointerException en) {
						result.comResult = thisAppContext.getString(R.string.err_no_esp);
						if (Utilities.isHomeWiFi(getApplicationContext())) {
							// Set spMonitor device as not available
							deviceIsOn[spMonitorIndex] = false;
							deviceIPs[spMonitorIndex] = "";
							result.comFailed = true;
						}
						return result;
					}
				}
			}

			if (result.comResult.equalsIgnoreCase("")) {
				result.comResult = thisAppContext.getString(R.string.err_esp);
				result.comFailed = true;
			}
			return result;
		}

		/*
		 * Called when AsyncTask background process is finished
		 *
		 * @param result
		 * 		CommResult with requester ID and result of communication
		 */
		protected void onPostExecute(CommResult result) {
			if (BuildConfig.DEBUG && result.comFailed) {
				if (showDebug) {
					if (!snackBarText.isEmpty()) {
						snackBarText += "\n" + result.comResult;
					} else {
						snackBarText = result.comResult;
					}
					Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
							snackBarText,
							Snackbar.LENGTH_INDEFINITE);
					mySnackbar.setAction("OK", mOnClickListener);
					mySnackbar.show();
					View snackbarView = mySnackbar.getView();
					TextView tv = snackbarView.findViewById(android.support.design.R.id.snackbar_text);
					tv.setMaxLines(5);
					tv.setVerticalScrollBarEnabled(true);
					tv.setMovementMethod(new ScrollingMovementMethod());
				}
			}
			if (!dataBaseIsEmpty) {
				solarViewUpdate(result.comResult, false);
			}
		}
	}

	/*
	 * Send topic to MQTT broker in AsyncTask
	 */
	@SuppressLint("StaticFieldLeak")
	private class doPublishAsync extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String payload = params[0];
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT topic publish: " + payload);
			if (MessageListener.mqttClient == null) { // If service is not (yet) active, don't publish
				if (BuildConfig.DEBUG)
					Log.d(DEBUG_LOG_TAG, "MQTT topic publish failed - not connected");
				return null;
			}
			IMqttToken token;
			try {
				byte[] encodedPayload;
				encodedPayload = payload.getBytes(StandardCharsets.UTF_8);
				MqttMessage message = new MqttMessage(encodedPayload);
				message.setQos(0);
				message.setRetained(true);
				token = MessageListener.mqttClient.publish("/CMD", message);
				token.waitForCompletion(5000);
			} catch (MqttSecurityException e) {
				if (BuildConfig.DEBUG)
					Log.d(DEBUG_LOG_TAG, "MQTT publish exception " + e.getMessage());
			} catch (MqttException e) {
				switch (e.getReasonCode()) {
					case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG, "BROKER_UNAVAILABLE " + e.getMessage());
						break;
					case MqttException.REASON_CODE_CLIENT_TIMEOUT:
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG, "CLIENT_TIMEOUT " + e.getMessage());
						break;
					case MqttException.REASON_CODE_CONNECTION_LOST:
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG, "CONNECTION_LOST " + e.getMessage());
						break;
					case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG, "SERVER_CONNECT_ERROR " + e.getMessage());
						break;
					case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG, "FAILED_AUTHENTICATION " + e.getMessage());
						break;
					default:
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG, "MQTT unknown error " + e.getMessage());
						break;
				}
			}
			return null;
		}
	}

	/*
	 * Communication in Async Task between Android and ESP8266 over TCP
	 */
	@SuppressLint("StaticFieldLeak")
	private class ESPbyTCPAsync extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String targetAddress = params[0];
			String targetMessage = params[1];

			if (targetAddress.equalsIgnoreCase("")) { // target address is empty, don't try to connect!
				return null;
			}
			try {
				InetAddress tcpServer = InetAddress.getByName(targetAddress);
				Socket tcpSocket = new Socket(tcpServer, MessageListener.TCP_CLIENT_PORT);

				tcpSocket.setSoTimeout(10000);
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Sending " + targetMessage
						+ " to " + targetAddress);
				PrintWriter out = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(tcpSocket.getOutputStream())), true);
				out.println(targetMessage);
				sleep(100); // Give server time to read the data
				tcpSocket.close();
			} catch (Exception e) {
				if (BuildConfig.DEBUG)
					Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
							+ " " + targetAddress);
			}
			return null;
		}
	}

	/*
	 * Communication as Runnable between Android and ESP8266 over TCP
	 */
	private class ESPbyTCP implements Runnable {

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
			if (!Utilities.isHomeWiFi(getApplicationContext()) && !targetDevice.equalsIgnoreCase("chk")) {
				String mqttTopic = "{\"ip\":\"" + targetDevice + "\","; // Device IP address
//				mqttTopic += "\"cm\":\"" + targetMessage.substring(1) + "\"}"; // The command
				mqttTopic += "\"cm\":\"" + targetMessage + "\"}"; // The command

				new doPublishAsync().execute(mqttTopic);
			} else {
				new ESPbyTCPAsync().execute(targetAddress, targetMessage);
			}
		}
	}

	/*
	 * Communication in Async Task between Android and Picture Gallery Server
	 */
	@SuppressLint("StaticFieldLeak")
	private class galleryCommunication extends AsyncTask<String, String, CommResult> {

		/*
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
		 * 	@return <code>CommResult</code>
		 * 			Requester ID and result of communication
		 */
		@Override
		protected CommResult doInBackground(String... params) {

			/* Return values for onPostExecute */
			CommResult result = new CommResult();

			result.httpURL = params[0];
			result.comCmd = params[1];
			result.deviceIndex = Integer.parseInt(params[2]);
			result.comFailed = false;

			Context thisAppContext = getApplicationContext();

			/* A HTTP client to access the gallery server */
			// Set timeout to 5 minutes in case we have a lot of data to load
			OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(300, TimeUnit.SECONDS)
					.writeTimeout(10, TimeUnit.SECONDS)
					.readTimeout(300, TimeUnit.SECONDS)
					.build();

			/* URL to be called */
//			String urlString = "https://" + result.httpURL + result.comCmd; // URL to call
			String urlString = "http://1s.giesecke.tk" + result.comCmd; // URL to call

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callGallery = " + urlString);

			/* Request to gallery server */
			Request request = new Request.Builder()
					.url(urlString)
					.build();

			if (request != null) {
				try {
					/* Response from gallery server */
					Response response = client.newCall(request).execute();
					if (response != null) {
						result.comResult = response.body().string();
					} else {
						result.comFailed = true;
					}
				} catch (IOException e) {
					result.comResult = e.getMessage();
					try {
						String errorMsg = thisAppContext.getString(R.string.err_gallery);
						if (result.comResult.contains("EHOSTUNREACH") || result.comResult.equalsIgnoreCase("")) {
							result.comResult = errorMsg;
						}
						result.comFailed = true;
						return result;
					} catch (NullPointerException en) {
						result.comResult = thisAppContext.getString(R.string.err_gallery);
						result.comFailed = true;
						return result;
					}
				}
			}

			if (result.comResult.equalsIgnoreCase("")) {
				result.comResult = thisAppContext.getString(R.string.err_gallery);
				result.comFailed = true;
			}
			return result;
		}

		/*
		 * Called when AsyncTask background process is finished
		 *
		 * @param result
		 * 		CommResult with requester ID and result of communication
		 */
		protected void onPostExecute(CommResult result) {
			if (BuildConfig.DEBUG && result.comFailed) {
				if (showDebug) {
					if (!snackBarText.isEmpty()) {
						snackBarText += "\n" + result.comResult;
					} else {
						snackBarText = result.comResult;
					}
					Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
							snackBarText,
							Snackbar.LENGTH_INDEFINITE);
					mySnackbar.setAction("OK", mOnClickListener);
					mySnackbar.show();
					View snackbarView = mySnackbar.getView();
					TextView tv = snackbarView.findViewById(android.support.design.R.id.snackbar_text);
					tv.setMaxLines(5);
					tv.setVerticalScrollBarEnabled(true);
					tv.setMovementMethod(new ScrollingMovementMethod());
				}
			}
//			availImages = Arrays.asList(result.comResult.split("\\s*,\\s*"));
			availImages = Arrays.asList(result.comResult.split("<br/>"));
//			if (result.deviceIndex == 0) { // Load latest image and display
			if (!availImages.isEmpty()) {
//				String lastImage = "http://1s.giesecke.tk/" + availImages.get(0) + ".jpg";
				String lastImage = "http://93.104.213.79/1s/" + availImages.get(0) + ".jpg";
				new LoadImage((LoadImage.Listener) appContext).execute(lastImage);
			}
			// TODO save list of available images
			// TODO ask user which image to show
			if (showDebug) {
				if (!snackBarText.isEmpty()) {
					snackBarText += "\n" + availImages.get(0);
				} else {
					snackBarText = availImages.get(0);
				}
				Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
						snackBarText,
						Snackbar.LENGTH_INDEFINITE);
				mySnackbar.setAction("OK", mOnClickListener);
				mySnackbar.show();
				View snackbarView = mySnackbar.getView();
				TextView tv = snackbarView.findViewById(android.support.design.R.id.snackbar_text);
				tv.setMaxLines(5);
				tv.setVerticalScrollBarEnabled(true);
				tv.setMovementMethod(new ScrollingMovementMethod());
			}
//			} else { // Present a list of available images and let user select which to show
//				// TODO show downloaded image
//			}
		}
	}

	/*
	 * Communication as Runnable between Android and CCTV footage Server
	 */
	private class cctvCommunication implements Runnable {

		cctvCommunication() {
			run();
		}

		public void run() {
			// If we are not on home WiFi, show error
			if (!Utilities.isHomeWiFi(getApplicationContext())
					|| dsURL == null) {
				Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
						getApplicationContext().getString(R.string.cctv_impossible),
						Snackbar.LENGTH_INDEFINITE);
				mySnackbar.setAction("OK", mOnClickListener);
				mySnackbar.show();
				// Start discovery of mDNS/NSD services available if not running already
				startService(new Intent(getApplicationContext(), CheckAvailDevices.class));
			} else {
				new cctvCommunicationAsync().execute();
			}
		}
	}

	/*
	 * Communication in Async Task between Android and CCTV footage Server
	 */
	@SuppressLint("StaticFieldLeak")
	private class cctvCommunicationAsync extends AsyncTask<String, String, CCTVfootages> {

		/*
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
		 * 	@return <code>CommResult</code>
		 * 			Requester ID and result of communication
		 */
		@Override
		protected CCTVfootages doInBackground(String... params) {

			/* Return values for onPostExecute */

			// Preset commError
			lists.commError = "";

			/* Communication result as string */
			String result;

			Context thisAppContext = getApplicationContext();

			/* A HTTP client to access the cctv footage server */
			// Set timeout to 5 minutes in case we have a lot of data to load
			OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(300, TimeUnit.SECONDS)
					.writeTimeout(10, TimeUnit.SECONDS)
					.readTimeout(300, TimeUnit.SECONDS)
					.build();

			/* First get list of directories */
			String urlString = "http://" + dsURL + ":8080/getdir.php";

			/* Request to cctv footage server */
			Request request = new Request.Builder()
					.url(urlString)
					.build();

			/* String builder for communication error */
			StringBuilder commError = new StringBuilder();

			if (request != null) {
				try {
					/* Response from cctv footage server */
					Response response = client.newCall(request).execute();
					if (response != null) {
						result = response.body().string();
						if (result.equalsIgnoreCase("No directory ???")) {
							commError.append(result);
							commError.append("\n");
						} else {
							String[] todayFiles = result.split(";");
							Collections.addAll(lists.availDaysList, todayFiles);
						}
					} else {
						commError.append(thisAppContext.getString(R.string.err_cctv));
						commError.append("\n");
					}
				} catch (IOException e) {
					result = e.getMessage();
					try {
						String errorMsg = thisAppContext.getString(R.string.err_cctv);
						if (result.contains("EHOSTUNREACH") || result.equalsIgnoreCase("")) {
							commError.append(errorMsg);
							commError.append("\n");
						}
					} catch (NullPointerException en) {
						commError.append(thisAppContext.getString(R.string.err_cctv));
						commError.append("\n");
					}
				}
			}

			/* Second get list of todays cctv footage */
			urlString = "http://" + dsURL + ":8080/getfiles.php";

			/* Request to cctv footage server */
			request = new Request.Builder()
					.url(urlString)
					.build();

			if (request != null) {
				try {
					/* Response from cctv footage server */
					Response response = client.newCall(request).execute();
					if (response != null) {
						result = response.body().string();
						if (result.equalsIgnoreCase("No directory ???")) {
							commError.append(result);
							commError.append("\n");
						} else {
							String[] todayFiles = result.split(";");
							Collections.addAll(lists.todaysList, todayFiles);
						}
					} else {
						commError.append(thisAppContext.getString(R.string.err_cctv));
						commError.append("\n");
					}
				} catch (IOException e) {
					result = e.getMessage();
					try {
						String errorMsg = thisAppContext.getString(R.string.err_cctv);
						if (result.contains("EHOSTUNREACH") || result.equalsIgnoreCase("")) {
							commError.append(errorMsg);
							commError.append("\n");
						}
					} catch (NullPointerException en) {
						commError.append(thisAppContext.getString(R.string.err_cctv));
						commError.append("\n");
					}
				}
			}

			/* Third get all older cctv footage */
			for (int index = 0; index < lists.availDaysList.size(); index++) {
				/* List of todays footage and directories as array */
				ArrayList<String> selDayList = new ArrayList<>();

				urlString = "http://" + dsURL + ":8080/getfiles.php?dir=" + lists.availDaysList.get(index);

				/* Request to cctv footage server */
				request = new Request.Builder()
						.url(urlString)
						.build();

				if (request != null) {
					try {
						/* Response from cctv footage server */
						Response response = client.newCall(request).execute();
						if (response != null) {
							result = response.body().string();
							if (result.equalsIgnoreCase("No directory ???")) {
								commError.append(result);
								commError.append("\n");
							} else {
								String[] todayFiles = result.split(";");
								Collections.addAll(selDayList, todayFiles);
								lists.daysList.add(selDayList);
							}
						} else {
							commError.append(thisAppContext.getString(R.string.err_cctv));
							commError.append("\n");
						}
					} catch (IOException e) {
						result = e.getMessage();
						try {
							String errorMsg = thisAppContext.getString(R.string.err_cctv);
							if (result.contains("EHOSTUNREACH") || result.equalsIgnoreCase("")) {
								commError.append(errorMsg);
								commError.append("\n");
							}
						} catch (NullPointerException en) {
							commError.append(thisAppContext.getString(R.string.err_cctv));
							commError.append("\n");
						}
					}
				}
			}
			lists.commError = commError.toString();
			return lists;
		}

		/*
		 * Called when AsyncTask background process is finished
		 *
		 * @param result
		 * 		CommResult with requester ID and result of communication
		 */
		protected void onPostExecute(CCTVfootages result) {
//			if (BuildConfig.DEBUG) {
			String snackBarText = getString(R.string.cctv_footage_wait);
			if (!result.commError.equalsIgnoreCase("")) {
				snackBarText = result.commError;
//					for (int index=0; index < result.availDaysList.size(); index++) {
//						snackBarText = snackBarText + result.availDaysList.get(index) + "\n";
//					}
//					snackBarText = snackBarText + "-----------------\n";
//					snackBarText = snackBarText + "Todays footage:\n";
//					for (int index = 0; index < result.todaysList.size(); index++) {
//						snackBarText = snackBarText + result.todaysList.get(index) + "\n";
//					}
//					for (int dayIndex = 0; dayIndex < result.availDaysList.size(); dayIndex++) {
//						snackBarText = snackBarText + "-----------------\n";
//						snackBarText = snackBarText + "Content of " + result.availDaysList.get(dayIndex) + ":\n";
//						for (int index=0; index < result.daysList.get(dayIndex).size(); index++) {
//							snackBarText = snackBarText + result.daysList.get(dayIndex).get(index) + "\n";
//						}
//					}
			}
//
			Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
					snackBarText,
					Snackbar.LENGTH_LONG);
//				mySnackbar.setAction("OK", mOnClickListener);
			mySnackbar.show();
//				View snackbarView = mySnackbar.getView();
//				TextView tv= (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
//				tv.setVerticalScrollBarEnabled(true);
//				tv.setMovementMethod(new ScrollingMovementMethod());
//				tv.setMaxLines(50);
//			}
			// Open the CCTV footage viewer
			Intent myIntent = new Intent(getApplicationContext(), SecCamViewer.class);
			startActivity(myIntent);
		}
	}

	/*
	 * Update UI with values received from ESP device
	 *
	 * @param result
	 * 		result sent by onPostExecute
	 */
	private void securityViewUpdate(final CommResult result) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/* String used for temporary conversions */
				String tempString;
				if (Utilities.isJSONValid(result.comResult)) {
					/* JSON object to hold the result received from the ESP8266 */
					JSONObject jsonResult;
					try {
						jsonResult = new JSONObject(result.comResult);
						/* String to hold complete status in viewable form */
						String message;
						/* Device ID */
						String deviceIDString;
						try {
							deviceIDString = jsonResult.getString("de");
						} catch (JSONException e) {
							deviceIDString = "unknown";
						}

						// Get device status and light status and add it to viewable status
						if (deviceIDString.equalsIgnoreCase("sf1")) {
							message = Utilities.getDeviceStatus(jsonResult, appContext,
									ivAlarmStatus, ivLightStatus,
									secBackView, secAutoAlarmFront, secChangeAlarm);
							message += Utilities.getLightStatus(jsonResult);
						} else if (deviceIDString.equalsIgnoreCase("sb1")) {
							message = Utilities.getDeviceStatus(jsonResult, appContext,
									ivAlarmStatusBack, ivLightStatusBack,
									secBackView, secAutoAlarmBack, secChangeAlarm);
							message += Utilities.getLightStatus(jsonResult);
						} else {
							message = "Camera snapshot ";
							try {
								int snapShotResult = jsonResult.getInt("pi");
								if (snapShotResult == 1) {
									message += "successful\n";
									new galleryCommunication().execute("giesecke.tk/gallery", "/get.php", "0");
								} else {
									message += "failed\n";
								}
							} catch (JSONException ignore) {
							}
						}
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
					} catch (JSONException e) {
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
					}
				}
			}
		});
	}

	/*
	 * Parse JSON and show received status in UI
	 *
	 * @param result
	 *            CommResult
	 *               isSearchDevice = flag that device search is active
	 *               deviceIndex = index of device that is investigated
	 *               reqCmd = command to be sent to the ESP device
	 *               comResult = return string as JSON from the ESP device
	 */
	private void airconViewUpdate(CommResult result) {

		Context thisAppContext = getApplicationContext();
		try {
			/* JSON object with the result from the ESP device */
			JSONObject deviceResult = new JSONObject(result.comResult);
			if (deviceResult.has("de")) {
				deviceName[result.deviceIndex] = deviceResult.getString("de");
				if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("fd")) {
					deviceType[result.deviceIndex] = FUJIDENZO;
				}
				if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("fb")) {
					deviceType[result.deviceIndex] = FUJIDENZO;
				}
				if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("ca")) {
					deviceType[result.deviceIndex] = CARRIER;
				}
				if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("am")) {
					deviceType[result.deviceIndex] = AMERICANHOME;
				}
				// TODO here is the place to add more layout versions for air cons
			}
			if (deviceResult.has("po")) {
				powerStatus[result.deviceIndex] = deviceResult.getInt("po");
			}
			if (deviceResult.has("mo")) {
				modeStatus[result.deviceIndex] = deviceResult.getInt("mo");
			}
			if (deviceResult.has("sp")) {
				fanStatus[result.deviceIndex] = deviceResult.getInt("sp");
			}
			if (deviceResult.has("te")) {
				coolStatus[result.deviceIndex] = deviceResult.getInt("te");
			}
			if (deviceResult.has("co")) {
				consStatus = deviceResult.getDouble("co");
			}
			if (deviceResult.has("st")) {
				autoStatus = deviceResult.getInt("st");
			}
			if (deviceResult.has("au")) {
				autoOnStatus[result.deviceIndex] = deviceResult.getInt("au");
			}
			if (deviceResult.has("sw")) {
				sweepStatus[result.deviceIndex] = deviceResult.getInt("sw");
			}
			if (deviceResult.has("tu")) {
				turboStatus[result.deviceIndex] = deviceResult.getInt("tu");
			}
			if (deviceResult.has("io")) {
				ionStatus[result.deviceIndex] = deviceResult.getInt("io");
			}
			if (deviceResult.has("ti")) {
				timerStatus[result.deviceIndex] = deviceResult.getInt("ti");
			}
			if (deviceResult.has("ot")) {
				deviceTimer[result.deviceIndex] = deviceResult.getInt("ot");
			}
			if (deviceResult.has("ts")) {
				deviceOffTime[result.deviceIndex] = deviceResult.getString("ts");
			}

			// TODO here is the place to add more status for other air cons

			// Update UI
			updateAirStatus(result.deviceIndex, thisAppContext);
		} catch (JSONException e) {
			if (BuildConfig.DEBUG)
				Log.d(DEBUG_LOG_TAG, "Received invalid JSON = " + result.comResult);
		}
		if (showDebug) {
			airStatus.setText(result.comResult);
		} else {
			airStatus.setText("");
		}
	}

	/*
	 * Update UI fields with the latest status of a device
	 *
	 * @param deviceIndex
	 *            Index of the device to be updated
	 */
	private void updateAirStatus(int deviceIndex, Context thisAppContext) {
		/* String for the average consumption value */
		@SuppressLint("DefaultLocale") String consText = String.format("%.0f", consStatus) + "W";
		/* String for the temperature setting value */
		String tempText = coolStatus[deviceIndex] + "C";
		/* String for the auto on/off status */
		String statusText = Integer.toString(autoStatus);
		/* String with timer duration */
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
					timerTime = deviceTimer[selDevice] +
							" " +
							thisAppContext.getResources().getString(R.string.bt_txt_hour);
				} else {
					btTimerFD.setBackgroundColor(colorOrange);
//					timerTime = thisAppContext.getResources().getString(R.string.timer_on);
					timerTime = deviceOffTime[selDevice];
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
					timerTime = deviceTimer[selDevice] +
							" " +
							thisAppContext.getResources().getString(R.string.bt_txt_hour);
				} else {
					btTimerCA.setBackgroundColor(colorOrange);
//					timerTime = thisAppContext.getResources().getString(R.string.timer_on);
					timerTime = deviceOffTime[selDevice];
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
			case AMERICANHOME:
				btOnOffLightAM.setBackgroundColor(
						(powerStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				switch (modeStatus[deviceIndex]) {
					case 0: // Fan mode
						btAutomLightAM.setBackgroundColor(colorGrey);
						btCoolLightAM.setBackgroundColor(colorGrey);
						btDryLightAM.setBackgroundColor(colorGrey);
						btFanLightAM.setBackgroundColor(colorRed);
						break;
					case 1: // Dry mode
						btAutomLightAM.setBackgroundColor(colorGrey);
						btCoolLightAM.setBackgroundColor(colorGrey);
						btDryLightAM.setBackgroundColor(colorRed);
						btFanLightAM.setBackgroundColor(colorGrey);
						break;
					case 2: // Cool mode
						btAutomLightAM.setBackgroundColor(colorGrey);
						btCoolLightAM.setBackgroundColor(colorRed);
						btDryLightAM.setBackgroundColor(colorGrey);
						btFanLightAM.setBackgroundColor(colorGrey);
						break;
					case 3: // Auto mode
						btAutomLightAM.setBackgroundColor(colorRed);
						btCoolLightAM.setBackgroundColor(colorGrey);
						btDryLightAM.setBackgroundColor(colorGrey);
						btFanLightAM.setBackgroundColor(colorGrey);
						break;
				}
				switch (fanStatus[deviceIndex]) {
					case 0: // Fan low mode
						btFanAM.setText(R.string.bt_txt_fan_low);
						break;
					case 1: // Fan medium mode
						btFanAM.setText(R.string.bt_txt_fan_med);
						break;
					case 2: // Fan high mode
						btFanAM.setText(R.string.bt_txt_fan_high);
						break;
					case 3: // Fan highest mode (Cool or Heat mode only)
						btFanAM.setText(R.string.bt_txt_turbo);
						break;
				}
				if (timerStatus[deviceIndex] == 0) {
					btTimerAM.setBackgroundColor(colorGreen);
					timerTime = deviceTimer[selDevice] +
							" " +
							thisAppContext.getResources().getString(R.string.bt_txt_hour);
				} else {
					btTimerAM.setBackgroundColor(colorOrange);
//					timerTime = thisAppContext.getResources().getString(R.string.timer_on);
					timerTime = deviceOffTime[selDevice];
				}
				btTimerAM.setText(timerTime);
				btSweepLightAM.setBackgroundColor(
						(sweepStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				btSleepLightAM.setBackgroundColor(
						(turboStatus[deviceIndex] == 1) ? colorRed : colorGrey);

				txtConsValAM.setText(consText);
				txtTempValAM.setText(tempText);
				txtAutoStatusValAM.setText(statusText);
				btAutoLightAM.setBackgroundColor(
						(autoOnStatus[deviceIndex] == 1) ? colorRed : colorGrey);

				break;
			// TODO here is the place to add more layouts for other air cons
		}
	}

	/*
	 * Update UI with values received from spMonitor device (Arduino part)
	 *
	 * @param value
	 *        result sent by spMonitor
	 */
	private void solarViewUpdate(final String value, final boolean isBroadCast) {
		runOnUiThread(new Runnable() {
			@SuppressLint("DefaultLocale")
			@Override
			public void run() {
				/* Pointer to text views to be updated */
				TextView valueFields;
				/* String with results received from spMonitor device */
				String result;

				if (value.length() != 0) {
					// decode JSON
					if (Utilities.isJSONValid(value)) {
						/* Flag for data from external server */
						boolean isFromLocal;
						/* JSON object containing the values */
						JSONObject jsonValues = null;
						try {
							jsonValues = new JSONObject(value.substring(1, value.length() - 1));
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
								/* JSON object containing result from server */
								JSONObject jsonResult = new JSONObject(value);
								if (jsonResult.has("value")) {
									/* JSON object containing the values */
									jsonValues = jsonResult.getJSONObject("value");
								} else {
									return;
								}
							}

							try {
								ChartHelper.solarPowerMin = isFromLocal ?
										Float.parseFloat(jsonValues.getString("S")) :
										Float.parseFloat(jsonValues.getString("s"));
								ChartHelper.lastSolarPowerMin = ChartHelper.solarPowerMin;
							} catch (Exception excError) {
								ChartHelper.solarPowerMin = ChartHelper.lastSolarPowerMin;
							}
							try {
								ChartHelper.consPowerMin = isFromLocal ?
										Float.parseFloat(jsonValues.getString("C")) :
										Float.parseFloat(jsonValues.getString("c"));
								ChartHelper.lastConsPowerMin = ChartHelper.consPowerMin;
							} catch (Exception excError) {
								ChartHelper.consPowerMin = ChartHelper.lastConsPowerMin;
							}

							result = "S=" + ChartHelper.solarPowerMin + "W ";
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
							result += "\nC=" + ChartHelper.consPowerMin + "W c=";
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

							/* Double for the result of solar current and consumption used at 1min updates */
							double resultPowerMin = ChartHelper.solarPowerMin + ChartHelper.consPowerMin;

							valueFields = findViewById(R.id.tv_solar_val);
							/* String for display */
							String displayTxt;
							displayTxt = String.format("%.0f", ChartHelper.solarPowerMin) + "W";
							valueFields.setText(displayTxt);
							valueFields = findViewById(R.id.tv_cons_val);
							displayTxt = String.format("%.0f", resultPowerMin) + "W";
							valueFields.setText(displayTxt);
							solStatus.setText(result);

							valueFields = findViewById(R.id.tv_result_txt);
							if (ChartHelper.consPowerMin > 0.0d) {
								valueFields.setText(getString(R.string.tv_result_txt_im));
								valueFields = findViewById(R.id.tv_result_val);
								valueFields.setTextColor(getResources()
										.getColor(android.R.color.holo_red_light));
							} else {
								valueFields.setText(getString(R.string.tv_result_txt_ex));
								valueFields = findViewById(R.id.tv_result_val);
								valueFields.setTextColor(getResources()
										.getColor(android.R.color.holo_green_light));
							}
							displayTxt = String.format("%.0f", Math.abs(ChartHelper.consPowerMin)) + "W";
							valueFields.setText(displayTxt);

							if (ChartHelper.autoRefreshOn) {
								if (ChartHelper.plotData != null) {
									/* Current time as string */
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
									/* Text view to show min and max poser values */
									TextView maxPowerText = findViewById(R.id.tv_cons_max);
									displayTxt = "(" + String.format("%.0f",
											Collections.max(ChartHelper.consumMPowerCont)) + "W)";
									maxPowerText.setText(displayTxt);
									maxPowerText = findViewById(R.id.tv_solar_max);
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

	/*
	 * Update Debug UI with status of MQTT broker
	 */
	private void debugViewUpdate() {
		Locale locale = Locale.getDefault();
		TextView viewToChange = findViewById(R.id.tv_mqtt_bytes_avg);
		String tvText = NumberFormat.getNumberInstance(locale).format(MessageListener.bytesLoadRcvd)
				+ " / " + NumberFormat.getNumberInstance(locale).format(MessageListener.bytesLoadSend);
		viewToChange.setText(tvText);
		viewToChange = findViewById(R.id.tv_mqtt_msg_avg);
		tvText = NumberFormat.getNumberInstance(locale).format(MessageListener.bytesMsgsRcvd)
				+ " / " + NumberFormat.getNumberInstance(locale).format(MessageListener.bytesMsgsSend);
		viewToChange.setText(tvText);

		viewToChange = findViewById(R.id.tv_mqtt_client_conn);
		viewToChange.setText(NumberFormat.getNumberInstance(locale).format(MessageListener.clientsConn));

		StringBuilder statusClients1 = new StringBuilder();
		StringBuilder statusClients2 = new StringBuilder();
		int mqttClientsNum = MessageListener.mqttClientList.size();
		if (mqttClientsNum != 0) {
			for (int i = 0; i < mqttClientsNum; i++) {
				if (i % 2 == 0) { // Get two clients into one line
					statusClients1.append(MessageListener.mqttClientList.get(i)).append("\n");
				} else {
					statusClients2.append(MessageListener.mqttClientList.get(i)).append("\n");
				}
			}
		}
		viewToChange = findViewById(R.id.tv_mqtt_client_list1);
		viewToChange.setText(statusClients1.toString());
		viewToChange = findViewById(R.id.tv_mqtt_client_list2);
		viewToChange.setText(statusClients2.toString());

		StringBuilder devNamesFound = new StringBuilder();
		StringBuilder devIPsFound = new StringBuilder();
		for (int i = 0; i < deviceNames.length; i++) {
			if (deviceIsOn[i]) {
				devNamesFound.append(deviceNames[i]).append("\n");
				devIPsFound.append(deviceIPs[i]).append("\n");
			}
		}
		viewToChange = findViewById(R.id.tv_device_name_list);
		viewToChange.setText(devNamesFound.toString());
		viewToChange = findViewById(R.id.tv_device_ip_list);
		viewToChange.setText(devIPsFound.toString());
	}

	/*
	 * Subscribe/Unsubscribe from MQTT broker status topics in AsyncTask
	 */
	@SuppressLint("StaticFieldLeak")
	private class mqttDebugAsync extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String task = params[0];
			if (task.equalsIgnoreCase("subscribe")) {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Subscribe to MQTT status");
				MessageListener.subscribeBrokerStatus();
			} else {
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Unsubscribe from MQTT status");
				MessageListener.unSubscribeBrokerStatus();
			}
			return null;
		}
	}

	/*
	 * Update UI with values received from spMonitor device (Linino part)
	 *
	 * @param syncMonth
	 *        Month that got synced
	 */
	private void updateSynced(final String syncMonth) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				solStatus.setText(getString(R.string.filesSynced));

				/* The application context */
				Context thisAppContext = getApplicationContext();
				if (!showingLog) {
					/* Today split into 3 integers for the database query */
					int[] todayDate = Utilities.getCurrentDate();
					/* Array with existing log dates on the Arduino */
					List<String> thisLogDates;

					/* Instance of data base */
					SQLiteDatabase dataBase = null;
					try {
						if (syncMonth.equalsIgnoreCase(dbNamesList[0])) {
							try {
								dataBase = dbHelperNow.getReadableDatabase();
							} catch (SQLiteDatabaseLockedException ignore) {
							}
							thisLogDates = logDates;
						} else {
							try {
								dataBase = dbHelperLast.getReadableDatabase();
							} catch (SQLiteDatabaseLockedException ignore) {
							}
							thisLogDates = lastLogDates;
						}

						if (dataBase != null) {
							try {
								dataBase.beginTransaction();

								/* Cursor with new data from the database */
								Cursor newDataSet = DataBaseHelper.getDay(dataBase, todayDate[2],
										todayDate[1], todayDate[0] - 2000);
								if (newDataSet != null) {
									ChartHelper.fillSeries(newDataSet, appView);
									newDataSet.close();
								}
								thisLogDates.clear();
								/* List with years in the database */
								ArrayList<Integer> yearsAvail = DataBaseHelper.getEntries(dataBase, "year", 0, 0);
								for (int year = 0; year < yearsAvail.size(); year++) {
									/* List with months of year in the database */
									ArrayList<Integer> monthsAvail = DataBaseHelper.getEntries(dataBase, "month",
											0, yearsAvail.get(year));
									for (int month = 0; month < monthsAvail.size(); month++) {
										/* List with days of month of year in the database */
										ArrayList<Integer> daysAvail = DataBaseHelper.getEntries(dataBase, "day",
												monthsAvail.get(month),
												yearsAvail.get(year));
										for (int day = 0; day < daysAvail.size(); day++) {
											thisLogDates.add(("00" + yearsAvail.get(year))
													.substring(String.valueOf(yearsAvail.get(year)).length()) +
													"-" + ("00" + monthsAvail.get(month))
													.substring(String.valueOf(monthsAvail.get(month)).length()) +
													"-" + ("00" + daysAvail.get(day))
													.substring(String.valueOf(daysAvail.get(day)).length()));
										}
									}
								}
								dataBase.endTransaction();
							} catch (SQLiteReadOnlyDatabaseException ignore) {
							}

							dataBase.close();
							if (syncMonth.equalsIgnoreCase(dbNamesList[0])) {
								logDatesIndex = thisLogDates.size() - 1;
								ChartHelper.initChart(true, thisAppContext, chartTitle);
							} else {
								lastLogDatesIndex = thisLogDates.size() - 1;
							}
						}
					} catch (SQLiteDatabaseLockedException ignore) {
					}
				}
				// Get latest value and update UI
				new SPMPcommunication().execute(deviceIPs[spMonitorIndex], "/data/get", "", "spm", Integer.toString(selDevice));
			}
		});
	}

	/*
	 * Check if service is running
	 *
	 * @param serviceClass
	 *              Service class we want to check if it is running
	 * @return <code>boolean</code>
	 *              True if service is running
	 *              False if service is not running
	 */
	private boolean myServiceIsStopped() {
		/* Activity manager for services */
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		if (manager != null) {
			for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
				if (MessageListener.class.getName().equals(service.service.getClassName())) {
					return false;
				}
			}
		}
		return true;
	}

	/*
	 * Set all global variables used
	 */
	private void setGlobalVar() {
		// For security view:
		secStatus = findViewById(R.id.security_status);
		ivAlarmStatus = findViewById(R.id.dot_alarm_status);
		ivLightStatus = findViewById(R.id.dot_light);
		secAutoAlarmFront = findViewById(R.id.cb_sec_auto_alarm);
		secAutoAlarmBack = findViewById(R.id.cb_sec_auto_alarm_2);
		secChangeAlarm = findViewById(R.id.tv_change_alarm);
		secBackView = findViewById(R.id.tl_alarm_back);
		ivAlarmStatusBack = findViewById(R.id.dot_alarm_status_back);
		ivLightStatusBack = findViewById(R.id.dot_light_back);

		// For solar view:
		solStatus = findViewById(R.id.solar_status);
		appView = getWindow().getDecorView().findViewById(android.R.id.content);
		ChartHelper.lineChart = findViewById(R.id.graph);
		chartTitle = findViewById(R.id.tv_plotTitle);

		// For aircon view:
		airFDView = findViewById(R.id.fuji_denzo);
		airCAView = findViewById(R.id.carrier);
		airAMView = findViewById(R.id.american);

		airStatus = findViewById(R.id.aircon_status);
		btAutoLightFD = findViewById(R.id.bt_auto_hl_fd);
		btOnOffLightFD = findViewById(R.id.bt_on_off_hl_fd);
		btFanHighLightFD = findViewById(R.id.bt_fan_high_hl_fd);
		btFanMedLightFD = findViewById(R.id.bt_fan_med_hl_fd);
		btFanLowLightFD = findViewById(R.id.bt_fan_low_hl_fd);
		btCoolLightFD = findViewById(R.id.bt_cool_hl_fd);
		btDryLightFD = findViewById(R.id.bt_dry_hl_fd);
		btFanLightFD = findViewById(R.id.bt_fan_hl_fd);

		btAutoLightCA = findViewById(R.id.bt_auto_hl_ca);
		btOnOffLightCA = findViewById(R.id.bt_on_off_hl_ca);
		btCoolLightCA = findViewById(R.id.bt_cool_hl_ca);
		btDryLightCA = findViewById(R.id.bt_dry_hl_ca);
		btFanLightCA = findViewById(R.id.bt_fan_hl_ca);
		btSweepLightCA = findViewById(R.id.bt_sweep_hl_ca);
		btTurboLightCA = findViewById(R.id.bt_turbo_hl_ca);
		btIonLightCA = findViewById(R.id.bt_ion_hl_ca);
		btAutomLightCA = findViewById(R.id.bt_autom_hl_ca);

		btAutoLightAM = findViewById(R.id.bt_auto_hl_am);
		btOnOffLightAM = findViewById(R.id.bt_on_off_hl_am);
		btCoolLightAM = findViewById(R.id.bt_cool_hl_am);
		btDryLightAM = findViewById(R.id.bt_dry_hl_am);
		btFanLightAM = findViewById(R.id.bt_fan_hl_am);
		btSweepLightAM = findViewById(R.id.bt_sweep_hl_am);
		btSleepLightAM = findViewById(R.id.bt_sleep_hl_am);
		btAutomLightAM = findViewById(R.id.bt_autom_hl_am);

		btTimerFD = findViewById(R.id.bt_timer_fd);
		btTimerCA = findViewById(R.id.bt_timer_ca);
		btTimerAM = findViewById(R.id.bt_timer_am);

		btFanCA = findViewById(R.id.bt_fanspeed_ca);
		btFanAM = findViewById(R.id.bt_fanspeed_am);

		txtConsValFD = findViewById(R.id.txt_cons_val_fd);
		txtTempValFD = findViewById(R.id.txt_temp_val_fd);
		txtAutoStatusValFD = findViewById(R.id.txt_auto_status_val_fd);
		txtConsValCA = findViewById(R.id.txt_cons_val_ca);
		txtTempValCA = findViewById(R.id.txt_temp_val_ca);
		txtAutoStatusValCA = findViewById(R.id.txt_auto_status_val_ca);
		txtConsValAM = findViewById(R.id.txt_cons_val_am);
		txtTempValAM = findViewById(R.id.txt_temp_val_am);
		txtAutoStatusValAM = findViewById(R.id.txt_auto_status_val_am);

		// For debug view
		final EditText edittext = findViewById(R.id.et_highlight);
		edittext.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					filterDbgMsg();
					return true;
				}
				return false;
			}
		});

		// For light control view
		bedRoomValSB = findViewById(R.id.sb_bedroom);
		bedRoomValSB.setOnSeekBarChangeListener(this);
		bedRoomVal = findViewById(R.id.tv_bedroom_value);

		ImageButton bedRoomDimIB = findViewById(R.id.ib_light_bed_dim);
		bedRoomDimIB.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				int brightnessVal = 222 - bedRoomValSB.getProgress();
				mPrefs.edit().putInt(prefsLightBedDim, brightnessVal).apply();
				/* Command for ESP */
				String cmd = String.valueOf(brightnessVal);
				cmd = "d=" + cmd;
				String url = deviceIPs[lb1Index];
				new ESPbyTCP(url, cmd, "0");
//				new ESPbyTCP(getString(R.string.LIGHTS_BEDROOM),cmd,"0");
				return true;
			}
		});

		colorRed = getResources().getColor(android.R.color.holo_red_light);
		colorGrey = getResources().getColor(android.R.color.darker_gray);
		colorOrange = getResources().getColor(android.R.color.holo_orange_light);
		colorGreen = getResources().getColor(android.R.color.holo_green_light);

		/* Pointer to text views showing the consumed / produced energy */
		TextView energyText = findViewById(R.id.tv_cons_energy);
		energyText.setVisibility(View.INVISIBLE);
		energyText = findViewById(R.id.tv_solar_energy);
		energyText.setVisibility(View.INVISIBLE);

		/* Button to stop/start continuous UI refresh */
		Button btStop = findViewById(R.id.bt_stop);
		if (showingLog) {
			btStop.setTextColor(getResources().getColor(android.R.color.holo_green_light));
			btStop.setText(getResources().getString(R.string.start));
		}

		// Get index of last selected device */
		selDevice = mPrefs.getInt(prefsSelDevice, 0);

		// Set visible view flag to security
		visibleView = mPrefs.getInt(prefsLastView, view_security_id);
		// Set flag for debug output
		showDebug = mPrefs.getBoolean(prefsShowDebug, false);
	}

	/*
	 * Initializing method
	 * - Find all available devices
	 * - Check if Google Cloud Messaging is registered
	 * - Call initializing methods for all devices
	 */
	@SuppressLint("StaticFieldLeak")
	private class Initialize extends AsyncTask<String, Void, Void> {

		@SuppressLint("CommitPrefEdits")
		@Override
		protected Void doInBackground(String... params) {

			String foundDevice = params[0];

			if (foundDevice.equalsIgnoreCase("spm") || foundDevice.equalsIgnoreCase("spMonitor")) {
				initSPM();
				return null;
			}
			if (foundDevice.equalsIgnoreCase("sf1") || foundDevice.equalsIgnoreCase("sb1")) {
				initSecurity(foundDevice);
				return null;
			}

			if (foundDevice.equalsIgnoreCase("fd1")
					|| foundDevice.equalsIgnoreCase("ca1")
					|| foundDevice.equalsIgnoreCase("am1")) {
				initAircons(foundDevice);
				return null;
			}

			if (foundDevice.equalsIgnoreCase("lb1") || foundDevice.equalsIgnoreCase("ly1")) {
				initLights(foundDevice);
				return null;
			}

			return null;
		}
	}

	/*
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
			initHandler(8, "s", deviceIPs[aircon1Index], "fd1", "0", null, null);
			if (mPrefs.contains(prefsLocationName + "0")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "0", "");
			}
			// Update aircon 1 location name
			initHandler(4, locationName[0], "1", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "0")) {
				deviceIcon[0] = mPrefs.getInt(prefsDeviceIcon + "0", 99);
			}
			// Update aircon 1 icon
			initHandler(7, "1", "", "", "",
					(ImageView) findViewById(R.id.im_icon_fd),
					getResources().getDrawable(iconIDs[deviceIcon[0]]));
		}
		if (foundDevice.equalsIgnoreCase("ca1")) { // Aircon 2 - Living room
			// Get initial status from Aircon 2
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of Aircon 2");
			// Update aircon 2 status
			initHandler(8, "s", deviceIPs[aircon2Index], "ca1", "1", null, null);
			if (mPrefs.contains(prefsLocationName + "1")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "1", "");
			}
			// Update aircon 2 location name
			initHandler(5, locationName[1], "", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "1")) {
				deviceIcon[1] = mPrefs.getInt(prefsDeviceIcon + "0", 99);
			}
			// Update aircon 2 icon
			initHandler(7, "", "", "", "",
					(ImageView) findViewById(R.id.im_icon_ca),
					getResources().getDrawable(iconIDs[deviceIcon[1]]));
		}
		// TODO add third aircon if ever available
		if (foundDevice.equalsIgnoreCase("am1")) { // Aircon 3 - Office
			// Get initial status from Aircon 3
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of Aircon 3");
			initHandler(8, "s", deviceIPs[aircon3Index], "am1", "2", null, null);
			if (mPrefs.contains(prefsLocationName + "2")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "2", "");
			}
			// Update aircon 3 location name
			initHandler(6, locationName[2], "", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "2")) {
				deviceIcon[0] = mPrefs.getInt(prefsDeviceIcon + "2", 99);
			}
			// Update aircon 3 icon
			initHandler(7, "", "", "", "",
					(ImageView) findViewById(R.id.im_icon_am),
					getResources().getDrawable(iconIDs[deviceIcon[2]]));
		}
		if (!deviceIsOn[aircon1Index] && !deviceIsOn[aircon2Index] && !deviceIsOn[aircon3Index]) {
			// Show message no aircons found
			initHandler(2, getResources().getString(R.string.err_aircon), "", "", "", null, null);
		}
	}

	/*
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
			initHandler(8, "s", deviceIPs[secFrontIndex], "sf1", Integer.toString(selDevice), null, null);
		}
		if (foundDevice.equalsIgnoreCase("sb1")) { // Security back
			// Get initial status from Security
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of back Security");
			// Update security status back sensor
			initHandler(8, "s", deviceIPs[secBackIndex], "sb1", Integer.toString(selDevice), null, null);
			initHandler(10, "", "", "", "", null, null);
		}
	}

	/*
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
			/* Instance of data base */
			SQLiteDatabase dataBase = null;
			try {
				dataBase = dbHelperNow.getReadableDatabase();
				dataBase.beginTransaction();
				/* Cursor with data from database */
				Cursor chCursor = DataBaseHelper.getLastRow(dataBase);
				if (chCursor != null) {
					dataBaseIsEmpty = chCursor.getCount() == 0;
					chCursor.close();
				}
				dataBase.endTransaction();
				dataBase.close();
				/* Instance of data base */
				dataBase = dbHelperLast.getReadableDatabase();
				dataBase.beginTransaction();
				dataBase.endTransaction();
				dataBase.close();
			} catch (SQLiteDatabaseLockedException | SQLiteReadOnlyDatabaseException ignore) {
			}

			// Start background sync of the database
			initHandler(9, dbNamesList[0], "", "", "", null, null);

			if (!dataBaseIsEmpty && dataBase != null) { // Sync second database only if first one is not empty
				// Check if we have already synced the last month
				/* Instance of data base */
				dataBase = dbHelperLast.getReadableDatabase();
				/* Cursor with data from database */
				Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
				if (dbCursor != null) {
					try {
						// create logged data array only if database is not empty
						if (dbCursor.getCount() != 0) {
							lastLogDates.clear();
							/* List with years in the database */
							ArrayList<Integer> yearsAvail = DataBaseHelper.getEntries(dataBase, "year", 0, 0);
							for (int year = 0; year < yearsAvail.size(); year++) {
								/* List with months of year in the database */
								ArrayList<Integer> monthsAvail = DataBaseHelper.getEntries(dataBase, "month",
										0, yearsAvail.get(year));
								for (int month = 0; month < monthsAvail.size(); month++) {
									/* List with days of month of year in the database */
									ArrayList<Integer> daysAvail = DataBaseHelper.getEntries(dataBase, "day",
											monthsAvail.get(month),
											yearsAvail.get(year));
									for (int day = 0; day < daysAvail.size(); day++) {
										lastLogDates.add(("00" + yearsAvail.get(year))
												.substring(String.valueOf(yearsAvail.get(year)).length()) +
												"-" + ("00" + monthsAvail.get(month))
												.substring(String.valueOf(monthsAvail.get(month)).length()) +
												"-" + ("00" + daysAvail.get(day))
												.substring(String.valueOf(daysAvail.get(day)).length()));
									}
								}
							}
							lastLogDatesIndex = lastLogDates.size() - 1;
						}
					} catch (IllegalStateException ignore) {
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
			initHandler(1, getResources().getString(R.string.err_spMonitor), "", "", "", null, null);
			// Update of solar panel values
			initHandler(3, "/data/get", deviceIPs[spMonitorIndex], "spm", Integer.toString(selDevice), null, null);
		}
	}

	/*
	 * Initializing method for light control
	 * Send status update request
	 *
	 * @param foundDevice
	 *          id of the found device
	 */
	private void initLights(String foundDevice) {
		if (foundDevice.equalsIgnoreCase("lb1")) { // Bedroom light
			// Get initial status from light control
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of bedroom lights");
			// Update bedroom light status
			initHandler(8, "s", deviceIPs[lb1Index], "lb1", "0", null, null);
		}
		if (foundDevice.equalsIgnoreCase("ly1")) { // Backyard light
			// Get initial status from light control
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of backyard lights");
			// Update backyard light status
			initHandler(8, "s", deviceIPs[ly1Index], "ly1", "0", null, null);
		}
	}

	/*
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
	private void initHandler(final int task,
							 final String message,
							 final String url,
							 final String deviceID,
							 final String airconID,
							 final ImageView iconImage,
							 final Drawable iconDrawable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/* Text view to show location name */
				TextView locationText;
				/* Timer button */
				Button btTimer;
				/* Text for timer button */
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
						new SPMPcommunication().execute(url, message, "", deviceID, airconID);
						break;
					case 4: // Aircon 1 location & timer button text
						locationText = findViewById(R.id.txt_device_fd);
						locationText.setText(message);
						btTimer = findViewById(R.id.bt_timer_fd);
						timerTime = deviceTimer[0] +
								" " +
								getString(R.string.bt_txt_hour);
						btTimer.setText(timerTime);
						break;
					case 5: // Aircon 2 location & timer button text
						locationText = findViewById(R.id.txt_device_ca);
						locationText.setText(message);
						btTimer = findViewById(R.id.bt_timer_ca);
						timerTime = deviceTimer[1] +
								" " +
								getString(R.string.bt_txt_hour);
						btTimer.setText(timerTime);
						break;
					case 6: // Aircon 3 location & timer button text
						locationText = findViewById(R.id.txt_device_am);
						locationText.setText(message);
						btTimer = findViewById(R.id.bt_timer_am);
						timerTime = deviceTimer[2] +
								" " +
								getString(R.string.bt_txt_hour);
						btTimer.setText(timerTime);
						break;
					case 7: // Aircon location icon
						iconImage.setImageDrawable(iconDrawable);
						break;
					case 8: // start communication with ESP8266
						if (url.equalsIgnoreCase("")) {
							if (BuildConfig.DEBUG)
								Log.d(DEBUG_LOG_TAG, "Empty address request on device " + deviceID
										+ " msg= " + message);
						}
						new ESPbyTCP(url, message, deviceID);
						break;
					case 9: // Start background sync of database
						startService(new Intent(getApplicationContext(), SolarSyncDataBase.class));
						break;
					case 10:
						TableLayout backYardDots = findViewById(R.id.tl_alarm_back);
						backYardDots.setVisibility(View.VISIBLE);
						break;
					case 11:
				}
			}
		});
	}

	/*
	 * Switch to requested UI
	 *
	 * @param uiSelected
	 *            0 = Security UI
	 *            1 = Solar panel UI
	 *            2 = Aircon control UI
	 */
	private void switchUI(int uiSelected) {

		/* Pointer to action bar */
		Toolbar actionBar = findViewById(R.id.toolbar);
		/* Color of toolBar background */
		Drawable toolBarDrawable;
		/* Menu item pointer */
		MenuItem menuItem;
		/* Color for status bar */
		int statusBarColor;
		/* Color for action bar */
		int actionBarColor;

		switch (uiSelected) {
			case view_security_id: // Security UI
				statusBarColor = getResources().getColor(R.color.colorPrimaryDark);
				actionBarColor = getResources().getColor(R.color.colorPrimary);
				if (abMenu != null) {
					// Make security menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(true);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				// Make security view visible
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				secView.setVisibility(View.VISIBLE);
				debugView.setVisibility(View.INVISIBLE);
				seccamView.setVisibility(View.INVISIBLE);
				lightsView.setVisibility(View.INVISIBLE);
				if (visibleView == view_devDebug_id) {
					new mqttDebugAsync().execute("unsubscribe");
				}
				visibleView = view_security_id;
				break;
			case view_aircon_id: // Aircon control UI
				statusBarColor = getResources().getColor(android.R.color.holo_blue_dark);
				actionBarColor = getResources().getColor(android.R.color.holo_blue_light);
				if (abMenu != null) {
					// Make aircon menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(true);
				}
				secView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.VISIBLE);
				debugView.setVisibility(View.INVISIBLE);
				seccamView.setVisibility(View.INVISIBLE);
				lightsView.setVisibility(View.INVISIBLE);
				if (visibleView == view_devDebug_id) {
					new mqttDebugAsync().execute("unsubscribe");
				}
				visibleView = view_aircon_id;
				break;
			case view_devDebug_id: // Device Debug UI
//				if (Utilities.isHomeWiFi(this)) {
//					// Start discovery of mDNS/NSD services available if not running already
//					if (myServiceIsStopped(CheckAvailDevices.class)) {
//						startService(new Intent(this, CheckAvailDevices.class));
//					}
//				}
				statusBarColor = getResources().getColor(android.R.color.holo_orange_dark);
				actionBarColor = getResources().getColor(android.R.color.holo_orange_light);
				if (abMenu != null) {
					// Make aircon menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				secView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				debugView.setVisibility(View.VISIBLE);
				seccamView.setVisibility(View.INVISIBLE);
				lightsView.setVisibility(View.INVISIBLE);
				visibleView = view_devDebug_id;
				new mqttDebugAsync().execute("subscribe");
				break;
			case view_seccam_id: // Security camera view
				statusBarColor = getResources().getColor(android.R.color.holo_orange_dark);
				actionBarColor = getResources().getColor(android.R.color.holo_orange_light);
				if (abMenu != null) {
					// Make security menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(true);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				secView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				debugView.setVisibility(View.INVISIBLE);
				seccamView.setVisibility(View.VISIBLE);
				lightsView.setVisibility(View.INVISIBLE);
				if (visibleView == view_devDebug_id) {
					new mqttDebugAsync().execute("unsubscribe");
				}
				visibleView = view_seccam_id;
				break;
			case view_lights_id: // Light control view
				statusBarColor = getResources().getColor(android.R.color.holo_blue_dark);
				actionBarColor = getResources().getColor(android.R.color.holo_blue_bright);
				if (abMenu != null) {
					// Make solar panel menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				secView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.INVISIBLE);
				debugView.setVisibility(View.INVISIBLE);
				seccamView.setVisibility(View.INVISIBLE);
				lightsView.setVisibility(View.VISIBLE);
				if (visibleView == view_devDebug_id) {
					new mqttDebugAsync().execute("unsubscribe");
				}
				visibleView = view_lights_id;
//				new ESPbyTCP(getString(R.string.LIGHTS_BEDROOM), "s", "0");
				String url = deviceIPs[lb1Index];
				new ESPbyTCP(url, "s", "0");
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Send status request to lights");
				break;
			case view_solar_id: // Solar panel UI == default view on startup
			default:
				statusBarColor = getResources().getColor(android.R.color.holo_green_dark);
				actionBarColor = getResources().getColor(android.R.color.holo_green_light);
				if (abMenu != null) {
					// Make solar panel menu items visible
					menuItem = abMenu.getItem(action_selAlarm_id); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(action_selWarning_id); // Solar alarm sound menu entry
					menuItem.setVisible(true);
					menuItem = abMenu.getItem(action_locations_id); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				secView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.VISIBLE);
				debugView.setVisibility(View.INVISIBLE);
				seccamView.setVisibility(View.INVISIBLE);
				lightsView.setVisibility(View.INVISIBLE);
				if (visibleView == view_devDebug_id) {
					new mqttDebugAsync().execute("unsubscribe");
				}
				visibleView = view_solar_id;
				ChartHelper.initChart(ChartHelper.autoRefreshOn, appContext, chartTitle);
				break;
		}
		if (android.os.Build.VERSION.SDK_INT >= 21) {
			getWindow().setStatusBarColor(statusBarColor);
		}
		toolBarDrawable = new ColorDrawable(actionBarColor);
		actionBar.setBackground(toolBarDrawable);
		mPrefs.edit().putInt(prefsLastView, visibleView).apply();
	}

	/*
	 * Handle Security view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from security view
	 */
	private boolean handleSecurityButtons(View v) {
		/* Flag if button was handled */
		boolean wasSecButton = true;
		/* URL for communication with ESP */
		String url = "";
		/* Command for ESP */
		String cmd = "";
		/* DeviceID used for MQTT */
		String deviceID = "";
		switch (v.getId()) {
			case R.id.dot_alarm_status:
				url = deviceIPs[secFrontIndex];
				if (hasAlarmOnFront) {
					ivAlarmStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_alarm_autooff));
					cmd = "a=0";
				} else {
					ivAlarmStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_alarm_on));
					cmd = "a=1";
				}
				deviceID = "sf1";
				break;
			case R.id.dot_alarm_status_back:
				url = deviceIPs[secBackIndex];
				if (hasAlarmOnBack) {
					ivAlarmStatusBack.setImageDrawable(getResources().getDrawable(R.mipmap.ic_alarm_autooff));
					cmd = "a=0";
				} else {
					ivAlarmStatusBack.setImageDrawable(getResources().getDrawable(R.mipmap.ic_alarm_on));
					cmd = "a=1";
				}
				deviceID = "sb1";
				break;
			case R.id.dot_light:
				ivLightStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_light_on));
				url = deviceIPs[secFrontIndex];
				cmd = "b";
				deviceID = "sf1";
				break;
			case R.id.dot_light_back:
				ivLightStatusBack.setImageDrawable(getResources().getDrawable(R.mipmap.ic_light_on));
				url = deviceIPs[secBackIndex];
				cmd = "b";
				deviceID = "sb1";
				break;
			case R.id.cb_sec_auto_alarm:
				url = deviceIPs[secFrontIndex];
				if (secAutoAlarmFront.isChecked()) {
					String onTime = Integer.toString(secAutoOnStored);
					if (secAutoOnStored < 10) {
						onTime = "0" + onTime;
					}
					String offTime = Integer.toString(secAutoOffStored);
					if (secAutoOffStored < 10) {
						offTime = "0" + offTime;
					}
					cmd = "a=2," + onTime + "," + offTime;
					secAutoAlarmFront.setText(getResources().getString(R.string.sec_auto_alarm_on, secAutoOn, secAutoOff));
					secChangeAlarm.setVisibility(View.VISIBLE);
				} else {
					cmd = "a=3";
					secAutoAlarmFront.setText(getResources().getString(R.string.sec_auto_alarm_off));
				}
				deviceID = "sf1";
				break;
			case R.id.cb_sec_auto_alarm_2:
				url = deviceIPs[secBackIndex];
				if (secAutoAlarmBack.isChecked()) {
					String onTime = Integer.toString(secAutoOnStored);
					if (secAutoOnStored < 10) {
						onTime = "0" + onTime;
					}
					String offTime = Integer.toString(secAutoOffStored);
					if (secAutoOffStored < 10) {
						offTime = "0" + offTime;
					}
					cmd = "a=2," + onTime + "," + offTime;
					secAutoAlarmBack.setText(getResources().getString(R.string.sec_auto_alarm_on, secAutoOn, secAutoOff));
					secChangeAlarm.setVisibility(View.VISIBLE);
				} else {
					cmd = "a=3";
					secAutoAlarmBack.setText(getResources().getString(R.string.sec_auto_alarm_off));
				}
				deviceID = "sb1";
				break;
			case R.id.tv_change_alarm:
				final Dialog alarmDlg = new Dialog(MyHomeControl.this);
				final int orgOnTime = secAutoOnStored;
				final int orgOffTime = secAutoOffStored;
				alarmDlg.setTitle("NumberPicker");
				alarmDlg.setContentView(R.layout.se_alarm_settings);
				Button cancelButton = alarmDlg.findViewById(R.id.bt_sec_cancel);
				Button okButton = alarmDlg.findViewById(R.id.bt_sec_ok);
				final NumberPicker npOnTime = alarmDlg.findViewById(R.id.np_Alarm_on);
				npOnTime.setMaxValue(23);
				npOnTime.setMinValue(0);
				npOnTime.setValue(secAutoOnStored);
				npOnTime.setWrapSelectorWheel(false);
				npOnTime.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
					@Override
					public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
						secAutoOnStored = newVal;
					}
				});
				final NumberPicker npOffTime = alarmDlg.findViewById(R.id.np_Alarm_off);
				npOffTime.setMaxValue(23);
				npOffTime.setMinValue(0);
				npOffTime.setValue(secAutoOffStored);
				npOffTime.setWrapSelectorWheel(false);
				npOffTime.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
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
						new ESPbyTCP(deviceIPs[secFrontIndex],
								"a=2," + onTime + "," + offTime, "sf1");
						new ESPbyTCP(deviceIPs[secBackIndex],
								"a=2," + onTime + "," + offTime, "sb1");

						alarmDlg.dismiss();
					}
				});
				alarmDlg.show();
				break;
			case R.id.ib_snapshot_front:
			case R.id.ib_snapshot_front2:
				url = deviceIPs[cam1Index];
				cmd = "t";
				deviceID = "cm1";
				break;
			case R.id.ib_snapshot_last:
			case R.id.ib_snapshot_last2:
				cmd = "";
				new galleryCommunication().execute("93.104.213.79/gallery", "/get.php", "1");
				break;
			case R.id.ib_snapshot_gallery:
			case R.id.ib_snapshot_gallery2:
				cmd = "";
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://giesecke.tk/gallery"));
				startActivity(browserIntent);
				break;
			case R.id.ib_seccam:
//				Intent myIntent = new Intent(this, SecCamViewer.class);
//				this.startActivity(myIntent);
				Intent launchIntent = getPackageManager().getLaunchIntentForPackage("tk.giesecke.cctvview");
				if (launchIntent != null) {
					startActivity(launchIntent);//null pointer check in case package name was not found
				} else {
					// Try VLC version
					launchIntent = getPackageManager().getLaunchIntentForPackage("tk.giesecke.cctvviewvlc");
					if (launchIntent != null) {
						startActivity(launchIntent);//null pointer check in case package name was not found
					} else {
						// Try use external VLC version
						launchIntent = getPackageManager().getLaunchIntentForPackage("tk.giesecke.cctvusingvlc");
						if (launchIntent != null) {
							startActivity(launchIntent);//null pointer check in case package name was not found
						} else {
							Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
									getString((R.string.sec_missing_app)),
									Snackbar.LENGTH_INDEFINITE);
							mySnackbar.setAction("OK", mOnClickListener);
							mySnackbar.show();
						}
					}
				}
				break;
			case R.id.ib_cctv:
				/* Prepare lists for CCTV footage */
				lists = new CCTVfootages();
				Snackbar mySnackbar = Snackbar.make(findViewById(android.R.id.content),
						getString(R.string.cctv_footage_wait),
						Snackbar.LENGTH_LONG);
				mySnackbar.show();
				new cctvCommunication();
				break;
			default:
				wasSecButton = false;
				break;
		}
		if (!cmd.equalsIgnoreCase("")) {
			new ESPbyTCP(url, cmd, deviceID);
		}
		return wasSecButton;
	}

	/*
	 * Handle Solar panel view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from solar panel view
	 */
	private boolean handleSPMbuttons(View v) {
		/* Flag if button was handled */
		boolean wasSPMbutton = true;
		/* Button to go to previous  log */
		Button prevButton = findViewById(R.id.bt_prevLog);
		/* Button to go to next log */
		Button nextButton = findViewById(R.id.bt_nextLog);
		/* The application context */
		Context thisAppContext = getApplicationContext();

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
						/* Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/* String list with requested date info */
						String[] requestedDate = logDates.get(logDatesIndex).substring(0, 8).split("-");
						/* Instance of data base */
						SQLiteDatabase dataBase = dbHelperNow.getReadableDatabase();

						/* Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						if (newDataSet != null) {
							ChartHelper.fillSeries(newDataSet, appView);
							ChartHelper.initChart(false, thisAppContext, chartTitle);
							newDataSet.close();
						}
						dataBase.close();

						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
				} else { // use last months database
					if (lastLogDatesIndex > 0) {
						lastLogDatesIndex--;
						/* Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/* String list with requested date info */
						String[] requestedDate = lastLogDates.get(lastLogDatesIndex).substring(0, 8).split("-");
						/* Instance of data base */
						SQLiteDatabase dataBase = dbHelperLast.getReadableDatabase();

						/* Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						if (newDataSet != null) {
							ChartHelper.fillSeries(newDataSet, appView);
							ChartHelper.initChart(false, thisAppContext, chartTitle);
							newDataSet.close();
						}
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
						/* Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/* String list with requested date info */
						String[] requestedDate = logDates.get(logDatesIndex).substring(0, 8).split("-");
						/* Instance of data base */
						SQLiteDatabase dataBase = dbHelperNow.getReadableDatabase();

						/* Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						if (newDataSet != null) {
							ChartHelper.fillSeries(newDataSet, appView);
							ChartHelper.initChart(false, thisAppContext, chartTitle);
							newDataSet.close();
						}
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
						/* Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
						Button stopButton = findViewById(R.id.bt_stop);
						stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
						stopButton.setText(getResources().getString(R.string.start));
						ChartHelper.autoRefreshOn = false;
						showingLog = true;
						// Get data from data base
						/* String list with requested date info */
						String[] requestedDate = lastLogDates.get(lastLogDatesIndex).substring(0, 8).split("-");
						/* Instance of data base */
						SQLiteDatabase dataBase = dbHelperLast.getReadableDatabase();

						/* Cursor with new data from the database */
						Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
								Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
						if (newDataSet != null) {
							ChartHelper.fillSeries(newDataSet, appView);
							ChartHelper.initChart(false, thisAppContext, chartTitle);
							newDataSet.close();
						}
						dataBase.close();

						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
				}
				break;
			case R.id.bt_stop:
				if (ChartHelper.autoRefreshOn) {
					/* Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
					stopButton.setText(getResources().getString(R.string.start));
					ChartHelper.autoRefreshOn = false;
				} else {
					if (showingLog) {
						showingLog = false;
						if (Utilities.isHomeWiFi(this)) {
							startService(new Intent(this, SolarSyncDataBase.class));
						}

						/* Pointer to text views showing the consumed / produced energy */
						TextView energyText = findViewById(R.id.tv_cons_energy);
						energyText.setVisibility(View.INVISIBLE);
						energyText = findViewById(R.id.tv_solar_energy);
						energyText.setVisibility(View.INVISIBLE);

						logDatesIndex = logDates.size() - 1;
						nextButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					}
					/* Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = findViewById(R.id.bt_stop);
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

	/*
	 * Handle Aircon view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from aircon view
	 */
	private boolean handleAirconButtons(View v) {
		/* Flag if button was handled */
		boolean wasAirconButton = true;
		/* URL for communication with ESP */
//		String url = espIP[selDevice];
		String url = deviceIPs[selDevice + aircon1Index];
		/* DeviceID used for MQTT */
		String deviceID = deviceName[selDevice];
		/* Command for ESP */
		String cmd = "";
		/* Timer button */
		Button btTimer;
		/* Text for timer button */
		String timerTime;

		switch (v.getId()) {
			case R.id.bt_auto_fd:
			case R.id.bt_auto_ca:
			case R.id.bt_auto_am:
				if (autoOnStatus[selDevice] == 1) {
					cmd = "c=" + CMD_AUTO_OFF;
				} else {
					cmd = "c=" + CMD_AUTO_ON;
				}
				break;
			case R.id.bt_on_off_fd:
			case R.id.bt_on_off_ca:
			case R.id.bt_on_off_am:
				cmd = "c=" + CMD_ON_OFF;
				break;
			case R.id.bt_fan_high_fd:
				if (fanStatus[selDevice] != 2) {
					cmd = "c=" + CMD_FAN_HIGH;
				}
				break;
			case R.id.bt_fan_med_fd:
				if (fanStatus[selDevice] != 1) {
					cmd = "c=" + CMD_FAN_MED;
				}
				break;
			case R.id.bt_fan_low_fd:
				if (fanStatus[selDevice] != 0) {
					cmd = "c=" + CMD_FAN_LOW;
				}
				break;
			case R.id.bt_autom_ca:
			case R.id.bt_autom_am:
				if (modeStatus[selDevice] != 3) {
					cmd = "c=" + CMD_MODE_AUTO;
				}
				break;
			case R.id.bt_cool_fd:
			case R.id.bt_cool_ca:
			case R.id.bt_cool_am:
				if (modeStatus[selDevice] != 2) {
					cmd = "c=" + CMD_MODE_COOL;
				}
				break;
			case R.id.bt_dry_fd:
			case R.id.bt_dry_ca:
			case R.id.bt_dry_am:
				if (modeStatus[selDevice] != 1) {
					cmd = "c=" + CMD_MODE_DRY;
				}
				break;
			case R.id.bt_fan_fd:
			case R.id.bt_fan_ca:
			case R.id.bt_fan_am:
				if (modeStatus[selDevice] != 0) {
					cmd = "c=" + CMD_MODE_FAN;
				}
				break;
			case R.id.bt_sweep_ca:
			case R.id.bt_sweep_am:
				cmd = "c=" + CMD_OTHER_SWEEP;
				break;
			case R.id.bt_turbo_ca:
			case R.id.bt_sleep_am:
				cmd = "c=" + CMD_OTHER_TURBO;
				break;
			case R.id.bt_ion_ca:
				cmd = "c=" + CMD_OTHER_ION;
				break;
			case R.id.bt_plus_fd:
			case R.id.bt_plus_ca:
			case R.id.bt_plus_am:
				cmd = "c=" + CMD_TEMP_PLUS;
				break;
			case R.id.bt_minus_fd:
			case R.id.bt_minus_ca:
			case R.id.bt_minus_am:
				cmd = "c=" + CMD_TEMP_MINUS;
				break;
			case R.id.bt_fanspeed_ca:
			case R.id.bt_fanspeed_am:
				cmd = "c=" + CMD_FAN_SPEED;
				break;
			case R.id.im_icon_fd:
				if (deviceIsOn[aircon2Index]) { // Is Carrier aircon online?
					airFDView.setVisibility(View.INVISIBLE);
					airCAView.setVisibility(View.VISIBLE);
					airAMView.setVisibility(View.INVISIBLE);
					selDevice = 1;
					mPrefs.edit().putInt(prefsSelDevice, selDevice).apply();
				} else if (deviceIsOn[aircon3Index]) { // Is other aircon online?
					airFDView.setVisibility(View.INVISIBLE);
					airCAView.setVisibility(View.INVISIBLE);
					airAMView.setVisibility(View.VISIBLE);
					selDevice = 2;
					mPrefs.edit().putInt(prefsSelDevice, selDevice).apply();
				}
				break;
			case R.id.im_icon_ca:
				if (deviceIsOn[aircon3Index]) { // Is other aircon online?
					airFDView.setVisibility(View.INVISIBLE);
					airCAView.setVisibility(View.INVISIBLE);
					airAMView.setVisibility(View.VISIBLE);
					selDevice = 2;
					mPrefs.edit().putInt(prefsSelDevice, selDevice).apply();
				} else if (deviceIsOn[aircon1Index]) {
					airCAView.setVisibility(View.INVISIBLE);
					airFDView.setVisibility(View.VISIBLE);
					airAMView.setVisibility(View.INVISIBLE);
					selDevice = 0;
					mPrefs.edit().putInt(prefsSelDevice, selDevice).apply();
				}
				break;
			case R.id.im_icon_am:
				if (deviceIsOn[aircon1Index]) { // Is FujiDenzo aircon online?
					airFDView.setVisibility(View.VISIBLE);
					airCAView.setVisibility(View.INVISIBLE);
					airAMView.setVisibility(View.INVISIBLE);
					selDevice = 0;
					mPrefs.edit().putInt(prefsSelDevice, selDevice).apply();
				} else if (deviceIsOn[aircon2Index]) { // Is other aircon online?
					airFDView.setVisibility(View.INVISIBLE);
					airCAView.setVisibility(View.VISIBLE);
					airAMView.setVisibility(View.INVISIBLE);
					selDevice = 1;
					mPrefs.edit().putInt(prefsSelDevice, selDevice).apply();
				}
				break;
			case R.id.bt_timer_fd:
			case R.id.bt_timer_ca:
			case R.id.bt_timer_am:
				if (BuildConfig.DEBUG)
					Log.d(DEBUG_LOG_TAG, "Setting timer to " + deviceTimer[selDevice]);
				if (BuildConfig.DEBUG)
					Log.d(DEBUG_LOG_TAG, "First command = " + "t=" + deviceTimer[selDevice]);
				if (BuildConfig.DEBUG)
					Log.d(DEBUG_LOG_TAG, "Second command = " + "c=" + CMD_OTHER_TIMER);
				new ESPbyTCP(url, "t=" + deviceTimer[selDevice], deviceID);
				cmd = "c=" + CMD_OTHER_TIMER;
				break;
			case R.id.bt_timer_minus_fd:
			case R.id.bt_timer_minus_ca:
			case R.id.bt_timer_minus_am:
				if (timerStatus[selDevice] == 0) {
					if (deviceTimer[selDevice] > 1) {
						deviceTimer[selDevice]--;
						cmd = "t=" + deviceTimer[selDevice];
					}
					btTimer = findViewById(R.id.bt_timer_fd);
					timerTime = deviceTimer[selDevice] +
							" " +
							getString(R.string.bt_txt_hour);
					btTimer.setText(timerTime);
				}
				break;
			case R.id.bt_timer_plus_fd:
			case R.id.bt_timer_plus_ca:
			case R.id.bt_timer_plus_am:
				if (timerStatus[selDevice] == 0) {
					if (deviceTimer[selDevice] < 9) {
						deviceTimer[selDevice]++;
						cmd = "t=" + deviceTimer[selDevice];
					}
					btTimer = findViewById(R.id.bt_timer_fd);
					timerTime = deviceTimer[selDevice] +
							" " +
							getString(R.string.bt_txt_hour);
					btTimer.setText(timerTime);
				}
				break;
			default: // End here if it was not an aircon view button
				wasAirconButton = false;
		}
		if (!cmd.equalsIgnoreCase("")) {
			new ESPbyTCP(url, cmd, deviceID);
		}
		return wasAirconButton;
	}

	/*
	 * Handle Debug view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from debug view
	 */
	@SuppressWarnings("ConstantConditions")
	private boolean handleDebugButtons(View v) {
		/* Flag if button was handled */
		boolean wasDebugButton = true;
		/* URL for communication with ESP */
//		String url = espIP[selDevice];
		String url = deviceIPs[selDevice + aircon1Index];
		/* DeviceID used for MQTT */
		String deviceID = "chk";
		/* Command for ESP */
		String cmd = "";
		/* Textview to show the incoming debug messages */
		TextView debugTxtView = findViewById(R.id.tv_sv_debug);

		switch (v.getId()) {
			case R.id.bt_clear:
				debugTxtView.setText("");
				debugMsgs = "";
				break;
//			case R.id.bt_debug_moni:
//				cmd = "d";
//				url = deviceIPs[moniIndex];
//				break;
//			case R.id.bt_res_moni:
//				cmd = "r";
//				url = deviceIPs[moniIndex];
//				break;
//			case R.id.bt_debug_secf:
//				cmd = "d";
//				url = deviceIPs[secFrontIndex];
//				break;
//			case R.id.bt_res_secf:
//				cmd = "r";
//				url = deviceIPs[secFrontIndex];
//				break;
//			case R.id.bt_debug_secb:
//				cmd = "d";
//				url = deviceIPs[secBackIndex];
//				break;
//			case R.id.bt_res_secb:
//				cmd = "r";
//				url = deviceIPs[secBackIndex];
//				break;
//			case R.id.bt_debug_ac1:
//				cmd = "d";
//				url = deviceIPs[aircon1Index];
//				break;
//			case R.id.bt_res_ac1:
//				cmd = "r";
//				url = deviceIPs[aircon1Index];
//				break;
//			case R.id.bt_debug_ac2:
//				cmd = "d";
//				url = deviceIPs[aircon2Index];
//				break;
//			case R.id.bt_res_ac2:
//				cmd = "r";
//				url = deviceIPs[aircon2Index];
//				break;
//			case R.id.bt_debug_ac3:
//				cmd = "d";
//				url = deviceIPs[aircon3Index];
//				break;
//			case R.id.bt_res_ac3:
//				cmd = "r";
//				url = deviceIPs[aircon3Index];
//				break;
//			case R.id.bt_debug_cam1:
//				cmd = "d";
//				url = deviceIPs[cam1Index];
//				break;
//			case R.id.bt_res_cam1:
//				cmd = "r";
//				url = deviceIPs[cam1Index];
//				break;
			case R.id.bt_highlight:
				filterDbgMsg();
				break;
			default: // End here if it was not an aircon view button
				wasDebugButton = handleDebugCmdButtons(v);
		}
		if (!cmd.equalsIgnoreCase("")) {
			new ESPbyTCP(url, cmd, deviceID);
		}
		return wasDebugButton;
	}

	/*
	 * Handle Debug command buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from debug view
	 */
	@SuppressWarnings("ConstantConditions")
	private boolean handleDebugCmdButtons(View v) {
		/* Toggle button that was pressed */
		ToggleButton tbPushed = null;
		/* Index for list of enabled devices to send command to */
		int listIndex = -1;
		if (v.getId() == R.id.bt_cmd_send) {
			String[] availDevices = getResources().getStringArray(R.array.mhc_devices);
			AlertDialog.Builder devListbuilder = new AlertDialog.Builder(this);
			devListbuilder.setTitle("Select device");
			devListbuilder.setItems(availDevices, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Selected device num = " + which);
					String[] availCmds = null;
					final int selDeviceNum = which;
					String cmdAsText;
					switch (which) {
						case 0:
						case 1:
							availCmds = getResources().getStringArray(R.array.sec_cmds_txt);
							break;
						case 2:
							availCmds = getResources().getStringArray(R.array.cam_cmds_txt);
							break;
						case 3:
							availCmds = getResources().getStringArray(R.array.byard_light_cmds_txt);
							break;
						case 4:
							availCmds = getResources().getStringArray(R.array.bed_light_cmds_txt);
							break;
						case 5:
						case 6:
							availCmds = getResources().getStringArray(R.array.ac_cmds_txt);
							break;
						case 7:
							availCmds = getResources().getStringArray(R.array.mhc_cmds_txt);
							break;
						case 8:
							cmdAsText = "d";
							for (int i = 0; i < deviceNames.length; i++) {
								if (!deviceIPs[i].equalsIgnoreCase("")) {
									if (BuildConfig.DEBUG)
										Log.d(DEBUG_LOG_TAG, "Request status from : " + deviceIPs[i]);
									new ESPbyTCP(deviceIPs[i], cmdAsText, "0");
								}
							}
							break;
						case 9:
							cmdAsText = "s";
							for (int i = 0; i < deviceNames.length; i++) {
								if (!deviceIPs[i].equalsIgnoreCase("")) {
									if (BuildConfig.DEBUG)
										Log.d(DEBUG_LOG_TAG, "Request status from : " + deviceIPs[i]);
									new ESPbyTCP(deviceIPs[i], cmdAsText, "0");
								}
							}
							break;
					}
					dialog.dismiss();
					if (availCmds != null) {
						AlertDialog.Builder cmdListBuilder = new AlertDialog.Builder(appContext);
						cmdListBuilder.setTitle("Select command");
						cmdListBuilder.setItems(availCmds, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (BuildConfig.DEBUG)
									Log.d(DEBUG_LOG_TAG, "Selected cmd num = " + which);
								String selURL = "";
								String[] cmdList = null;
								switch (selDeviceNum) {
									case 0:
										selURL = deviceIPs[secFrontIndex];
										cmdList = getResources().getStringArray(R.array.sec_cmds);
										break;
									case 1:
										selURL = deviceIPs[secBackIndex];
										cmdList = getResources().getStringArray(R.array.sec_cmds);
										break;
									case 2:
										selURL = deviceIPs[cam1Index];
										cmdList = getResources().getStringArray(R.array.cam_cmds);
										break;
									case 3:
										selURL = deviceIPs[ly1Index];
										cmdList = getResources().getStringArray(R.array.byard_light_cmds);
										break;
									case 4:
										selURL = deviceIPs[lb1Index];
										cmdList = getResources().getStringArray(R.array.bed_light_cmds);
										break;
									case 5:
										selURL = deviceIPs[aircon1Index];
										cmdList = getResources().getStringArray(R.array.ac_cmds);
										break;
									case 6:
										selURL = deviceIPs[aircon2Index];
										cmdList = getResources().getStringArray(R.array.ac_cmds);
										break;
									case 7:
										selURL = deviceIPs[11];
										cmdList = getResources().getStringArray(R.array.mhc_cmds);
										break;
								}
								dialog.dismiss();
								if (cmdList != null) {
									if (BuildConfig.DEBUG)
										Log.d(DEBUG_LOG_TAG, "Selected cmd = " + cmdList[which]);
									new ESPbyTCP(selURL, cmdList[which], "0");
								}
							}
						});
						cmdListBuilder.create();
						cmdListBuilder.show();
					}
				}
			});
			devListbuilder.show();
		} else {
			return false;
		}

		if ((listIndex != -1) && tbPushed != null) {
			if (tbPushed.isChecked()) {
				tbPushed.setTextColor(colorGreen);
				cmdDeviceList[listIndex] = true;
			} else {
				tbPushed.setTextColor(colorGrey);
				cmdDeviceList[listIndex] = false;
			}
		}
		return true;
	}

	/*
	 * Handle Light control view buttons
	 *
	 * @param v
	 * 		View with the ID of the clicked button
	 * @return <code>boolean</code>
	 * 		True if button was handled
	 * 		False if button was not from light control view
	 */
	@SuppressLint("SetTextI18n")
	private boolean handleLightButtons(View v) {
		/* Flag if button was handled */
		boolean wasLightButton = true;
		/* DeviceID used for MQTT */
//		String deviceID = getString(R.string.LIGHTS_BEDROOM);
		String deviceID = deviceIPs[lb1Index];
		/* Command for ESP */
		String cmd = "";

		switch (v.getId()) {
			case R.id.ib_light_bed_dim:
				int dimLightLevel = mPrefs.getInt(prefsLightBedDim, 200);
				String lightValue = String.valueOf(dimLightLevel);
				cmd = "b=" + lightValue;
				bedRoomVal.setText(getString(R.string.lights_val_dim));
				bedRoomValSB.setProgress(222 - dimLightLevel);
				lightsBedRoomVal = dimLightLevel;
				break;
			case R.id.ib_light_bed_off:
				cmd = "b=255";
				bedRoomVal.setText(getString(R.string.lights_val_off));
				bedRoomValSB.setProgress(0);
				lightsBedRoomVal = 255;
				break;
			case R.id.ib_light_bed_on:
				cmd = "b=140";
				bedRoomVal.setText(getString(R.string.lights_val_on));
				bedRoomValSB.setProgress(82);
				lightsBedRoomVal = 140;
				break;
			case R.id.tb_byard_light:
//				deviceID = getString(R.string.LIGHTS_BACKYARD);
				deviceID = deviceIPs[ly1Index];
				ToggleButton byLightSwitch = findViewById(R.id.tb_byard_light);
				if (byLightSwitch.isChecked()) {
					cmd = "l=1";
				} else {
					cmd = "l=0";
				}
			default: // End here if it was not an light control view button
				wasLightButton = false;
		}
		if (!cmd.equalsIgnoreCase("")) {
			new ESPbyTCP(deviceID, cmd, deviceID);
		}
		return wasLightButton;
	}

	/*
	 * Filter debug message with new search phrase
	 * or reset to full text if search phrase is empty
	 */
	private void filterDbgMsg() {
		/* Textview to show the incoming debug messages */
		TextView debugTxtView = findViewById(R.id.tv_sv_debug);
		/* Edittext to highlight search word in the incoming debug messages */
		EditText debugTxtHighlight = findViewById(R.id.et_highlight);

		highlightText = debugTxtHighlight.getText().toString();
		ScrollView debugTxtScroll = findViewById(R.id.sv_debugview);

		if (highlightText == null || highlightText.isEmpty()) { //if no search text is entered
			debugTxtView.setText(debugMsgs);
			debugTxtScroll.fullScroll(View.FOCUS_DOWN);
		} else {
			StringBuilder output = new StringBuilder();
			String line;
			if (debugMsgs.startsWith("null")) {
				debugMsgs = debugMsgs.substring(4);
			}
			try {
				BufferedReader reader = new BufferedReader(
						new StringReader(debugMsgs));
				while ((line = reader.readLine()) != null) {

					if ((line.length() > 0)
							&& (line.toUpperCase().contains(highlightText.toUpperCase()))) {
						output.append(line).append("\n");
					}
				}
			} catch (Exception ignore) {
				debugTxtView.setText(debugMsgs);
				debugTxtScroll.fullScroll(View.FOCUS_DOWN);
			}
			debugTxtView.setText(output);
		}
		debugTxtScroll.fullScroll(View.FOCUS_DOWN);
	}

	private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			snackBarText = "";
		}
	};
}

