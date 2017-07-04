#include "Setup.h"

void getTemperature() {
	// Reading temperature for humidity takes about 250 milliseconds!
	// Sensor readings may also be up to 2 seconds 'old' (it's a very slow sensor)
	wdt_reset();
	float newHumidValue = dht.readHumidity(); // Read humidity (percent)
	wdt_reset();
	float newTempValue = dht.readTemperature(false); // Read temperature as Celsius
	wdt_reset();
	// Check if any reads failed and exit early (to try again).
	if (isnan(newHumidValue) || isnan(newTempValue)) {
		if (debugOn) {
			sendDebug("Error reading from DHT11", OTA_HOST);
		}
		return;
	}
	/******************************************************* */
	/* Trying to calibrate the humidity values               */
	/******************************************************* */
	// newHumidValue = 10*sqrt(newHumidValue);
	newHumidValue = 20+newHumidValue;
	humidValue = newHumidValue;
	tempValue = newTempValue;
}
