#include <Setup.h>
#include <declarations.h>

void setup() {
	initLeds();
	pinMode(IR_LED_OUT, OUTPUT); // IR LED red
	digitalWrite(IR_LED_OUT, LOW); // Turn off IR LED

	Serial.begin(115200);

	Serial.setDebugOutput(false);
	Serial.println("");
	Serial.println("Hello from ESP8266 aircon control");
	Serial.print("SW build: ");
	Serial.println(compileDate);

	// Try to connect to WiFi with captive portal
	ipAddr = connectWiFi(ipAddr, ipGateWay, ipSubNet, "ESP8266 Aircon 2");

	sendDebug("Reboot", OTA_HOST);

	Serial.println("");
	Serial.print("Connected to ");
	Serial.println(ssid);
	Serial.print("IP address: ");
	Serial.println(WiFi.localIP());

	// Start the tcp socket server to listen on port tcpComPort
	tcpServer.begin();

	// Initialize IR led
	My_Sender.begin();

	// Initialize file system.
	boolean foundStatus = SPIFFS.begin();
	if (foundStatus) { // File system found
		// Try to get last status from status.txt
		if (!readStatus()) {
			foundStatus = false;
			sendDebug("Status file not found", OTA_HOST);
		}
	} else {
		sendDebug("Filesystem failure", OTA_HOST);
	}
	if (!foundStatus) // Could not get last status
	{
		sendDebug("Status file not found", OTA_HOST);
		/* Asume aircon off, timer off, power control enabled, */
		/* aircon mode fan, fan speed low, temperature set to 25 deg Celsius */
		acMode = acMode | AUTO_ON | TIM_OFF | AC_OFF | MODE_FAN | FAN_LOW | TUR_OFF | SWP_OFF | ION_OFF;
		acTemp = acTemp & TEMP_CLR; // set temperature bits to 0
		acTemp = acTemp + 25; // set temperature bits to 25
		savedAcTemp = 25; // Set saved AC temperature setting
	}

	if (acTemp == 0 & TEMP_MASK) acTemp += 16;
	if (savedAcTemp == 0) savedAcTemp += 16;
	writeStatus();

	// If device was not in auto power mode before reset set powerStatus to 0
	if ((acMode & AUTO_MASK) == AUTO_OFF) {
		powerStatus = 0;
		writeStatus();
	}

	// If device was in timer mode and AC was on => stop the timer mode and switch off the AC
	// TODO save the remaining
	if ((acMode & TIM_MASK) == TIM_ON) {
		if ((acMode & AC_ON) == AC_ON) { // AC is on
			sendDebug("Stop AC and timer", OTA_HOST);
			irCmd = CMD_OTHER_TIMER;
			sendCmd();
			sendDebug("Timer & AC was on after reboot", OTA_HOST);
		} else {
			acMode = acMode & TIM_CLR;
			acMode = acMode & TIM_OFF;
		}
		writeStatus();
	}

	// Get initial time
	getNtpTime();
	// Try second time if it failed
	if (!gotTime) {
		getNtpTime();
	}
	// Set initial time
	setTime(getNtpTime());

	// // Initialize NTP client
	// setSyncProvider(getNtpTime);
	// setSyncInterval(43200); // Sync every 12 hours

	// If time is after 5pm (17) and before 8am we stop automatic function and switch off the aircon
	if (gotTime) {
		if (hour() > endOfDay || hour() < startOfDay) {
			dayTime = false;
		}else {
			dayTime = true;
		}
	}
	writeStatus();

	// Start sending status update every 1 minute
	sendUpdateTimer.attach(60, triggerSendUpdate);

	// Start FTP server
	ftpSrv.begin(DEVICE_ID,DEVICE_ID);    //username, password for ftp.  set ports in ESP8266FtpServer.h  (default 21, 50009 for PASV)

	ArduinoOTA.onStart([]() {
		String debugMsg = "OTA start";
		sendDebug(debugMsg, OTA_HOST);
		Serial.println(debugMsg);
		// Safe actual status
		writeStatus();
		doubleLedFlashStart(0.1);
		getPowerTimer.detach();
		sendUpdateTimer.detach();
		timerEndTimer.detach();
		WiFiUDP::stopAll();
		WiFiClient::stopAll();
		tcpServer.close();
		otaRunning = true;
	});

	// Start OTA server.
	ArduinoOTA.setHostname(DEVICE_ID);
	ArduinoOTA.begin();

  /* Send boot info debug message */
  sendStatusToDebug();

  // Send aircon restart message
	sendBroadCast();
	inSetup = false;
}
