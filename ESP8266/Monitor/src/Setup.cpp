// BeeGee home control monitor
// Uses an Adafruit Huzzah ESP8266
// to connect to a MQTT server and
// displays the measured values and
// system status on a 128x128 display.

#include "Setup.h"
#include "declarations.h"

/** Build time */
const char compileDate[] = __DATE__ " " __TIME__;
/** Timer to collect status information from home devices */
Ticker getStatusTimer;

/** OTA update status */
unsigned int otaStatus = 0;

void setup() {
	// Initialize display
	ucg.begin(UCG_FONT_MODE_TRANSPARENT);
	ucg.setFont(ucg_font_helvB08_tr);
	ucg.setRotate180();
	ucg.clearScreen();
	ucg.setColor(0, 255, 0);
	ucg.drawBox(0, 0, 128, 128);
	ucg.setColor(0, 0, 0);
	ucg_print_ln("Welcome to", true);
	ucg_print_ln("BeeGee", true);
	ucg_print_ln("HomeControl", true);
	ucg_print_ln(" ", true);
	ucg_print_ln("SW build: ", false);
	ucg_print_ln(String(compileDate), false);

	// Initialize standard LEDs
	initLeds();

	// Initialize other pins
	pinMode(buttonPin, INPUT); // Button input pin

	// Initialize serial port
	Serial.begin(115200);
	Serial.println();

	// Try to connect to WiFi with captive portal
	ipAddr = connectWiFi(ipAddr, ipGateWay, ipSubNet, "ESP8266 Monitor");

	if (WiFi.status() == WL_CONNECTED) {
		ucg_print_ln("Connected to ", false);
		ucg_print_ln(ssid, false);
		// ipAddr = WiFi.localIP();
		String ipString = "";
		for (int i=0; i<4; i++) {
			ipString += i	? "." + String(ipAddr[i]) : String(ipAddr[i]);
		}
		ipString = "IP address: " + ipString;
		ucg_print_ln(ipString, false);
	} else {
		ucg_print_ln("Could not find", false);
		ucg_print_ln(ssid, false);
		ucg_print_ln("Not connected", false);
	}

	sendDebug("Reboot", OTA_HOST);

	// Initialize file system.
	if (!SPIFFS.begin())
	{
		sendDebug("Failed to mount file system", OTA_HOST);
		if (SPIFFS.format()){
			sendDebug("SPIFFS formatted", OTA_HOST);
		} else {
			sendDebug("SPIFFS format failed", OTA_HOST);
		}
	}

	// Start UDP listener
	udpListener.begin(5000);

	ucg_print_ln("Setting time", false);
	// Set initial time
	setTime(getNtpTime());
	// Set initial time
	setTime(getNtpTime());

	// Initialize NTP client
	setSyncProvider(getNtpTime);
	setSyncInterval(3600); // Sync every hour

	// Connect to MQTT broker
	mqttClient.begin(mqttBroker, mqttReceiver);

	sendDebug("Connecting to MQTT broker", OTA_HOST);
	int connectTimeout = 0;
	int retryConnection = 0;
	while (!mqttClient.connect(mqttID, mqttUser, mqttPwd)) {
		delay(500);
		Serial.print(".");
		connectTimeout++;
		if (connectTimeout > 60) { //Wait for 30 seconds (60 x 500 milliseconds) to reconnect
			retryConnection++;
			if (retryConnection == 5) {
				sendDebug("Can't connect to MQTT broker", OTA_HOST);
				ucg_print_ln("Can't connect to MQTT", false);
				break;
			}
		}
	}

	if (mqttClient.connected()) {
		sendDebug("Connected to MQTT", OTA_HOST);
		ucg_print_ln("Connected to MQTT", false);
	}

	mqttClient.subscribe("/CMD");

	// Start the tcp socket server to listen on port 6000
	tcpServer.begin();

	// Start FTP server
	ftpSrv.begin(OTA_HOST, OTA_HOST);	 //username, password for ftp.

	// Initialize temperature sensor
	dht.begin();

	// Start update of data every 1 minute
	getStatusTimer.attach(60, triggerGetStatus);

	// Start update of weather data every 15 seconds
	getDHTTimer.attach(15, triggerGetDHT);

	ArduinoOTA.onStart([]() {
		if (debugOn) {
			sendDebug("OTA start", OTA_HOST);
		}
		udpListener.stop();
		ucg.clearScreen();
		ucg.setFont(ucg_font_helvB18_tr);
		ucg.setRotate180();
		ucg.setColor(255, 0, 0);
		ucg.drawBox(0, 0, 128, 128);
		ucg.setColor(0, 0, 0);
		ucg_print_center("OTA !!!!", 0, 43);
		otaRunning = true;
		getStatusTimer.detach();
		// Serial.println("Unsubscribe from MQTT");
		// mqttClient.unsubscribe("/CMD");
		// Serial.println("Disconnect from MQTT");
		// mqttClient.disconnect();
		doubleLedFlashStart(0.1);
	});

	ArduinoOTA.onProgress([](unsigned int progress, unsigned int total) {
		unsigned int achieved = progress / (total / 100);
		if (otaStatus == 0 || achieved == otaStatus + 5) {
			otaStatus = achieved;
			ucg.setColor(255, 0, 0);
			ucg.drawBox(32, 80, 64, 40);
			ucg.setColor(0, 0, 0);
			ucg_print_center("Progress:", 0, 73);
			String progressStr = String(progress / (total / 100)) + "%";
			ucg_print_center(progressStr, 0, 103);
		}
		if (progress == total) {
			ucg.clearScreen();
			ucg.setFont(ucg_font_helvB18_tr);
			ucg.setRotate180();
			ucg.setColor(0, 255, 0, 0);
			ucg.setColor(1, 0, 255, 0);
			ucg.setColor(2, 0, 0, 255);
			ucg.setColor(3, 255, 255, 0);
			ucg.drawGradientBox(0, 0, 128, 128);
			ucg.setColor(0, 0, 0);
			ucg_print_center("OTA", 0, 50);
			ucg_print_center("finished!", 0, 80);
		}
	});

	ArduinoOTA.onError([](ota_error_t error) {
		ucg.clearScreen();
		ucg.setFont(ucg_font_helvB18_tr);
		ucg.setRotate180();
		ucg.setColor(255, 128, 0);
		ucg.drawBox(0, 0, 128, 128);
		ucg.setColor(0, 0, 0);
		ucg_print_center("MQTT ERROR", 0, 43);
		if (error == OTA_AUTH_ERROR) {
			ucg_print_center("Auth", 0, 73);
		} else if (error == OTA_BEGIN_ERROR) {
			ucg_print_center("Begin", 0, 73);
		} else if (error == OTA_CONNECT_ERROR) {
			ucg_print_center("Connect", 0, 73);
		} else if (error == OTA_RECEIVE_ERROR) {
			ucg_print_center("Receive", 0, 73);
		} else if (error == OTA_END_ERROR) {
			ucg_print_center("End", 0, 73);
		}
		ucg_print_center("Failed", 0, 103);
		if (debugOn) {
			sendDebug("OTA failed", OTA_HOST);
		}
	});


	// Start OTA server.
	ArduinoOTA.setHostname(OTA_HOST);
	ArduinoOTA.begin();

	// Get initial data and status
	getHomeInfo(true);
}
