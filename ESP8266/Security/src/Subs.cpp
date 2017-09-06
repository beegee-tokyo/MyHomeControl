#include "Setup.h"

/**
 * Sets flag lightLDRTriggered to true for handling in loop()
 * called by Ticker updateLDRTimer
 * will initiate a call to getLDR() from loop()
 */
void triggerGetLDR() {
	lightLDRTriggered = true;
}

/**
 * Sets flag heartBeatTriggered to true for handling in loop()
 * called by Ticker heartBeatTimer
 * will initiate sending out a status message from loop()
 */
void triggerHeartBeat() {
	heartBeatTriggered = true;
}

/**
 * Reads analog input where LDR is connected
 * sets flag switchLights if value is lower than 850
 *
 * @return <code>boolean</code>
 *		true if status changed
 *		false if status is the same
 */
boolean getLDR() {
	/** Flag for light status change */
	boolean hasChanged = false;
	 // Check light only if relay is off and light is switched off
	if (digitalRead(relayPort) == LOW) {
		ldrValue = (analogRead(A0));
		if (ldrValue < 500) { // was 850
			if (switchLights == false) { // On change send status
				hasChanged = true;
			}
			switchLights = true;
		} else {
			if (switchLights == true) { // On change send status
				hasChanged = true;
			}
			switchLights = false;
		}
	}
	return hasChanged;
}

/**
 * Is called by relayOffTimer when onTime is reached
 */
void relayOff() {
	digitalWrite(relayPort, LOW);
	lightOffTriggered = true;
	relayOffTimer.detach();
}

/**
 * Interrupt routine called if status of PIR detection status changes
 */
void pirTrigger() {
	if (digitalRead(pirPort) == HIGH) { // Detection of movement
		pirTriggered = true;
		hasDetection = true;
		if (alarmOn) {
			digitalWrite(speakerPin,LOW); // Switch Piezo buzzer on
		}
		actLedFlashStart(0.2);
	} else { // No detection
		pirTriggered = true;
		hasDetection = false;
		actLedFlashStop();
		digitalWrite(speakerPin,HIGH); // Switch Piezo buzzer off
		if (alarmOn) { // If alarm is active, continue to flash slowly
			actLedFlashStart(1);
		}
	}
}

/**
 * Create status JSON object
 *
 * @param root
 * 		Json object to be filled with the status
 */
void createStatus(JsonObject& root, boolean makeShort) {
	// Create status
	// structure is:
	// {"de":DEVICE_ID,"al":0/1,"ao":0/1,"lo":0/1,"bo":0/1,"au":0/1,"an":1...24,"af":1...24,
	//			"rs":-100...+100,"re":"rebootReason","te":-20...+40,"hu":0...100,"hi":-20...+40,"bu":"Build version"}

	root["de"] = DEVICE_ID; //root["device"] = DEVICE_ID;
	if (hasDetection) {
		root["al"] = 1; //root["alarm"] = 1;
	} else {
		root["al"] = 0; //root["alarm"] = 0;
	}
	if (alarmOn) {
		root["ao"] = 1; //root["alarm_on"] = 1;
	} else {
		root["ao"] = 0; //root["alarm_on"] = 0;
	}
	if (hasAutoActivation) {
		root["au"] = 1; //root["auto"] = 1;
	} else {
		root["au"] = 0; //root["auto"] = 0;
	}
	root["an"] = autoActivOn; //root["auto_on"] = autoActivOn;
	root["af"] = autoActivOff; //root["auto_off"] = autoActivOff;
	if (switchLights) {
		root["lo"] = 1; //root["light_on"] = 1;
	} else {
		root["lo"] = 0; //root["light_on"] = 0;
	}

	if (!makeShort) {
		if (inSetup) {
			root["bo"] = 1; //root["boot"] = 1;
		} else {
			root["bo"] = 0; //root["boot"] = 0;
		}

		root["ld"] = ldrValue; //root["ldr_val"] = ldrValue;

		root["rs"] = getRSSI(); //root["rssi"] = getRSSI();

		root["bu"] = compileDate; //root["build"] = compileDate;

		root["re"] = lastRebootReason; //root["reboot"] = lastRebootReason;

		root["dt"] = digitalClockDisplay();

		if (debugOn) {
			root["db"] = 1;
		} else {
			root["db"] = 0;
		}
	}
}

