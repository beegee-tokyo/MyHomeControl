#include <Setup.h>

/** Counter for "I am alive" red LED blinking in loop() */
long liveCnt = 0;

void loop() {
	// Handle OTA updates
	ArduinoOTA.handle();

	if (otaRunning) { // If the OTA update is active we do nothing else here in the main loop
		return;
	}

	// In case we don't have a time from NTP, retry
	if (!gotTime) {
		tryGetTime();
	}

	// Resync time every 12 hours
	if (now() > lastSyncTime+43200) {
		tryGetTime();
		lastSyncTime = now();
	}

	// Handle new request on tcp socket server if available
	WiFiClient tcpClient = tcpServer.available();
	if (tcpClient) {
		socketServer(tcpClient);
		digitalWrite(flashLED, LOW);
	}

  // // Check if motion was detected
  // if (hasCamera) {
  //   if (cam.motionDetected()) {
  //     cam.setMotionDetect(false);
  //     takeShot();
  //     cam.setMotionDetect(true);
  //   }
  // }

	// Handle FTP access
	ftpSrv.handleFTP();

	// Give a "I am alive" signal
	liveCnt++;
	if (liveCnt >= 100000) { // 100000
		digitalWrite(blinkLED, !digitalRead(blinkLED));
		liveCnt = 0;
	}

	// TODO find out why sometimes the flashLED is on!!!!!!!!!!!!
	digitalWrite(flashLED, LOW);
}
