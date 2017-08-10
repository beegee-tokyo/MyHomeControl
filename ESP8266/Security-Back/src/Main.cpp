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

 	// Trial to catch time changing Bug
 	if ((lastKnownYear != 0) && (year() != lastKnownYear) && gotTime) {
 		if (!tryGetTime(debugOn)) {
 			tryGetTime(debugOn);
 		}
 		if (gotTime) {
 			lastKnownYear = year();
 		}
 	}

	wdt_reset();
	/* Handle new PIR status if available
	*	if there is a detection
	*	- the detection led starts to flash
	*	- the relay is switched on (if flag switchLights is true)
	*	- piezo alarm buzzer is activated (if flag switchLights is true)
	*	- msgText is set to detection message
	*	if detection is finished
	*	- the detection led stops flashing
	*	- msgText is set to no detection message
	*/
	if (pirTriggered) {
		pirTriggered = false;
		if (hasDetection) { // Detection of movement
			if (switchLights) {
				triggerLights(); // Switch on other backyard light
				relayOffTimer.detach();
				relayOffTimer.once(onTime, relayOff);
				digitalWrite(relayPort, HIGH);
				if (debugOn) {
					sendDebug("Retriggered lights", OTA_HOST);
				}
			}
			// if (alarmOn || switchLights) {
			// 	triggerPic(); // Trigger picture from security camera
			// }
			if (alarmOn) {
				sendAlarm(true);
			}
			if (debugOn) {
				sendDebug("Detection interrupt from PIR pin", OTA_HOST);
			}
		} else { // No detection
			if (alarmOn) { // If alarm is active, send status message
				sendAlarm(true);
			}
			if (debugOn) {
				sendDebug("No detection interrupt from PIR pin", OTA_HOST);
			}
		}
	}

	if (lightOffTriggered) {
		lightOffTriggered = false;
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
		if (debugOn) {
			sendDebug("tcpClient connected", OTA_HOST);
		}
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
		if (!WiFi.isConnected()) {
			if (debugOn) {
				sendDebug("Lost WiFi connection", OTA_HOST);
			}
		}
		if (!wmIsConnected) { // Connection to WiFi failed, retry to connect
			// Try to connect to WiFi with captive portal
			ipAddr = connectWiFi(ipAddr, ipGateWay, ipSubNet, "ESP8266 Security Back");
		}
 		if (!gotTime) { // Got no time from the NTP server, retry to get it
 			if (!tryGetTime(debugOn)) {
 				tryGetTime(debugOn); // Failed to get time from NTP server, retry
 			}
 		}
		heartBeatTriggered = false;
		// Stop the tcp socket server
		tcpServer.stop();
		// Give a "I am alive" signal
		sendAlarm(true);
		// Restart the tcp socket server to listen on port tcpComPort
		tcpServer.begin();
	}
}
