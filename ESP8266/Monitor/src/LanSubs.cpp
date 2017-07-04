#include "Setup.h"

/** WiFiUDP class for creating UDP communication */
WiFiUDP udpClientServer;

/** Status of the solar panel */
String spmStatus = "";
/** Status of the office aircon */
String ac1Status = "";
/** Status of the living aircon */
String ac2Status = "";
/** Status of the front yard security */
String secFrontStatus = "";
/** Status of the back yard security */
String secBackStatus = "";
/** Status of the security camera */
String secCamStatus = "";
/** Status of the bedroom light */
String secLightStatus = "";

// Timekeepers for last received status message
/** Time in seconds since last status from AC1 */
int lastAC1status = 0;
/** Time in seconds since last status from AC2 */
int lastAC2status = 0;
/** Time in seconds since last status from SEF */
int lastSEFstatus = 0;
/** Time in seconds since last status from SEB */
int lastSEBstatus = 0;

/**
	sendToMQTT
	Send values to MQTT broker
*/
void sendToMQTT() {
	doubleLedFlashStart(0.1);
	if (debugOn) {
		String debugMsg = "Prepare to send topics:";
		sendDebug(debugMsg, OTA_HOST);
		debugMsg = "/SPM length:" + String(spmStatus.length()) + " - " + spmStatus;
		sendDebug(debugMsg, OTA_HOST);
		debugMsg = "/AC1 length:" + String(ac1Status.length()) + " - " + ac1Status;
		sendDebug(debugMsg, OTA_HOST);
		debugMsg = "/AC2 length:" + String(ac2Status.length()) + " - " + ac2Status;
		sendDebug(debugMsg, OTA_HOST);
		debugMsg = "/WEI length:" + String(inWeatherStatus.length()) + " - " + inWeatherStatus;
		sendDebug(debugMsg, OTA_HOST);
		debugMsg = "/WEO length:" + String(outWeatherStatus.length()) + " - " + outWeatherStatus;
		sendDebug(debugMsg, OTA_HOST);
		debugMsg = "/SEF length:" + String(secFrontStatus.length()) + " - " + secFrontStatus;
		sendDebug(debugMsg, OTA_HOST);
		debugMsg = "/SEB length:" + String(secBackStatus.length()) + " - " + secBackStatus;
		sendDebug(debugMsg, OTA_HOST);
		debugMsg = "/CM1 length:" + String(secCamStatus.length()) + " - " + secCamStatus;
		sendDebug(debugMsg, OTA_HOST);
		debugMsg = "/LB1 length:" + String(secLightStatus.length()) + " - " + secLightStatus;
		sendDebug(debugMsg, OTA_HOST);
	}

  MQTTMessage mqttMsg;
  mqttMsg.retained = true;
  char * charPayload;

	if (spmStatus.length() != 0) {
    mqttMsg.topic = (char *)"/SPM";
    mqttMsg.payload = (char *)&spmStatus[0];
    mqttMsg.length = spmStatus.length();
    mqttClient.publish(&mqttMsg);
	}
	if (ac1Status.length() != 0) {
    mqttMsg.topic = (char *)"/AC1";
    mqttMsg.payload = (char *)&ac1Status[0];
    mqttMsg.length = ac1Status.length();
    mqttClient.publish(&mqttMsg);
	}
	if ((ac1On != 2) && (elapsedSecsToday(now()) - lastAC1status >= 600)) { // more than 10 minutes since last updae
		ac1Status = "";
		ac1On = 2;
	}
	if (ac2Status.length() != 0) {
    mqttMsg.topic = (char *)"/AC2";
    mqttMsg.payload = (char *)&ac2Status[0];
    mqttMsg.length = ac2Status.length();
    mqttClient.publish(&mqttMsg);
	}
	if ((ac2On != 2) && (elapsedSecsToday(now()) - lastAC2status >= 600)) { // more than 10 minutes since last updae
		ac2Status = "";
		ac2On = 2;
	}
	if (inWeatherStatus.length() != 0) {
    mqttMsg.topic = (char *)"/WEI";
    mqttMsg.payload = (char *)&inWeatherStatus[0];
    mqttMsg.length = inWeatherStatus.length();
    mqttClient.publish(&mqttMsg);
		sendWEIBroadCast(inWeatherStatus);
	}
	if (outWeatherStatus.length() != 0) {
    mqttMsg.topic = (char *)"/WEO";
    mqttMsg.payload = (char *)&outWeatherStatus[0];
    mqttMsg.length = outWeatherStatus.length();
    mqttClient.publish(&mqttMsg);
	}
	if (secFrontStatus.length() != 0) {
    mqttMsg.topic = (char *)"/SEF";
    mqttMsg.payload = (char *)&secFrontStatus[0];
    mqttMsg.length = secFrontStatus.length();
    mqttClient.publish(&mqttMsg);
	}
	if ((secFrontOn != 2) && (elapsedSecsToday(now()) - lastSEFstatus >= 600)) { // more than 10 minutes since last updae
		secFrontStatus = "";
		secFrontOn = 2;
	}
	if (secBackStatus.length() != 0) {
    mqttMsg.topic = (char *)"/SEB";
    mqttMsg.payload = (char *)&secBackStatus[0];
    mqttMsg.length = secBackStatus.length();
    mqttClient.publish(&mqttMsg);
	}
	if ((secBackOn != 2) && (elapsedSecsToday(now()) - lastSEBstatus >= 600)) { // more than 10 minutes since last updae
		secBackStatus = "";
		secBackOn = 2;
	}
	if (secCamStatus.length() != 0) {
    mqttMsg.topic = (char *)"/CM1";
    mqttMsg.payload = (char *)&secCamStatus[0];
    mqttMsg.length = secCamStatus.length();
    mqttClient.publish(&mqttMsg);
	}
	if (secLightStatus.length() != 0) {
    mqttMsg.topic = (char *)"/LB1";
    mqttMsg.payload = (char *)&secLightStatus[0];
    mqttMsg.length = secLightStatus.length();
    mqttClient.publish(&mqttMsg);
	}

	spmStatus = "";
	inWeatherStatus = "";
	outWeatherStatus = "";
	secCamStatus = "";
	doubleLedFlashStop();
}

