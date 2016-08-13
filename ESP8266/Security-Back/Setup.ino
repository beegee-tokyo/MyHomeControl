/**
 * Initialization of GPIO pins, WiFi connection, timers and sensors
 */
void setup() {
	pinMode(alarmLED, OUTPUT); // Detection LED red
	pinMode(comLED, OUTPUT); // Communication LED blue
	pinMode(pirPort, INPUT); // PIR signal
	pinMode(relayPort, OUTPUT); // Relay trigger signal
	pinMode(speakerPin, OUTPUT); // Loudspeaker/piezo signal
	digitalWrite(alarmLED, HIGH); // Turn off LED
	digitalWrite(comLED, HIGH); // Turn off LED
	digitalWrite(relayPort, LOW); // Turn off Relay
	digitalWrite(speakerPin, LOW); // Speaker off

	Serial.begin(115200);
	Serial.setDebugOutput(false);
	Serial.println("");
	Serial.println("====================");
	Serial.println("ESP8266 Security");

	// Setup WiFi event handler
	WiFi.onEvent(WiFiEvent);
	
	connectWiFi();

	Serial.println("");
	Serial.print("Connected to ");
	Serial.println(ssid);
	Serial.print("IP address: ");
	Serial.println(WiFi.localIP());

	Serial.print("Build: ");
	Serial.println(compileDate);

	Serial.print("Device: ");
	Serial.println(DEVICE_ID);
	Serial.println("====================");

	/* Configure the Adafruit TSL2561 light sensor */
	/* Set SDA and SCL pin numbers */
//	tsl.setI2C(sdaPin, sclPin);
	/* Initialise the sensor */
//	if ( tsl.begin() ) {
//		/* Setup the sensor gain and integration time */
//		configureSensor();
//	}
	
	// Get initial light
//	getLight();
	getTemperature();
	// Start update of light value every 10 seconds
	getWeatherTimer.attach(10, triggerGetWeather);

	// Set initial time
	setTime(getNtpTime());

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
		Serial.println("Failed to mount file system");
		return;
	}

	// Try to get last status & last reboot reason from status.txt
	Serial.println("====================");
	if (!readStatus()) {
		Serial.println("No status file found");
		Serial.println("Try to format the SPIFFS");
		if (SPIFFS.format()){
			Serial.println("SPIFFS formatted");
		} else {
			Serial.println("SPIFFS format failed");
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

	// Start the web server to serve incoming requests
//	server.begin();

	// Start the tcp socket server to listen on port 6000
	tcpServer.begin();
	
	if (alarmOn) {
		ledFlasher.attach(1, redLedFlash);
	} else {
		ledFlasher.detach();
		digitalWrite(alarmLED, HIGH); // Turn off LED
	}

	// Start FTP server
	ftpSrv.begin(DEVICE_ID,DEVICE_ID);    //username, password for ftp.  set ports in ESP8266FtpServer.h  (default 21, 50009 for PASV)
		
	ArduinoOTA.onStart([]() {
		Serial.println("OTA start");
		// Safe reboot reason
		writeRebootReason("OTA");
		otaRunning = true;
		// Detach all interrupts and timers
		wdt_disable();
		ledFlasher.attach(0.1, blueLedFlash); // Flash very fast if we started update
		relayOffTimer.detach();
		getWeatherTimer.detach();
		alarmTimer.detach();

		WiFiUDP::stopAll();
		WiFiClient::stopAll();
		server.close();
	});

	// Start OTA server.
	ArduinoOTA.setHostname(OTA_HOST);
	ArduinoOTA.begin();

	wdt_enable(WDTO_8S);
}

