#include "Setup.h"
#include "declarations.h"
/** Timer for heart beat */
Ticker heartBeatTimer;

/**
 * Initialization of GPIO pins, WiFi connection, timers and sensors
 */
void setup() {
	initLeds();
	pinMode(relayPort, OUTPUT); // Relay trigger signal
	digitalWrite(relayPort, LOW); // Turn off Relay

	Serial.begin(115200);
	Serial.setDebugOutput(false);
	Serial.println("");
	Serial.println("====================");
	Serial.println("ESP8266 Lights Backyard");

// wifiManager.resetSettings();
	// Try to connect to WiFi with captive portal
	ipAddr = connectWiFi(ipAddr, ipGateWay, ipSubNet, "ESP8266 Lights Backyard");

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

	// Set initial time
	if (!tryGetTime(debugOn)) {
		tryGetTime(debugOn); // Failed to get time from NTP server, retry
	}
	if (gotTime) {
		lastKnownYear = year();
	} else {
		lastKnownYear = 0;
	}

	// Start heart beat sending every 1 minute
	heartBeatTimer.attach(60, triggerHeartBeat);

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
		if (SPIFFS.format()){
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

	// Send Lights restart message
	sendAlarm(true);

	// Reset boot status flag
	inSetup = false;

	// Start the tcp socket server to listen on port tcpComPort
	tcpServer.begin();

	ArduinoOTA.onStart([]() {
		sendDebug("OTA start", OTA_HOST);
		// Safe reboot reason
		writeRebootReason("OTA");
		otaRunning = true;
		// Detach all interrupts and timers
		wdt_disable();
		doubleLedFlashStart(0.1);
		heartBeatTimer.detach();

		WiFiUDP::stopAll();
		WiFiClient::stopAll();
	});

	// Start OTA server.
	ArduinoOTA.setHostname(OTA_HOST);
	ArduinoOTA.begin();

	wdt_enable(WDTO_8S);
}
