void setup() {
	boolean debugWasOn = false;
	if (debugOn == true) {
		debugWasOn = true;
	}
	debugOn = true; // During startup
	pinMode(IR_LED_OUT, OUTPUT); // IR LED red
	pinMode(COM_LED, OUTPUT); // Communication LED blue
	pinMode(ACT_LED, OUTPUT); // Communication LED red
	digitalWrite(IR_LED_OUT, LOW); // Turn off IR LED
	digitalWrite(COM_LED, HIGH); // Turn off blue LED
	digitalWrite(ACT_LED, HIGH); // Turn off red LED

	Serial.begin(115200);

	Serial.setDebugOutput(false);
	Serial.println("");
	Serial.println("Hello from ESP8266 aircon control");
	Serial.print("SW build: ");
	Serial.println(compileDate);

	// Setup WiFi event handler
	WiFi.onEvent(WiFiEvent);
	
	// Try to connect to WiFi
	connectWiFi();

	sendDebug("Reboot");
	
	Serial.println("");
	Serial.print("Connected to ");
	Serial.println(ssid);
	Serial.print("IP address: ");
	Serial.println(WiFi.localIP());

	// Start the web server to serve incoming requests
//	server.begin();

	// Start the tcp socket server to listen on port 6000
	tcpServer.begin();

	My_Sender.begin();

	// Initialize file system.
	boolean foundStatus = SPIFFS.begin();
	if (foundStatus) { // File system found
		// Try to get last status from status.txt
		if (!readStatus()) {
			foundStatus = false;
			sendDebug("Status file not found");
		}
	} else {
		sendDebug("Filesystem failure");
	}
	if (!foundStatus) // Could not get last status
	{
		sendDebug("Status file not found");
		/* Asume aircon off, timer off, power control enabled, */
		/* aircon mode fan, fan speed low, temperature set to 25 deg Celsius */
		acMode = acMode | AUTO_ON | TIM_OFF | AC_OFF | MODE_FAN | FAN_LOW | TUR_OFF | SWP_OFF | ION_OFF;
		acTemp = acTemp & TEMP_CLR; // set temperature bits to 0
		acTemp = acTemp + 25; // set temperature bits to 25
	}

	/* Send boot info debug message */
	sendStatusToDebug();

	/** Set saved AC temperature setting */
	savedAcTemp = acTemp & TEMP_MASK;

	// If device was not in auto power mode before reset set powerStatus to 0
	if ((acMode & AUTO_MASK) == AUTO_OFF) {
		powerStatus = 0;
		writeStatus();
	}
	
	// If device was in timer mode and AC was on => stop the timer mode and switch off the AC
	// TODO save the remaining 
	if ((acMode & TIM_MASK) == TIM_ON) {
		if ((acMode & AC_ON) == AC_ON) { // AC is on
			sendDebug("Stop AC and timer");
			irCmd = CMD_OTHER_TIMER;
			sendCmd();
			sendDebug("Timer & AC was on after reboot");
		} else {
			acMode = acMode & TIM_CLR;
			acMode = acMode & TIM_OFF;
		}
		writeStatus();
	}

	// Set initial time
	setTime(getNtpTime());
	// Try second time if it failed
	if (!gotTime) {
		setTime(getNtpTime());
	}

	// Initialize NTP client
	setSyncProvider(getNtpTime);
	setSyncInterval(43200); // Sync every 12 hours

	// If time is after 5pm (17) and before 8am we stop automatic function and switch off the aircon
	if (gotTime) {
		if (hour() > endOfDay || hour() < startOfDay) {
			dayTime = false;
		}else {
			dayTime = true;
		}
	}
	writeStatus();

	// Get first values from spMonitor
	// Only used in main control ESP on 192.168.0.142 address
	//getPowerVal(false);

	// Start update of consumption value every 60 seconds
	// Only used in main control ESP on 192.168.0.142 address
	// getPowerTimer.attach(60, triggerGetPower);

	// Start sending status update every 5 minutes (5x60=300 seconds)
	sendUpdateTimer.attach(300, triggerSendUpdate);

	// Send aircon restart message
	sendBroadCast();
	inSetup = false;
	
	// Start FTP server
	ftpSrv.begin(DEVICE_ID,DEVICE_ID);    //username, password for ftp.  set ports in ESP8266FtpServer.h  (default 21, 50009 for PASV)
		
	ArduinoOTA.onStart([]() {
		String debugMsg = "OTA start";
		sendDebug(debugMsg);
		Serial.println(debugMsg);
		// Safe actual status
		writeStatus();
		ledFlasher.attach(0.1, blueLedFlash); // Flash very fast if we started update
		getPowerTimer.detach();
		sendUpdateTimer.detach();
		timerEndTimer.detach();
		WiFiUDP::stopAll();
		WiFiClient::stopAll();
		server.close();
		otaUpdate = true;
	});

	// Start OTA server.
	ArduinoOTA.setHostname(DEVICE_ID);
	ArduinoOTA.begin();
	if (!debugWasOn) {
		debugOn = false; // During startup
	}
}

