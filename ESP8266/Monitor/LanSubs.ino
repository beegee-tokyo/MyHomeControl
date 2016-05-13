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
			delay(60000); // Wait for a minute before retry
			WiFi.disconnect();
			WiFi.mode(WIFI_STA);
			WiFi.config(ipAddr, ipGateWay, ipSubNet);
			WiFi.begin(ssid, password);
		}
	}
	Serial.println(".");
	digitalWrite(COM_LED, HIGH); // Turn off LED
}

/**
	sendToMQTT
	Send values to MQTT broker
*/
void sendToMQTT() {
	digitalWrite(ACT_LED, LOW); // Turn on red LED
	// mqttClient.publish("/SPM", cryptMessage(spmStatus), spmStatus.length());
	// mqttClient.publish("/AC1", cryptMessage(ac1Status), ac1Status.length());
	// mqttClient.publish("/WEA", cryptMessage(weatherStatus), weatherStatus.length());
	// mqttClient.publish("/SEC", cryptMessage(securityStatus), securityStatus.length());
	mqttClient.publish("/SPM", spmStatus);
	mqttClient.publish("/AC1", ac1Status);
	mqttClient.publish("/WEA", weatherStatus);
	mqttClient.publish("/SEC", securityStatus);
	digitalWrite(ACT_LED, HIGH); // Turn off red LED
}

/**
	messageReceived
	Called when subsribed message was received from MQTT broker
*/
void messageReceived(String topic, String payload, char * bytes, unsigned int length) {
	digitalWrite(ACT_LED, LOW); // Turn on red LED
	Serial.print("incoming: ");
	Serial.print(topic);
	Serial.print(" - ");
	// Serial.print(cryptMessage(payload));
	Serial.print(payload);
	Serial.println();
	digitalWrite(ACT_LED, HIGH); // Turn off red LED
}

/**
	getSPMStatus
	Get current power consumption from spMonitor device on address ipSPM
*/
void getSPMStatus() {
	digitalWrite(COM_LED, LOW);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	ledFlasher.attach(0.1, blueLedFlash); // Flash very fast while we get data
	const int httpPort = 80;
	if (!tcpClient.connect(ipSPM, httpPort)) {
		Serial.println("connection to time server " + String(ipSPM[0]) + "." + String(ipSPM[1]) + "." + String(ipSPM[2]) + "." + String(ipSPM[3]) + " failed");
		tcpClient.stop();
		ledFlasher.detach();
		digitalWrite(COM_LED, HIGH);
		return;
	}

	tcpClient.print("GET /data/get HTTP/1.0\r\n\r\n");

	String line = "";
	int waitTimeOut = 0;
	while (tcpClient.connected()) {
		line = tcpClient.readStringUntil('\r');
		delay(1);
		waitTimeOut++;
		if (waitTimeOut > 2000) { // If no more response for 2 seconds return
			ledFlasher.detach();
			digitalWrite(COM_LED, HIGH);
			return;
		}
	}
	tcpClient.stop();
	/** Buffer for incoming JSON string */
	DynamicJsonBuffer jsonInBuffer;
	/** Buffer for outgoing JSON string */
	DynamicJsonBuffer jsonOutBuffer;
	/** Char buffer for incoming data */
	char json[line.length()];
	line.toCharArray(json, line.length() + 1);
	/** Json object for incoming data */
	JsonObject& jsonIn = jsonInBuffer.parseObject(json);
	/** Json object for outgoing data */
	JsonObject& jsonOut = jsonOutBuffer.createObject();
	if (!jsonIn.success()) {
		Serial.println("parseObject() failed");
		ledFlasher.detach();
		digitalWrite(COM_LED, HIGH);
		return;
	}
	ledFlasher.detach();
	digitalWrite(COM_LED, HIGH);

	// {"value":{"c":"2.93","L":"0","sv":"219.10","cv":"218.93","cp":"1.17","sa":"252.47","ca":"641.18","cr":"748.54",
	//  "C":"741.61","s":"1.15","S":"249.04","sp":"0.99","l":"0","sr":"246.35"},"response":"get"}
	// Get solar production and house consumption values
	if (jsonIn.containsKey("value")) {
		jsonOut["de"] = "spm";
		jsonOut["co"] = jsonIn["value"]["C"].as<double>();
		jsonOut["so"] = jsonIn["value"]["S"].as<double>();
		consPower = jsonIn["value"]["C"].as<double>();
		solarPower = jsonIn["value"]["S"].as<double>();
		spmStatus = "";
		jsonOut.printTo(spmStatus);
	}
}

