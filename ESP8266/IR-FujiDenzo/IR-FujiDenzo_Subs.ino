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
 * Change status of red led on each call
 * called by Ticker ledFlasher
 */
void redLedFlash() {
	int state = digitalRead(ACT_LED);
	digitalWrite(ACT_LED, !state);
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
	
	String jsonTxt;
	root.printTo(jsonTxt);
	
	Serial.println("writeStatus:");
	Serial.print("acMode = ");
	Serial.println(acMode);
	Serial.print("acTemp = ");
	Serial.println(acTemp);
	Serial.print("powerStatus = ");
	Serial.println(powerStatus);
	root.printTo(Serial);
	Serial.println();
	
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
	Serial.println("readStatus:");
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
		Serial.println("Failed to parse status json");
		return false;
	}
	if (root.containsKey("acMode")) {
		acMode = root["acMode"];
	} else {
		Serial.println("Could not find acMode");
		return false;
	}
	if (root.containsKey("acTemp")) {
		acTemp = root["acTemp"];
	} else {
		Serial.println("Could not find acTemp");
		return false;
	}
	if (root.containsKey("powerStatus")) {
		powerStatus = root["powerStatus"];
	} else {
		Serial.println("Could not find powerStatus");
		return false;
	}
	Serial.print("acMode = ");
	Serial.println(acMode);
	Serial.print("acTemp = ");
	Serial.println(acTemp);
	Serial.print("powerStatus = ");
	Serial.println(powerStatus);
	root.printTo(Serial);
	Serial.println();
	
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
		case CMD_REMOTE_0: // Auto mode controlled off
			if ((acMode & AUTO_ON) != AUTO_ON) {
				irCmd = 9999;
				root["result"] = "auto_off";
			} else {
				root["result"] = "success";
				root["cmd"] = CMD_REMOTE_0;
				powerStatus = 0;
				writeStatus();
			}
			break;
		case CMD_REMOTE_1: // Auto mode controlled fan mode
			if ((acMode & AUTO_ON) != AUTO_ON) {
				irCmd = 9999;
				root["result"] = "auto_off";
			} else {
				root["result"] = "success";
				root["cmd"] = CMD_REMOTE_1;
				powerStatus = 1;
				writeStatus();
			}
			break;
		case CMD_REMOTE_2: // Auto mode controlled cool mode
			if ((acMode & AUTO_ON) != AUTO_ON) {
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
					case CMD_OTHER_TIMER: // Switch to Timer
						root["cmd"] = CMD_OTHER_TIMER;
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
						root["result"] = "fail - unknown command";
						irCmd = 9999;
						break;
				}
			} else { // AC is off
				root["result"] = "success";
				switch (irCmd) {
					case CMD_ON_OFF: // Switch aircon On/Off
						root["cmd"] = CMD_ON_OFF;
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

