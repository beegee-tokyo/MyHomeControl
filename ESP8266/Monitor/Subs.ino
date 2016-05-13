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
 * Sets flag statusUpdated to true for handling in loop()
 * called by Ticker updateLightTimer
 * will initiate an update of data and send to MQTT from loop()
 */
void triggerGetStatus() {
	statusUpdated = true;
}

/**
 * Sets flag dhtUpdated to true for handling in loop()
 * called by Ticker getDHTTimer
 * will initiate a call to getTemperature() from loop()
 */
void triggerGetDHT() {
	dhtUpdated = true;
}

void updateScreen(boolean all) {
	/** String for some constructed text */
	String outText = "";
	// Draw background boxes 
	ucg.setColor(255, 255, 0);
	if (all) {
		ucg.drawBox(0, 0, 128, 22); // Solar production
	} else {
		ucg.drawBox(30, 0, 94, 22); // Solar production
	}
	if (consPower < 0.0) {
		ucg.setColor(0, 255, 0);
	} else {
		ucg.setColor(255, 0, 0);
	}
	ucg.drawBox(0, 22, 128, 22); // House consumption

	ucg.setColor(0, 0, 0);
	if (all) {
		ucg.drawBox(0, 44, 128, 21); // Weather text
	}

	ucg.setColor(102, 255, 255);
	if (all) {
		ucg.drawBox(0, 86, 128, 21); // AC Status
	} else {
		ucg.drawBox(32, 86, 96, 21); // AC Status
	}
	
	ucg.setColor(255, 128, 0);
	if (all) {
		ucg.drawBox(0, 107, 128, 21); // Security Status
	} else {
		ucg.drawBox(32, 107, 96, 21); // Security Status
	}
	
	// Print fixed text for solar values, aircon and security
	ucg.setFont(ucg_font_helvB18_tf);
	ucg.setColor(0, 0, 0);
	if (all) {
		ucg.setPrintPos(0,20);
		ucg.print("S");
		ucg.setFont(ucg_font_helvB10_tf);
		ucg.setPrintPos(0,103);
		ucg.print("AC");
		ucg.setPrintPos(0,124);
		ucg.print("SEC");
	}
	
	// Print solar values
	ucg.setFont(ucg_font_helvB18_tf);
//	outText = String(solarPower,0) + "W";
	ucg_print_center(String(solarPower,0) + "W", 15, 20);
	ucg.setPrintPos(0,42);
	ucg.print("C");
//	outText = String(abs(consPower),0) + "W";
	ucg_print_center(String(abs(consPower),0) + "W", 15, 42);
	
	// Print fixed text for weather values
	ucg.setFont(ucg_font_helvB10_tf);
	if (all) {
		ucg.setColor(255, 255, 255);
		ucg.setPrintPos(0,61);
		outText = "1C";
		outText.setCharAt(0, 176);
		ucg.print(outText);
		ucg.setPrintPos(45,61);
		ucg.print("%h");
		ucg.setPrintPos(90,61);
		ucg.print("lux");
		ucg.setColor(0, 0, 0);
	}
	
	// Print weather values 
	updateWeather();
	
	// Print AC status
	ucg.setFont(ucg_font_helvB10_tf);
	ucg_print_center(ac1Display, 20, 103);
	
	// Print Security status
	ucg_print_center(securityDisplay, 20, 124);
}

void updateWeather() {
	// Print weather values 
	ucg.setColor(224, 224, 209);
	ucg.drawBox(0, 65, 128, 21); // Clear weather values

	ucg.setColor(0, 0, 0);
	ucg.setFont(ucg_font_helvB10_tf);
	ucg.setPrintPos(2,80);
	ucg.print(String((tempValue/sensorReadings),1));
	ucg.setPrintPos(45,80);
	ucg.print(String((humidValue/sensorReadings),1));
	ucg_print_center(String(lightValue/sensorReadings), 70, 80);
}

