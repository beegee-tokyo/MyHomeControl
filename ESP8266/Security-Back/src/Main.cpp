#include "Setup.h"

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
		if (hasDetection) { // Detection of movement
			if (alarmOn) {
				melodyPoint = 0; // Reset melody pointer to 0
				alarmTimer.attach_ms(melodyTuneTime, playAlarmSound);
				sendAlarm(true);
			}
			actLedFlashStart(0.2);
			if (switchLights) {
				relayOffTimer.detach();
				if (debugOn) {
					sendDebug("Retriggered lights", OTA_HOST);
				}
				relayOffTimer.once(onTime, relayOff);
				digitalWrite(relayPort, HIGH);
			}
		} else { // No detection
			actLedFlashStop();
			if (alarmOn) { // If alarm is active, continue to flash slowly
				actLedFlashStart(1);
				sendAlarm(true);
			}
		}
		if (debugOn) {
			sendDebug("Interrupt from PIR pin", OTA_HOST);
		}
	}

	if (lightOffTriggered) {
		lightOffTriggered = false;
		relayOffTimer.detach();
		sendAlarm(true);
		if (debugOn) {
		  sendDebug("lightOffTriggered", OTA_HOST);
		}
	}

	wdt_reset();
	// Handle new light & temperature update request
	if (weatherUpdateTriggered) {
		weatherUpdateTriggered = false;
		getTemperature();
	}

	wdt_reset();
	// Handle new request on tcp socket server if available
	WiFiClient tcpClient = tcpServer.available();
	if (tcpClient) {
		comLedFlashStart(0.2);
		socketServer(tcpClient);
		comLedFlashStop();
	}

	wdt_reset();
	// Check time in case automatic de/activation of alarm is set */
	if (hasAutoActivation) {
		if ((hour()==autoActivOn) && !alarmOn) {
			// Set alarm_on to active
			alarmOn = true;
			actLedFlashStart(1);
			sendAlarm(true);
		}
		if ((hour()== autoActivOff) && alarmOn) {
			// Set alarm_on to inactive
			alarmOn = false;
			actLedFlashStop();
			sendAlarm(true);
		}
	}

	wdt_reset();
	// Handle FTP access
	ftpSrv.handleFTP();

	wdt_reset();
	if (heartBeatTriggered) {
		heartBeatTriggered = false;
		// Stop the tcp socket server
		tcpServer.stop();
		// Give a "I am alive" signal
		sendAlarm(true);
		// Restart the tcp socket server to listen on port 6000
		tcpServer.begin();
	}
}
