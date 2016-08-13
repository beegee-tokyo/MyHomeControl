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
	handleCmd
	Handles commands received over the webserver
*/
void handleCmd() {
	// Handle the different AC commands
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
			sendDebug("Request to reset the module");
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
		default: // All other commands
			//Serial.println("Send command triggered");
			sendCmd();
	}
	sendBroadCast(); // Inform about the change
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
				savedAcTemp = acTemp & TEMP_MASK;
				acTemp = acTemp & TEMP_CLR; // Dry mode resets AC temperature to 25
				acTemp += 25;
			}
			buildBuffer(&sendBuffer[0], &DRY[0]);
			break;
		case CMD_MODE_FAN: // Fan
			if ((acMode & AC_ON) == AC_ON) {
				acMode = acMode & MODE_CLR; // set mode bits to 0
				acMode = acMode | MODE_FAN; // set mode bits to FAN mode
				isValidCmd = true;
				acTemp = acTemp & TEMP_CLR;
				acTemp = acTemp + savedAcTemp;
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
						acTemp = acTemp & TEMP_CLR;
						acTemp += lastTemp;
						savedAcTemp = acTemp;
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
						acTemp = acTemp & TEMP_CLR;
						acTemp += lastTemp;
						savedAcTemp = acTemp;
						isValidCmd = true;
					}
				}
			}
			buildBuffer(&sendBuffer[0], &MINUS[0]);
			break;
		case CMD_OTHER_TIMER: // Timer
			if ((acMode & TIM_ON) == TIM_ON) { // TIMER is already on
				timerEndTimer.detach(); // Stop timer
				timerEndTriggered = false;
				acMode = acMode & TIM_CLR; // set timer bit to 0
				// Switch off the aircon
				if ((acMode & AC_ON) == AC_ON) { // // Switch off the aircon if still on
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
					String debugMsg = "Timer stopped manually, switch off AC (" + String(hour()) + ":" + formatInt(minute()) + ")";
					sendDebug(debugMsg);
					#ifdef DEBUG_OUT 
					Serial.println(debugMsg);
					#endif
				} else {
					String debugMsg = "Timer stopped manually, AC was already off (" + String(hour()) + ":" + formatInt(minute()) + ")";
					sendDebug(debugMsg);
					#ifdef DEBUG_OUT 
					Serial.println(debugMsg);
					#endif
				}
				powerStatus = 0;
			} else { // Timer is not yet on
				acMode = acMode & TIM_CLR; // set timer bit to 0
				acMode = acMode | TIM_ON; // set timer bit to 1 (on)
				// Switch on the aircon
				if (!((acMode & AC_ON) == AC_ON)) { // // Switch on the aircon if off
					// Switch AC on
					irCmd = CMD_ON_OFF;
					sendCmd();
					delay(1000);
				}
				// Set fan speed to LOW
				irCmd = CMD_FAN_LOW;
				sendCmd();
				delay(1000);
				// Set mode to cooling
				irCmd = CMD_MODE_COOL;
				sendCmd();
				powerStatus = 0;
				// Start timer to switch off the aircon after "onTime" (default = 1 hour = 60mx60s+1000ms=3600000 milliseconds)
				// Because of 32bit limit (== max 71min) the timer is triggered
				// every hour and a counter is used if timer time is 2 hours or more
				timerEndTimer.attach_ms(3600000, triggerTimerEnd);
				String debugMsg = "Start of timer, switch on AC (" + String(hour()) + ":" + formatInt(minute()) + ") for " + onTime + "hours";
				sendDebug(debugMsg);
				#ifdef DEBUG_OUT 
				Serial.println(debugMsg);
				#endif
			}
			break;
	}
	// Send the command
	if (isValidCmd) {
		writeStatus();
		sendCode(0, &sendBuffer[0], 67);
		if (irCmd == CMD_MODE_COOL) {
			restoreTempSetting();
		}
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
	if (!tcpClientOut.connect(ipSlave, httpPort)) {
		Serial.println("connection to slave AC " + String(ipSlave[0]) + "." + String(ipSlave[1]) + "." + String(ipSlave[2]) + "." + String(ipSlave[3]) + " failed");
		tcpClientOut.stop();
		digitalWrite(COM_LED, HIGH);
		return false;
	}

	switch (reqMode) {
		case CMD_REMOTE_0:
			tcpClientOut.print("GET /?c=" + String(CMD_REMOTE_0) + " HTTP/1.0\r\n\r\n");
			break;
		case CMD_REMOTE_1:
			tcpClientOut.print("GET /?c=" + String(CMD_REMOTE_1) + " HTTP/1.0\r\n\r\n");
			break;
		case CMD_REMOTE_2:
			tcpClientOut.print("GET /?c=" + String(CMD_REMOTE_2) + " HTTP/1.0\r\n\r\n");
			break;
	}

	String line = "";
	int waitTimeOut = 0;
	while (tcpClientOut.connected()) {
		line = tcpClientOut.readStringUntil('\r');
		delay(1);
		waitTimeOut++;
		if (waitTimeOut > 2000) { // If no more response for 2 seconds return
			tcpClientOut.stop();
			return false;
		}
	}
	tcpClientOut.stop();
	/** Buffer for JSON string */
	DynamicJsonBuffer jsonBuffer;
	/** JSON object with parsed JSON data */
	JsonObject& root = jsonBuffer.parseObject(line,2);
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
	/** String for debug message */
	String debugMsg = "";
#ifdef SIMULATION
	for (int i = 0; i < 3; i++) {
		consPower += avgConsPower[i];
	}
	consPower = consPower / 3;
	Serial.println("average consPower = " + String(consPower));
#else
	for (int i = 0; i < 10; i++) {
		consPower += avgConsPower[i];
	}
	consPower = consPower / 10;
#endif
	
	/** Calculate average solar production of the last 10 minutes */
	solarPower = 0;
	for (int i = 0; i < 10; i++) {
		solarPower += avgSolarPower[i];
	}

	/***************************************************/
	/* Below code is for use with one aircon */
	/***************************************************/
	// Check if the solar production of the last 10 minutes was 0 Watt
	if (solarPower == 0) { // No solar panel production
		if (powerStatus != 0) {
			if ((acMode & AC_ON) == AC_ON) {
				irCmd = CMD_MODE_FAN; // Switch aircon to FAN mode
				sendCmd();
				delay(1000);
				irCmd = CMD_ON_OFF; // Switch off aircon
				sendCmd();
				powerStatus = 0;
				mustBroadcast = true;
				mustWriteStatus = true;
				avgConsIndex = 0; // reset average calculation
				debugMsg = "Status was " + String(powerStatus) + ", Solar power == 0, switch aircon off, status to 0";
				sendDebug(debugMsg);
				#ifdef DEBUG_OUT 
				Serial.println(debugMsg);
				#endif
			} else {
				powerStatus = 0;
				mustBroadcast = true;
				mustWriteStatus = true;
				avgConsIndex = 0; // reset average calculation
				debugMsg = "Status was " + String(powerStatus) + ", Solar power == 0, aircon was already off, switch status to 0";
				sendDebug(debugMsg);
				#ifdef DEBUG_OUT 
				Serial.println(debugMsg);
				#endif
			}
		}
	} else {
		/** Check current status */
		switch (powerStatus) {
			case 0: // aircon is off
				// at -300 switch directly to cool mode
				// at -75 switch to fan mode
				if (consPower < -300) { // production exceeds 300W
					powerStatus = 3;
					debugMsg = "Status was 0, consumption < -300W, switch aircon to cool mode, status to 3";
				} else if (consPower < -75) { // production exceeds 75W
					powerStatus = 1;
					debugMsg = "Status was 0, consumption < -75W, switch aircon to fan mode, status to 1";
				}
				if (powerStatus != 0) {
					if ((acMode & AC_ON) != AC_ON) { // Aircon might be already on (manually)
						irCmd = CMD_ON_OFF; // Switch on aircon in FAN mode
						sendCmd();
						delay(1000);
					}
					if (powerStatus == 3) {
						irCmd = CMD_MODE_COOL; // Switch aircon to COOL mode
						restoreTempSetting();
					} else {
						irCmd = CMD_MODE_FAN; // Switch aircon to FAN mode
					}
					sendCmd(); 
					mustBroadcast = true;
					mustWriteStatus = true;
					avgConsIndex = 0; // reset average calculation
					sendDebug(debugMsg);
					#ifdef DEBUG_OUT 
					Serial.println(debugMsg);
					#endif
				}
				break;
			case 1: // aircon is in fan mode
				// at +200 switch off aircon
				// at -300 switch to cool mode
				if (consPower > 200) { // consuming more than 200W
					irCmd = CMD_ON_OFF; // Switch off aircon
					powerStatus = 0;
					debugMsg = "Status was 1, consumption > 200W, switch aircon off, status to 0";
				} else if (consPower < -300) { // production exceeds 300W
					irCmd = CMD_MODE_COOL; // Switch aircon to COOL mode
					restoreTempSetting();
					powerStatus = 3;
					debugMsg = "Status was 1, consumption < -300W, switch aircon to cool mode, status to 3";
				}
				if (powerStatus != 1) {
					sendCmd(); 
					mustBroadcast = true;
					mustWriteStatus = true;
					avgConsIndex = 0; // reset average calculation
					sendDebug(debugMsg);
					#ifdef DEBUG_OUT 
					Serial.println(debugMsg);
					#endif
				}
				break;
			case 2: // aircon is in dry mode
				// at +300 switch to fan mode
				// at -75 switch to cool mode
				if (consPower > 300) { // consuming more than 300W
					irCmd = CMD_MODE_FAN; // Switch aircon to FAN mode
					powerStatus = 1;
					debugMsg = "Status was 2, consumption > 300W, switch aircon to fan mode, status to 1";
				} else if (consPower < -75) { // over production more than 75W
					irCmd = CMD_MODE_COOL; // Switch aircon to COOL mode
					powerStatus = 3;
					debugMsg = "Status was 2, consumption < -75W, switch aircon to cool mode, status to 3";
				}
				if (powerStatus != 2) { // If changed send command to air con
					sendCmd();
					mustBroadcast = true;
					mustWriteStatus = true;
					avgConsIndex = 0; // reset average calculation
					sendDebug(debugMsg);
					#ifdef DEBUG_OUT 
					Serial.println(debugMsg);
					#endif
				}
				break;
			case 3: // aircon is in cool mode
				// at +400 switch to dry mode
				if (consPower > 400) { // consuming more than 400W
					irCmd = CMD_MODE_DRY; // Switch aircon to DRY mode
					sendCmd();
					savedAcTemp = acTemp & TEMP_MASK;
					acTemp = acTemp & TEMP_CLR; // Dry mode resets AC temperature to 25
					acTemp += 25;
					powerStatus = 2;
					mustBroadcast = true;
					mustWriteStatus = true;
					avgConsIndex = 0; // reset average calculation
					debugMsg = "Status was 3, consumption > 400W, switch aircon to dry mode, status to 2";
					sendDebug(debugMsg);
					#ifdef DEBUG_OUT 
					Serial.println(debugMsg);
					#endif
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
	String debugMsg;
	digitalWrite(COM_LED, LOW);
	ledFlasher.attach(0.1, blueLedFlash); // Flash very fast while we get data
	const int httpPort = 80;
	if (!tcpClientOut.connect(ipSPM, httpPort)) {
		Serial.println("connection to time server " + String(ipSPM[0]) + "." + String(ipSPM[1]) + "." + String(ipSPM[2]) + "." + String(ipSPM[3]) + " failed");
		tcpClientOut.stop();
		ledFlasher.detach();
		digitalWrite(COM_LED, HIGH);
		return;
	}

	tcpClientOut.print("GET /data/get HTTP/1.0\r\n\r\n");

	String line = "";
	int waitTimeOut = 0;
	while (tcpClientOut.connected()) {
		line = tcpClientOut.readStringUntil('\r');
		delay(1);
		waitTimeOut++;
		if (waitTimeOut > 2000) { // If no more response for 2 seconds return
			ledFlasher.detach();
			digitalWrite(COM_LED, HIGH);
			return;
		}
	}
	tcpClientOut.stop();
	/** Buffer for JSON string */
	DynamicJsonBuffer jsonBuffer;
	/** JSON object with parsed JSON data */
	JsonObject& root = jsonBuffer.parseObject(line,2);
	if (!root.success()) {
		Serial.println("parseObject() failed");
		ledFlasher.detach();
		digitalWrite(COM_LED, HIGH);
		return;
	}
	ledFlasher.detach();
	digitalWrite(COM_LED, HIGH);

	// Switch between status depending on consumption
	if (root.containsKey("value")) {
#ifdef SIMULATION
		consPower = simConsValues[simValueIndex];
		solarPower = 700.0;
		simValueIndex++;
		if (simValueIndex == 33) {
			simValueIndex = 0;
		}
		#ifdef DEBUG_OUT 
		Serial.println("consPower = " + String(consPower));
		#endif
		
		if (avgConsIndex < 3) {
			avgConsPower[avgConsIndex] = consPower;
			avgSolarPower[avgConsIndex] = solarPower;
			avgConsIndex++;
			if (avgConsIndex != 3) {
				debugMsg = "Status " + String(powerStatus) + ", still collecting data, counter = " + String(avgConsIndex);
				sendDebug(debugMsg);
				Serial.println(debugMsg);
			} else {
				checkPower();
				Serial.println("New power status = " + String(powerStatus));
			}
		} else {
			for (int i = 0; i < 2; i++) { // Shift values in array
				avgConsPower[i] = avgConsPower[i + 1];
				avgSolarPower[i] = avgSolarPower[i + 1];
			}
			avgConsPower[2] = consPower;
			avgSolarPower[2] = solarPower;

			checkPower();
			Serial.println("New power status = " + String(powerStatus));
		}
#else
		consPower = root["value"]["C"];
		solarPower = root["value"]["S"];
		if (avgConsIndex < 10) {
			avgConsPower[avgConsIndex] = consPower;
			avgSolarPower[avgConsIndex] = solarPower;
			avgConsIndex++;
			if (avgConsIndex != 10) {
				debugMsg = "Status " + String(powerStatus) + ", still collecting data, counter = " + String(avgConsIndex);
				sendDebug(debugMsg);
				#ifdef DEBUG_OUT 
				Serial.println(debugMsg);
				#endif
			} else {
				if (doPowerCheck && (acMode & AUTO_MASK) == AUTO_ON && dayTime && (acMode & TIM_MASK) == TIM_OFF) {
					checkPower();
					debugMsg = "Status " + String(powerStatus) + ", still collecting data, counter = " + String(avgConsIndex);
					sendDebug(debugMsg);
					#ifdef DEBUG_OUT 
					Serial.println(debugMsg);
					#endif
				}
			}
		} else {
			for (int i = 0; i < 9; i++) { // Shift values in array
				avgConsPower[i] = avgConsPower[i + 1];
				avgSolarPower[i] = avgSolarPower[i + 1];
			}
			avgConsPower[9] = consPower;
			avgSolarPower[9] = solarPower;

			// If deviation to old power is bigger than 50 Watt send a broadcast
			double powerChange = consPowerOld - consPower;
			if (abs(powerChange) > 100) {
				debugMsg = "Status " + String(powerStatus) + " C:" + String(consPower, 0) + " S:" + String(solarPower, 0);
				sendDebug(debugMsg);
				#ifdef DEBUG_OUT 
				Serial.println(debugMsg);
				#endif
				sendBroadCast();
				consPowerOld = consPower;
			}
			if (doPowerCheck && (acMode & AUTO_MASK) == AUTO_ON && dayTime && (acMode & TIM_MASK) == TIM_OFF) {
				debugMsg = "Do checkPower now";
				sendDebug(debugMsg);
				checkPower();
			}
		}
#endif
	}
}

/**
	Restore temperature setting after DRY mode
*/
void restoreTempSetting() {
	/** Debug message */
	String debugMsg;
	/** Command for temperature restore */
	int tempCmd;

#ifdef SIMULATION
	savedAcTemp = simLastTemp[simTempIndex];
	simTempIndex++;
	if (simTempIndex == 2) {
		simTempIndex = 0;
	}
#endif
	debugMsg = "Current temperature setting = " + String(acTemp & TEMP_MASK);
	sendDebug(debugMsg);
#ifdef SIMULATION
	Serial.println(debugMsg);
#endif
	debugMsg = "Saved temperature setting = " + String(savedAcTemp);
	sendDebug(debugMsg);
#ifdef SIMULATION
	Serial.println(debugMsg);
#endif
	if ((acTemp & TEMP_MASK) != savedAcTemp) { // Coming from DRY mode?
		byte tempChange;
		if ((acTemp & TEMP_MASK) > savedAcTemp) {
			tempChange =  (acTemp & TEMP_MASK) - savedAcTemp;
			tempCmd = CMD_TEMP_MINUS;
			debugMsg = "Temp restore with CMD_TEMP_MINUS - Difference = " + String(tempChange);
			sendDebug(debugMsg);
#ifdef SIMULATION
			Serial.println(debugMsg);
#endif
		} else {
			tempChange =  savedAcTemp - (acTemp & TEMP_MASK);
			tempCmd = CMD_TEMP_PLUS;
			debugMsg = "Temp restore with CMD_TEMP_PLUS - Difference = " + String(tempChange);
			sendDebug(debugMsg);
#ifdef SIMULATION
			Serial.println("Temp restore with CMD_TEMP_PLUS");
#endif
		}
		for (int i=0; i<tempChange; i++) { // Reset old temperature
			irCmd = tempCmd;
			sendCmd();
			delay(500);
#ifdef SIMULATION
			Serial.println("Temp restore by " + String(i+1) + "deg");
#endif
		}
#ifdef SIMULATION
		acTemp = acTemp & TEMP_CLR;
		acTemp = acTemp + 16;
#else
		acTemp = acTemp & TEMP_CLR;
		acTemp = acTemp + savedAcTemp;
#endif
	}
}