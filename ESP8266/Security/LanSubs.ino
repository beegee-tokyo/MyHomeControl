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
		// case WIFI_EVENT_STAMODE_CONNECTED:
			// Serial.println("WiFiEvent: WiFi connected");
			// break;
		case WIFI_EVENT_STAMODE_DISCONNECTED:
			Serial.println("WiFiEvent: WiFi lost connection");
			if (!wifiConnecting && !otaRunning) {
				connectWiFi();
			}
			break;
		// case WIFI_EVENT_STAMODE_AUTHMODE_CHANGE:
			// Serial.println("WiFiEvent: WiFi authentication mode changed");
			// break;
		// case WIFI_EVENT_STAMODE_GOT_IP:
			// Serial.println("WiFiEvent: WiFi got IP");
			// Serial.println("IP address: ");
			// Serial.println(WiFi.localIP());
			// break;
		// case WIFI_EVENT_STAMODE_DHCP_TIMEOUT:
			// Serial.println("WiFiEvent: WiFi DHCP timeout");
			// break;
		// case WIFI_EVENT_MAX:
			// Serial.println("WiFiEvent: WiFi MAX event");
			// break;
	}
}

/**
 * Send broadcast message over UDP into local network
 *
 * @param makeShort
 *              If true send short status, else send long status
 */
void sendAlarm(boolean makeShort) {
	digitalWrite(comLED, LOW);
	/** Buffer for Json object */
	DynamicJsonBuffer jsonBuffer;

	// Prepare json object for the response
	/* Json object with the alarm message */
	JsonObject& root = jsonBuffer.createObject();

	Serial.println("Create status");
	// Create status
	createStatus(root, makeShort);

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
	String broadCast;
	root.printTo(broadCast);
	udpClientServer.print(broadCast);
	udpClientServer.endPacket();
	udpClientServer.stop();

	udpClientServer.beginPacket(monitorIP,5000);
	udpClientServer.print(broadCast);
	udpClientServer.endPacket();
	udpClientServer.stop();

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
 *
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
		sendAlarm(true);
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

/**
 * Answer request on tcp socket server
 * Commands: 
 * 		a=0 to switch off alarm
 *		a=1 to switch on alarm
 * 		a=2 to switch on the defined hour on/off alarm
 * 		a=3 to switch off the defined hour on/off alarm
 * 		a=4 to switch on automatic light  
 * 		a=5 to switch off automatic light  
 *		s   to get short status message
 *		p   to switch on alarm sound (panic button function)
 *		i   to get detailed status information
 *		b   to switch on the lights for 2 minutes
 *
 * @param httpClient
 *              Connected WiFi client
 */
void socketServer(WiFiClient tcpClient) {

	// Get data from client until he stops the connection or timeout occurs
	long timeoutStart = now();
	String req = "123456789012345";
	char inByte;
	byte index = 0;
	while (tcpClient.connected()) {
		if (tcpClient.available()) {
			inByte = tcpClient.read();
			req[index] = inByte;
			index++;
		}
		if (now() > timeoutStart + 3000) { // Wait a maximum of 3 seconds
			break; // End the while loop because of timeout
		}
	}

	req[index] = 0;
	
	tcpClient.flush();
	tcpClient.stop();
	if (req.length() < 1) { // No data received
		return;
	}

	// Switch on/off the alarm
	if (req.substring(0, 2) == "a=") {
		if (req.substring(2, 3) == "0") { // Alarm off
			alarmOn = false;
			ledFlasher.detach();
			digitalWrite(alarmLED, HIGH);
		} else if (req.substring(2, 3) == "1") { // Alarm on
			alarmOn = true;
			ledFlasher.attach(1, redLedFlash);
		} else if (req.substring(2, 3) == "2") { // Alarm auto
			if (req.substring(3, 4) == "," 
					&& req.substring(6, 7) == ",") {
				int timeIn = req.substring(4, 6).toInt(); // Get activation time
				if (timeIn >= 1 && timeIn <= 24) {
					if (timeIn == 24) {
						timeIn = 0;
					}
					autoActivOn = timeIn;
				} else {
					autoActivOn = 22;
				}
				timeIn = req.substring(7, 9).toInt(); // Get deactivation time
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
			}
		} else if (req.substring(2, 3) == "3") { // Alarm auto off
			hasAutoActivation = false;
		} else if (req.substring(2, 3) == "4") { // Auto lights on
			switchLights = true;
		} else if (req.substring(2, 3) == "5") { // Auto lights off
			switchLights = false;
		}
		// Send back status over UDP
		sendAlarm(true);
		return;
		// Request short status
	} else if (req.substring(0, 1) == "s") {
		// Send back status over UDP
		sendAlarm(true);
		return;
		// PANIC!!!! set the alarm off
	} else if (req.substring(0, 1) == "p") {
		if (panicOn) {
			alarmTimer.detach();
			analogWrite(speakerPin, LOW); // Switch off speaker
			panicOn = false;
		} else {
			melodyPoint = 0; // Reset melody pointer to 0
			alarmTimer.attach_ms(melodyTuneTime, playAlarmSound);
			panicOn = true;
		}
		// Send back status over UDP
		sendAlarm(true);
		return;
		// Request long status
	} else if (req.substring(0, 1) == "i") {
	
		// Send back long status over UDP
		sendAlarm(false);
		return;
	// Switch lights on for 2 minutes
	} else if (req.substring(0, 1) == "b") {
		// Switch on lights for 2 minutes
		offDelay = 0;
		relayOffTimer.attach(1, relayOff);
		digitalWrite(relayPort, HIGH);
		// Send back status over UDP
		sendAlarm(true);
		return;
	}
}

/**
	sendLightStatus
	Send auto light on/off command to backyard security module
*/
void sendLightStatus(boolean switchOn) {
	digitalWrite(comLED, LOW);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;
	/** IP address of backyard security module */
	IPAddress ipSecB IPSECB;

	ledFlasher.attach(0.1, blueLedFlash); // Flash very fast while we get data
	const int httpPort = 6000;
	if (!tcpClient.connect(ipSecB, httpPort)) {
		Serial.println("connection to backyard security " + String(ipSecB[0]) + "." + String(ipSecB[1]) + "." + String(ipSecB[2]) + "." + String(ipSecB[3]) + " failed");
		tcpClient.stop();
		ledFlasher.detach();
		digitalWrite(comLED, HIGH);
		return;
	}

	if (switchOn) {
		tcpClient.print("a=4");
	} else {
		tcpClient.print("a=5");
	}

	tcpClient.flush();
	tcpClient.stop();
	// String line = "";
	// int waitTimeOut = 0;
	// while (tcpClient.connected()) {
		// line = tcpClient.readStringUntil('\r');
		// delay(1);
		// waitTimeOut++;
		// if (waitTimeOut > 2000) { // If no more response for 2 seconds return
			// ledFlasher.detach();
			// digitalWrite(comLED, HIGH);
			// return;
		// }
	// }
	// tcpClient.stop();
	ledFlasher.detach();
	digitalWrite(comLED, HIGH);
}

// For debug over TCP
void sendDebug(String debugMsg) {
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	const int httpPort = 9999;
	if (!tcpClient.connect(debugIP, httpPort)) {
		Serial.println("connection to Debug PC " + String(debugIP[0]) + "." + String(debugIP[1]) + "." + String(debugIP[2]) + "." + String(debugIP[3]) + " failed");
		tcpClient.stop();
		return;
	}

	String sendMsg = host;
	debugMsg = sendMsg + " " + debugMsg;
	tcpClient.print(debugMsg);

	tcpClient.stop();
}

