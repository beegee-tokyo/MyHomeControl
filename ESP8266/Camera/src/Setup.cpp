#include <Setup.h>
#include <declarations.h>

void setup() {
  initLeds(blinkLED,flashLED); // COM LED -- ACT LED
	digitalWrite(flashLED,LOW);
	digitalWrite(blinkLED,LOW);

	Serial.begin(115200);

	Serial.setDebugOutput(false);
	Serial.println("");
	Serial.println("Hello from ESP8266 home security camera");
	Serial.print("SW build: ");
	Serial.println(compileDate);

  //resetWiFiCredentials();
	// Try to connect to WiFi with captive portal
	ipAddr = connectWiFi(ipAddr, ipGateWay, ipSubNet, "ESP8266 Home 1");

	sendDebug("Reboot", OTA_HOST);

	// Prepare OTA update listener
	ArduinoOTA.onStart([]() {
		String debugMsg = "OTA start";
		sendDebug(debugMsg, OTA_HOST);
		Serial.println(debugMsg);
		doubleLedFlashStart(0.1);
		WiFiUDP::stopAll();
		WiFiClient::stopAll();
		tcpServer.close();
		otaRunning = true;
	});

	// Start OTA server.
	ArduinoOTA.setHostname(DEVICE_ID);
	ArduinoOTA.begin();

	// Start the tcp socket server to listen on port 6000
	tcpServer.begin();

	// Start FTP server
	ftpSrv.begin(OTA_HOST, OTA_HOST);	 //username, password for ftp.

	// Initialize file system.
	boolean foundStatus = SPIFFS.begin();
	if (foundStatus) { // File system found
		// Try to get last saved image
		File imgFile = SPIFFS.open("/last.jpg", "r");
		if (imgFile != 0) {
			sendDebug("Saved image found", OTA_HOST);
			imgFile.close();
		} else {
			foundStatus = false;
		}
	} else {
		sendDebug("Filesystem failure", OTA_HOST);
	}
	if (!foundStatus) // Could not get last status
	{
		sendDebug("SPIFFS failure, try to format", OTA_HOST);
		if (SPIFFS.format()){
			sendDebug("SPIFFS formatted", OTA_HOST);
		} else {
			sendDebug("SPIFFS format failed", OTA_HOST);
		}
	}

	// Set initial time
	tryGetTime();

	// Prepare NTP time update timer
	lastSyncTime = now();

	inSetup = false;

  // Start camera connection
  // TODO put this into library!
  uint32_t foundBaud = cam.autoDetectBaudRate();
  delay(1000);

  if (foundBaud != 0) {
    if (cam.begin(foundBaud)) {
			sendDebug("Camera found!", OTA_HOST);

      // Set the picture size - you can choose one of 640x480, 320x240 or 160x120
      // Remember that bigger pictures take longer to transmit!
      cam.setImageSize(VC0706_640x480);        // biggest
      // cam.setImageSize(VC0706_320x240);        // medium
      // cam.setImageSize(VC0706_160x120);          // small

			// Reset is necessary only if resolution other than 640x480 is selected
			cam.reset();
			delay(500);
			String debugMsg = "Image size = " + String(cam.getImageSize());
			sendDebug(debugMsg, OTA_HOST);
			// Serial.print("Downsize = "); Serial.println(cam.getDownsize());
      cam.setCompression(255);
			debugMsg = "Compression = " + String(cam.getCompression());
			sendDebug(debugMsg, OTA_HOST);

      // //  Motion detection system can alert you when the camera 'sees' motion!
      // cam.setMotionDetect(true);           // turn it on
      // Serial.print("Motionstatus = "); Serial.println(cam.getMotionDetect());
			cam.resumeVideo();

			digitalWrite(flashLED,LOW);
    }
  } else {
		sendDebug("No camera found?", OTA_HOST);
    doubleLedFlashStart(1);
    hasCamera = false;
  }
}
