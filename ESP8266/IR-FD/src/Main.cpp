#include <Setup.h>

/** Counter for "I am alive" red LED blinking in loop() */
long liveCnt = 0;

void loop() {
	// Handle OTA updates
	ArduinoOTA.handle();

	if (otaRunning) { // If the OTA update is active we do nothing else here in the main loop
		return;
	}

	// In case we don't have a time from NTP, retry
	if (!gotTime) {
		setTime(getNtpTime());
	}

	// Handle new request on tcp socket server if available
	WiFiClient tcpClient = tcpServer.available();
	if (tcpClient) {
		socketServer(tcpClient);
		// Handle the received commands (if any)
		if (irCmd != 9999) { // Valid command received
			handleCmd();
		}
		sendBroadCast();
	}

	// Handle update of consumption power
	if (powerUpdateTriggered) {
		if (debugOn) {
			sendDebug("Power Update triggered", OTA_HOST);
		}
		powerUpdateTriggered = false;
		getPowerVal(true);
	}

	// Handle frequent status update
	if (sendUpdateTriggered) {
		if (debugOn) {
			sendDebug("Send Update triggered", OTA_HOST);
		}
		sendUpdateTriggered = false;
		sendBroadCast();
	}

	// Handle end of timer
	if (timerEndTriggered) {
		if (debugOn) {
			sendDebug("Timer end reached", OTA_HOST);
		}
		timerEndTimer.detach(); // Stop timer
		timerEndTriggered = false;
		// If AC is on, switch it to FAN low speed and then switch it off
		if ((acMode & AC_ON) == AC_ON) { // AC is on
			// Set mode to FAN
			My_Sender.sendNEC(Fan,32);
			// irCmd = CMD_MODE_FAN;
			// sendCmd();
			delay(1000);
			// Set fan speed to LOW
			My_Sender.sendNEC(L_Fan,32);
			// irCmd = CMD_FAN_LOW;
			// sendCmd();
			delay(1000);
			// Switch AC off
			My_Sender.sendNEC(On_Off,32);
			// irCmd = CMD_ON_OFF;
			// sendCmd();
			if (debugOn) {
				String debugMsg = "End of timer, switch off AC (" + String(hour()) + ":" + formatInt(minute()) + ")";
				sendDebug(debugMsg, OTA_HOST);
			}
		} else {
			if (debugOn) {
				String debugMsg = "End of timer, AC was already off (" + String(hour()) + ":" + formatInt(minute()) + ")";
				sendDebug(debugMsg, OTA_HOST);
			}
		}
		acMode = acMode & TIM_CLR; // set timer bit to 0 (off)
		powerStatus = 0;
		writeStatus();
		sendBroadCast();
	}

	// Give a "I am alive" signal
	liveCnt++;
	if (liveCnt == 100000) {
		digitalWrite(actLED, !digitalRead(actLED));
		liveCnt = 0;
	}

	// If time is later than "endOfDay" or earlier than "startOfDay" we stop automatic function and switch off the aircon
	// Handle only if we have a time from NTP
	if (gotTime) {
		if (hour() > endOfDay || hour() < startOfDay) {
			if (dayTime) {
				if ((acMode & TIM_MASK) == TIM_OFF) { // If timer is not active switch off the aircon if still on
					// If AC is on, switch it to FAN low speed and then switch it off
					if ((acMode & AC_ON) == AC_ON) { // AC is on
						// Set mode to FAN
						My_Sender.sendNEC(Fan,32);
						// irCmd = CMD_MODE_FAN;
						// sendCmd();
						delay(1000);
						// Set fan speed to LOW
						My_Sender.sendNEC(L_Fan,32);
						// irCmd = CMD_FAN_LOW;
						// sendCmd();
						delay(1000);
						// Switch AC off
						My_Sender.sendNEC(On_Off,32);
						// irCmd = CMD_ON_OFF;
						// sendCmd();
						delay(1000);
					}
					dayTime = false;
					powerStatus = 0;
					writeStatus();
					if (debugOn) {
						String debugMsg = "End of day, disable aircon auto mode (hour = " + String(hour()) + ")";
						sendDebug(debugMsg, OTA_HOST);
					}
					sendBroadCast();
				}
			}
		}else {
			if (!dayTime) {
				dayTime = true;
				writeStatus();
				if (debugOn) {
					String debugMsg = "Start of day, enable aircon auto mode (hour = " + String(hour()) + ")";
					sendDebug(debugMsg, OTA_HOST);
				}
				irCmd = CMD_AUTO_ON;
				parseSocketCmd();
				// sendCmd();
				sendBroadCast();
			}
		}
	}

	// Handle FTP access
	ftpSrv.handleFTP();
}
