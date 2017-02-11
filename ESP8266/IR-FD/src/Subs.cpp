#include <Setup.h>

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
	will initiate switching off the aircon after programmed hours
*/
void triggerTimerEnd() {
	timerCounter++;
	if (timerCounter >= onTime) {
		timerEndTriggered = true;
		timerCounter = 0;
	}
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
		if (debugOn) {
			sendDebug("Failed to open status.txt for writing", OTA_HOST);
		}
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

	if (debugOn) {
		String debugMsg = "writeStatus: acMode = " + String(acMode) + " acTemp = " + String(acTemp) + " powerStatus = " + String(powerStatus) + " onTime = " + String(onTime);
		sendDebug(debugMsg, OTA_HOST);
		debugMsg = "writeStatus: JSON = " + jsonTxt;
		sendDebug(debugMsg, OTA_HOST);
	}

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
	// open file for reading.
	File statusFile = SPIFFS.open("/status.txt", "r");
	if (!statusFile)
	{
		if (debugOn) {
			sendDebug("Failed to open status.txt.", OTA_HOST);
		}
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
		if (debugOn) {
			sendDebug("Failed to parse status json", OTA_HOST);
		}
		return false;
	}
	if (root.containsKey("acMode")) {
		acMode = root["acMode"];
	} else {
		if (debugOn) {
			sendDebug("Could not find acMode", OTA_HOST);
		}
		return false;
	}
	if (root.containsKey("acTemp")) {
		acTemp = root["acTemp"];
	} else {
		if (debugOn) {
			sendDebug("Could not find acTemp", OTA_HOST);
		}
		return false;
	}
	if (root.containsKey("powerStatus")) {
		powerStatus = root["powerStatus"];
	} else {
		if (debugOn) {
			sendDebug("Could not find powerStatus", OTA_HOST);
		}
		powerStatus = 0;
		return false;
	}
	if (root.containsKey("onTime")) {
		onTime = root["onTime"];
	} else {
		if (debugOn) {
			sendDebug("Could not find onTime", OTA_HOST);
		}
		onTime = 1;
		return false;
	}
	if (debugOn) {
		String debugMsg = "readStatus : acMode = " + String(acMode) + " acTemp = " + String(acTemp) + " powerStatus = " + String(powerStatus) + " onTime = " + String(onTime);
		sendDebug(debugMsg, OTA_HOST);
		String status;
		root.printTo(status);
		debugMsg = "readStatus : JSON = " + status;
		sendDebug(debugMsg, OTA_HOST);
	}
	return true;
}

