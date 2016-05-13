/**
 * Return signal strength or 0 if target SSID not found
 *
 * @return <code>int32_t</code>
 *              Signal strength as unsinged int or 0
 */
int32_t getRSSI() {
	/** Number of retries */
	byte retryNum = 0;
	/** Number of available networks */
	byte available_networks;
	/** The SSID we are connected to */
	String target_ssid(ssid);

	while (retryNum <= 3) {
		retryNum++;
		available_networks= WiFi.scanNetworks();
		if (available_networks == 0) { // Retryone time
			available_networks = WiFi.scanNetworks();
		}

		for (int network = 0; network < available_networks; network++) {
			if (WiFi.SSID(network).equals(target_ssid)) {
				return WiFi.RSSI(network);
			}
		}
	}
	return 0;
}

/**
 * Connect to WiFi AP
 * if no WiFi is found for 30 seconds
 * module will reboot
 */
void connectWiFi() {
	comFlasher.attach(0.5, blueLedFlash);
	wifiConnecting = true;
	wdt_disable();
	WiFi.disconnect();
	WiFi.mode(WIFI_STA);
	WiFi.config(ipAddr, ipGateWay, ipSubNet);
	WiFi.begin(ssid, password);
	Serial.println("Waiting for WiFi connection ");
	int connectTimeout = 0;
	while (WiFi.status() != WL_CONNECTED) {
		delay(500);
		Serial.print(".");
		connectTimeout++;
		if (connectTimeout > 60) { //Wait for 30 seconds (60 x 500 milliseconds) to reconnect
			writeRebootReason("No WiFi found");
			pinMode(16, OUTPUT); // Connected to RST pin
			digitalWrite(16,LOW); // Initiate reset
			ESP.reset(); // In case it didn't work
		}
	}
	comFlasher.detach();
	wifiConnecting = false;
	digitalWrite(comLED, HIGH); // Turn off LED
	wdt_enable(WDTO_8S);
}

/**
 * Called if there is a change in the WiFi connection
 *
 * @param event
 *              Event that happened
 */
void WiFiEvent(WiFiEvent_t event) {
	Serial.printf("[WiFi-event] event: %d\n", event);

	switch (event) {
		case WIFI_EVENT_STAMODE_CONNECTED:
			Serial.println("WiFiEvent: WiFi connected");
			break;
		case WIFI_EVENT_STAMODE_DISCONNECTED:
			Serial.println("WiFiEvent: WiFi lost connection");
			if (!wifiConnecting && !otaRunning) {
				connectWiFi();
			}
			break;
		case WIFI_EVENT_STAMODE_AUTHMODE_CHANGE:
			Serial.println("WiFiEvent: WiFi authentication mode changed");
			break;
		case WIFI_EVENT_STAMODE_GOT_IP:
			Serial.println("WiFiEvent: WiFi got IP");
			Serial.println("IP address: ");
			Serial.println(WiFi.localIP());
			break;
		case WIFI_EVENT_STAMODE_DHCP_TIMEOUT:
			Serial.println("WiFiEvent: WiFi DHCP timeout");
			break;
		case WIFI_EVENT_MAX:
			Serial.println("WiFiEvent: WiFi MAX event");
			break;
	}
}

/**
 * Send broadcast message over UDP into local network
 *
 * @param doGCM
 *              Flag if message is pushed over GCM as well
 */
void sendAlarm(boolean doGCM) {
	digitalWrite(comLED, LOW);
	/** Buffer for Json object */
	DynamicJsonBuffer jsonBuffer;

	// Prepare json object for the response
	/* Json object with the alarm message */
	JsonObject& root = jsonBuffer.createObject();

Serial.println("Create status");
	// Create status
	createStatus(root, true);

	/** WiFiUDP class for creating UDP communication */
	WiFiUDP udpClientServer;
	
	// Start UDP client for sending broadcasts
	udpClientServer.begin(5000);

	int connectionOK = udpClientServer.beginPacketMulticast(multiIP, 5000, ipAddr);
	if (connectionOK == 0) { // Problem occured!
		digitalWrite(comLED, HIGH);
		udpClientServer.stop();
		connectWiFi();
		return;
	}
	root.printTo(udpClientServer);
	udpClientServer.endPacket();
	udpClientServer.stop();

	if (doGCM) {
		/** Buffer for Json object */
		DynamicJsonBuffer msgBuffer;

		// Prepare json object for the response
		/** Json object with the push notification for GCM */
		JsonObject& msgJson = msgBuffer.createObject();
		msgJson["message"] = root;
		gcmSendMsg(msgJson);
	}
	digitalWrite(comLED, HIGH);
}

/**
 * Answer request on http server
 * send last measured light value to requester
 * Commands: 
 * 		/?a=0 to switch off alarm
 *		/?a=1 to switch on alarm
 * 		/?a=2 to switch on the defined hour on/off alarm
 * 		/?a=3 to switch off the defined hour on/off alarm
 * 		/?a=4 to switch on automatic light  
 * 		/?a=5 to switch off automatic light  
 *		/?s   to get short status message
 *		/?t   to get system time and date
 *		/?p   to switch on alarm sound (panic button function)
 *		/?i   to get detailed status information
 *		/?b   to switch on the lights for 2 minutes
 *		/?regid= to register a new device for GCM
 *		/?l   to list all devices registered for GCM
 *		/?d   to delete one or all registered GCM devices
 * @param httpClient
 *              Connected WiFi client
 */
