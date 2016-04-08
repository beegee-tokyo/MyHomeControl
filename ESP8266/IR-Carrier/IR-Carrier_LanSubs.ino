/**
	connectWiFi
	Connect to WiFi AP
	if no WiFi is found for 60 seconds
	module is restarted
*/
void connectWiFi() {
	digitalWrite(COM_LED, LOW);
	WiFi.disconnect();
	WiFi.mode(WIFI_STA);
	WiFi.config(ipAddr, ipGateWay, ipSubNet);
	WiFi.begin(ssid, password);
	Serial.print("Waiting for WiFi connection ");
	int connectTimeout = 0;
	while (WiFi.status() != WL_CONNECTED) {
		delay(500);
		Serial.print(".");
		connectTimeout++;
		if (connectTimeout > 60) { //Wait for 30 seconds (60 x 500 milliseconds) to reconnect
			// pinMode(16, OUTPUT); // Connected to RST pin
			// digitalWrite(16,LOW); // Initiate reset
			// ESP.reset(); // In case it didn't work
			delay(60); // Wait for a minute before retry
			WiFi.disconnect();
			WiFi.mode(WIFI_STA);
			WiFi.config(ipAddr, ipGateWay, ipSubNet);
			WiFi.begin(ssid, password);
		}
	}
	digitalWrite(COM_LED, HIGH); // Turn off LED
}

/**
	WiFiEvent
	called if there is a change in the WiFi connection
*/
void WiFiEvent(WiFiEvent_t event) {
	Serial.printf("[WiFi-event] event: %d\n", event);

	switch (event) {
		case WIFI_EVENT_STAMODE_CONNECTED:
			Serial.println("WiFi connected");
			break;
		case WIFI_EVENT_STAMODE_DISCONNECTED:
			Serial.println("WiFi lost connection");
			connectWiFi();
			break;
		case WIFI_EVENT_STAMODE_AUTHMODE_CHANGE:
			Serial.println("WiFi authentication mode changed");
			break;
		case WIFI_EVENT_STAMODE_GOT_IP:
			Serial.println("WiFi got IP");
			Serial.println("IP address: ");
			Serial.println(WiFi.localIP());
			break;
		case WIFI_EVENT_STAMODE_DHCP_TIMEOUT:
			Serial.println("WiFi DHCP timeout");
			break;
		case WIFI_EVENT_MAX:
			Serial.println("WiFi MAX event");
			break;
	}
}

/**
	 sendBroadCast
	 send updated status over LAN
	 - to my gcm server for broadcast to
	 		registered Android devices
	 - by UTP broadcast over local lan
*/
void sendBroadCast() {
	digitalWrite(COM_LED, LOW);
	DynamicJsonBuffer jsonBuffer;
	// Prepare json object for the response
	JsonObject& root = jsonBuffer.createObject();
	root["result"] = "fail";
	root["device"] = DEVICE_ID;

	root["result"] = "success";
	// Display status of aircon
	if ((acMode & AC_ON) == AC_ON) {
		root["power"] = 1;
	} else {
		root["power"] = 0;
	}
	byte testMode = acMode & MODE_MASK;
	if (testMode == MODE_FAN) {
		root["mode"] = 0;
	} else if (testMode == MODE_DRY) {
		root["mode"] = 1;
	} else if (testMode == MODE_COOL) {
		root["mode"] = 2;
	} else if (testMode == MODE_AUTO) {
		root["mode"] = 3;
	}
	testMode = acMode & FAN_MASK;
	if (testMode == FAN_LOW) {
		root["speed"] = 0;
	} else if (testMode == FAN_MED) {
		root["speed"] = 1;
	} else if (testMode == FAN_HIGH) {
		root["speed"] = 2;
	}
	testMode = acTemp & TEMP_MASK;
	root["temp"] = testMode;

	// Display power consumption and production values
	// /** Calculate average power consumption of the last 10 minutes */
	// consPower = 0;
	// for (int i = 0; i < 10; i++) {
		// consPower += avgConsPower[i];
	// }
	// consPower = consPower / 10;

	// root["cons"] = consPower;

	// Display power cycle status
	root["status"] = powerStatus;

	// Display status of auto control by power consumption
	if ((acMode & AUTO_ON) == AUTO_ON) {
		root["auto"] = 1;
	} else {
		root["auto"] = 0;
	}
	
	// Display device id
	root["device"] = DEVICE_ID;
	
	// Set flag for restart
	if (inSetup) {
		root["boot"] = 1;
	} else {
		root["boot"] = 0;
	}

	// String message;
	// root.printTo(message);

	// if (tcpClient.connect(myServerName, 80)) {
		// // This will send the request to the server
		// tcpClient.print(String("GET ") + "/device_sendall.php/?message=" + message + " HTTP/1.1\r\n" +
						// "Host: " + myServerName + "\r\n" +
						// "Connection: close\r\n\r\n");
	// } else {
		// Serial.println("connection failed");
	// }
	// tcpClient.stop();
	
	// Broadcast per UTP to LAN
	udpClientServer.beginPacketMulticast(multiIP, 5000, ipAddr);
	root.printTo(udpClientServer);
	udpClientServer.endPacket();
	udpClientServer.stop();

	// Send over Google Cloud Messaging
	gcmSendMsg(root);

	digitalWrite(COM_LED, HIGH);
}

