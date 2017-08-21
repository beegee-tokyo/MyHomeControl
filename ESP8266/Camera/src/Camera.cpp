#include <Setup.h>

// /** Buffer for received data */
// char outBuf[128];
// /** Counter for sent/received data */
// char outCount;

/**
	Take camera shot and save to FTP & SPIFFS
	@return <code>boolean</code>
			true if photo was taken and saved to FTP & SPIFFS
			false if error occured
*/
boolean takeShot() {
	String debugMsg;
	wdt_reset();
	if (debugOn) {
		sendRpiDebug("takeShot started", OTA_HOST);
	}
	comLedFlashStart(0.1);
	digitalWrite(flashLED, HIGH);
	uint32_t startTime = millis();
	wdt_reset();
	if (!cam.takePicture()) {
		if (debugOn) {
			sendRpiDebug("Failed to snap!", OTA_HOST);
		}
		wdt_reset();
		cam.resumeVideo();
		comLedFlashStop();
		digitalWrite(flashLED, LOW);
		return false;
	}
	// Get the size of the image (frame) taken
	uint16_t jpglen = cam.frameLength();
	if (debugOn) {
		debugMsg = "Got image with size " + String(jpglen);
		sendRpiDebug(debugMsg, OTA_HOST);
	}

	wdt_reset();
	if (jpglen == 0) {
		if (debugOn) {
			debugMsg = "Image size wrong: " + String(jpglen);
			sendRpiDebug(debugMsg, OTA_HOST);
		}
		cam.resumeVideo();
		comLedFlashStop();
		digitalWrite(flashLED, LOW);
		return false;
	}

	// Create an image with the name MM-DD-hh-mm-ss.JPG
	// char filename[19];
	String dateTime = getDigits(month());
	dateTime += "-" + getDigits(day());
	dateTime += "-" + getDigits(hour());
	dateTime += "-" + getDigits(minute());
	dateTime += "-" + getDigits(second());
	for (int index=0; index < 14; index ++) {
		filename[index] = dateTime[index];
	}
	filename[14] = '.';
	filename[15] = 'j';
	filename[16] = 'p';
	filename[17] = 'g';
	filename[18] = 0;

	if (debugOn) {
		debugMsg = "Saving " + String(filename) + " Image size: " + String(jpglen);
		sendRpiDebug(debugMsg, OTA_HOST);
	}

	wdt_reset();
	// Prepare file to save image
	bool fileOpen = true;
	File imgFile = SPIFFS.open("/last.jpg", "w");
	if (imgFile == 0) {
		fileOpen = false;
		if (debugOn) {
			sendRpiDebug("Failed to open file /last.jpg", OTA_HOST);
		}
	}

	// Prepare FTP connection
	bool ftpConnected = true;
	if (debugOn) {
		sendRpiDebug("Connecting to FTP", OTA_HOST);
	}
	wdt_reset();
	if (!ftpConnect()) {
		ftpConnected = false;
		if (debugOn) {
			sendRpiDebug("Connecting to FTP failed", OTA_HOST);
		}
		// Maybe we lost WiFi connection!
		wmIsConnected = false;
	}

	if (ftpConnected) {
		// Prepare data upload
		ftpClient.println(F("CWD /var/www/html/1s"));
		// Check result
		wdt_reset();
		if (!ftpReceive()) {
			if (debugOn) {
				debugMsg = "FTP: CD failed: " + String(ftpBuf);
				sendRpiDebug(debugMsg, OTA_HOST);
			}
			ftpDataClient.stop();
			ftpClient.stop();
			ftpConnected = false;
			// Maybe we lost WiFi connection!
			wmIsConnected = false;
		} else {
			ftpClient.print(F("STOR "));
			ftpClient.println(filename);
			// Check result
			wdt_reset();
			if (!ftpReceive()) {
				if (debugOn) {
					debugMsg = "FTP: Passive mode not available: " + String(ftpBuf);
					sendRpiDebug(debugMsg, OTA_HOST);
				}
				ftpDataClient.stop();
				ftpClient.stop();
				ftpConnected = false;
				// Maybe we lost WiFi connection!
				wmIsConnected = false;
			}
		}
	}

	digitalWrite(flashLED, LOW);

	#define bufSizeFTP 1440 //1440
	#define blocksBeforeWrite 45 //45 // bufSizeFTP/32
	uint8_t clientBuf[bufSizeFTP];
	size_t clientCount = 0;
	uint32_t bytesWrittenFTP = 0;
	uint32_t bytesWrittenFS = 0;

	// Read all the data up to jpglen # bytes!
	uint32_t timeOut = millis(); // timout counter
	while (jpglen > 0) {
		uint16_t wCount = 0; // For counting # of writes
		uint8_t bytesToRead;
		uint8_t readFailures = 0;
		for (int blocks = 0; blocks < blocksBeforeWrite; blocks++) {
			uint8_t *buffer;
			if (jpglen < 32) {
				bytesToRead = jpglen;
			} else {
				bytesToRead = 32;
			}
			wdt_reset();
			buffer = cam.readPicture(bytesToRead);

			if (buffer == 0) {
				if (debugOn) {
					sendRpiDebug("Read from camera failed", OTA_HOST);
				}
				readFailures++;
				if (readFailures > 100) { // Too many read errors, better to stop
					jpglen = 0;
					break;
				}
			} else {
				wdt_reset();
				memcpy(&clientBuf[blocks*32],buffer,bytesToRead);
				wCount += bytesToRead;
				jpglen -= bytesToRead;
			}
		}
		if (ftpConnected) {
			wdt_reset();
			bytesWrittenFTP += ftpDataClient.write((const uint8_t *) clientBuf, wCount);
		}
		if (fileOpen) {
			wdt_reset();
			bytesWrittenFS += imgFile.write((const uint8_t *) clientBuf, wCount);

			debugMsg = "Saved " + String(bytesWrittenFS) + " bytes from " + String(wCount);
			sendRpiDebug(debugMsg, OTA_HOST);
		}
		if (millis()-timeOut > 60000) { // if transfer takes more than a minute, stop it
			if (debugOn) {
				sendRpiDebug("Timeout saving picture", OTA_HOST);
			}
			bytesWrittenFTP = bytesWrittenFS = jpglen = 0;
			digitalWrite(flashLED, LOW);
			// Maybe we lost WiFi connection!
			wmIsConnected = false;
			break;
		}
	}
	if (ftpConnected) {
		wdt_reset();
		ftpDataClient.stop();
		ftpClient.println("QUIT");
		// Check result
		wdt_reset();
		if (!ftpReceive()) {
			if (debugOn) {
				debugMsg = "FTP: Disconnect failed: " + String(ftpBuf);
				sendRpiDebug(debugMsg, OTA_HOST);
			}
			// Maybe we lost WiFi connection!
			wmIsConnected = false;
		}
		if (debugOn) {
			sendRpiDebug("STOP FTP", OTA_HOST);
		}
		ftpClient.stop();
	}
	if (fileOpen) {
		wdt_reset();
		imgFile.close();
	}

	uint32_t endTime = millis();
	digitalWrite(flashLED, LOW);
	if (debugOn) {
		debugMsg = "Read from camera & save to FTP finished: " + String(bytesWrittenFTP) + "/" + String(bytesWrittenFS) + " bytes in " + String(endTime-startTime) + "ms";
		sendRpiDebug(debugMsg, OTA_HOST);
	}

	// Restart camera
	wdt_reset();
	cam.resumeVideo();
	comLedFlashStop();
	return true;
}