void showMQTTerrorScreen() {
	ucg.clearScreen();
	ucg.setFont(ucg_font_helvB18_tf);
	ucg.setRotate180();
	ucg.setColor(255, 128, 0);
	ucg.drawBox(0, 0, 128, 128);
	ucg.setColor(0, 0, 0);
	ucg_print_center("OTA ERROR", 0, 34);
	ucg_print_center("disconnected", 0, 59);
	ucg_print_center("from", 0, 84);
	ucg_print_center(mqttBroker, 0, 109);
}

void ucg_print_ln(String text, boolean center) {
	int xPos=0;
	if (center) {
		char textChar[sizeof(text)];
		text.toCharArray(textChar, sizeof(textChar));
		xPos = (64 - (ucg.getStrWidth(textChar)/2));
	}
	ucg.setPrintPos(xPos,currLine);
	ucg.print(text);
	currLine = currLine + ucg.getFontAscent() - ucg.getFontDescent();
	currLine = currLine + 1;
	if (currLine > 128) {
		// TODO find out how to scroll the whole display
	}
}

void ucg_print_center(String text, int xPos, int yPos) {
	char textChar[sizeof(text)];
	text.toCharArray(textChar, sizeof(textChar));
	int textCenter = ucg.getStrWidth(textChar)/2;
	ucg.setPrintPos(64 + (xPos/2) - textCenter,yPos);
	ucg.print(text);
}

/**
 * Get latest data & status from
 * the home devices
 */
void getHomeInfo(boolean all) {
	getLight();
	getTemperature();
	makeWeather();
	getSPMStatus();
	getACStatus();
	getSECStatus();
	updateScreen(all);
	// Reset weather values for next reading cycle
	sensorReadings = 0;
	tempValue = 0;
	humidValue = 0;
	heatIndex = 0;
	lightValue = 0;
}

/**
 * Build weather info JSON object
 */
 void makeWeather() {
 	/** Buffer for outgoing JSON string */
	DynamicJsonBuffer jsonOutBuffer;
	/** Json object for outgoing data */
	JsonObject& jsonOut = jsonOutBuffer.createObject();

	heatIndex = dht.computeHeatIndex(tempValue/sensorReadings, humidValue/sensorReadings, false);
	
	jsonOut["de"] = "wea";
	jsonOut["li"] = lightValue/sensorReadings;
	jsonOut["te"] = tempValue/sensorReadings; 
	jsonOut["hu"] = humidValue/sensorReadings;
	jsonOut["hi"] = heatIndex;
	weatherStatus = "";
	jsonOut.printTo(weatherStatus);
}

void getTemperature() {
	// Reading temperature for humidity takes about 250 milliseconds!
	// Sensor readings may also be up to 2 seconds 'old' (it's a very slow sensor)
	float newHumidValue = dht.readHumidity();          // Read humidity (percent)
	float newTempValue = dht.readTemperature(false);     // Read temperature as Celsius
	// Check if any reads failed and exit early (to try again).
	if (isnan(newHumidValue) || isnan(newTempValue)) {
		Serial.println("Failed to read from DHT sensor!");
		Serial.print("Temperature value: ");
		Serial.println(newTempValue);
		Serial.print("Humidity value: ");
		Serial.println(newHumidValue);
		return;
	}
	humidValue += newHumidValue;
	tempValue += newTempValue;
	sensorReadings++;
}

/**
 * Encrypt/Decrypt a string with my secret key
 */
char* cryptMessage(String message) {
	char result[130];
	char test[130];
	byte encryptIndex = 0;
	int index = 0;

	for (int i=0; i<message.length(); i++) {
		result[i] = message.charAt(i) ^ encryptBase.charAt(encryptIndex);
		test[i] = result[i] ^ encryptBase.charAt(encryptIndex);
		Serial.print(result[i],DEC);
		Serial.print(".");
		encryptIndex++;
		index++;
		if (encryptIndex == encryptBase.length()) {
			encryptIndex = 0;
		}
	}
	Serial.println();
	result[index] = 0;
	test[index] = 0;
	// Serial.println("-- Org --");
	// Serial.println(message);
	Serial.println("-- Enc --");
	// Serial.println(result);
	Serial.println(message.length());
	// Serial.println(index);
	// Serial.println("-- Dec --");
	// Serial.println(test);
	Serial.println("--------");
	return result;
}
