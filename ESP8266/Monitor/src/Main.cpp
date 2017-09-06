#include "Setup.h"

/** Button status */
byte lastButtonStatus = HIGH;
/** Button debounce time value */
unsigned long lastButtonChange;
/** Flag for button accepted */
boolean displayChanged = false;

void loop() {
	if (otaRunning) { // While OTA update is running we do nothing else
		return;
	}
	// Handle OTA updates
	ArduinoOTA.handle();

	byte buttonState = digitalRead(buttonPin);
	if (buttonState == LOW && lastButtonStatus != LOW) {
		lastButtonChange = millis();
		lastButtonStatus = LOW;
	}
	if (buttonState == LOW && lastButtonStatus == LOW && !displayChanged) {
		if (millis() > lastButtonChange + 100) { // Button low for 100 ms?
			digitalWrite(comLED, LOW);
			displayLayout++;
			if (displayLayout == 5) {
				displayLayout = 0;
			}
			resetDisplay.detach();
			resetDisplay.once(300,switchBackDisplay);
			updateDisplay(true);
			displayChanged = true;
		}
	}
	if (buttonState == HIGH && lastButtonStatus == LOW) {
		if (millis() > lastButtonChange + 100) { // Ignore change for debouncing for 100ms
			lastButtonStatus = HIGH;
			digitalWrite(comLED, HIGH);
			displayChanged = false;
		}
	}

	// Check if display change is requested */
	if (displayChange) {
		updateDisplay(true);
		displayChange = false;
	}

	// Check if broadcast arrived
	udpMsgLength = udpListener.parsePacket();
	if (udpMsgLength != 0) {
		getUDPbroadcast(udpMsgLength);
	}

	/** Handle MQTT subscribed */
	mqttClient.loop();
	delay(10); // <- fixes some issues with WiFi stability

	if(!mqttClient.connected()) {
		mqttClient.begin(mqttBroker, mqttReceiver);

		if (debugOn) {
			sendDebug("MQTT connection lost, try to reconnect", OTA_HOST);
		}
		int connectTimeout = 0;
		while (!mqttClient.connect(mqttID, mqttUser, mqttPwd)) {
			delay(500);
			Serial.print(".");
			connectTimeout++;
			if (connectTimeout > 240) { //Wait for 2 minutes (240 x 500 milliseconds) then reset WiFi connection
				if (debugOn) {
					sendDebug("Can't connect to MQTT broker", OTA_HOST);
				}
				udpListener.stop();
				// Try to reset WiFi connection
				reConnectWiFi();
				udpListener.begin(udpBcPort);
			}
			// Handle OTA updates
			ArduinoOTA.handle();
			// Check if broadcast arrived
			udpMsgLength = udpListener.parsePacket();
			if (udpMsgLength != 0) {
				getUDPbroadcast(udpMsgLength);
			}
		}
		// mqttClient.subscribe("/CMD");
	}

	// Handle new request on tcp socket server if available
	WiFiClient tcpClient = tcpServer.available();
	if (tcpClient) {
		comLedFlashStart(0.2);
		socketServer(tcpClient);
		comLedFlashStop();
	}

	// Handle FTP access
	ftpSrv.handleFTP();

	// Handle new temperature & humidity update request
	if (dhtUpdated) {
		dhtUpdated = false;
		getTemperature();
		updateWeather(false);
	}

	// Handle new data update request
	if (statusUpdated) {
		statusUpdated = false;
		getHomeInfo(false);
		delay(10); // <- fixes some issues with WiFi stability
		// sendToMQTT();
		if (inWeatherStatus.length() != 0) {
			MQTTMessage mqttMsg;
	    mqttMsg.topic = (char *)"/WEI";
	    mqttMsg.payload = (char *)&inWeatherStatus[0];
	    mqttMsg.length = inWeatherStatus.length();
	    mqttClient.publish(&mqttMsg);
			sendWEIBroadCast(inWeatherStatus);
		}
	}
}
