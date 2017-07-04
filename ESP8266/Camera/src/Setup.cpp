#include <Setup.h>
#include <declarations.h>

/** Timer for heart beat */
Ticker heartBeatTimer;

void setup() {
	inSetup = true;
  initLeds(blinkLED,flashLED); // COM LED -- ACT LED
	digitalWrite(flashLED,LOW);
	digitalWrite(blinkLED,LOW);

	Serial.begin(115200);

	Serial.setDebugOutput(false);
	Serial.println("");
	Serial.println("Hello from ESP8266 home security camera");
	Serial.print("SW build: ");
	Serial.println(compileDate);

  // resetWiFiCredentials();
	// Try to connect to WiFi with captive portal
	ipAddr = connectWiFi(ipAddr, ipGateWay, ipSubNet, "ESP8266 CAM 1");

	sendRpiDebug("Reboot", OTA_HOST);

	// Start the tcp socket server to listen on port tcpComPort
	tcpServer.begin();

	// Initialize file system.
	boolean foundStatus = SPIFFS.begin();
	if (foundStatus) { // File system found
		// Try to get last saved image
		File imgFile = SPIFFS.open("/last.jpg", "r");
		if (imgFile != 0) {
			String debugMsg = "Saved image found: " + String(imgFile.size()) + "bytes";
			sendRpiDebug(debugMsg, OTA_HOST);
			imgFile.close();
		} else {
			foundStatus = false;
		}
	} else {
		sendRpiDebug("Filesystem failure", OTA_HOST);
	}
	if (!foundStatus) // Could not get last status or file system not ready
	{
		sendRpiDebug("SPIFFS failure, try to format", OTA_HOST);
		if (SPIFFS.format()){
			sendRpiDebug("SPIFFS formatted", OTA_HOST);
		} else {
			sendRpiDebug("SPIFFS format failed", OTA_HOST);
		}
	}

	// Set initial time
	tryGetTime(false);

	// Prepare NTP time update timer
	lastSyncTime = now();

  // Start camera connection
  // TODO put this into library!
  uint32_t foundBaud = cam.autoDetectBaudRate();
  delay(1000);

  if (foundBaud != 0) {
    if (cam.begin(foundBaud)) {
			sendRpiDebug("Camera found!", OTA_HOST);

      // Set the picture size - you can choose one of 640x480, 320x240 or 160x120
      // Remember that bigger pictures take longer to transmit!
      cam.setImageSize(VC0706_640x480);        // biggest
      // cam.setImageSize(VC0706_320x240);        // medium
      // cam.setImageSize(VC0706_160x120);          // small

			// Reset is necessary only if resolution other than 640x480 is selected
			cam.reset();
			delay(500);
			String debugMsg = "Image size = " + String(cam.getImageSize());
			sendRpiDebug(debugMsg, OTA_HOST);
			// Serial.print("Downsize = "); Serial.println(cam.getDownsize());
      cam.setCompression(255);
			debugMsg = "Compression = " + String(cam.getCompression());
			sendRpiDebug(debugMsg, OTA_HOST);

      // //  Motion detection system can alert you when the camera 'sees' motion!
      // cam.setMotionDetect(true);           // turn it on
      // Serial.print("Motionstatus = "); Serial.println(cam.getMotionDetect());
			cam.resumeVideo();

			digitalWrite(flashLED,LOW);
    }
  } else {
		sendRpiDebug("No camera found?", OTA_HOST);
    doubleLedFlashStart(1);
    hasCamera = false;
  }

	// Prepare OTA update listener
	ArduinoOTA.onStart([]() {
		wdt_disable();
		String debugMsg = "OTA start";
		sendRpiDebug(debugMsg, OTA_HOST);
		Serial.println(debugMsg);
		doubleLedFlashStart(0.1);
		WiFiUDP::stopAll();
		WiFiClient::stopAll();
		tcpServer.close();
		otaRunning = true;
	});

	// Start OTA server.
	ArduinoOTA.setHostname(OTA_HOST);
	ArduinoOTA.begin();

	// Start heart beat sending every 1 minutes
	heartBeatTimer.attach(60, triggerHeartBeat);
	sendBroadCast(false);
	inSetup = false;
	wdt_enable(WDTO_8S);
}
