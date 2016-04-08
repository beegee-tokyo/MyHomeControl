/**
	sendCode
	Send IR command
*/
void sendCode(int repeat, unsigned int *rawCodes, int rawCount) {
	// Assume 38 KHz
	My_Sender.IRsend::sendRaw(rawCodes, rawCount, 38);
}

/**
	getVal
	Returns correct duration value depending on bit value
*/
unsigned int getVal(byte testVal, byte maskVal) {
	if ((testVal & maskVal) == maskVal) {
		return 1800;
	} else {
		return 700;
	}
}

/**
	buildBuffer
	Converts bit stream into IR command made of durations
*/
void buildBuffer(unsigned int *newBuffer, byte *cmd) {
	for (int i = 0; i < 4; i++) {
		newBuffer[(i * 16) + 3]	= getVal(cmd[i], B10000000);
		newBuffer[(i * 16) + 5]	= getVal(cmd[i], B01000000);
		newBuffer[(i * 16) + 7]	= getVal(cmd[i], B00100000);
		newBuffer[(i * 16) + 9]	= getVal(cmd[i], B00010000);
		newBuffer[(i * 16) + 11] = getVal(cmd[i], B00001000);
		newBuffer[(i * 16) + 13] = getVal(cmd[i], B00000100);
		newBuffer[(i * 16) + 15] = getVal(cmd[i], B00000010);
		newBuffer[(i * 16) + 17] = getVal(cmd[i], B00000001);
	}
}

/**
	sendCmd
	Prepares and sends IR command to aircon depending on
	requested command irCmd
*/
void sendCmd() {
	digitalWrite(ACT_LED, LOW); // Switch on activity led
	boolean isValidCmd = false;
	byte lastTemp;
	switch (irCmd) {
		case CMD_ON_OFF: // On-Off
			if ((acMode & AC_ON) == AC_ON) { // AC is on
				acMode = acMode & AC_CLR; // set power bit to 0
				acMode = acMode | AC_OFF;
				isValidCmd = true;
			} else {
				acMode = acMode & AC_CLR; // set power bit to 0
				acMode = acMode | AC_ON;
				isValidCmd = true;
			}
			buildBuffer(&sendBuffer[0], &POWER[0]);
			break;
		case CMD_MODE_COOL: // Cool
			if ((acMode & AC_ON) == AC_ON) {
				acMode = acMode & MODE_CLR; // set mode bits to 0
				acMode = acMode | MODE_COOL; // set mode bits to COOL mode
				isValidCmd = true;
			}
			buildBuffer(&sendBuffer[0], &COOL[0]);
			break;
		case CMD_MODE_DRY: // Dry
			if ((acMode & AC_ON) == AC_ON) {
				acMode = acMode & MODE_CLR; // set mode bits to 0
				acMode = acMode | MODE_DRY; // set mode bits to DRY mode
				isValidCmd = true;
			}
			buildBuffer(&sendBuffer[0], &DRY[0]);
			break;
		case CMD_MODE_FAN: // Fan
			if ((acMode & AC_ON) == AC_ON) {
				acMode = acMode & MODE_CLR; // set mode bits to 0
				acMode = acMode | MODE_FAN; // set mode bits to FAN mode
				isValidCmd = true;
			}
			buildBuffer(&sendBuffer[0], &FAN[0]);
			break;
		case CMD_FAN_HIGH: // H-Fan
			if ((acMode & AC_ON) == AC_ON) {
				acMode = acMode & FAN_CLR; // set fan bits to 0
				acMode = acMode | FAN_HIGH; // set mode bits to FAN HIGH mode
				isValidCmd = true;
			}
			buildBuffer(&sendBuffer[0], &H_FAN[0]);
			break;
		case CMD_FAN_MED: // M-Fan
			if ((acMode & AC_ON) == AC_ON) {
				acMode = acMode & FAN_CLR; // set fan bits to 0
				acMode = acMode | FAN_MED; // set mode bits to FAN MEDIUM mode
				isValidCmd = true;
			}
			buildBuffer(&sendBuffer[0], &M_FAN[0]);
			break;
		case CMD_FAN_LOW: // L-Fan
			if ((acMode & AC_ON) == AC_ON) {
				acMode = acMode & FAN_CLR; // set fan bits to 0
				acMode = acMode | FAN_LOW; // set mode bits to FAN LOW mode
				isValidCmd = true;
			}
			buildBuffer(&sendBuffer[0], &L_FAN[0]);
			break;
		case CMD_TEMP_PLUS: // Temp +
			if ((acMode & AC_ON) == AC_ON) {
				if ((acMode & MODE_COOL) == MODE_COOL) {
					lastTemp = acTemp & TEMP_MASK;
					if (lastTemp < MAX_TEMP) {
						lastTemp++;
						acTemp = lastTemp;
						isValidCmd = true;
					}
				}
			}
			buildBuffer(&sendBuffer[0], &PLUS[0]);
			break;
		case CMD_TEMP_MINUS: // Temp -
			if ((acMode & AC_ON) == AC_ON) {
				if ((acMode & MODE_COOL) == MODE_COOL) {
					lastTemp = acTemp & TEMP_MASK;
					if (lastTemp > MIN_TEMP) {
						lastTemp--;
						acTemp = lastTemp;
						isValidCmd = true;
					}
				}
			}
			buildBuffer(&sendBuffer[0], &MINUS[0]);
			break;
		case CMD_OTHER_TIMER: // Timer
			if ((acMode & AC_ON) == AC_ON) {
				if ((acMode & TIM_ON) == TIM_ON) { // TIMER is on
					acMode = acMode & TIM_CLR; // set timer bit to 0
					acMode = acMode | TIM_OFF;
					isValidCmd = true;
				} else {
					acMode = acMode & TIM_CLR; // set power bit to 0
					acMode = acMode | TIM_ON;
					isValidCmd = true;
				}
			}
			buildBuffer(&sendBuffer[0], &TIMER[0]);
			break;
	}
	// Send the command
	if (isValidCmd) {
		writeStatus();
		sendCode(0, &sendBuffer[0], 67);
	}

	// Reset irCmd
	irCmd = 9999;
	digitalWrite(ACT_LED, HIGH); // Switch off activity led
}

