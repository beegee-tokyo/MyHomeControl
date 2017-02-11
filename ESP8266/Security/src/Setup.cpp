#include "Setup.h"
#include "declarations.h"

/** Timer to collect light information from LDR */
Ticker getLDRTimer;
/** Timer for heart beat */
Ticker heartBeatTimer;

/**
 * Initialization of GPIO pins, WiFi connection, timers and sensors
 */
void setup() {
	initLeds();
	pinMode(pirPort, INPUT_PULLUP); // PIR signal
	pinMode(relayPort, OUTPUT); // Relay trigger signal
	pinMode(speakerPin, OUTPUT); // Loudspeaker/piezo signal
	digitalWrite(relayPort, LOW); // Turn off Relay
//	digitalWrite(speakerPin, LOW); // Speaker off
	digitalWrite(speakerPin, HIGH); // Switch Piezo buzzer off

	Serial.begin(115200);
	Serial.setDebugOutput(false);
	Serial.println("");
	Serial.println("====================");
	Serial.println("ESP8266 Security Front");

	// Try to connect to WiFi with captive portal
	ipAddr = connectWiFi(ipAddr, ipGateWay, ipSubNet, "ESP8266 Security Front");

	if (!wmIsConnected) {
		Serial.println("WiFi connection failed!");
		Serial.println("Only audible alert and auto light is available!");
	} else {
		Serial.println("");
		Serial.print("Connected to ");
		Serial.println(ssid);
		Serial.print("IP address: ");
		Serial.println(WiFi.localIP());
	}

	Serial.print("Build: ");
	Serial.println(compileDate);

	Serial.print("Device: ");
	Serial.println(DEVICE_ID);
	Serial.println("====================");

	// Start update of LDR value every 60 seconds
	getLDRTimer.attach(60, triggerGetLDR);
	// Get initial value from LDR
	getLDR();

	// Set initial time
	setTime(getNtpTime());
	if (!gotTime) { // Failed to get time from NTP server, retry once
		setTime(getNtpTime());
	}

	// Initialize NTP client
	setSyncProvider(getNtpTime);
	setSyncInterval(3600); // Sync every hour

	// Start heart beat sending every 5 minutes
	heartBeatTimer.attach(300, triggerHeartBeat);

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

	// Send Security restart message
	sendAlarm(true);

	// Reset boot status flag
	inSetup = false;

	// Start the tcp socket server to listen on port 6000
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
		getLDRTimer.detach();
		alarmTimer.detach();
		heartBeatTimer.detach();

		WiFiUDP::stopAll();
		WiFiClient::stopAll();
	});

	// Start OTA server.
	ArduinoOTA.setHostname(OTA_HOST);
	ArduinoOTA.begin();

	// Send light status to backyard security device
	getLDR();
	sendLightStatus(switchLights);

	wdt_enable(WDTO_8S);
}
