#include <Setup.h>

/** Counter for "I am alive" red LED blinking in loop() */
long liveCnt = 0;

void loop() {
	// Handle OTA updates
	ArduinoOTA.handle();

	if (otaRunning) { // If the OTA update is active we do nothing else here in the main loop
		return;
	}

	// In case we don't have a time from NTP or local server, retry
	if (!gotTime) {
		sendDebug("tryGetTime", OTA_HOST);
		tryGetTime(false);
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
			acMode = acMode & MODE_CLR; // set mode bits to 0
			acMode = acMode | MODE_FAN; // set mode bits to FAN mode
			My_Sender.sendNEC(Fan,32);
			delay(1000);
			// Set fan speed to LOW
			acMode = acMode & FAN_CLR; // set fan bits to 0
			acMode = acMode | FAN_LOW; // set mode bits to FAN LOW mode
			My_Sender.sendNEC(L_Fan,32);
			delay(1000);
			// Switch AC off
			My_Sender.sendNEC(On_Off,32);
			if (debugOn) {
				String debugMsg = "End of timer, switch off AC (" + String(hour()) + ":" + getDigits(minute()) + ")";
				sendDebug(debugMsg, OTA_HOST);
			}
		} else {
			if (debugOn) {
				String debugMsg = "End of timer, AC was already off (" + String(hour()) + ":" + getDigits(minute()) + ")";
				sendDebug(debugMsg, OTA_HOST);
			}
		}
		acMode = acMode & TIM_CLR; // set timer bit to 0 (off)
		acMode = acMode & AC_CLR; // set power bit to 0
		acMode = acMode | AC_OFF; // set power bit to OFF
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
						acMode = acMode & MODE_CLR; // set mode bits to 0
						acMode = acMode | MODE_FAN; // set mode bits to FAN mode
						My_Sender.sendNEC(Fan,32);
						delay(1000);
						// Set fan speed to LOW
						acMode = acMode & FAN_CLR; // set fan bits to 0
						acMode = acMode | FAN_LOW; // set mode bits to FAN LOW mode
						My_Sender.sendNEC(L_Fan,32);
						delay(1000);
						// Switch AC off
						acMode = acMode & AC_CLR; // set power bit to 0
						acMode = acMode | AC_OFF; // set power bit to OFF
						My_Sender.sendNEC(On_Off,32);
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
				handleCmd();
				sendBroadCast();
			}
		}
	}

	// Handle FTP access
	ftpSrv.handleFTP();
}
