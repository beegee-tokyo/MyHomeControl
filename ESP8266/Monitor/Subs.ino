/**
	redLedFlash
	Change status of red led on each call
	called by Ticker ledFlasher
*/
void redLedFlash() {
	int state = digitalRead(ACT_LED);
	digitalWrite(ACT_LED, !state);
}

/**
	blueLedFlash
	Change status of blue led on each call
	called by Ticker ledFlasher
*/
void blueLedFlash() {
	int state = digitalRead(COM_LED);
	digitalWrite(COM_LED, !state);
}

/**
	triggerGetStatus
	Sets flag statusUpdated to true for handling in loop()
	called by Ticker updateLightTimer
	will initiate an update of data and send to MQTT from loop()
*/
void triggerGetStatus() {
	statusUpdated = true;
}

/**
	triggerGetDHT
	Sets flag dhtUpdated to true for handling in loop()
	called by Ticker getDHTTimer
*/
void triggerGetDHT() {
	dhtUpdated = true;
}

/**
	updateSolar
	Update solar values
*/
void updateSolar(boolean all) {
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

	// Print fixed text for solar values
	ucg.setFont(ucg_font_helvB18_tf);
	ucg.setColor(0, 0, 0);
	if (all) {
		ucg.setPrintPos(0,20);
		ucg.print("S");
	}

	// Print solar values
	ucg.setFont(ucg_font_helvB18_tf);
	ucg_print_center(String(solarPower,0) + "W", 15, 20);
	ucg.setPrintPos(0,42);
	ucg.print("C");
	ucg_print_center(String(abs(consPower),0) + "W", 15, 42);
}

/**
	updateWeather
	Update weather values
*/
void updateWeather(boolean all) {
	/** String for some constructed text */
	String outText = "";
	// Draw background boxes 
	ucg.setColor(0, 0, 0);
	if (all) {
		ucg.drawBox(0, 44, 128, 21); // Weather text
	}

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
	ucg.setColor(102, 255, 255);
	ucg.drawBox(0, 65, 128, 21); // Clear weather values

	ucg.setColor(0, 0, 0);
	ucg.setFont(ucg_font_helvB10_tf);
	ucg.setPrintPos(2,80);
	ucg.print(String((tempInside/sensorReadings),1));
	ucg.setPrintPos(45,80);
	ucg.print(String((humidInside/sensorReadings),1));
	ucg_print_center(String(lightValue/sensorReadings), 70, 80);
}

/**
	updateAC
	Update AC values
*/
void updateAC(boolean all) {
	// Draw background boxes 
	ucg.setColor(224, 224, 209);
	if (all) {
		ucg.drawBox(0, 86, 128, 21); // AC Status
	} else {
		ucg.drawBox(25, 86, 103, 21); // AC Status
	}
	
	// Print fixed text for aircon
	ucg.setFont(ucg_font_helvB18_tf);
	ucg.setColor(0, 0, 0);
	if (all) {
		ucg.setFont(ucg_font_helvB10_tf);
		ucg.setPrintPos(0,103);
		ucg.print("AC");
	}
	
	// Print AC status
	ucg.setFont(ucg_font_helvB10_tf);
	if (ac1On == 2) {
		ucg.setColor(0, 0, 0);
		ucg_print_center("  ", 20, 103);
	} else {
		if (ac1On == 1) {
			bmDraw("/on.tga",25,104);
		} else {
			bmDraw("/off.tga",25,104);
		}
		if (ac1Mode == 2) {
			bmDraw("/cool.tga",42,104);
		} else if (ac1Mode == 1) {
			bmDraw("/dry.tga",42,104);
		} else {
			bmDraw("/fan.tga",42,104);
		}
		if (ac1Timer == 1) {
			bmDraw("/timer.tga",59,104);
		} else {
			if (ac1Auto == 1) {
				bmDraw("/auto.tga",59,104);
			} else {
				bmDraw("/manual.tga",59,104);
			}
		}
	}
	if (ac2On == 2) {
		ucg.setColor(0, 0, 0);
		ucg.setFont(ucg_font_helvB10_tf);
		ucg_print_center("  ", 90, 103);
	} else {
		if (ac2On == 1) {
			bmDraw("/on.tga",78,104);
		} else {
			bmDraw("/off.tga",78,104);
		}
		if (ac2Mode == 2) {
			bmDraw("/cool.tga",95,104);
		} else if (ac1Mode == 1) {
			bmDraw("/dry.tga",95,104);
		} else {
			bmDraw("/fan.tga",95,104);
		}
		if (ac2Timer == 1) {
			bmDraw("/timer.tga",112,104);
		} else {
			if (ac2Auto == 1) {
				bmDraw("/auto.tga",112,104);
			} else {
				bmDraw("/manual.tga",112,104);
			}
		}
	}
}

