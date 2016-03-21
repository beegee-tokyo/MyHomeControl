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
		// Serial.println(getTime());
		sendBroadCast();
	}

	if (timeStatus() != timeNotSet) {
		if (now() != prevDisplay) { //update the display only if 	time has changed
			prevDisplay = now();
			digitalClockDisplay();	
		}
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
	
	// Handle FTP access
	ftpSrv.handleFTP();
}