/**
 * Answer request on tcp socket server
 * Commands:
 *		d to enable debugging over TCP
 *		r to reset saved WiFi configuration
 *		x to reset the device
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

	// Enable debugging
	if (req.substring(0, 1) == "d") {
		// toggle debug flag
		debugOn = !debugOn;
		if (debugOn) {
			sendDebug("Debug over TCP is on", OTA_HOST);
		} else {
			sendDebug("Debug over TCP is off", OTA_HOST);
		}

		return;
		// Delete saved WiFi configuration
	} else if (req.substring(0, 1) == "r") {
		sendDebug("Delete WiFi credentials and reset device", OTA_HOST);
		wifiManager.resetSettings();
		// Reset the ESP
		delay(3000);
		ESP.reset();
		delay(5000);
		return;
		// Reset device
	} else if (req.substring(0, 1) == "x") {
		sendDebug("Reset device", OTA_HOST);
		tcpClient.flush();
		tcpClient.stop();
		// Reset the ESP
		delay(3000);
		ESP.reset();
		delay(5000);
	}
}
/**
	messageReceived
	Called when subscribed message was received from MQTT broker
*/
void messageReceived(String topic, String payload, char * bytes, unsigned int length) {
	actLedFlashStart(0.2);

	if (debugOn) {
		String debugMsg = "incoming: " + topic + " message " + payload;
		sendDebug(debugMsg, OTA_HOST);
	}

	/** Buffer for incoming JSON string */
	DynamicJsonBuffer jsonInBuffer;
	/** Char buffer for incoming data */
	char json[payload.length()];
	payload.toCharArray(json, payload.length() + 1);
	/** Json object for incoming data */
	JsonObject& jsonIn = jsonInBuffer.parseObject(json);
	if (!jsonIn.success()) {
		if (debugOn) {
			String debugMsg = "incoming: " + topic + " message " + payload + " parseObject() failed";
			sendDebug(debugMsg, OTA_HOST);
		}
		actLedFlashStop();
		return;
	}

	String deviceID = "";
	String deviceCMD = "";
	if (!jsonIn.containsKey("ip")) {
		if (debugOn) {
			String debugMsg = "incoming: " + topic + " missing deviceID";
			sendDebug(debugMsg, OTA_HOST);
		}
		actLedFlashStop();
		return;
	} else {
		deviceID = jsonIn["ip"].as<String>();
		if (!jsonIn.containsKey("cm")) {
			if (debugOn) {
				String debugMsg = "incoming: " + topic + " missing command";
				sendDebug(debugMsg, OTA_HOST);
			}
			actLedFlashStop();
			return;
		} else {
			deviceCMD = jsonIn["cm"].as<String>();
		}
	}

	// IP address of target device
	IPAddress deviceIP;
	// Check if deviceID or deviceCMD are empty
	if ((deviceID == "") || (deviceCMD == "")) {
		actLedFlashStop();
		return;
	} else if (deviceID == "sf1") { // front security
		deviceIP = ipSecFront;
	} else if (deviceID == "sb1") { // back security
		deviceIP = ipSecBack;
	} else if (deviceID == "fd1") { // office aircon
		deviceIP = ipAC1;
	} else if (deviceID == "ca1") { // living room aircon
		deviceIP = ipAC2;
	} else if (deviceID == "cm1") { // front camera
		deviceIP = ipCam1;
	} else if (deviceID == "lb1") { // bedroom lights
		deviceIP = ipBedLight;
	} else {
		actLedFlashStop();
		return;
	}
	if (debugOn) {
		String debugMsg = "sending command " + deviceCMD + " to device " + deviceID + " at IP ";
		debugMsg +=  String(deviceIP[0]) + "." + String(deviceIP[1]) + "." + String(deviceIP[2]) + "." + String(deviceIP[3]);
		sendDebug(debugMsg, OTA_HOST);
	}
	sendCmd(deviceIP, deviceCMD);
	actLedFlashStop();
}

