void loop() {
	// Handle OTA updates
	ArduinoOTA.handle();

	if (otaUpdate) { // If the OTA update is active we do nothing else here in the main loop
		return;
	}
	
	// Handle new client request on HTTP server if available
	WiFiClient client = server.available();
	if (client) {
		replyClient(client);
		// Handle the received commands (if any)
		if (irCmd != 9999) { // Valid command received
			handleCmd();
		}
	}

	// Handle frequent status update
	if (sendUpdateTriggered) {
		#ifdef DEBUG_OUT 
		Serial.println("Send Update triggered");
		#endif
		sendUpdateTriggered = false;
		sendBroadCast();
	}

	// Handle end of timer
	if (timerEndTriggered) {
		#ifdef DEBUG_OUT 
		Serial.println("Timer reached 1 hour");
		#endif
		timerEndTimer.detach(); // Stop timer
		timerEndTriggered = false;
		String debugMsg;
		// If AC is on, switch it to FAN low speed and then switch it off
		if ((acMode & AC_ON) == AC_ON) { // AC is on
			// Set mode to FAN
			irCmd = CMD_MODE_FAN;
			sendCmd();
			delay(1000);
			// Set fan speed to LOW
			// Get last fan speed
			byte lastFanSpeed = acMode & FAN_MASK;
			// Set fan speed to LOW
			// If in high speed send command once to get into low speed mode
			if (lastFanSpeed == 2) { 
				irCmd = CMD_FAN_SPEED;
				sendCmd();
				delay(1000);
			}
			// If in medium speed send command once again to get into low speed mode
			if (lastFanSpeed >= 1) {
				irCmd = CMD_FAN_SPEED;
				sendCmd();
				delay(1000);
			}
			// Switch AC off
			irCmd = CMD_ON_OFF;
			sendCmd();
			debugMsg = "End of timer, switch off AC (" + String(hour()) + ":" + formatInt(minute()) + ")";
		} else {
			debugMsg = "End of timer, AC was already off (" + String(hour()) + ":" + formatInt(minute()) + ")";
		}
		acMode = acMode & TIM_CLR; // set timer bit to 0 (off)
		powerStatus = 0;
		sendDebug(debugMsg);
		#ifdef DEBUG_OUT 
		Serial.println(debugMsg);
		#endif
	}

	// Give a "I am alive" signal
	liveCnt++;
	if (liveCnt == 100000) {
		digitalWrite(ACT_LED, !digitalRead(ACT_LED));
		liveCnt = 0;
	}
	
	// If time is later than "endOfDay" or earlier than "startOfDay" we stop automatic function and switch off the aircon
	if (hour() > endOfDay || hour() < startOfDay) {
		if (dayTime) {
			if ((acMode & TIM_OFF) == TIM_OFF) { // If timer is active wait for the end of the timer
				// If AC is on, switch it to FAN low speed and then switch it off
				if ((acMode & AC_ON) == AC_ON) { // AC is on
					// Set mode to FAN
					irCmd = CMD_MODE_FAN;
					sendCmd();
					delay(1000);
					// Set fan speed to LOW
					byte currentSpeed = acMode & FAN_MASK;
					irCmd = CMD_FAN_SPEED;
					if (currentSpeed == 1 && fanSpeedUp) { // Fan speed is 1 and going up
						// send fan speed command 3 times to achieve fan speed 0
						sendCmd();
						delay(1000);
						sendCmd();
						delay(1000);
						sendCmd();
						delay(1000);
					} else if (currentSpeed == 1 && !fanSpeedUp) { // Fan speed is 1 and going down
						// send fan speed command once to achieve fan speed 0
						sendCmd();
						delay(1000);
					} else if (currentSpeed == 2) { // Fan speed is 2
						// send fan speed command twice to achieve fan speed 0
						sendCmd();
						delay(1000);
						sendCmd();
						delay(1000);
					}
					// Switch AC off
					irCmd = CMD_ON_OFF;
					sendCmd();
					delay(1000);
				}
				dayTime = false;
				powerStatus = 0;
				String debugMsg = "End of day, disable aircon auto mode (hour = " + String(hour()) + ")";
				sendDebug(debugMsg);
				#ifdef DEBUG_OUT 
				Serial.println(debugMsg);
				#endif
			}
		}
	}else {
		if (!dayTime) {
			dayTime = true;
			String debugMsg = "Start of day, enable aircon auto mode (hour = " + String(hour()) + ")";
			sendDebug(debugMsg);
			#ifdef DEBUG_OUT 
			Serial.println(debugMsg);
			#endif
		}
	}
	
	// Handle FTP access
	ftpSrv.handleFTP();
}

