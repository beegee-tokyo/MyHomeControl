/**
 * Sets flag dhtUpdated to true for handling in loop()
 * called by Ticker getDHTTimer
 * will initiate a call to getTemperature() from loop()
 */
void triggerGetDHT() {
	dhtUpdated = true;
}

void getTemperature() {
	// Reading temperature for humidity takes about 250 milliseconds!
	// Sensor readings may also be up to 2 seconds 'old' (it's a very slow sensor)
	wdt_reset();
	float newHumidValue = dht.readHumidity();          // Read humidity (percent)
	wdt_reset();
	float newTempValue = dht.readTemperature(false);     // Read temperature as Celsius
	wdt_reset();
	// Check if any reads failed and exit early (to try again).
	if (isnan(newHumidValue) || isnan(newTempValue)) {
		Serial.println("Failed to read from DHT sensor!");
		Serial.print("Temperature value: ");
		Serial.println(newTempValue);
		Serial.print("Humidity value: ");
		Serial.println(newHumidValue);
		return;
	}
	humidValue = newHumidValue;
	tempValue = newTempValue;
}

