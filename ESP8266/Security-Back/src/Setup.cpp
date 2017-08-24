#include "Setup.h"
#include "declarations.h"

/** Timer to collect light information from TSL2561 sensor */
Ticker getWeatherTimer;
/** Timer for heart beat */
Ticker heartBeatTimer;

/**
 * Initialization of GPIO pins, WiFi connection, timers and sensors
 */
void setup() {

	initLeds();
	pinMode(pirPort, INPUT); // PIR signal
	pinMode(relayPort, OUTPUT); // Relay trigger signal
	pinMode(speakerPin, OUTPUT); // Loudspeaker/piezo signal
	digitalWrite(relayPort, LOW); // Turn off Relay
	digitalWrite(speakerPin, HIGH); // Switch Piezo buzzer off

	Serial.begin(115200);
	Serial.setDebugOutput(false);
	Serial.println("");
	Serial.println("====================");
	Serial.println("ESP8266 Security Back");

	// resetWiFiCredentials();
	// Try to connect to WiFi with captive portal
	ipAddr = connectWiFi(ipAddr, ipGateWay, ipSubNet, "ESP8266 Security Back");

	if (!wmIsConnected) {
		Serial.println("WiFi connection failed!");
		Serial.println("Only audible alert and auto light is available!");
	} else {
		Serial.println("");
		Serial.print("Connected to ");
		Serial.println(ssidMHC);
		Serial.print("IP address: ");
		Serial.println(WiFi.localIP());
	}

	Serial.print("Build: ");
	Serial.println(compileDate);

	Serial.print("Device: ");
	Serial.println(DEVICE_ID);
	Serial.println("====================");

	// Get initial temperature
	getTemperature();
	// Start update of weather value every 10 seconds
	getWeatherTimer.attach(10, triggerGetWeather);

	// Set initial time
	if (!tryGetTime(debugOn)) {
		tryGetTime(debugOn); // Failed to get time from NTP server, retry
	}
	if (gotTime) {
		lastKnownYear = year();
	} else {
		lastKnownYear = 0;
	}

	// // Initialize NTP client
	// setSyncProvider(getNtpTime);
	// setSyncInterval(21600); // Sync every 6 hours

	// Start heart beat sending every 1 minute
	heartBeatTimer.attach(60, triggerHeartBeat);

	// Initialize interrupt for PIR signal
	attachInterrupt(pirPort, pirTrigger, CHANGE);

	// Initialize file system.
	if (!SPIFFS.begin())
	{
		sendDebug("Failed to mount file system", OTA_HOST);
		return;
	}

	// Try to get last status & last reboot reason from status.txt
	Serial.println("====================");
	if (!readStatus()) {
		sendDebug("No status file found, try to format the SPIFFS", OTA_HOST);
		if (formatSPIFFS(OTA_HOST)){
			sendDebug("SPIFFS formatted", OTA_HOST);
		} else {
			sendDebug("SPIFFS format failed", OTA_HOST);
		}
		writeRebootReason("Unknown");
		lastRebootReason = "No status file found";
	} else {
		Serial.println("Last reboot because: " + rebootReason);
		lastRebootReason = rebootReason;
	}
	Serial.println("====================");

	// Send Security restart message
	sendAlarm(true);

	// Reset boot status flag
	inSetup = false;

	// Start the tcp socket server to listen on port tcpComPort
	tcpServer.begin();

	if (alarmOn) {
		actLedFlashStart(1);
	} else {
		actLedFlashStop();
	}

	// Start FTP server
	ftpSrv.begin(DEVICE_ID,DEVICE_ID); //username, password for ftp. set ports in ESP8266FtpServer.h(default 21, 50009 for PASV)

	ArduinoOTA.onStart([]() {
		sendDebug("OTA start", OTA_HOST);
		// Safe reboot reason
		writeRebootReason("OTA");
		otaRunning = true;
		// Detach all interrupts and timers
		wdt_disable();
		doubleLedFlashStart(0.1);
		getWeatherTimer.detach();
		alarmTimer.detach();
		heartBeatTimer.detach();

		WiFiUDP::stopAll();
		WiFiClient::stopAll();
	});

	// Start OTA server.
	ArduinoOTA.setHostname(OTA_HOST);
	ArduinoOTA.begin();

	wdt_enable(WDTO_8S);
}