/**
	parseSocketCmd
	Parse the received command
*/
void parseSocketCmd() {
	String statResponse;
	// The following commands are followed independant of AC on or off
	switch (irCmd) {
		case CMD_REMOTE_0: // Second aircon off
			if ((acMode & AUTO_ON) != AUTO_ON) { // Are we in auto mode?
				irCmd = 9999;
			} else {
				powerStatus = 0;
				writeStatus();
			}
			break;
		case CMD_REMOTE_1: // Second aircon fan mode
			if ((acMode & AUTO_ON) != AUTO_ON) { // Are we in auto mode?
				irCmd = 9999;
			} else {
				powerStatus = 1;
				writeStatus();
			}
			break;
		case CMD_REMOTE_2: // Second aircon cool mode
			if ((acMode & AUTO_ON) != AUTO_ON) { // Are we in auto mode?
				irCmd = 9999;
			} else {
				powerStatus = 2;
				writeStatus();
			}
			break;
		case CMD_AUTO_ON: // Command to (re)start auto control
			acMode = acMode & AUTO_CLR;
			acMode = acMode | AUTO_ON;
			writeStatus();
			break;
		case CMD_AUTO_OFF: // Command to stop auto control
			acMode = acMode & AUTO_CLR;
			acMode = acMode | AUTO_OFF;
			writeStatus();
			powerStatus = 0; // Independendant from current status it will be set to 0!
			irCmd = 9999; // No further actions necessary
			sendBroadCast(); // Send broadcast about the change
			break;
		case CMD_OTHER_TIMER: // Timer on/off
			break;
		case CMD_RESET: // Command to reset device
			writeStatus();
			break;
		default: // Handle other commands
			if ((acMode & AC_ON) == AC_ON) { // AC is on
				switch (irCmd) {
					case CMD_ON_OFF: // Switch aircon On/Off
						break;
					case CMD_MODE_AUTO: // Switch to Auto
						break;
					case CMD_MODE_COOL: // Switch to Cool
						break;
					case CMD_MODE_DRY: // Switch to Dry
						break;
					case CMD_MODE_FAN: // Switch to Fan
						break;
					case CMD_FAN_HIGH: // Switch to High Fan
						break;
					case CMD_FAN_MED: // Switch to Medium Fan
						break;
					case CMD_FAN_LOW: // Switch to Low Fan
						break;
					case CMD_FAN_SPEED: // Switch to next fan speed
						break;
					case CMD_TEMP_PLUS: // Temp +
						break;
					case CMD_TEMP_MINUS: // Temp -
						break;
					case CMD_OTHER_SWEEP: // Switch on/off sweep
						break;
					case CMD_OTHER_TURBO: // Switch on/off turbo
						if (((acMode & MODE_COOL) == MODE_COOL) || ((acMode & MODE_AUTO) == MODE_AUTO)) {
						} else {
							irCmd = 9999;
						}
						break;
					case CMD_OTHER_ION: // Switch on/off ion
						break;
					default:
						String wrongCmd = "fail - unknown command: " + String(irCmd);
						irCmd = 9999;
						break;
				}
			} else { // AC is off
				switch (irCmd) {
					case CMD_ON_OFF: // Switch aircon on
						// On manual ON request switch off auto mode
						acMode = acMode & AUTO_CLR;
						acMode = acMode | AUTO_OFF;
						writeStatus();
						break;
					default:
						irCmd = 9999;
						break;
				}
			}
			break;
	}
}

/**
	sendStatusToDebug
	collects last status as string and sends it as debug message
*/
void sendStatusToDebug() {
	String debugMsg = "";
	// Display status of aircon
	debugMsg = "Power ";
	if ((acMode & AC_MASK) == AC_ON) {
		debugMsg += "on";
	} else {
		debugMsg += "off";
	}
	debugMsg += ", Mode ";
	byte testMode = acMode & MODE_MASK;
	if (testMode == MODE_FAN) {
		debugMsg += "Fan";
	} else if (testMode == MODE_DRY) {
		debugMsg += "Dry";
	} else if (testMode == MODE_COOL) {
		debugMsg += "Cool";
	} else if (testMode == MODE_AUTO) {
		debugMsg += "Auto";
	}
	debugMsg += ", Speed ";
	testMode = acMode & FAN_MASK;
	if (testMode == FAN_LOW) {
		debugMsg += "low";
	} else if (testMode == FAN_MED) {
		debugMsg += "med";
	} else if (testMode == FAN_HIGH) {
		debugMsg += "high";
	}
	testMode = acTemp & TEMP_MASK;
	debugMsg += ", Temp " + String(testMode);

	// Display power consumption and production values
	/** Calculate average power consumption of the last 10 minutes */
	consPower = 0;
	for (int i = 0; i < 10; i++) {
		consPower += avgConsPower[i];
	}
	consPower = consPower / 10;
	debugMsg += ", Cons " + String(consPower);

	// Display power cycle status
	debugMsg += ", Status " + String(powerStatus);

	// Display status of auto control by power consumption
	debugMsg += ", AutoMode ";
	if ((acMode & AUTO_ON) == AUTO_ON) {
		debugMsg += "on";
	} else {
		debugMsg += "off";
	}

	// Display timer status of aircon
	debugMsg += ", Timer ";
	if ((acMode & TIM_ON) == TIM_ON) {
		debugMsg += "on";
	} else {
		debugMsg += "off";
	}

  // Display timer status of aircon
	debugMsg += ", Timer ends:" + timerEnd;

	sendDebug(debugMsg, OTA_HOST);
}
