/**
	triggerGetPower
	called by Ticker getPowerTimer
	sets flag powerUpdateTriggered to true for handling in loop()
	will initiate a call to getPowerVal() from loop()
*/
void triggerGetPower() {
	powerUpdateTriggered = true;
}

/**
	triggerSendUpdate
	called by Ticker sendUpdateTimer
	sets flag sendUpdateTriggered to true for handling in loop()
	will initiate a call to sendBroadCast() from loop()
*/
void triggerSendUpdate() {
	sendUpdateTriggered = true;
}

/**
	triggerTimerEnd
	called by Ticker timerEndTimer
	sets flag timerEndTriggered to true for handling in loop()
	will initiate switching off the aircon after 1 hour
*/
void triggerTimerEnd() {
	timerEndTriggered = true;
}

/**
 * Change status of red led on each call
 * called by Ticker ledFlasher
 */
void redLedFlash() {
	int state = digitalRead(ACT_LED);
	digitalWrite(ACT_LED, !state);
}

/**
 * Change status of blue led on each call
 * called by Ticker ledFlasher
 */
void blueLedFlash() {
	int state = digitalRead(COM_LED);
	digitalWrite(COM_LED, !state);
}

/** 
 * Format int number as string with leading 0
 */
 String formatInt(int number) {
	if (number < 10) {
		return "0" + String(number);
	} else {
		return String(number);
	}
 }
 
/**
	writeStatus
	writes current status into status.txt
*/
bool writeStatus() {
	// Open config file for writing.
	File statusFile = SPIFFS.open("/status.txt", "w");
	if (!statusFile)
	{
		Serial.println("Failed to open status.txt for writing");
		return false;
	}
	// Create current status as JSON
	DynamicJsonBuffer jsonBuffer;

	// Prepare json object for the response
	JsonObject& root = jsonBuffer.createObject();

	root["acMode"] = acMode;
	root["acTemp"] = acTemp;
	root["powerStatus"] = powerStatus;
	root["onTime"] = onTime;
	
	String jsonTxt;
	root.printTo(jsonTxt);
	
	#ifdef DEBUG_OUT 
	Serial.println("writeStatus:");
	Serial.print("acMode = ");
	Serial.println(acMode);
	Serial.print("acTemp = ");
	Serial.println(acTemp);
	Serial.print("powerStatus = ");
	Serial.println(powerStatus);
	Serial.print("onTime = ");
	Serial.println(onTime);
	Serial.println(jsonTxt);
	#endif
	
	// Save status to file
	statusFile.println(jsonTxt);
	statusFile.close();
	return true;
}

/**
	readStatus
	reads current status from status.txt
	global variables are updated from the content
*/
bool readStatus() {
	#ifdef DEBUG_OUT 
	Serial.println("readStatus:");
	#endif
	// open file for reading.
	File statusFile = SPIFFS.open("/status.txt", "r");
	if (!statusFile)
	{
		Serial.println("Failed to open status.txt.");
		return false;
	}

	// Read content from config file.
	String content = statusFile.readString();
	statusFile.close();

	content.trim();

	// Create current status as from file as JSON
	DynamicJsonBuffer jsonBuffer;

	// Prepare json object for the response
	JsonObject& root = jsonBuffer.parseObject(content);

	// Parse JSON
	if (!root.success())
	{
		// Parsing fail
		#ifdef DEBUG_OUT 
		Serial.println("Failed to parse status json");
		#endif
		return false;
	}
	if (root.containsKey("acMode")) {
		acMode = root["acMode"];
	} else {
		#ifdef DEBUG_OUT 
		Serial.println("Could not find acMode");
		#endif
		return false;
	}
	if (root.containsKey("acTemp")) {
		acTemp = root["acTemp"];
	} else {
		#ifdef DEBUG_OUT 
		Serial.println("Could not find acTemp");
		#endif
		return false;
	}
	if (root.containsKey("powerStatus")) {
		powerStatus = root["powerStatus"];
	} else {
		#ifdef DEBUG_OUT 
		Serial.println("Could not find powerStatus");
		#endif
		powerStatus = 0;
		return false;
	}
	if (root.containsKey("onTime")) {
		onTime = root["onTime"];
	} else {
		#ifdef DEBUG_OUT 
		Serial.println("Could not find onTime");
		#endif
		onTime = 1;
		return false;
	}
	#ifdef DEBUG_OUT 
	Serial.print("acMode = ");
	Serial.println(acMode);
	Serial.print("acTemp = ");
	Serial.println(acTemp);
	Serial.print("powerStatus = ");
	Serial.println(powerStatus);
	Serial.print("onTime = ");
	Serial.println(onTime);
	String status;
	root.printTo(status);
	Serial.println(status);
	#endif
	
	return true;
}