/**
	getSPMStatus
	Get current power consumption from spMonitor device on address ipSPM
*/
void getSPMStatus() {
	comLedFlashStart(0.2);

	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	const int httpPort = 80;
	if (!tcpClient.connect(ipSPM, httpPort)) {
		if (debugOn) {
			String debugMsg = "connection to SPM " + String(ipSPM[0]) + "." + String(ipSPM[1]) + "." + String(ipSPM[2]) + "." + String(ipSPM[3]) + " failed";
			sendDebug(debugMsg, OTA_HOST);
		}
		tcpClient.stop();
		comLedFlashStop();
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
			comLedFlashStop();
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
		if (debugOn) {
			String debugMsg = "spm data parseObject() failed: " + String(json);
			sendDebug(debugMsg, OTA_HOST);
		}
		comLedFlashStop();
		return;
	}
	comLedFlashStop();

	// Get solar production and house consumption values
	if (jsonIn.containsKey("value")) {
		jsonOut["de"] = "spm"; //jsonOut["device"] = "spm";
		jsonOut["c"] = jsonIn["value"]["C"].as<double>();
		jsonOut["s"] = jsonIn["value"]["S"].as<double>();
		consPower = jsonIn["value"]["C"].as<double>();
		solarPower = jsonIn["value"]["S"].as<double>();
		spmStatus = "";
		jsonOut.printTo(spmStatus);
	}
}

