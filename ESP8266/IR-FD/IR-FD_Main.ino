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
	}

	// Handle the different AC commands
	if (irCmd != 9999) { // Valid command received
		switch (irCmd) {
			case CMD_REMOTE_0: // Should only be received in slave AC
				if ((acMode & AC_ON) == AC_ON) { // AC is on
					irCmd = CMD_MODE_FAN;
					sendCmd();
					delay(1000);
					irCmd = CMD_ON_OFF;
					sendCmd();
				}
				irCmd = 9999;
				break;
			case CMD_REMOTE_1: // Should only be received in slave AC
				if ((acMode & AC_ON) != AC_ON) {
					irCmd = CMD_ON_OFF;
					sendCmd();
					delay(1000);
				}
				irCmd = CMD_MODE_FAN;
				sendCmd();
				irCmd = 9999;
				break;
			case CMD_REMOTE_2: // Should only be received in slave AC
				if ((acMode & AC_ON) != AC_ON) {
					irCmd = CMD_ON_OFF;
					sendCmd();
					delay(1000);
				}
				irCmd = CMD_MODE_AUTO;
				sendCmd();
				irCmd = 9999;
				break;
			case CMD_INIT_AC: // Initialize aircon
				initAC();
				irCmd = 9999;
				break;
			case CMD_RESET: // Reboot the ESP module
				pinMode(16, OUTPUT); // Connected to RST pin
				digitalWrite(16,LOW); // Initiate reset
				ESP.reset(); // In case it didn't work
				break;
			case CMD_AUTO_ON: // Command to (re)start auto control
				// If AC is on, switch it to FAN low speed and then switch it off
				if ((acMode & AC_ON) == AC_ON) { // AC is on
					// Set mode to FAN
					irCmd = CMD_MODE_FAN;
					sendCmd();
					delay(1000);
					// Set fan speed to LOW
					irCmd = CMD_FAN_LOW;
					sendCmd();
					delay(1000);
					// Switch AC off
					irCmd = CMD_ON_OFF;
					sendCmd();
				}
				irCmd = 9999;
				break;
			case CMD_AUTO_OFF: // Command to stop auto control
				powerStatus = 0; // Independendant from current status it will be set to 0!
				irCmd = 9999;
				break;
			default: // All other commands
				//Serial.println("Send command triggered");
				sendCmd();
				irCmd = 9999;
		}
	}

	// Handle update of consumption power
	if (powerUpdateTriggered) {
		#ifdef DEBUG_OUT 
		Serial.println("Power Update triggered");
		#endif
		powerUpdateTriggered = false;
		getPowerVal(true);
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
			irCmd = CMD_FAN_LOW;
			sendCmd();
			delay(1000);
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
					irCmd = CMD_FAN_LOW;
					sendCmd();
					delay(1000);
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