/**
	initAC
	Initialize aircon to fan mode, low fan speed, temperature 25 deg C
	Call this only once after power on and make sure aircon is off when
	ESP8266 is powered off!
	This will fail if aircon is already on and there is no (easy) way
	to detect if the aircon is already on
*/
void initAC() {
	digitalWrite(ACT_LED, LOW); // Switch on activity led

	/* FIRST: switch on AC */
	acMode = acMode & AC_CLR; // set power bit to 0
	acMode = acMode | AC_ON;
	buildBuffer(&sendBuffer[0], &POWER[0]);
	sendCode(0, &sendBuffer[0], 67);
	delay(2000); // Wait 2 seconds to make sure the aircon is on

	/* SECOND: switch to COOL mode */
	acMode = acMode & MODE_CLR; // set mode bits to 0
	acMode = acMode | MODE_COOL; // set mode bits to COOL mode
	buildBuffer(&sendBuffer[0], &COOL[0]);
	sendCode(0, &sendBuffer[0], 67);
	delay(1000); // Wait 1 second to make sure the aircon mode is switched

	/* THIRD: switch to LOW FAN speed */
	acMode = acMode & FAN_CLR; // set fan bits to 0
	acMode = acMode | FAN_LOW; // set mode bits to FAN LOW mode
	buildBuffer(&sendBuffer[0], &L_FAN[0]);
	sendCode(0, &sendBuffer[0], 67);
	delay(1000); // Wait 1 second to make sure the aircon mode is switched

	/* FORTH: set temperature to 25 deg Celsius */
	acTemp = acTemp & TEMP_CLR; // set temperature bits to 0
	acTemp = acTemp + 25; // set temperature bits to 25
	/* We do not know the temperature setting of the aircon
			therefor we first raise 17 times (to set 32 degrees)
			then we lower 7 times to get back to 25 degrees
	*/
	for (int i = 0; i < 16; i++) {
		buildBuffer(&sendBuffer[0], &PLUS[0]);
		sendCode(0, &sendBuffer[0], 67);
		delay(1000); // Wait 1 second to make sure the aircon mode is switched
	}
	for (int i = 0; i < 7; i++) {
		buildBuffer(&sendBuffer[0], &MINUS[0]);
		sendCode(0, &sendBuffer[0], 67);
		delay(1000); // Wait 1 second to make sure the aircon mode is switched
	}

	/* FIFTH: switch to FAN mode */
	acMode = acMode & MODE_CLR; // set mode bits to 0
	acMode = acMode | MODE_FAN; // set mode bits to FAN mode
	buildBuffer(&sendBuffer[0], &FAN[0]);
	sendCode(0, &sendBuffer[0], 67);
	delay(1000); // Wait 1 second to make sure the aircon mode is switched

	/* SIXTH: switch AC off */
	acMode = acMode & AC_CLR; // set status bits to 0
	acMode = acMode | AC_OFF; // set status to aircon off
	buildBuffer(&sendBuffer[0], &POWER[0]);
	sendCode(0, &sendBuffer[0], 67);
	writeStatus();
	digitalWrite(ACT_LED, HIGH); // Switch off activity led
}