/**
	parseCmd
	Parse the received command
*/
void parseCmd(JsonObject& root) {
	String statResponse;
	// The following commands are followed independant of AC on or off
	switch (irCmd) {
		case CMD_REMOTE_0: // Second aircon off
			if ((acMode & AUTO_ON) != AUTO_ON) { // Are we in auto mode?
				irCmd = 9999;
				root["result"] = "auto_off";
			} else {
				root["result"] = "success";
				root["cmd"] = CMD_REMOTE_0;
				powerStatus = 0;
				writeStatus();
			}
			break;
		case CMD_REMOTE_1: // Second aircon fan mode
			if ((acMode & AUTO_ON) != AUTO_ON) { // Are we in auto mode?
				irCmd = 9999;
				root["result"] = "auto_off";
			} else {
				root["result"] = "success";
				root["cmd"] = CMD_REMOTE_1;
				powerStatus = 1;
				writeStatus();
			}
			break;
		case CMD_REMOTE_2: // Second aircon cool mode
			if ((acMode & AUTO_ON) != AUTO_ON) { // Are we in auto mode?
				irCmd = 9999;
				root["result"] = "auto_off";
			} else {
				root["result"] = "success";
				root["cmd"] = CMD_REMOTE_2;
				powerStatus = 2;
				writeStatus();
			}
			break;
		case CMD_AUTO_ON: // Command to (re)start auto control
			acMode = acMode & AUTO_CLR;
			acMode = acMode | AUTO_ON;
			root["result"] = "success";
			root["cmd"] = CMD_AUTO_ON;
			writeStatus();
			break;
		case CMD_AUTO_OFF: // Command to stop auto control
			acMode = acMode & AUTO_CLR;
			acMode = acMode | AUTO_OFF;
			root["result"] = "success";
			root["cmd"] = CMD_AUTO_OFF;
			writeStatus();
			break;
		case CMD_OTHER_TIMER: // Timer on/off
			root["cmd"] = CMD_OTHER_TIMER;
			break;
		case CMD_RESET: // Command to reset device
			root["result"] = "success";
			root["cmd"] = CMD_RESET;
			writeStatus();
			break;
		default: // Handle other commands
			if ((acMode & AC_ON) == AC_ON) { // AC is on
				root["result"] = "success";
				switch (irCmd) {
					case CMD_ON_OFF: // Switch aircon On/Off
						root["cmd"] = CMD_ON_OFF;
						break;
					case CMD_MODE_AUTO: // Switch to Auto
						root["cmd"] = CMD_MODE_AUTO;
						break;
					case CMD_MODE_COOL: // Switch to Cool
						root["cmd"] = CMD_MODE_COOL;
						break;
					case CMD_MODE_DRY: // Switch to Dry
						root["cmd"] = CMD_MODE_DRY;
						break;
					case CMD_MODE_FAN: // Switch to Fan
						root["cmd"] = CMD_MODE_FAN;
						break;
					case CMD_FAN_HIGH: // Switch to High Fan
						root["cmd"] = CMD_FAN_HIGH;
						break;
					case CMD_FAN_MED: // Switch to Medium Fan
						root["cmd"] = CMD_FAN_MED;
						break;
					case CMD_FAN_LOW: // Switch to Low Fan
						root["cmd"] = CMD_FAN_LOW;
						break;
					case CMD_FAN_SPEED: // Switch to next fan speed
						root["cmd"] = CMD_FAN_SPEED;
						break;
					case CMD_TEMP_PLUS: // Temp +
						root["cmd"] = CMD_TEMP_PLUS;
						break;
					case CMD_TEMP_MINUS: // Temp -
						root["cmd"] = CMD_TEMP_MINUS;
						break;
					case CMD_OTHER_SWEEP: // Switch on/off sweep
						root["cmd"] = CMD_OTHER_SWEEP;
						break;
					case CMD_OTHER_TURBO: // Switch on/off turbo
						if (((acMode & MODE_COOL) == MODE_COOL) || ((acMode & MODE_AUTO) == MODE_AUTO)) {
							root["cmd"] = CMD_OTHER_TURBO;
						} else {
							root["result"] = "fail - AC not in cool mode";
							irCmd = 9999;
						}
						break;
					case CMD_OTHER_ION: // Switch on/off ion
						root["cmd"] = CMD_OTHER_ION;
						break;
					default:
						String wrongCmd = "fail - unknown command: " + String(irCmd);
						root["result"] = wrongCmd;
						irCmd = 9999;
						break;
				}
			} else { // AC is off
				root["result"] = "success";
				switch (irCmd) {
					case CMD_ON_OFF: // Switch aircon on
						root["cmd"] = CMD_ON_OFF;
						// On manual ON request switch off auto mode
						acMode = acMode & AUTO_CLR;
						acMode = acMode | AUTO_OFF;
						writeStatus();
						break;
					default:
						root["result"] = "fail - AC is off";
						irCmd = 9999;
						break;
				}
			}
			break;
	}
}

