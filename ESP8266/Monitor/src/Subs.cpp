#include "Setup.h"

/**
	switchBackDisplay
	Change display back to layout 0
	called by Ticker resetDisplay
*/
void switchBackDisplay() {
	displayLayout = 0;
	displayChange = true;
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
	getHomeInfo
	Get latest data & status from
	the home devices
*/
void getHomeInfo(boolean all) {
	getTemperature();
	makeInWeather();
	makeOutWeather();
	if (all) {
		updateSolar(all);
		updateWeather(all);
		updateAC(all);
		updateSecurity(all);

		getSPMStatus();

		/** Wait out time for client request */
		int waitTimeOut = 0;

		sendCmd(ipAC1, "s");
		udpMsgLength = 0;
		while (udpMsgLength == 0) {
			// Check if broadcast arrived
			udpMsgLength = udpListener.parsePacket();
			if (udpMsgLength != 0) {
				getUDPbroadcast(udpMsgLength);
				break;
			} else {
				delay(10);
				waitTimeOut++;
				if (waitTimeOut == 200) { // Wait 2000 ms
					udpMsgLength = 1; // Finish waiting
				}
			}
		}

		sendCmd(ipAC2, "s");
		udpMsgLength = 0;
		while (udpMsgLength == 0) {
			// Check if broadcast arrived
			udpMsgLength = udpListener.parsePacket();
			if (udpMsgLength != 0) {
				getUDPbroadcast(udpMsgLength);
				break;
			} else {
				delay(10);
				waitTimeOut++;
				if (waitTimeOut == 200) { // Wait 2000 ms
					udpMsgLength = 1; // Finish waiting
				}
			}
		}

		sendCmd(ipSecFront, "s");
		udpMsgLength = 0;
		while (udpMsgLength == 0) {
			// Check if broadcast arrived
			udpMsgLength = udpListener.parsePacket();
			if (udpMsgLength != 0) {
				getUDPbroadcast(udpMsgLength);
				break;
			} else {
				delay(10);
				waitTimeOut++;
				if (waitTimeOut == 200) { // Wait 2000 ms
					udpMsgLength = 1; // Finish waiting
				}
			}
		}

		sendCmd(ipSecBack, "s");
		udpMsgLength = 0;
		while (udpMsgLength == 0) {
			// Check if broadcast arrived
			udpMsgLength = udpListener.parsePacket();
			if (udpMsgLength != 0) {
				getUDPbroadcast(udpMsgLength);
				break;
			} else {
				delay(10);
				waitTimeOut++;
				if (waitTimeOut == 200) { // Wait 2000 ms
					udpMsgLength = 1; // Finish waiting
				}
			}
		}

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
}

/**
	makeInWeather
	Build inside weather info JSON object
*/
void makeInWeather() {
 	/** Buffer for outgoing JSON string */
	DynamicJsonBuffer jsonOutBuffer;
	/** Json object for outgoing data */
	JsonObject& jsonOut = jsonOutBuffer.createObject();

	jsonOut["de"] = "wei"; //jsonOut["device"] = "wei";
	if (sensorReadings != 0) {
		heatIndexIn = dht.computeHeatIndex(tempInside/sensorReadings, humidInside/sensorReadings, false);
		jsonOut["te"] = tempInside/sensorReadings;
		jsonOut["hu"] = humidInside/sensorReadings;
		jsonOut["hi"] = heatIndexIn;
	} else {
		heatIndexIn = dht.computeHeatIndex(tempInside, humidInside, false);
		jsonOut["te"] = tempInside;
		jsonOut["hu"] = humidInside;
		jsonOut["hi"] = heatIndexIn;
	}
	inWeatherStatus = "";
	jsonOut.printTo(inWeatherStatus);
}

/**
	makeOutWeather
	Build outside weather info JSON object
*/
void makeOutWeather() {
 	/** Buffer for outgoing JSON string */
	DynamicJsonBuffer jsonOutBuffer;
	/** Json object for outgoing data */
	JsonObject& jsonOut = jsonOutBuffer.createObject();

	jsonOut["de"] = "weo"; //jsonOut["device"] = "weo";
	jsonOut["te"] = tempOutside;
	jsonOut["hu"] = humidOutside;
	jsonOut["hi"] = heatIndexOut;

	outWeatherStatus = "";
	jsonOut.printTo(outWeatherStatus);
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
		if (debugOn) {
			sendDebug("Failed to read from DHT", OTA_HOST);
		}
		return;
	}
	/******************************************************* */
	/* Trying to calibrate the humidity values               */
	/******************************************************* */
	// newHumidValue = 10*sqrt(newHumidValue);
	newHumidValue = 20+newHumidValue;
	humidInside += newHumidValue;
	tempInside += newTempValue;
	sensorReadings++;
}