/**
	getACStatus
	Get current status of office aircon
*/
void getACStatus() {
	digitalWrite(COM_LED, LOW);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	ledFlasher.attach(0.1, blueLedFlash); // Flash very fast while we get data
	const int httpPort = 80;

	if (!tcpClient.connect(ipAC1, httpPort)) {
		Serial.println("connection to time server " + String(ipAC1[0]) + "." + String(ipAC1[1]) + "." + String(ipAC1[2]) + "." + String(ipAC1[3]) + " failed");
		tcpClient.stop();
		ledFlasher.detach();
		digitalWrite(COM_LED, HIGH);
		return;
	}

	tcpClient.print("GET /?s HTTP/1.0\r\n\r\n");

	String line = "";
	int waitTimeOut = 0;
	while (tcpClient.connected()) {
		line = tcpClient.readStringUntil('\r');
		delay(1);
		waitTimeOut++;
		if (waitTimeOut > 2000) { // If no more response for 2 seconds return
			ledFlasher.detach();
			digitalWrite(COM_LED, HIGH);
			return;
		}
	}
	tcpClient.stop();
	/** Buffer for incoming JSON string */
	DynamicJsonBuffer jsonInBuffer;
	/** Buffer for outgoing JSON string */
	DynamicJsonBuffer jsonOutBuffer;
	/** Char buffer for incoming data */
	char json[line.length()];
	line.toCharArray(json, line.length() + 1);
	/** Json object for incoming data */
	JsonObject& jsonIn = jsonInBuffer.parseObject(json);
	/** Json object for outgoing data */
	JsonObject& jsonOut = jsonOutBuffer.createObject();
	if (!jsonIn.success()) {
		Serial.println("parseObject() failed");
		ledFlasher.detach();
		digitalWrite(COM_LED, HIGH);
		return;
	}
	ledFlasher.detach();
	digitalWrite(COM_LED, HIGH);

	// {"result":"success","device":"fd1","power":1,"mode":2,"timer":0,"speed":0,"temp":16,"cons":471.95,"status":0,"auto":0,"build":"May  2 2016 11:44:34"}
	// Prepare json object for the response
	if (jsonIn.containsKey("device")) {
		jsonOut["de"] = jsonIn["device"];
		jsonOut["po"] = jsonIn["power"];
		jsonOut["mo"] = jsonIn["mode"];
		jsonOut["ti"] = jsonIn["timer"];
		jsonOut["sp"] = jsonIn["speed"];
		jsonOut["te"] = jsonIn["temp"];
		jsonOut["co"] = jsonIn["cons"];
		jsonOut["st"] = jsonIn["status"];
		jsonOut["au"] = jsonIn["auto"];
		ac1Status = "";
		jsonOut.printTo(ac1Status);

		if (jsonOut["po"] == 1) {
			ac1Display = "On ";
		} else {
			ac1Display = "Off ";
		}
		if (jsonOut["mo"] == 2) {
			ac1Display += "Cool ";
		} else if (jsonOut["mo"] == 1) {
			ac1Display += "Dry ";
		} else {
			ac1Display += "Fan ";
		}
		if (jsonOut["ti"] == 1) {
			ac1Display += "Tim";
		} else {
			if (jsonOut["au"] == 1) {
				ac1Display += "Auto";
			} else {
				ac1Display += "Man";
			}
		}
	}
}

/**
	getSECStatus
	Get current status of front yard security
*/
void getSECStatus() {
	digitalWrite(COM_LED, LOW);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	ledFlasher.attach(0.1, blueLedFlash); // Flash very fast while we get data
	const int httpPort = 80;

	if (!tcpClient.connect(ipSecFront, httpPort)) {
		Serial.println("connection to time server " + String(ipSecFront[0]) + "." + String(ipSecFront[1]) + "." + String(ipSecFront[2]) + "." + String(ipSecFront[3]) + " failed");
		tcpClient.stop();
		ledFlasher.detach();
		digitalWrite(COM_LED, HIGH);
		return;
	}

	tcpClient.print("GET /?s HTTP/1.0\r\n\r\n");

	String line = "";
	int waitTimeOut = 0;
	boolean isJson = false;
	while (tcpClient.connected()) {
		char incoming = tcpClient.read();
		if (incoming == '{') {
			isJson = true;
		}
		if (isJson) {
			line += incoming;
		}
		if (incoming == '}') {
			break;
		}
		delay(1);
		waitTimeOut++;
		if (waitTimeOut > 2000) { // If no more response for 2 seconds return
			ledFlasher.detach();
			digitalWrite(COM_LED, HIGH);
			return;
		}
	}
	tcpClient.stop();
	/** Buffer for incoming JSON string */
	DynamicJsonBuffer jsonInBuffer;
	/** Buffer for outgoing JSON string */
	DynamicJsonBuffer jsonOutBuffer;
	/** Char buffer for incoming data */
	char json[line.length()];
	line.toCharArray(json, line.length() + 1);
	/** Json object for incoming data */
	JsonObject& jsonIn = jsonInBuffer.parseObject(json);
	/** Json object for outgoing data */
	JsonObject& jsonOut = jsonOutBuffer.createObject();
	if (!jsonIn.success()) {
		Serial.println("parseObject() failed");
		ledFlasher.detach();
		digitalWrite(COM_LED, HIGH);
		return;
	}
	ledFlasher.detach();
	digitalWrite(COM_LED, HIGH);

	// {"device":"sf1","alarm":0,"alarm_on":0,"auto":1,"auto_on":22,"auto_off":8,"light_on":1}
	// Prepare json object for the response
	if (jsonIn.containsKey("device")) {
		jsonOut["de"] = jsonIn["device"];
		jsonOut["al"] = jsonIn["alarm"];
		jsonOut["ao"] = jsonIn["alarm_on"];
		jsonOut["au"] = jsonIn["auto"];
		jsonOut["an"] = jsonIn["auto_on"];
		jsonOut["af"] = jsonIn["auto_off"];
		jsonOut["lo"] = jsonIn["light_on"];
		securityStatus = "";
		jsonOut.printTo(securityStatus);

		if (jsonOut["ao"] == 1) {
			securityDisplay = "On ";
		} else {
			securityDisplay = "Off ";
		}
		if (jsonOut["lo"] == 1) {
			securityDisplay += "Light on";
		} else {
			securityDisplay += "Light off";
		}
	}
}