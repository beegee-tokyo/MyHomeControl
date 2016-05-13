// This example uses an Adafruit Huzzah ESP8266
// to connect to shiftr.io.
//
// You can check on your device after a successful
// connection here: https://shiftr.io/try.
//
// by Joël Gähwiler
// https://github.com/256dpi/arduino-mqtt

#include "declarations.h"
#include "functions.h"
#include "wifiAPInfo.h"

void setup() {
	// Initialize display
	ucg.begin(UCG_FONT_MODE_TRANSPARENT);
	ucg.setFont(ucg_font_helvB08_tf);
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
	String buildVersion = "SW build: " + String(compileDate);
	ucg_print_ln(String(compileDate), false);
	ucg_print_ln(" ", true);

	pinMode(COM_LED, OUTPUT); // Communication LED blue
	pinMode(ACT_LED, OUTPUT); // Communication LED red
	digitalWrite(COM_LED, HIGH); // Turn off blue LED
	digitalWrite(ACT_LED, HIGH); // Turn off red LED
	Serial.begin(115200);
	Serial.println();
	connectWiFi();
	if (WiFi.status() == WL_CONNECTED) {
		ucg_print_ln("Connected to ", false);
		ucg_print_ln(ssid, false);
		IPAddress receivedIP = WiFi.localIP();
		String ipString = "";
		for (int i=0; i<4; i++) {
			ipString += i	? "." + String(receivedIP[i]) : String(receivedIP[i]); 
		}
		ipString = "IP address: " + ipString;
		ucg_print_ln(ipString, false);
	} else {
		ucg_print_ln("Could not find", false);
		ucg_print_ln(ssid, false);
		ucg_print_ln("Not connected", false);
	}
	
	mqttClient.begin(mqttBroker, mqttReceiver);

	Serial.print("\nConnecting to MQTT broker");
	int connectTimeout = 0;
	int retryConnection = 0;
	while (!mqttClient.connect(mqttID, mqttUser, mqttPwd)) {
		delay(500);
		Serial.print(".");
		connectTimeout++;
		if (connectTimeout > 60) { //Wait for 30 seconds (60 x 500 milliseconds) to reconnect
			retryConnection++;
			if (retryConnection == 5) {
				Serial.print("\nCan't connect to MQTT broker");
				ucg_print_ln("Can't connect to MQTT", false);
				break;
			}
		}
	}

	if (mqttClient.connected()) {
		Serial.println("\nconnected!");
		ucg_print_ln("Connected to MQTT", false);
	}

	mqttClient.subscribe("CMD");
	// mqttClient.unsubscribe("CMD");

	// Start FTP server
	Serial.println("Starting FTP server");
	Serial.print("Connect with user: \"");
	Serial.print(host);
	Serial.print("\" and pw: \"");
	Serial.print(host);
	Serial.println("\"");
	ftpSrv.begin(host, host);	 //username, password for ftp.	set ports in ESP8266FtpServer.h	(default 21, 50009 for PASV)

	/* Configure the Adafruit TSL2561 light sensor */
	/* Set SDA and SCL pin numbers */
	tsl.setI2C(sdaPin, sclPin);
	/* Initialise the sensor */
	if (tsl.begin()) {
		/* Setup the sensor gain and integration time */
		configureSensor();
	}

	// Initialize temperature sensor
	dht.begin();

	// Get initial data and status
	getHomeInfo(true);

	// Send initial values
	sendToMQTT();

	// Start update of data every 1 minute
	getStatusTimer.attach(60, triggerGetStatus);

	// Start update of weather data every 15 seconds
	getDHTTimer.attach(15, triggerGetDHT);

	ArduinoOTA.onStart([]() {
		Serial.println("OTA start");
		ucg.clearScreen();
		ucg.setFont(ucg_font_helvB18_tf);
		ucg.setRotate180();
		ucg.setColor(255, 0, 0);
		ucg.drawBox(0, 0, 128, 128);
		ucg.setColor(0, 0, 0);
		ucg_print_center("OTA !!!!", 0, 43);
		otaRunning = true;
		getStatusTimer.detach();
		// Serial.println("Unsubscribe from MQTT");
		// mqttClient.unsubscribe("/cmd");
		// Serial.println("Disconnect from MQTT");
		// mqttClient.disconnect();
		ledFlasher.attach(0.1, blueLedFlash); // Flash very fast if we started update
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
			ucg.setFont(ucg_font_helvB18_tf);
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
		ucg.setFont(ucg_font_helvB18_tf);
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
	});
	// Start OTA server.
	ArduinoOTA.setHostname(host);
	ArduinoOTA.begin();
}

void loop() {
	if (otaRunning) { // While OTA update is running we do nothing else
		return;
	}
	// Handle OTA updates
	ArduinoOTA.handle();

	/** Handle MQTT subscribed */
	mqttClient.loop();
	delay(10); // <- fixes some issues with WiFi stability

	if(!mqttClient.connected()) {
		showMQTTerrorScreen();
		mqttClient.begin(mqttBroker, mqttReceiver);

		Serial.print("\nConnecting to MQTT broker");
		int connectTimeout = 0;
		while (!mqttClient.connect(mqttID, mqttUser, mqttPwd)) {
			delay(500);
			Serial.print(".");
			connectTimeout++;
			if (connectTimeout > 120) { //Wait for 60 seconds (120 x 500 milliseconds) to reconnect
				Serial.print("\nCan't connect to MQTT broker");
				// Try to reset WiFi connection
				connectWiFi();
			}
			// Handle OTA updates
			ArduinoOTA.handle();
		}
		// Reconnected ==> refresh screen
		delay(10); // <- fixes some issues with WiFi stability
		getHomeInfo(true);
	}

	// Handle FTP access
	ftpSrv.handleFTP();
	
	// Handle new data update request
	if (statusUpdated) {
		statusUpdated = false;
		getHomeInfo(false);
		delay(10); // <- fixes some issues with WiFi stability
		sendToMQTT();
	}

	// Handle new temperature & humidity update request
	if (dhtUpdated) {
		dhtUpdated = false;
		getTemperature();
		getLight();
		updateWeather();
	}
}
