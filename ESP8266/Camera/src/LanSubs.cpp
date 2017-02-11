#include <Setup.h>

/** WiFiUDP class for creating UDP communication */
WiFiUDP udpClientServer;

/**
	 sendBroadCast
	 send updated status over LAN
	 by UTP broadcast over local lan
	 @param shotResult
	 	Flag if taking an image was successful
*/
void sendBroadCast(boolean shotResult) {
	comLedFlashStart(0.4);

	DynamicJsonBuffer jsonBuffer;
	// Prepare json object for the response
	JsonObject& root = jsonBuffer.createObject();
	root["de"] = DEVICE_ID;

	// Set flag for restart
	if (inSetup) {
		root["bo"] = 1;
	} else {
		root["bo"] = 0;
	}

	String nowTime = String(hour()) + ":";
	if (minute() < 10) {
		nowTime += "0";
	}
	nowTime += String(minute());
	root["tm"] = nowTime;

	if (shotResult) {
		root["pi"] = 1;
	} else {
		root["pi"] = 0;
	}
	// Broadcast per UTP to LAN
	String broadCast;
	root.printTo(broadCast);
	udpClientServer.beginPacketMulticast(multiIP, 5000, ipAddr);
	udpClientServer.print(broadCast);
	udpClientServer.endPacket();
	udpClientServer.stop();

	comLedFlashStop();
}

/**
	socketServer
	answer request on tcp socket server
	returns status to client
	@param tcpClient
		Instance of WiFiClient that has connected
*/
void socketServer(WiFiClient tcpClient) {
	comLedFlashStart(0.4);

	// Get data from client until he stops the connection or timeout occurs
	long timeoutStart = now();
	String req = "1234567890";
	String cmd;
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
		comLedFlashStop();
		return;
	}

  // Take a picture
  if (req.substring(0, 1) == "t"){
		sendDebug("Request to take a picture", OTA_HOST);
		sendBroadCast(takeShot());
		// Switch on debug output
	} else if (req.substring(0, 1) == "d") {
		debugOn = !debugOn;
		if (debugOn) {
			sendDebug("Debug over TCP is on", OTA_HOST);
		} else {
			sendDebug("Debug over TCP is off", OTA_HOST);
		}
		// Delete saved WiFi configuration
	} else if (req.substring(0, 1) == "r") {
		sendDebug("Delete WiFi credentials and reset device", OTA_HOST);
		wifiManager.resetSettings();
		// Reset the ESP
		delay(3000);
		ESP.reset();
		delay(5000);
	}
	comLedFlashStop();
}
