void loop() {
	// Handle OTA updates
	ArduinoOTA.handle();

	// Handle new client request on HTTP server if available
	WiFiClient client = server.available();
	if (client) {
		replyClient(client);
	}

	// Handle serial communication
	while (Serial.available()) {
		int inChar = Serial.read();
		if (isDigit(inChar)) {
			// convert the incoming byte to a char
			// and add it to the string:
			inString += (char)inChar;
		}
		// if you get a newline, print the string,
		// then the string's value:
		if (inChar == '\n') {
			irCmd = inString.toInt();
			if (irCmd > 99) {
				Serial.println("Invalid command");
				irCmd = 9999;
			}
			// clear the string for new input:
			Serial.println("inString after receiving \n " + inString);
			inString = "";
			Serial.println("irCmd after receiving \n " + String(irCmd));
			replySerial();
		}
	}

	// Handle frequent status update
	if (sendUpdateTriggered) {
		//Serial.println("Send Update triggered");
		sendUpdateTriggered = false;
		sendBroadCast();
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
					// Get last fan speed
					byte lastFanSpeed = acMode & FAN_MASK;
					// Set fan speed to LOW
					// If in high speed send command once to get into low speed mode
					if (lastFanSpeed == 2) { 
						irCmd = CMD_FAN_SPEED;
						sendCmd();
					}
					// If in medium speed send command once again to get into low speed mode
					if (lastFanSpeed >= 1) {
						irCmd = CMD_FAN_SPEED;
						sendCmd();
					}
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

	// Give a "I am alive" signal
	liveCnt++;
	if (liveCnt == 100000) {
		digitalWrite(ACT_LED, !digitalRead(ACT_LED));
		liveCnt = 0;
	}
	
	// If time is later than "endOfDay" or earlier than "startOfDay" we stop automatic function and switch off the aircon
	if (hour() > endOfDay || hour() < startOfDay) {
		if (dayTime) {
			// If AC is on, switch it to FAN low speed and then switch it off
			if ((acMode & AC_ON) == AC_ON && dayTime) { // AC is on
				// Set mode to FAN
				irCmd = CMD_MODE_FAN;
				sendCmd();
				// Set fan speed to LOW
				byte currentSpeed = acMode & FAN_MASK;
				irCmd = CMD_FAN_SPEED;
				if (currentSpeed == 1 && fanSpeedUp) { // Fan speed is 1 and going up
					// send fan speed command 3 times to achieve fan speed 0
					sendCmd();
					sendCmd();
					sendCmd();
				} else if (currentSpeed == 1 && !fanSpeedUp) { // Fan speed is 1 and going down
					// send fan speed command once to achieve fan speed 0
					sendCmd();
				} else if (currentSpeed == 2) { // Fan speed is 2
					// send fan speed command twice to achieve fan speed 0
					sendCmd();
					sendCmd();
				}
				// Switch AC off
				irCmd = CMD_ON_OFF;
				sendCmd();
			}
			Serial.print("Switching off the auto mode at ");
			Serial.print(hour());
			Serial.println("h");
			dayTime = false;
			powerStatus = 0;
			String debugMsg = "End of day, disable aircon auto mode (hour = " + String(hour()) + ")";
			sendDebug(debugMsg);
		}
	}else {
		if (!dayTime) {
			Serial.print("Switching on the auto mode at ");
			Serial.print(hour());
			Serial.println("h");
			dayTime = true;
			String debugMsg = "Start of day, enable aircon auto mode (hour = " + String(hour()) + ")";
			sendDebug(debugMsg);
		}
	}
	
	// Catch a bug with power status = 0 but aircon is on when in auto mode
	// If AC is on and Auto mode is on but powerStatus is 0, switch it to FAN low speed and then switch it off
	if ((acMode & AC_ON) == AC_ON && (acMode & AUTO_ON) == AUTO_ON && powerStatus == 0) {
		// Set mode to FAN
		irCmd = CMD_MODE_FAN;
		sendCmd();
		byte currentSpeed = acMode & FAN_MASK;
		irCmd = CMD_FAN_SPEED;
		if (currentSpeed == 1 && fanSpeedUp) { // Fan speed is 1 and going up
			// send fan speed command 3 times to achieve fan speed 0
			sendCmd();
			sendCmd();
			sendCmd();
		} else if (currentSpeed == 1 && !fanSpeedUp) { // Fan speed is 1 and going down
			// send fan speed command once to achieve fan speed 0
			sendCmd();
		} else if (currentSpeed == 2) { // Fan speed is 2
			// send fan speed command twice to achieve fan speed 0
			sendCmd();
			sendCmd();
		}
		// Switch AC off
		irCmd = CMD_ON_OFF;
		sendCmd();
	}
	
	// Handle FTP access
	ftpSrv.handleFTP();
}

