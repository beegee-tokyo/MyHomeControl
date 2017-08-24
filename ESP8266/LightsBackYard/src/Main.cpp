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

	if (lightOffTriggered) {
		lightOffTriggered = false;
		sendAlarm(true);
		if (debugOn) {
			sendDebug("lightOffTriggered", OTA_HOST);
		}
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
	if (heartBeatTriggered) {
		if (!WiFi.isConnected()) {
			if (debugOn) {
				sendDebug("Lost WiFi connection", OTA_HOST);
			}
		}
		if (!wmIsConnected) { // Connection to WiFi failed, retry to connect
			wdt_disable();
			// Try to connect to WiFi with captive portal
			ipAddr = connectWiFi(ipAddr, ipGateWay, ipSubNet, "ESP8266 Security Front");
			wdt_enable(WDTO_8S);
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