/**
	updateSecurity
	Update Security values
*/
void updateSecurity(boolean all) {
	// Draw background boxes 
	ucg.setColor(224, 224, 209);
	if (all) {
		ucg.drawBox(0, 107, 128, 21); // Security Status
	} else {
		ucg.drawBox(32, 107, 96, 21); // Security Status
	}
	
	// Print fixed text for and security
	ucg.setFont(ucg_font_helvB10_tf);
	ucg.setColor(0, 0, 0);
	if (all) {
		ucg.setPrintPos(0,124);
		ucg.print("SEC");
	}
	
	// Print Security status
	if (secFrontOn == 2) {
		ucg.setColor(0, 0, 0);
		ucg_print_center("  ", 20, 124);
	} else {
		if (secFrontOn == 1) {
			bmDraw("/s_on.tga",40,125);
		} else {
			bmDraw("/s_off.tga",40,125);
		}
		if (secFrontLight == 1) {
			bmDraw("/l_on.tga",57,125);
		} else {
			bmDraw("/l_off.tga",57,125);
		}
	}
	if (secBackOn == 2) {
		ucg.setColor(0, 0, 0);
		ucg_print_center("  ", 80, 124);
	} else {
		if (secBackOn == 1) {
			bmDraw("/on.tga",90,125);
		} else {
			bmDraw("/off.tga",90,125);
		}
		if (secBackLight == 1) {
			bmDraw("/l_on.tga",107,125);
		} else {
			bmDraw("/l_off.tga",107,125);
		}
	}
}

/**
	showMQTTerrorScreen
	Shows error screen when connection
	to MQTT broker gets disconnected
*/
void showMQTTerrorScreen() {
	ucg.clearScreen();
	ucg.setFont(ucg_font_helvB14_tf);
	// ucg.setRotate180();
	ucg.setColor(255, 128, 0);
	ucg.drawBox(0, 0, 128, 128);
	ucg.setColor(0, 0, 0);
	ucg_print_center("MQTT ERR", 0, 34);
	ucg_print_center("disconnected", 0, 59);
	ucg_print_center("from", 0, 84);
	ucg_print_center(mqttBroker, 0, 109);
}

/**
	ucg_print_ln
	Prints text and puts cursor to next line
*/
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

/**
	ucg_print_center
	Prints a text centered between
	<yPos> and end of screen
*/
void ucg_print_center(String text, int xPos, int yPos) {
	char textChar[sizeof(text)];
	text.toCharArray(textChar, sizeof(textChar));
	int textCenter = ucg.getStrWidth(textChar)/2;
	ucg.setPrintPos(64 + (xPos/2) - textCenter,yPos);
	ucg.print(text);
}

/**
	getHomeInfo
	Get latest data & status from
	the home devices
*/
void getHomeInfo(boolean all) {
	// getLight();
	getTemperature();
	makeWeather();
	if (all) {
		getSPMStatus();
		getAC1Status();
		getAC2Status();
		getSEFStatus();
		getSERStatus();
	}
	if (all) {
		updateSolar(all);
		updateWeather(all);
		updateAC(all);
		updateSecurity(all);
	}

	// Reset weather values for next reading cycle
	sensorReadings = 0;
	tempInside = 0;
	humidInside = 0;
	heatIndexIn = 0;
	lightValue = 0;
}

/**
	makeWeather
	Build weather info JSON object
*/
void makeWeather() {
 	/** Buffer for outgoing JSON string */
	DynamicJsonBuffer jsonOutBuffer;
	/** Json object for outgoing data */
	JsonObject& jsonOut = jsonOutBuffer.createObject();

	heatIndexIn = dht.computeHeatIndex(tempInside/sensorReadings, humidInside/sensorReadings, false);
	
	jsonOut["de"] = "wei";
	if (sensorReadings != 0) {
		jsonOut["te"] = tempInside/sensorReadings; 
		jsonOut["hu"] = humidInside/sensorReadings;
		jsonOut["hi"] = heatIndexIn;
	} else {
		jsonOut["te"] = tempInside; 
		jsonOut["hu"] = humidInside;
		jsonOut["hi"] = heatIndexIn;
	}
	inWeatherStatus = "";
	jsonOut.printTo(inWeatherStatus);
}

/**
	getTemperature
	Reads temperature from DHT11 sensor
*/
void getTemperature() {
	// Reading temperature for humidity takes about 250 milliseconds!
	// Sensor readings may also be up to 2 seconds 'old' (it's a very slow sensor)
	float newHumidValue = dht.readHumidity();          // Read humidity (percent)
	float newTempValue = dht.readTemperature(false);     // Read temperature as Celsius
	// Check if any reads failed and exit early (to try again).
	if (isnan(newHumidValue) || isnan(newTempValue)) {
		#ifdef DEBUG_OUT 
		Serial.println("Failed to read from DHT sensor!");
		Serial.print("Temperature value: ");
		Serial.println(newTempValue);
		Serial.print("Humidity value: ");
		Serial.println(newHumidValue);
		#endif
		return;
	}
	humidInside += newHumidValue;
	tempInside += newTempValue;
	sensorReadings++;
}

/**
 * Plays the tune defined with melody[] endless until ticker is detached
 */
void playSound() {
	/** Current tone to be played */
	int toneLength = melody[melodyPoint];
	analogWriteFreq(toneLength / 2);
	analogWrite(SPEAKER, toneLength / 4);

	melodyPoint ++;
	if (melodyPoint == melodyLenght) {
		melodyPoint = 0;
		soundTimer.detach();
//		analogWriteFreq(0);
		analogWrite(SPEAKER, 0);
		digitalWrite(SPEAKER, LOW);
	}
}