/**
	switchSlaveAC
	Puts slave AC into requested mode
*/
boolean switchSlaveAC(IPAddress ipSlave, byte reqMode) {
	digitalWrite(COM_LED, LOW);
	const int httpPort = 80;
	if (!tcpClient.connect(ipSlave, httpPort)) {
		Serial.println("connection to slave AC " + String(ipSlave[0]) + "." + String(ipSlave[1]) + "." + String(ipSlave[2]) + "." + String(ipSlave[3]) + " failed");
		tcpClient.stop();
		digitalWrite(COM_LED, HIGH);
		return false;
	}

	switch (reqMode) {
		case CMD_REMOTE_0:
			tcpClient.print("GET /?c=" + String(CMD_REMOTE_0) + " HTTP/1.0\r\n\r\n");
			break;
		case CMD_REMOTE_1:
			tcpClient.print("GET /?c=" + String(CMD_REMOTE_1) + " HTTP/1.0\r\n\r\n");
			break;
		case CMD_REMOTE_2:
			tcpClient.print("GET /?c=" + String(CMD_REMOTE_2) + " HTTP/1.0\r\n\r\n");
			break;
	}

	String line = "";
	int waitTimeOut = 0;
	while (tcpClient.connected()) {
		line = tcpClient.readStringUntil('\r');
		delay(1);
		waitTimeOut++;
		if (waitTimeOut > 2000) { // If no more response for 2 seconds return
			tcpClient.stop();
			return false;
		}
	}
	tcpClient.stop();
	/** Buffer for JSON string */
	DynamicJsonBuffer jsonBuffer;
	char json[line.length()];
	line.toCharArray(json, line.length() + 1);
	JsonObject& root = jsonBuffer.parseObject(json);
	if (!root.success()) {
		Serial.println("parseObject() failed");
		digitalWrite(COM_LED, HIGH);
		return false;
	}

	if (root["result"] == "success") {
		digitalWrite(COM_LED, HIGH);
		return true;
	}

	digitalWrite(COM_LED, HIGH);
	return false;
}

