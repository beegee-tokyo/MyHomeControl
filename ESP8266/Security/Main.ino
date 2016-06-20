/**
 * Main loop
 * Processing of the result of GPIO and timer interrupts
 * Calling replyClient if a web client is contacting
 */
void loop() {
	wdt_reset();
	// Handle OTA updates
	ArduinoOTA.handle();
	
	if (otaRunning) { // Do nothing anymore
		wdt_reset();
		return;
	}

	wdt_reset();
	/* Handle new PIR status if available
	*	if there is a detection
	*	- the detection led starts to flash
	*	- the relay is switched on (if flag switchLights is true)
	*	- alarm sound is played (if flag switchLights is true)
	*	- msgText is set to detection message
	*	if detection is finished
	*	- the detection led stops flashing
	*	- msgText is set to no detection message
	*/

	if (pirTriggered) {
		pirTriggered = false;
		Serial.println("Interrupt from PIR pin");
		if (hasDetection) { // Detection of movement
			ledFlasher.attach(0.2, redLedFlash); // Flash fast if we have a detection
			relayOffTimer.detach();
			if (switchLights) {
				offDelay = 0;
				relayOffTimer.attach(1, relayOff);
				digitalWrite(relayPort, HIGH);
			} else {
				digitalWrite(relayPort, LOW);
			}
			if (alarmOn) {
				melodyPoint = 0; // Reset melody pointer to 0
				alarmTimer.attach_ms(melodyTuneTime, playAlarmSound);
				sendAlarm(true);
			}
		} else { // No detection
			ledFlasher.detach(); // Stop fast flashing if we have no detection
			digitalWrite(alarmLED, HIGH); // Turn off LED
			alarmTimer.detach();
			analogWrite(speakerPin, LOW); // Switch off speaker
			digitalWrite(alarmLED, HIGH);
			if (alarmOn) { // If alarm is active, continue to flash slowly
				ledFlasher.attach(0.4, redLedFlash);
				sendAlarm(true);
			}
		}
	}
	
	if (lightOffTriggered) {
		lightOffTriggered = false;
		sendAlarm(true);
	}

	wdt_reset();
	// Handle new LDR update request
	if (lightLDRTriggered) {
		lightLDRTriggered = false;
		if (getLDR()) {
			sendAlarm(true);
			sendLightStatus(switchLights);		}
	}

	wdt_reset();
	// Handle new client request on HTTP server if available
	WiFiClient client = server.available();
	if (client) {
		digitalWrite(comLED, LOW);
		replyClient(client);
		digitalWrite(comLED, HIGH);
	}

	wdt_reset();
	// Check time in case automatic de/activation of alarm is set */
	if (hasAutoActivation) {
		if ((hour()==autoActivOn) && !alarmOn) {
			// Set alarm_on to active
			alarmOn = true;
			ledFlasher.attach(0.4, redLedFlash);
			sendAlarm(true);
		}
		if ((hour()==autoActivOff) && alarmOn) {
			// Set alarm_on to inactive
			alarmOn = false;
			ledFlasher.detach();
			digitalWrite(alarmLED, HIGH); // Turn off LED
			sendAlarm(true);
		}
	}
	
	wdt_reset();
	// Handle FTP access
	ftpSrv.handleFTP();

	wdt_reset();
	if (heartBeatTriggered) {
		heartBeatTriggered = false;
		// Give a "I am alive" signal
		sendAlarm(true);
	}
}


