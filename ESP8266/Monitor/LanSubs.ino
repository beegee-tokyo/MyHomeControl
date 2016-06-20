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
		if (connectTimeout > 60) { // Wait for 30 seconds (60 x 500 milliseconds) to reconnect
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
	mqttClient.publish("/SPM", spmStatus);
	mqttClient.publish("/AC1", ac1Status);
	mqttClient.publish("/AC2", ac2Status);
	mqttClient.publish("/WEI", inWeatherStatus);
	mqttClient.publish("/WEO", outWeatherStatus);
	mqttClient.publish("/SEF", secFrontStatus);
	mqttClient.publish("/SEB", secBackStatus);
	digitalWrite(ACT_LED, HIGH); // Turn off red LED
}

/**
	messageReceived
	Called when subscribed message was received from MQTT broker
*/
void messageReceived(String topic, String payload, char * bytes, unsigned int length) {
	digitalWrite(ACT_LED, LOW); // Turn on red LED
	Serial.print("incoming: ");
	Serial.print(topic);
	Serial.print(" - ");
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
		Serial.println("connection to spm " + String(ipSPM[0]) + "." + String(ipSPM[1]) + "." + String(ipSPM[2]) + "." + String(ipSPM[3]) + " failed");
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
		jsonOut["c"] = jsonIn["value"]["C"].as<double>();
		jsonOut["s"] = jsonIn["value"]["S"].as<double>();
		consPower = jsonIn["value"]["C"].as<double>();
		solarPower = jsonIn["value"]["S"].as<double>();
		spmStatus = "";
		jsonOut.printTo(spmStatus);
	}
}

/**
	getAC1Status
	Get current status of office aircon
*/
void getAC1Status() {
	digitalWrite(COM_LED, LOW);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	ac1On = 2; // indicate connection error
	
	ledFlasher.attach(0.1, blueLedFlash); // Flash very fast while we get data
	const int httpPort = 80;

	if (!tcpClient.connect(ipAC1, httpPort)) {
		Serial.println("connection to ac 1 " + String(ipAC1[0]) + "." + String(ipAC1[1]) + "." + String(ipAC1[2]) + "." + String(ipAC1[3]) + " failed");
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
		jsonOut["to"] = jsonIn["onTime"];
		jsonOut["sp"] = jsonIn["speed"];
		jsonOut["te"] = jsonIn["temp"];
		jsonOut["co"] = jsonIn["cons"];
		jsonOut["st"] = jsonIn["status"];
		jsonOut["au"] = jsonIn["auto"];
		ac1Status = "";
		jsonOut.printTo(ac1Status);

		ac1On = jsonOut["po"];
		ac1Mode = jsonOut["mo"];
		ac1Timer = jsonOut["ti"];
		ac1Auto = jsonOut["au"];
	}
}

/**
	getAC2Status
	Get current status of living aircon
*/
void getAC2Status() {
	digitalWrite(COM_LED, LOW);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	ac2On = 2; // indicate connection error
	
	ledFlasher.attach(0.1, blueLedFlash); // Flash very fast while we get data
	const int httpPort = 80;

	if (!tcpClient.connect(ipAC2, httpPort)) {
		Serial.println("connection to ac 2 " + String(ipAC2[0]) + "." + String(ipAC2[1]) + "." + String(ipAC2[2]) + "." + String(ipAC2[3]) + " failed");
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
		ac2Status = "";
		jsonOut.printTo(ac2Status);

		ac2On = jsonOut["po"];
		ac2Mode = jsonOut["mo"];
		ac2Timer = jsonOut["ti"];
		ac2Auto = jsonOut["au"];
	}
}

/**
	getSEFStatus
	Get current status of front yard security
*/
void getSEFStatus() {
	digitalWrite(COM_LED, LOW);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	secFrontOn = 2; // indicate connection error
		
	ledFlasher.attach(0.1, blueLedFlash); // Flash very fast while we get data
	const int httpPort = 80;

	if (!tcpClient.connect(ipSecFront, httpPort)) {
		Serial.println("connection to sec front " + String(ipSecFront[0]) + "." + String(ipSecFront[1]) + "." + String(ipSecFront[2]) + "." + String(ipSecFront[3]) + " failed");
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
		secFrontStatus = "";
		jsonOut.printTo(secFrontStatus);

		secFrontOn = jsonOut["ao"];
		secFrontLight = jsonOut["lo"];
	}
}