/**
	checkPower
	Uses the power production of the solar panel to switch on/to/off
	aircon in fan or cool mode
*/
void checkPower() {
	/** Flag if we need to send update of status */
	boolean mustBroadcast = false;
	/** Flag if we need to save a new status */
	boolean mustWriteStatus = false;
	/** Calculate average power consumption of the last 10 minutes */
	consPower = 0;
	for (int i = 0; i < 10; i++) {
		consPower += avgConsPower[i];
	}
	consPower = consPower / 10;
	/** Calculate average solar production of the last 10 minutes */
	solarPower = 0;
	for (int i = 0; i < 10; i++) {
		solarPower += avgSolarPower[i];
	}

	// If deviation to old power is bigger than 50 Watt send a broadcast
	double powerChange = consPowerOld - consPower;
	if (abs(powerChange) > 50) {
		// Set flag to report the change
		mustBroadcast = true;
		// Save new 'old' power value
		consPowerOld = consPower;
	}
	
	/***************************************************/
	/* Below code is for future use with second aircon */
	/***************************************************/
	// if (solarPower == 0) { // No solar panel production
		// if (powerStatus != 0) {
			// irCmd = CMD_ON_OFF; // Switch off aircon
			// sendCmd();
			// // Switch off slave aircon
			// switchSlaveAC(ipSlave1,CMD_REMOTE_0);
			// powerStatus = 0;
			// mustBroadcast = true;
		// }
	// } else {
		// /** Check current status */
		// switch (powerStatus) {
			// case 0: // aircon is off, no overproduction
				// // aircon should be off, but maybe user switched it on manually
				// if ((acMode & AC_ON) != AC_ON) {
					// if (consPower < -100) { // over production exceeds 100W
						// Serial.print("Over production > 100W : ");
						// Serial.print(consPower, 2);
						// Serial.println("Switching on AC in FAN mode");
						// irCmd = CMD_ON_OFF; // Switch on aircon in FAN mode
						// sendCmd();
						// powerStatus = 1;
						// mustBroadcast = true;
					// }
				// }
				// break;
			// case 1: // aircon is in fan mode, over production was > 100W
				// if (consPower < -100) { // over production exceeds 200W
					// Serial.print("Over production > 200W : ");
					// Serial.print(consPower, 2);
					// Serial.println("Switching on slave AC in FAN mode");
					// // Switch slave aircon to FAN mode
					// if (switchSlaveAC(ipSlave1,CMD_REMOTE_1)) {
						// powerStatus = 2;
						// mustBroadcast = true;
					// } else {
					// }
				// }
				// if (consPower > 200) { // consuming more than 200W
					// Serial.print("Consumption > 200W : ");
					// Serial.print(consPower, 2);
					// Serial.println("Switching off AC");
					// irCmd = CMD_ON_OFF; // Switch off aircon
					// sendCmd();
					// powerStatus = 0;
					// mustBroadcast = true;
				// }
				// break;
			// case 2: // both aircons are in fan mode, over production was > 200W
				// if (consPower < -400) { // over production exceeds 600W
					// Serial.print("Over production > 600W : ");
					// Serial.print(consPower, 2);
					// Serial.println("Switching on AC in COOL mode");
					// irCmd = CMD_MODE_COOL; // Switch aircon to COOL mode
					// sendCmd();
					// powerStatus = 3;
					// mustBroadcast = true;
				// }
				// if (consPower > 300) { // consuming more than 300W
					// Serial.print("Consumption > 300W : ");
					// Serial.print(consPower, 2);
					// Serial.println("Switching off slave AC");
					// // Switch off slave aircon
					// switchSlaveAC(ipSlave1,CMD_REMOTE_0);
					// powerStatus = 1;
					// mustBroadcast = true;
				// }
				// break;
			// case 3: // aircon is in cool mode, over production was > 600W
				// if (consPower < -400) { // over production exceeds 1000W
					// // Switch on second aircon in COOL mode
					// Serial.print("Over production > 1000W : ");
					// Serial.print(consPower, 2);
					// Serial.println("Switching on second AC in COOL mode");
					// // Switch slave aircon to COOL mode
					// switchSlaveAC(ipSlave1,CMD_REMOTE_2);
					// powerStatus = 4;
					// mustBroadcast = true;
				// }
				// if (consPower > 400) { // consuming more than 400W
					// Serial.print("Consumption > 400W : ");
					// Serial.print(consPower, 2);
					// Serial.println("Switching back to FAN mode");
					// irCmd = CMD_MODE_FAN; // Switch aircon to FAN mode
					// sendCmd();
					// powerStatus = 1;
					// mustBroadcast = true;
				// }
				// break;
			// case 4: // both aircons are in COOL mode, over production was > 1400W
				// if (consPower > 400) { // consuming more than 400W
					// Serial.print("Consumption > 400W : ");
					// Serial.print(consPower, 2);
					// Serial.println("Switching slave AC back to FAN mode");
					// // Switch slave aircon to COOL mode
					// switchSlaveAC(ipSlave1,CMD_REMOTE_1);
					// powerStatus = 3;
					// mustBroadcast = true;
				// }
				// break;
		// }
	// }
	/***************************************************/
	/* Below code is for use with one aircon */
	/***************************************************/
	// Check if the solar production of the last 10 minutes was 0 Watt
	if (solarPower == 0) { // No solar panel production
		if (powerStatus != 0) {
			irCmd = CMD_MODE_FAN; // Switch aircon to FAN mode
			sendCmd();
			irCmd = CMD_ON_OFF; // Switch off aircon
			sendCmd();
			powerStatus = 0;
			mustBroadcast = true;
			mustWriteStatus = true;
			avgConsIndex = 0; // reset average calculation
			String debugMsg = "Status was " + String(powerStatus) + ", Solar power == 0, switch aircon off, status to 0";
			sendDebug(debugMsg);
		}
	} else {
		/** Check current status */
		switch (powerStatus) {
			case 0: // aircon is off, no overproduction
				// aircon should be off, but maybe user switched it on manually
				if ((acMode & AC_ON) != AC_ON) {
					if (consPower < -75) { // over production exceeds 75W
						//Serial.print("Over production > 75W : ");
						//Serial.print(consPower, 2);
						//Serial.println("Switching on AC in FAN mode");
						irCmd = CMD_ON_OFF; // Switch on aircon in FAN mode
						sendCmd();
						irCmd = CMD_MODE_FAN; // Switch aircon to FAN mode
						sendCmd();
						powerStatus = 1;
						mustBroadcast = true;
						mustWriteStatus = true;
						avgConsIndex = 0; // reset average calculation
						String debugMsg = "Status was 0, consumption < -75W, switch aircon to fan mode, status to 1";
						sendDebug(debugMsg);
					}
				}
				break;
			case 1: // aircon is in fan mode, over production was > 75W
				if (consPower < -300) { // over production exceeds 375W
					//Serial.print("Over production > 375W : ");
					//Serial.print(consPower, 2);
					//Serial.println("Switching on AC in COOL mode");
					irCmd = CMD_MODE_COOL; // Switch aircon to COOL mode
					sendCmd();
					powerStatus = 2;
					mustBroadcast = true;
					mustWriteStatus = true;
					avgConsIndex = 0; // reset average calculation
					String debugMsg = "Status was 1, consumption < -300W, switch aircon to cool mode, status to 2";
					sendDebug(debugMsg);
				}
				if (consPower > 200) { // consuming more than 200W
					//Serial.print("Consumption > 200W : ");
					//Serial.print(consPower, 2);
					//Serial.println("Switching off AC");
					irCmd = CMD_ON_OFF; // Switch off aircon
					sendCmd();
					powerStatus = 0;
					mustBroadcast = true;
					mustWriteStatus = true;
					avgConsIndex = 0; // reset average calculation
					String debugMsg = "Status was 1, consumption > 200W, switch aircon off, status to 0";
					sendDebug(debugMsg);
				}
				break;
			case 2: // aircon is in cool mode, over production was > 375W
				if (consPower > 400) { // consuming more than 400W
					//Serial.print("Consumption > 400W : ");
					//Serial.print(consPower, 2);
					//Serial.println("Switching back to FAN mode");
					irCmd = CMD_MODE_FAN; // Switch aircon to FAN mode
					sendCmd();
					powerStatus = 1;
					mustBroadcast = true;
					mustWriteStatus = true;
					avgConsIndex = 0; // reset average calculation
					String debugMsg = "Status was 2, consumption > 400W, switch aircon to fan mode, status to 1";
					sendDebug(debugMsg);
				}
				break;
		}
	}
	if (mustWriteStatus) {
		// Save status change
		writeStatus();
	}
	if (mustBroadcast) {
		// Broadcast status change
		sendBroadCast();
	}
}

