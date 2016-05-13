
void setup() {
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

	Serial.println("");
	Serial.print("Connected to ");
	Serial.println(ssid);
	Serial.print("IP address: ");
	Serial.println(WiFi.localIP());

	// Start the web server to serve incoming requests
	server.begin();

	My_Sender.begin();

	// Initialize file system.
	boolean foundStatus = SPIFFS.begin();
	if (foundStatus) { // File system found
		// Try to get last status from status.txt
		if (!readStatus()) {
			foundStatus = false;
		}
	}
	if (!foundStatus) // Could not get last status
	{
		/* Asume aircon off, timer off, power control enabled, */
		/* aircon mode fan, fan speed low, temperature set to 25 deg Celsius */
		acMode = acMode | AUTO_ON | TIM_OFF | AC_OFF | MODE_FAN | FAN_LOW;
		acTemp = acTemp & TEMP_CLR; // set temperature bits to 0
		acTemp = acTemp + 25; // set temperature bits to 25
	}

	// If device was in auto power mode before reset set powerStatus to 0
	if ((acMode & AUTO_OFF) == AUTO_OFF) {
		powerStatus = 0;
		writeStatus();
	}
	
	// If device was in timer mode before reset reset the flag
	// As we do not know why the device restarted we do not know the status of the aircon
	if ((acMode & TIM_ON) == TIM_ON) {
		acMode = acMode & TIM_CLR;
		acMode = acMode | TIM_OFF;
	}

	// Set initial time
	setTime(getNtpTime());

	// Initialize NTP client
	setSyncProvider(getNtpTime);
	setSyncInterval(3600); // Sync every hour

	// If time is after 5pm (17) and before 8am we stop automatic function and switch off the aircon
	if (hour() > endOfDay || hour() < startOfDay) {
		dayTime = false;
	}else {
		dayTime = true;
	}

	// Send reboot log to debug
	String debugMsg = "Restart: Status " + String(powerStatus) + " C:" + String(consPower, 0) + " S:" + String(solarPower, 0);
	if (dayTime) {
		debugMsg += " ,daytime is true (hour = " + String(hour()) + ")";
	} else {
		debugMsg += " ,daytime is false (hour = " + String(hour()) + ")";
	}
	sendDebug(debugMsg);

	// Get first values from spMonitor
	getPowerVal(false);

	// Start update of consumption value every 60 seconds
	getPowerTimer.attach(60, triggerGetPower);

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
}

