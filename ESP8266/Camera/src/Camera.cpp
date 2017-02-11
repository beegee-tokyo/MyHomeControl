#include <Setup.h>

/** Buffer for received data */
char outBuf[128];
/** Counter for sent/received data */
char outCount;

/**
	Take camera shot and save to FTP & SPIFFS
	@return <code>boolean</code>
			true if photo was taken and saved to FTP & SPIFFS
			false if error occured
*/
boolean takeShot() {
	comLedFlashStart(0.1);
	digitalWrite(flashLED, HIGH);
	uint32_t startTime = millis();
	if (! cam.takePicture()) {
		if (debugOn) {
			sendDebug("Failed to snap!", OTA_HOST);
		}
		cam.resumeVideo();
		comLedFlashStop();
		digitalWrite(flashLED, LOW);
		sendBroadCast(false);
		return false;
	}
	// Get the size of the image (frame) taken
	uint16_t jpglen = cam.frameLength();

	// Create an image with the name MM-DD-hh-mm-ss.JPG
	char filename[19];
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
		String debugMsg = "Saving " + String(filename) + " Image size: " + String(jpglen);
		sendDebug(debugMsg, OTA_HOST);
	}

	// Prepare file to save image
	bool fileOpen = true;
	File imgFile = SPIFFS.open("/last.jpg", "w");
	if (imgFile == 0) {
		fileOpen = false;
	}

	// Prepare FTP connection
	bool ftpConnected = true;
if (debugOn) {
	sendDebug("Connecting to FTP", OTA_HOST);
}
	if (!ftpConnect()) {
		ftpConnected = false;
	}

	if (ftpConnected) {
		// Prepare data upload
if (debugOn) {
	sendDebug("CD on FTP", OTA_HOST);
}
		ftpClient.println(F("CWD /var/www/html/1s"));
		// Check result
		if (!ftpReceive()) {
			if (debugOn) {
				String debugMsg = "FTP: CD failed: " + String(ftpBuf);
				sendDebug(debugMsg, OTA_HOST);
			}
			ftpDataClient.stop();
			ftpClient.stop();
			ftpConnected = false;
		} else {
if (debugOn) {
	sendDebug("STOR on FTP", OTA_HOST);
}
			ftpClient.print(F("STOR "));
			ftpClient.println(filename);
			// Check result
			if (!ftpReceive()) {
				if (debugOn) {
					String debugMsg = "FTP: Passive mode not available: " + String(ftpBuf);
					sendDebug(debugMsg, OTA_HOST);
				}
				ftpDataClient.stop();
				ftpClient.stop();
				ftpConnected = false;
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

	// Read all the data up to # bytes!
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
			buffer = cam.readPicture(bytesToRead);

			if (buffer == 0) {
				if (debugOn) {
					sendDebug("Read from camera failed", OTA_HOST);
				}
				readFailures++;
				if (readFailures > 100) { // Too many read errors, better to stop
					jpglen = 0;
					break;
				}
			} else {
				memcpy(&clientBuf[blocks*32],buffer,bytesToRead);
				wCount += bytesToRead;
				jpglen -= bytesToRead;
			}
		}
		if (ftpConnected) {
			bytesWrittenFTP += ftpDataClient.write((const uint8_t *) clientBuf, wCount);
		}
		if (fileOpen) {
			bytesWrittenFS += imgFile.write((const uint8_t *) clientBuf, wCount);
		}
		if (millis()-timeOut > 60000) { // if transfer takes more than a minute, stop it
			if (debugOn) {
				sendDebug("Timeout saving picture", OTA_HOST);
			}
			bytesWrittenFTP = bytesWrittenFS = jpglen = 0;
			break;
		}
	}
	if (ftpConnected) {
if (debugOn) {
	sendDebug("STOP on FTP data client", OTA_HOST);
}
		ftpDataClient.stop();
	}
	if (fileOpen) {
if (debugOn) {
	sendDebug("Close file", OTA_HOST);
}
		imgFile.close();
	}

if (debugOn) {
	sendDebug("QUIT FTP", OTA_HOST);
}
	if (ftpConnected) {
		ftpClient.println("QUIT");
		// Check result
		if (!ftpReceive()) {
			if (debugOn) {
				String debugMsg = "FTP: Disconnect failed: " + String(ftpBuf);
				sendDebug(debugMsg, OTA_HOST);
			}
			ftpClient.stop();
			cam.resumeVideo();
			comLedFlashStop();
			digitalWrite(flashLED, LOW);
			return false;
		}
	}
	if (debugOn) {
		sendDebug("STOP FTP", OTA_HOST);
	}
	if (ftpConnected) {
		ftpClient.stop();
	}
	uint32_t endTime = millis();
	digitalWrite(flashLED, LOW);
	if (debugOn) {
		String debugMsg = "Read from camera & save to FTP finished: " + String(bytesWrittenFTP) + "/" + String(bytesWrittenFS) + " bytes in " + String(endTime-startTime) + "ms";
		sendDebug(debugMsg, OTA_HOST);
	}

	// Restart camera
	cam.resumeVideo();
	comLedFlashStop();
	// if ((ftpConnected && bytesWrittenFTP == 0) || (fileOpen && bytesWrittenFS == 0)) {
	if (ftpConnected && (bytesWrittenFTP == 0)) {
		return false;
	} else {
		return true;
	}
}