/**
 * Write status to file
 *
 * @return <code>boolean</code>
 *			True if status was saved
 *			False if file error occured
 */
bool writeStatus() {
	// Open config file for writing.
	/** Pointer to file */
	File statusFile = SPIFFS.open("/status.txt", "w");
	if (!statusFile)
	{
		if (debugOn) {
			sendDebug("Failed to open status.txt for writing", OTA_HOST);
		}
		return false;
	}
	// Create current status as JSON
	/** Buffer for Json object */
	DynamicJsonBuffer jsonBuffer;

	// Prepare json object for the response
	/* Json object with the status */
	JsonObject& root = jsonBuffer.createObject();

	// Create status
	createStatus(root, false);

	/** String in Json format with the status */
	String jsonTxt;
	root.printTo(jsonTxt);

	// Save status to file
	statusFile.println(jsonTxt);
	statusFile.close();
	return true;
}

/**
 * Write reboot reason to file
 *
 * @param message
 *			Reboot reason as string
 * @return <code>boolean</code>
 *			True if reboot reason was saved
 *			False if file error occured
 */
bool writeRebootReason(String message) {
	// Write current status to file
	writeStatus();
	// Now append reboot reason to file
	// Open config file for writing.
	/** Pointer to file */
	File statusFile = SPIFFS.open("/status.txt", "a");
	if (!statusFile)
	{
		if (debugOn) {
			sendDebug("Failed to open status.txt for writing", OTA_HOST);
		}
		return false;
	}
	// Save reboot reason to file
	statusFile.println(message);
	statusFile.close();
	return true;
}

/**
 * Reads current status from status.txt
 * global variables are updated from the content
 *
 * @return <code>boolean</code>
 *			True if status could be read
 *			False if file error occured
 */
bool readStatus() {
	// open file for reading.
	/** Pointer to file */
	File statusFile = SPIFFS.open("/status.txt", "r");
	if (!statusFile)
	{
		if (debugOn) {
			sendDebug("Failed to open status.txt", OTA_HOST);
		}
		return false;
	}

	// Read content from config file.
	/** String with the status from the file */
	String content = statusFile.readString();
	statusFile.close();

	content.trim();

	// Check if there is a second line available.
	/** Index to end of first line in the string */
	uint8_t pos = content.indexOf("\r\n");
	/** Index of start of secnd line */
	uint8_t le = 2;
	// check for linux and mac line ending.
	if (pos == -1)
	{
		le = 1;
		pos = content.indexOf("\n");
		if (pos == -1)
		{
			pos = content.indexOf("\r");
		}
	}

	// If there is no second line: Reboot reason is missing.
	if (pos != -1)
	{
		rebootReason = content.substring(pos + le);
	} else {
		rebootReason = "Not saved";
	}

	// Create current status as from file as JSON
	/** Buffer for Json object */
	DynamicJsonBuffer jsonBuffer;

	// Prepare json object for the response
	/** String with content of first line of file */
	String jsonString = content.substring(0, pos);
	/** Json object with the last saved status */
	JsonObject& root = jsonBuffer.parseObject(jsonString);

	// Parse JSON
	if (!root.success())
	{
		// Parsing fail
		return false;
	}
	if (root.containsKey("ao")) { //if (root.containsKey("alarm_on")) {
		if (root["ao"] == 0) { //if (root["alarm_on"] == 0) {
			alarmOn = false;
		} else {
			alarmOn = true;
		}
	} else {
		alarmOn = false;
	}
	if (root.containsKey("au")) { //if (root.containsKey("auto")) {
		if (root["au"] == 0) { //if (root["auto"] == 0) {
			hasAutoActivation = false;
		} else {
			hasAutoActivation = true;
		}
	} else {
		hasAutoActivation = false;
	}
	if (root.containsKey("an")) { //if (root.containsKey("auto_on")) {
		autoActivOn = root["an"]; //autoActivOn = root["auto_on"];
	} else {
		autoActivOn = 22;
	}
	if (root.containsKey("af")) { //if (root.containsKey("auto_off")) {
		autoActivOff = root["af"]; //autoActivOff = root["auto_off"];
	} else {
		autoActivOff = 8;
	}
	if (root.containsKey("db")) {
		if (root["db"] == 0) {
			debugOn = false;
		} else {
			debugOn = true;
		}
	} else {
		debugOn = false;
	}
	return true;
}
