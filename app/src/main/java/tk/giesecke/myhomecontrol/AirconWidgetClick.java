package tk.giesecke.myhomecontrol;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static tk.giesecke.myhomecontrol.MyHomeControl.sharedPrefName;

/**
 * Handles clicks on specific parts of the aircon widget
 */
public class AirconWidgetClick extends IntentService {

	/** Debug tag */
	private static final String DEBUG_LOG_TAG = "MHC-AC-W";

	public AirconWidgetClick() {
		super("AirconWidgetClick");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			/** Access to shared preferences of app widget */
			SharedPreferences mPrefs = this.getSharedPreferences(MyHomeControl.sharedPrefName, 0);
			int timerTime = mPrefs.getInt("acTimerTime", 1);
			boolean isRunning = mPrefs.getBoolean("acTimerOn", false);
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Widget click: " + intent.getAction());

			if (intent.getAction().equals("m")){
				if (timerTime > 1) {
					timerTime--;
					espComm("t=" + Integer.toString(timerTime));
				}
			}
			if (intent.getAction().equals("p")){
				if (timerTime < 9) {
					timerTime++;
					espComm("t=" + Integer.toString(timerTime));
				}
			}
			if (intent.getAction().equals("s")){
				if (!isRunning) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Starting timer");
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "First command = " + "t=" + Integer.toString(timerTime));
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Second command = " + "c="  + MyHomeControl.CMD_OTHER_TIMER);
					if (espComm("t=" + Integer.toString(timerTime))) {
						if (espComm("c=" + MyHomeControl.CMD_OTHER_TIMER)) {
							isRunning = true;
							mPrefs.edit().putBoolean("acTimerOn", true).apply();
						}
					}
				} else {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Stopping timer");
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "First command = " + "c="  + MyHomeControl.CMD_OTHER_TIMER);
					if (espComm("c=" + MyHomeControl.CMD_OTHER_TIMER)) {
						isRunning = false;
						mPrefs.edit().putBoolean("acTimerOn", false).apply();
					}
				}
			}
			mPrefs.edit().putInt("acTimerTime", timerTime).apply();
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY)+2);
			SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.GERMANY);
			String timerEnd = df.format(calendar.getTime());
			mPrefs.edit().putString("acTimerEnd", timerEnd).apply();

			/** App widget manager for all widgets of this app */
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
			/** Component name of this widget */
			ComponentName thisAppWidget = new ComponentName(this.getPackageName(),
					AirconWidget.class.getName());
			/** List of all active widgets */
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

			for (int appWidgetId : appWidgetIds) {
				AirconWidget.updateAppWidget(this,appWidgetManager,appWidgetId, timerTime, timerEnd, isRunning);
			}
		}
	}

	/**
	 * Send command to ESP
	 *
	 * @param cmd
	 * 		Command to be sent to ESP
	 */
	@SuppressLint("CommitPrefEdits")
	private boolean espComm(String cmd) {
		// If we are not on home WiFi, send command to MQTT broker
		if (!Utilities.isHomeWiFi(getApplicationContext())) {
			String mqttTopic = "{\"ip\":\"fd1\","; // Device IP address
			mqttTopic += "\"cm\":\"" + cmd + "\"}"; // The command

			doPublish(mqttTopic, getApplicationContext());
			return true;
		}

		SharedPreferences mPrefs = getSharedPreferences(sharedPrefName,0);
		String url = mPrefs.getString( MyHomeControl.deviceNames[ MyHomeControl.aircon1Index],"NA");
		try {
			InetAddress tcpServer = InetAddress.getByName(url);
			Socket tcpSocket = new Socket(tcpServer, 6000);

			tcpSocket.setSoTimeout(1000);
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(tcpSocket.getOutputStream())), true);
			out.println(cmd);
			tcpSocket.close();
		} catch (Exception e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "TCP connection failed: " + e.getMessage()
					+ " " + url);
			return false;
		}
		return true;
	}

	/**
	 * Send topic to MQTT broker
	 *
	 * @param payload
	 * 		Topic message
	 * @param widgetContext
	 * 		Context
	 */
	static void doPublish(String payload, Context widgetContext){
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT topic publish: " + payload);
		if (MessageListener.mqttClient != null) { // If service is not (yet) active, don't publish
			IMqttToken token;
			MqttConnectOptions options = new MqttConnectOptions();
			String mqttUser = widgetContext.getResources().getString(R.string.MQTT_USER_SUB_1);
			String mqttPw = widgetContext.getResources().getString(R.string.MQTT_PW);
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
				if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "MQTT connect exception " +e.getMessage());
			} catch (MqttException e) {
				switch (e.getReasonCode()) {
					case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "BROKER_UNAVAILABLE " +e.getMessage());
						break;
					case MqttException.REASON_CODE_CLIENT_TIMEOUT:
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "CLIENT_TIMEOUT " +e.getMessage());
						break;
					case MqttException.REASON_CODE_CONNECTION_LOST:
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "CONNECTION_LOST " +e.getMessage());
						break;
					case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
						if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "SERVER_CONNECT_ERROR " +e.getMessage());
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
	}
}
