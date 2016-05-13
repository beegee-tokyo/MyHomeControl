/**
 * Change status of red led on each call
 * called by Ticker ledFlasher
 */
void redLedFlash() {
	int state = digitalRead(alarmLED);
	digitalWrite(alarmLED, !state);
}

/**
 * Change status of blue led on each call
 * called by Ticker comFlasher
 */
void blueLedFlash() {
	int state = digitalRead(comLED);
	digitalWrite(comLED, !state);
}

 /**
  * Sets flag weatherUpdateTriggered to true for handling in loop()
  * called by Ticker getWeatherTimer
  * will initiate a call to getLight() & getTemperature() from loop()
  */
void triggerGetWeather() {
	weatherUpdateTriggered = true;
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
 * Counts up until offDelay reaches onTime, then
 * switch off the relay
 * turn off the alarm sound
 * called by relayOffTimer
 */
void relayOff() {
	offDelay += 1;
	if (offDelay == onTime) {
		digitalWrite(relayPort, LOW);
		relayOffTimer.detach();
		alarmTimer.detach();
		analogWrite(speakerPin, 0);
		lightOffTriggered = true;
	}
}

/**
 * Create status JSON object
 *
 * @param root
 *              Json object to be filled with the status
 */
void createStatus(JsonObject& root, boolean makeShort) {
	// Create status
	// structure is:
	// {"device":DEVICE_ID,"alarm":0/1,"alarm_on":0/1,"light_on":0/1,"boot":0/1,"auto":0/1,"auto_on":1...24,"auto_off":1...24,
	//			"light_val":0..65536,"ldr_val":0..1024,"rssi":-100...+100,"reboot":rebootReason}
	// {"device":"sf1","alarm":1,"alarm_on":1,"light_on":0,"boot":1,"auto":0,"auto_on":22,"auto_off":8,"light_val":18652,"ldr_val":754,"rssi":-73,"reboot":"Lost connection"}
	root["device"] = DEVICE_ID;
	if (hasDetection) {
		root["alarm"] = 1;
	} else {
		root["alarm"] = 0;
	}
	if (alarmOn) {
		root["alarm_on"] = 1;
	} else {
		root["alarm_on"] = 0;
	}
	if (hasAutoActivation) {
		root["auto"] = 1;
	} else {
		root["auto"] = 0;
	}
	root["auto_on"] = autoActivOn;
	root["auto_off"] = autoActivOff;
	if (switchLights) {
		root["light_on"] = 1;
	} else {
		root["light_on"] = 0;
	}
	root["light_val"] = lightValue;
	root["temp"] = tempValue;
	root["humid"] = humidValue;
	root["heat"] = dht.computeHeatIndex(tempValue, humidValue, false);;
	
	if (!makeShort) {
		if (inSetup) {
			root["boot"] = 1;
		} else {
			root["boot"] = 0;
		}

		root["rssi"] = getRSSI();

		root["build"] = compileDate;

		root["reboot"] = lastRebootReason;
	}
}

/**
 * Write status to file
 *
 * @return <code>boolean</code>
 *              True if status was saved
 *              False if file error occured
 */
bool writeStatus() {
	// Open config file for writing.
	/** Pointer to file */
	File statusFile = SPIFFS.open("/status.txt", "w");
	if (!statusFile)
	{
		Serial.println("Failed to open status.txt for writing");
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
 *              Reboot reason as string
 * @return <code>boolean</code>
 *              True if reboot reason was saved
 *              False if file error occured
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
		Serial.println("Failed to open status.txt for writing");
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
 *              True if status could be read
 *              False if file error occured
 */
bool readStatus() {
	// open file for reading.
	/** Pointer to file */
	File statusFile = SPIFFS.open("/status.txt", "r");
	if (!statusFile)
	{
		Serial.println("Failed to open status.txt.");
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
	if (root.containsKey("alarm_on")) {
		if (root["alarm_on"] == 0) {
			alarmOn = false;
		} else {
			alarmOn = true;
		}
	} else {
		alarmOn = false;
	}
	if (root.containsKey("auto")) {
		if (root["auto"] == 0) {
			hasAutoActivation = false;
		} else {
			hasAutoActivation = true;
		}
	} else {
		hasAutoActivation = false;
	}
	if (root.containsKey("auto_on")) {
		autoActivOn = root["auto_on"];
	} else {
		autoActivOn = 22;
	}
	if (root.containsKey("auto_off")) {
		autoActivOff = root["auto_off"];
	} else {
		autoActivOff = 8;
	}
	return true;
}

/**
 * Plays the tune defined with melody[] endless until ticker is detached
 */
void playAlarmSound() {
	/** Current tone to be played */
	int toneLength = melody[melodyPoint];
	analogWriteFreq(toneLength / 2);
	analogWrite(speakerPin, toneLength / 4);

	melodyPoint ++;
	if (melodyPoint == melodyLenght) {
		melodyPoint = 0;
	}
}

/**
 * Interrupt routine called if status of PIR detection status changes
 */
void pirTrigger() {
	Serial.println("Interrupt from PIR pin");
	if (digitalRead(pirPort) == HIGH) { // Detection of movement
		pirTriggered = true;
		hasDetection = true;
	} else { // No detection
		pirTriggered = true;
		hasDetection = false;
	}
}
