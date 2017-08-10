#include "Setup.h"

/**
 * Sets flag heartBeatTriggered to true for handling in loop()
 * called by Ticker heartBeatTimer
 * will initiate sending out a status message from loop()
 */
void triggerHeartBeat() {
	heartBeatTriggered = true;
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
 * Create status JSON object
 *
 * @param root
 * 		Json object to be filled with the status
 */
void createStatus(JsonObject& root, boolean makeShort) {
	// Create status
	// structure is:
	// {"de":ly1,"lo":0/1,"bo":0/1,
	//			"rs":-100...+100,"re":rebootReason,"bu":"Build version","dt":"currentDate","db":0/1}

	root["de"] = DEVICE_ID; //root["device"] = DEVICE_ID;
	if (digitalRead(relayPort) == HIGH) {
		root["lo"] = 1; //root["alarm"] = 1;
	} else {
		root["lo"] = 0; //root["alarm"] = 0;
	}
	if (!makeShort) {
		if (inSetup) {
			root["bo"] = 1; //root["boot"] = 1;
		} else {
			root["bo"] = 0; //root["boot"] = 0;
		}

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
	if (root.containsKey("lo")) {
		if (root["lo"] == 0) {
			digitalWrite(relayPort, LOW);
		} else {
			digitalWrite(relayPort, HIGH);
		}
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