/**
	sendCmd
	Request current status of a device
	Returns true if request could be sent or false if connection failed
*/
void sendCmd(IPAddress serverIP, String deviceCmd) {
	comLedFlashStart(0.2);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	if (!tcpClient.connect(serverIP, tcpComPort)) {
		if (debugOn) {
			String debugMsg = "connection to " + String(serverIP[0]) + "." + String(serverIP[1]) + "." + String(serverIP[2]) + "." + String(serverIP[3]) + " failed";
			sendDebug(debugMsg, OTA_HOST);
		}
		tcpClient.stop();
		comLedFlashStop();
		return;
	}

	tcpClient.print(deviceCmd);

	tcpClient.stop();
	comLedFlashStop();

}

/**
	getUDPbroadcast
	Get UDP broadcast message
*/
void getUDPbroadcast(int udpMsgLength) {
	doubleLedFlashStart(0.1);
	byte udpPacket[udpMsgLength+1];
	IPAddress udpIP;

	udpListener.read(udpPacket, udpMsgLength);
	udpPacket[udpMsgLength] = 0;

	if (debugOn) {
		String debugMsg = "UDP broadcast from ";
		udpIP = udpListener.remoteIP();
		debugMsg += "Sender IP: " + String(udpIP[0]) + "." + String(udpIP[1]) + "." + String(udpIP[2]) + "." + String(udpIP[3]);
		udpIP = udpListener.destinationIP();
		debugMsg += " - Destination IP: " + String(udpIP[0]) + "." + String(udpIP[1]) + "." + String(udpIP[2]) + "." + String(udpIP[3]);
		sendDebug(debugMsg, OTA_HOST);
	}

	udpListener.flush(); // empty UDP buffer for next packet

	/** Buffer for incoming JSON string */
	DynamicJsonBuffer jsonInBuffer;
	/** Json object for incoming data */
	JsonObject& jsonIn = jsonInBuffer.parseObject((char *)udpPacket);
	if (!jsonIn.success()) {
		if (debugOn) {
			String debugMsg = "Invalid Json: " + String((char *)udpPacket);
			sendDebug(debugMsg, OTA_HOST);
		}
		doubleLedFlashStop();
		return;
	}

	if (!jsonIn.containsKey("de")) { //if (!jsonIn.containsKey("device")) {
		if (debugOn) {
			sendDebug("Missing key device in JSON", OTA_HOST);
		}
		doubleLedFlashStop();
		return;
	}

	String device = jsonIn["de"]; //String device = jsonIn["device"];

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
	if (device == "cm1") { // Broadcast from security camera
		parseCAMPacket(jsonIn);
	}
	if (device == "lb1") { // Broadcast from bedroom lights
		parseLightPacket(jsonIn);
	}
	doubleLedFlashStop();
}

/**
	parseACpacket
	Parse aircon status packet
*/
void parseACpacket (JsonObject& jsonIn, String device) {
	// jsonIn.remove("result");
	jsonIn.remove("bo"); //jsonIn.remove("boot");
	jsonIn.remove("dt"); //jsonIn.remove("daytime");
	jsonIn.remove("tm"); //jsonIn.remove("time");
	if (device == "fd1") {
		ac1Status = "";
		jsonIn.printTo(ac1Status);

		ac1On = jsonIn["po"]; //ac1On = jsonIn["power"];
		ac1Mode = jsonIn["mo"]; //ac1Mode = jsonIn["mode"];
		ac1Timer = jsonIn["ti"]; //ac1Timer = jsonIn["timer"];
		ac1Auto = jsonIn["au"]; //ac1Auto = jsonIn["auto"];
		ac1Speed = jsonIn["sp"]; //ac1Speed = jsonIn["speed"];
		ac1Temp = jsonIn["te"]; //ac1Temp = jsonIn["temp"];
		lastAC1status = elapsedSecsToday(now());
	} else if (device == "ca1") {
		ac2Status = "";
		jsonIn.printTo(ac2Status);

		ac2On = jsonIn["po"]; //ac2On = jsonIn["power"];
		ac2Mode = jsonIn["mo"]; //ac2Mode = jsonIn["mode"];
		ac2Timer = jsonIn["ti"]; //ac2Timer = jsonIn["timer"];
		ac2Auto = jsonIn["au"]; //ac2Auto = jsonIn["auto"];
		ac2Speed = jsonIn["sp"]; //ac2Speed = jsonIn["speed"];
		ac2Temp = jsonIn["te"]; //ac2Temp = jsonIn["temp"];
		lastAC2status = elapsedSecsToday(now());
	}
	// statusUpdated = true;
}