void replyClient(WiFiClient httpClient) {
	/** String for response to client */
	String s = "HTTP/1.1 200 OK\r\nAccess-Control-Allow-Origin: *\r\nContent-Type: application/json\r\n\r\n";
	/** Wait out time for client request */
	int waitTimeOut = 0;
	/** String to hold the response */
	String jsonString;

	/** Buffer for Json object */
	DynamicJsonBuffer jsonBuffer;
	
	// Prepare json object for the response
	/** Json object for the response to the client */
	JsonObject& root = jsonBuffer.createObject();
	root["device"] = DEVICE_ID;

	// Wait until the client sends some data
	while (!httpClient.available()) {
		delay(1);
		waitTimeOut++;
		if (waitTimeOut > 3000) { // If no response for 3 seconds return
			root["result"] = "timeout";
			root.printTo(jsonString);
			s += jsonString;
			httpClient.print(s);
			httpClient.flush();
			httpClient.stop();
			connectWiFi();
			return;
		}
	}

	// Read the first line of the request
	/** String with the client request */
	String req = httpClient.readStringUntil('\r');
	// Strip leading (GET, PUSH) and trailing (HTTP/1) characters
	req = req.substring(req.indexOf("/"),req.length()-9);
	// String for response

	// Switch on/off the alarm
	if (req.substring(0, 4) == "/?a=") {
		root["result"] = "success";
		if (req.substring(4, 5) == "0") { // Alarm off
			alarmOn = false;
			ledFlasher.detach();
			digitalWrite(alarmLED, HIGH);
		} else if (req.substring(4, 5) == "1") { // Alarm on
			alarmOn = true;
			ledFlasher.attach(1, redLedFlash);
		} else if (req.substring(4, 5) == "2") { // Alarm auto
			if (req.substring(5,6) == "," 
					&& req.substring(8,9) == ","
					&& req.length() == 11) {
				int timeIn = req.substring(6,8).toInt(); // Get activation time
				if (timeIn >= 1 && timeIn <= 24) {
					if (timeIn == 24) {
						timeIn = 0;
					}
					autoActivOn = timeIn;
				} else {
					autoActivOn = 22;
				}
				timeIn = req.substring(9,11).toInt(); // Get deactivation time
				if (timeIn >= 1 && timeIn <= 24) {
					if (timeIn == 24) {
						timeIn = 0;
					}
					autoActivOff = timeIn;
				} else {
					autoActivOff = 8;
				}
				hasAutoActivation = true;
				writeStatus();
			} else {
				root["result"] = "invalid";
				root["reason"] = "Missing on and off time";
			}
		} else if (req.substring(4, 5) == "3") { // Alarm auto off
			hasAutoActivation = false;
		} else if (req.substring(4, 5) == "4") { // Auto lights on
			switchLights = true;
		} else if (req.substring(4, 5) == "5") { // Auto lights off
			switchLights = false;
		} else {
			root["result"] = "invalid";
			root["reason"] = "Missing alarm command";
		}
		createStatus(root, true);
		root.printTo(jsonString);
		s += jsonString;
		httpClient.print(s);
		httpClient.flush();
		httpClient.stop();
		delay(500);
		sendAlarm(false);
		return;
		// Request short status
	} else if (req.substring(0, 3) == "/?s") {
		// Create status
		createStatus(root, true);

		root.printTo(jsonString);
		s += jsonString;
		httpClient.print(s);
		httpClient.flush();
		httpClient.stop();
		delay(1000);
		return;
		// Request time
	} else if (req.substring(0, 3) == "/?t") {
		root["time"] = now();
		root["day"] = day();
		root["month"] = month();
		root["year"] = year();
		root["hour"] = hour();
		root["minute"] = minute();
		root.printTo(jsonString);
		s += jsonString;
		httpClient.print(s);
		httpClient.flush();
		httpClient.stop();
		delay(1000);
		return;
		// PANIC!!!! set the alarm off
	} else if (req.substring(0, 3) == "/?p") {
		if (panicOn) {
			alarmTimer.detach();
			analogWrite(speakerPin, LOW); // Switch off speaker
			panicOn = false;
			root["panic"] = "off";
		} else {
			melodyPoint = 0; // Reset melody pointer to 0
			alarmTimer.attach_ms(melodyTuneTime, playAlarmSound);
			panicOn = true;
			root["panic"] = "on";
		}
		root.printTo(jsonString);
		s += jsonString;
		httpClient.print(s);
		httpClient.flush();
		httpClient.stop();
		delay(1000);
		return;
		// Request long status
	} else if (req.substring(0, 3) == "/?i") {
	
		// Create status
		createStatus(root, false);

		root["ssid"] = String(ssid);
		root["ip"] = WiFi.localIP().toString();

		/** Byte array for the local MAC address */
		byte mac[6];
		WiFi.macAddress(mac);
		localMac = String(mac[0], HEX) + ":";
		localMac += String(mac[1], HEX) + ":";
		localMac += String(mac[2], HEX) + ":";
		localMac += String(mac[3], HEX) + ":";
		localMac += String(mac[4], HEX) + ":";
		localMac += String(mac[5], HEX);

		root["mac"] = localMac;

		root["sketch"] = String(ESP.getSketchSize());
		root["freemem"] = String(ESP.getFreeSketchSpace());

		root.printTo(jsonString);
		s += jsonString;
		httpClient.print(s);
		httpClient.flush();
		httpClient.stop();
		delay(1000);
		return;
	// Switch lights on for 2 minutes
	} else if (req.substring(0, 3) == "/?b") {
		root["result"] = "success";
		createStatus(root, true);

		// Switch on lights for 2 minutes
		offDelay = 0;
		relayOffTimer.attach(1, relayOff);
		digitalWrite(relayPort, HIGH);
		
		root.printTo(jsonString);
		s += jsonString;
		httpClient.print(s);
		httpClient.flush();
		httpClient.stop();
		delay(1000);
		return;
		// Registration of new device
	} else if (req.substring(0, 8) == "/?regid=") {
		/** String to hold the received registration ID */
		String regID = req.substring(8,req.length());
		#ifdef DEBUG_OUT Serial.println("RegID: "+regID);
		Serial.println("Length: "+String(regID.length()));
		#endif
		// Check if length of ID is correct
		if (regID.length() != 140) {
			#ifdef DEBUG_OUT 
			Serial.println("Length of ID is wrong");
			#endif
			root["result"] = "invalid";
			root["reason"] = "Length of ID is wrong";
		} else {
			// Try to save ID 
			if (!addRegisteredDevice(regID)) {
				#ifdef DEBUG_OUT 
				Serial.println("Failed to save ID");
				#endif
				root["result"] = "failed";
				root["reason"] = failReason;
			} else {
				#ifdef DEBUG_OUT 
				Serial.println("Successful saved ID");
				#endif
				root["result"] = "success";
				getRegisteredDevices();
				for (int i=0; i<regDevNum; i++) {
					root[String(i)] = regAndroidIds[i];
				}
				root["num"] = regDevNum;
			}
		}
		root.printTo(jsonString);
		s += jsonString;
		httpClient.print(s);
		httpClient.flush();
		httpClient.stop();
		delay(1000);
		return;
	// Send list of registered devices
	} else if (req.substring(0, 3) == "/?l"){
		if (getRegisteredDevices()) {
			if (regDevNum != 0) { // Any devices already registered?
				for (int i=0; i<regDevNum; i++) {
					root[String(i)] = regAndroidIds[i];
				}
			}
			root["num"] = regDevNum;
			root["result"] = "success";
		} else {
			root["result"] = "failed";
			root["reason"] = failReason;
		}
		root.printTo(jsonString);
		s += jsonString;
		httpClient.print(s);
		httpClient.flush();
		httpClient.stop();
		delay(1000);
		return;
	// Delete one or all registered device
	} else if (req.substring(0, 3) == "/?d"){
		/** String for the sub command */
		String delReq = req.substring(3,4);
		if (delReq == "a") { // Delete all registered devices
			if (delRegisteredDevice()) {
				root["result"] = "success";
			} else {
				root["result"] = "failed";
				root["reason"] = failReason;
			}
		} else if (delReq == "i") {
			/** String to hold the ID that should be deleted */
			String delRegId = req.substring(5,146);
			delRegId.trim();
			if (delRegisteredDevice(delRegId)) {
				root["result"] = "success";
			} else {
				root["result"] = "failed";
				root["reason"] = failReason;
			}
		} else if (delReq == "x") {
			/** Index of the registration ID that should be deleted */
			int delRegIndex = req.substring(5,req.length()).toInt();
			if ((delRegIndex < 0) || (delRegIndex > MAX_DEVICE_NUM-1)) {
				root["result"] = "invalid";
				root["reason"] = "Index out of range";
			} else {
				if (delRegisteredDevice(delRegIndex)) {
					root["result"] = "success";
				} else {
					root["result"] = "failed";
					root["reason"] = failReason;
				}
			}
		}
		// Send list of registered devices
		if (getRegisteredDevices()) {
			if (regDevNum != 0) { // Any devices already registered?
				for (int i=0; i<regDevNum; i++) {
					root[String(i)] = regAndroidIds[i];
				}
			}
			root["num"] = regDevNum;
		}
		root.printTo(jsonString);
		s += jsonString;
		httpClient.print(s);
		httpClient.flush();
		httpClient.stop();
		delay(1000);
		return;
	}

	root["result"] = "failed";
	root["reason"] = "Unknown request";
	root.printTo(jsonString);
	s += jsonString;
	httpClient.print(s);
	httpClient.flush();
	httpClient.stop();
	delay(1000);
}