/**
	replyClient
	answer request on http server
	returns command to client
*/
void replyClient(WiFiClient httpClient) {
	digitalWrite(COM_LED, LOW);
	/** Flag for valid command */
	boolean isValidCmd = false;
	/** String for response to client */
	String s = "HTTP/1.1 200 OK\r\nAccess-Control-Allow-Origin: *\r\nContent-Type: application/json\r\n\r\n";
	/** Wait out time for client request */
	int waitTimeOut = 0;
	/** String to hold the response */
	String jsonString;

	/** Buffer for Json object */
	DynamicJsonBuffer jsonBuffer;

	// Prepare json object for the response
	JsonObject& root = jsonBuffer.createObject();
	root["result"] = "fail";
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
			digitalWrite(COM_LED, HIGH);
			return;
		}
	}

	// Read the first line of the request
	String req = httpClient.readStringUntil('\r');
	// Strip leading (GET, PUSH) and trailing (HTTP/1) characters
	req = req.substring(req.indexOf("/"),req.length()-9);
	// Prepare the response
	String statResponse = "fail " + req;
	root["result"] = statResponse;

	if (req.substring(0, 4) == "/?c=") { // command received
		if (req.substring(5, 6) != " ") {
			statResponse = req.substring(4, 6);
			irCmd = statResponse.toInt();
			parseCmd(root);
		} else {
			irCmd = 9999;
		}
	} else if (req.substring(0, 3) == "/?s") { // status request received
		root["result"] = "success";
		// Display status of aircon
		if ((acMode & AC_ON) == AC_ON) {
			root["power"] = 1;
		} else {
			root["power"] = 0;
		}
		byte testMode = acMode & MODE_MASK;
		if (testMode == MODE_FAN) {
			root["mode"] = 0;
		} else if (testMode == MODE_DRY) {
			root["mode"] = 1;
		} else if (testMode == MODE_COOL) {
			root["mode"] = 2;
		} else if (testMode == MODE_AUTO) {
			root["mode"] = 3;
		}
		testMode = acMode & FAN_MASK;
		root["speed"] = testMode;
		if (isInFanMode) {
			root["fan"] = true;
		} else {
			root["fan"] = false;
		}
		testMode = acMode & SWP_MASK;
		if (testMode == SWP_ON) {
			root["sweep"] = 1;
		} else {
			root["sweep"] = 0;
		}
		testMode = acTemp & TUR_MASK;
		if (testMode == TUR_ON) {
			root["turbo"] = 1;
		} else {
			root["turbo"] = 0;
		}
		testMode = acTemp & ION_MASK;
		if (testMode == ION_ON) {
			root["ion"] = 1;
		} else {
			root["ion"] = 0;
		}
		testMode = acTemp & TEMP_MASK;
		root["temp"] = testMode;

		// Display power consumption and production values
		/** Calculate average power consumption of the last 10 minutes */
		// consPower = 0;
		// for (int i = 0; i < 10; i++) {
			// consPower += avgConsPower[i];
		// }
		// consPower = consPower / 10;

		//statResponse += String(consPower, 2) + "W\n";
		// root["cons"] = consPower;

		// Display power cycle status
		root["status"] = powerStatus;

		// Display status of auto control by power consumption
		if ((acMode & AUTO_ON) == AUTO_ON) {
			root["auto"] = 1;
		} else {
			root["auto"] = 0;
		}
		root["build"] = compileDate;
		// Registration of new device
	} else if (req.substring(0, 8) == "/?regid=") {
		/** String to hold the received registration ID */
		String regID = req.substring(8,req.length());
		#ifdef DEBUG_OUT
		Serial.println("RegID: "+regID);
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
	} else if (req.substring(0, 3) == "/?r") { // initialization request received
		irCmd = CMD_INIT_AC;
		root["result"] = "success";
	}
	// Send the response to the client
	root.printTo(jsonString);
	s += jsonString;
	httpClient.print(s);
	httpClient.flush();
	httpClient.stop();

	digitalWrite(COM_LED, HIGH);
}

// For debug over TCP
void sendDebug(String debugMsg) {
	digitalWrite(COM_LED, LOW);
	const int httpPort = 9999;
	if (!tcpClient.connect(debugIP, httpPort)) {
		Serial.println("connection to Debug PC " + String(debugIP[0]) + "." + String(debugIP[1]) + "." + String(debugIP[2]) + "." + String(debugIP[3]) + " failed");
		tcpClient.stop();
		digitalWrite(COM_LED, HIGH);
		return;
	}

	debugMsg = "ca " + debugMsg;
	tcpClient.print(debugMsg);

	tcpClient.stop();
	digitalWrite(COM_LED, HIGH);
}