/**
	getSERStatus
	Get current status of back yard security
*/
void getSERStatus() {
	digitalWrite(COM_LED, LOW);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	secBackOn = 2; // indicate connection error

	ledFlasher.attach(0.1, blueLedFlash); // Flash very fast while we get data
	const int httpPort = 80;

	if (!tcpClient.connect(ipSecBack, httpPort)) {
		Serial.println("connection to sec back " + String(ipSecBack[0]) + "." + String(ipSecBack[1]) + "." + String(ipSecBack[2]) + "." + String(ipSecBack[3]) + " failed");
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

	// {"device":"sb1","alarm":0,"alarm_on":0,"auto":1,"auto_on":22,"auto_off":8,"light_on":1}
	// Prepare json object for the response
	if (jsonIn.containsKey("device")) {
		jsonOut["de"] = jsonIn["device"];
		jsonOut["al"] = jsonIn["alarm"];
		jsonOut["ao"] = jsonIn["alarm_on"];
		jsonOut["au"] = jsonIn["auto"];
		jsonOut["an"] = jsonIn["auto_on"];
		jsonOut["af"] = jsonIn["auto_off"];
		jsonOut["lo"] = jsonIn["light_on"];
		secBackStatus = "";
		jsonOut.printTo(secBackStatus);

		secBackOn = jsonOut["ao"];
		secBackLight = jsonOut["lo"];
	}
}

/** 
	getUDPbroadcast
	Get UDP broadcast message
*/
void getUDPbroadcast(int udpMsgLength) {
	digitalWrite(COM_LED, LOW);
	digitalWrite(ACT_LED, LOW);
	byte udpPacket[udpMsgLength+1];
	IPAddress udpIP;
	String debugMsg;
	
	udpListener.read(udpPacket, udpMsgLength);
	udpPacket[udpMsgLength] = 0;
	
	debugMsg = "UDP broadcast from ";
	udpIP = udpListener.remoteIP();
	debugMsg += "Sender IP: " + String(udpIP[0]) + "." + String(udpIP[1]) + "." + String(udpIP[2]) + "." + String(udpIP[3]);
	udpIP = udpListener.destinationIP();
	debugMsg += " - Destination IP: " + String(udpIP[0]) + "." + String(udpIP[1]) + "." + String(udpIP[2]) + "." + String(udpIP[3]);
	sendDebug(debugMsg);

	udpListener.flush(); // empty UDP buffer for next packet

	/** Buffer for incoming JSON string */
	DynamicJsonBuffer jsonInBuffer;
	/** Json object for incoming data */
	JsonObject& jsonIn = jsonInBuffer.parseObject((char *)udpPacket);
	if (!jsonIn.success()) {
		Serial.println("parseObject() failed");
		digitalWrite(COM_LED, HIGH);
		digitalWrite(ACT_LED, HIGH);
		sendDebug("Invalid JSON");
		return;
	}
	
	if (!jsonIn.containsKey("device")) {
		sendDebug("Missing key device in JSON");
		return;
	}
	
	String device = jsonIn["device"];
	
	if ((device == "fd1") || (device == "ca1")) { // Broadcast from aircon
		parseACpacket(jsonIn, device);
		updateAC(false);
	}
	if (device == "sf1") { // Broadcast from front security
		parseSecFrontPacket(jsonIn);
		updateSecurity(false);
	}
	if (device == "sb1") { // Broadcast from back yard security
		parseSecBackPacket(jsonIn);
		updateSecurity(false);
	}
	if (device == "spm") { // Broadcast from solar panel monitor
		parseSPMPacket(jsonIn);
		updateSolar(false);
	}
	digitalWrite(COM_LED, HIGH);
	digitalWrite(ACT_LED, HIGH);
}

/**
	parseACpacket
	Parse aircon status packet
*/
void parseACpacket (JsonObject& jsonIn, String device) {
	/** Buffer for outgoing JSON string */
	DynamicJsonBuffer jsonOutBuffer;
	/** Json object for outgoing data */
	JsonObject& jsonOut = jsonOutBuffer.createObject();

	// {"result":"success","device":"fd1","power":1,"mode":2,"timer":0,"speed":0,"temp":16,"cons":471.95,"status":0,"auto":0,"build":"May  2 2016 11:44:34"}
	// Prepare json object for the response
	jsonOut["de"] = jsonIn["device"];
	jsonOut["po"] = jsonIn["power"];
	jsonOut["mo"] = jsonIn["mode"];
	jsonOut["ti"] = jsonIn["timer"];
	jsonOut["to"] = jsonIn["onTime"];
	jsonOut["sp"] = jsonIn["speed"];
	jsonOut["te"] = jsonIn["temp"];
	jsonOut["co"] = jsonIn["cons"];
	jsonOut["st"] = jsonIn["status"];
	jsonOut["au"] = jsonIn["auto"];
	if (device == "fd1") {
		ac1Status = "";
		jsonOut.printTo(ac1Status);

		ac1On = jsonOut["po"];
		ac1Mode = jsonOut["mo"];
		ac1Timer = jsonOut["ti"];
		ac1Auto = jsonOut["au"];
	} else if (device == "ca1") {
		ac2Status = "";
		jsonOut.printTo(ac2Status);

		ac2On = jsonOut["po"];
		ac2Mode = jsonOut["mo"];
		ac2Timer = jsonOut["ti"];
		ac2Auto = jsonOut["au"];
	}
}

/**
	parseSecFrontPacket
	Parse front security status packet
*/
void parseSecFrontPacket (JsonObject& jsonIn) {
	/** Buffer for outgoing JSON string */
	DynamicJsonBuffer jsonOutBuffer;
	/** Json object for outgoing data */
	JsonObject& jsonOut = jsonOutBuffer.createObject();

	// {"device":"sf1","alarm":0,"alarm_on":0,"auto":1,"auto_on":22,"auto_off":8,"light_on":1}
	// Prepare json object for the response
	jsonOut["de"] = jsonIn["device"];
	jsonOut["al"] = jsonIn["alarm"];
	jsonOut["ao"] = jsonIn["alarm_on"];
	jsonOut["au"] = jsonIn["auto"];
	jsonOut["an"] = jsonIn["auto_on"];
	jsonOut["af"] = jsonIn["auto_off"];
	jsonOut["lo"] = jsonIn["light_on"];
	secFrontStatus = "";
	jsonOut.printTo(secFrontStatus);

	secFrontOn = jsonOut["ao"];
	secFrontLight = jsonOut["lo"];
}

/**
	parseSecBackPacket
	Parse back security status packet
*/
void parseSecBackPacket (JsonObject& jsonIn) {
	/** Buffer for outgoing JSON string */
	DynamicJsonBuffer jsonOutBuffer;
	/** Json object for outgoing data */
	JsonObject& jsonOut = jsonOutBuffer.createObject();

	// {"device":"sf1","alarm":0,"alarm_on":0,"auto":1,"auto_on":22,"auto_off":8,"light_on":1}
	// Prepare json object for the response
	jsonOut["de"] = jsonIn["device"];
	jsonOut["al"] = jsonIn["alarm"];
	jsonOut["ao"] = jsonIn["alarm_on"];
	jsonOut["au"] = jsonIn["auto"];
	jsonOut["an"] = jsonIn["auto_on"];
	jsonOut["af"] = jsonIn["auto_off"];
	jsonOut["lo"] = jsonIn["light_on"];
	secBackStatus = "";
	jsonOut.printTo(secBackStatus);

	secBackOn = jsonOut["ao"];
	secBackLight = jsonOut["lo"];
}

/**
	parseSPMPacket
	Parse solar panel monitor status packet
*/
void parseSPMPacket (JsonObject& jsonIn) {
	/** Buffer for outgoing JSON string */
	DynamicJsonBuffer jsonOutBuffer;
	/** Json object for outgoing data */
	JsonObject& jsonOut = jsonOutBuffer.createObject();

	// {"device":"spm","s":100,"c":300}
	// Get solar production and house consumption values
	jsonOut["de"] = "spm";
	jsonOut["c"] = jsonIn["c"].as<double>();
	jsonOut["s"] = jsonIn["s"].as<double>();
	consPower = jsonIn["c"].as<double>();
	solarPower = jsonIn["s"].as<double>();
	spmStatus = "";
	jsonOut.printTo(spmStatus);
}

// For debug over TCP
void sendDebug(String debugMsg) {
#ifdef DEBUG_OUT
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	const int httpPort = 9999;
	if (!tcpClient.connect(debugIP, httpPort)) {
		Serial.println("connection to Debug PC " + String(debugIP[0]) + "." + String(debugIP[1]) + "." + String(debugIP[2]) + "." + String(debugIP[3]) + " failed");
		tcpClient.stop();
		digitalWrite(COM_LED, HIGH);
		return;
	}

	String sendMsg = host;
	debugMsg = sendMsg + " " + debugMsg;
	tcpClient.print(debugMsg);

	tcpClient.stop();
#endif
}