/**
	getPowerVal
	Get current power consumption from spMonitor device on address ipSPM
*/
void getPowerVal(boolean doPowerCheck) {
	digitalWrite(COM_LED, LOW);
	const int httpPort = 80;
	if (!tcpClient.connect(ipSPM, httpPort)) {
		Serial.println("connection to time server " + String(ipSPM[0]) + "." + String(ipSPM[1]) + "." + String(ipSPM[2]) + "." + String(ipSPM[3]) + " failed");
		tcpClient.stop();
		digitalWrite(COM_LED, HIGH);
		return;
	}

	tcpClient.print("GET /data/get HTTP/1.0\r\n\r\n");

	String line = "";
	int waitTimeOut = 0;
	while (tcpClient.connected()) {
		line = tcpClient.readStringUntil('\r');
		delay(1);
		waitTimeOut++;
		if (waitTimeOut > 2000) { // If no more response for 2 seconds return
			digitalWrite(COM_LED, HIGH);
			return;
		}
	}
	tcpClient.stop();
	/** Buffer for JSON string */
	DynamicJsonBuffer jsonBuffer;
	char json[line.length()];
	line.toCharArray(json, line.length() + 1);
	JsonObject& root = jsonBuffer.parseObject(json);
	if (!root.success()) {
		Serial.println("parseObject() failed");
		digitalWrite(COM_LED, HIGH);
		return;
	}

	// Switch between status depending on consumption
	if (root.containsKey("value")) {
		consPower = root["value"]["C"];
		solarPower = root["value"]["S"];
		if (avgConsIndex < 10) {
			avgConsPower[avgConsIndex] = consPower;
			avgSolarPower[avgConsIndex] = solarPower;
			avgConsIndex++;
			String debugMsg = "Status " + String(powerStatus) + ", still collecting data, counter = " + String(avgConsIndex);
			sendDebug(debugMsg);
		} else {
			for (int i = 0; i < 9; i++) { // Shift values in array
				avgConsPower[i] = avgConsPower[i + 1];
				avgSolarPower[i] = avgSolarPower[i + 1];
			}
			avgConsPower[9] = consPower;
			avgSolarPower[9] = solarPower;

			String debugMsg = "Status " + String(powerStatus) + " C:" + String(consPower, 0) + " S:" + String(solarPower, 0);
			sendDebug(debugMsg);

			if (doPowerCheck && (acMode & AUTO_ON) == AUTO_ON && dayTime) {
				checkPower();
			}
		}
	}
	digitalWrite(COM_LED, HIGH);
}