/**
	parseSecFrontPacket
	Parse front security status packet
*/
void parseSecFrontPacket (JsonObject& jsonIn) {
	secFrontStatus = "";
	jsonIn.printTo(secFrontStatus);

	secFrontOn = jsonIn["ao"]; //secFrontOn = jsonIn["alarm_on"];
	secFrontLight = jsonIn["lo"]; //secFrontLight = jsonIn["light_on"];
	secFrontAuto = jsonIn["au"]; //secFrontAuto = jsonIn["auto"];
	secFrontOnTime = jsonIn["an"]; //secFrontOnTime = jsonIn["auto_on"];
	secFrontOffTime = jsonIn["af"]; //secFrontOffTime = jsonIn["auto_off"];
	lastSEFstatus = elapsedSecsToday(now());
	// statusUpdated = true;
}

/**
	parseSecBackPacket
	Parse back security status packet
*/
void parseSecBackPacket (JsonObject& jsonIn) {
	secBackOn = jsonIn["ao"]; //secBackOn = jsonIn["alarm_on"];
	secBackLight = jsonIn["lo"]; //secBackLight = jsonIn["light_on"];
	secBackAuto = jsonIn["au"]; //secBackAuto = jsonIn["auto"];
	secBackOnTime = jsonIn["an"]; //secBackOnTime = jsonIn["auto_on"];
	secBackOffTime = jsonIn["af"]; //secBackOffTime = jsonIn["auto_off"];
	tempOutside = jsonIn["te"]; //tempOutside = jsonIn["temp"];
	humidOutside = jsonIn["hu"]; //humidOutside = jsonIn["humid"];
	heatIndexOut = jsonIn["he"]; //heatIndexOut = jsonIn["heat"];

	jsonIn.remove("te"); //jsonIn.remove("temp");
	jsonIn.remove("hu"); //jsonIn.remove("humid");
	jsonIn.remove("he"); //jsonIn.remove("heat");
	secBackStatus = "";

	jsonIn.printTo(secBackStatus);

	lastSEBstatus = elapsedSecsToday(now());
	// statusUpdated = true;
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

	// Get solar production and house consumption values
	jsonOut["de"] = "spm"; //jsonOut["device"] = "spm";
	jsonOut["c"] = jsonIn["c"].as<double>();
	jsonOut["s"] = jsonIn["s"].as<double>();
	consPower = jsonIn["c"].as<double>();
	solarPower = jsonIn["s"].as<double>();
	spmStatus = "";
	jsonOut.printTo(spmStatus);
	// statusUpdated = true;
}

/**
	parseCAMPacket
	Parse security camera status packet
*/
void parseCAMPacket (JsonObject& jsonIn) {
	secCamStatus = "";
	jsonIn.printTo(secCamStatus);
}

/**
	parseLightPacket
	Parse bedroom light status packet
*/
void parseLightPacket (JsonObject& jsonIn) {
	secLightStatus = "";
	jsonIn.printTo(secLightStatus);
}

/**
	 sendWEIBroadCast
	 send updated inside weather status over LAN
	 - by UTP broadcast over local lan
*/
void sendWEIBroadCast(String broadCast) {
	if (debugOn) {
		String debugMsg = "Sending broadcast: " + broadCast;
		sendDebug(debugMsg, OTA_HOST);
	}
	// Broadcast per UTP to LAN
	udpClientServer.beginPacketMulticast(multiIP, udpBcPort, ipAddr);
	udpClientServer.print(broadCast);
	udpClientServer.endPacket();
	udpClientServer.stop();
}
