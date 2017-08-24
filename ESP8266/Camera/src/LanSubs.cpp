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
		root["fi"] = String(filename);
	} else {
		root["pi"] = 0;
	}
	// Broadcast per UTP to LAN
	String broadCast;
	root.printTo(broadCast);
	if (udpClientServer.beginPacketMulticast(multiIP, udpBcPort, ipAddr) == 0) {
		wmIsConnected = false;
	} else {
		if (udpClientServer.print(broadCast) == 0) {
			wmIsConnected = false;
		}
		udpClientServer.endPacket();
		udpClientServer.stop();
	}

	comLedFlashStop();
}

/**
	socketServer
	answer request on tcp socket server
	returns status to client
	@param tcpClient
		Instance of WiFiClient that has connected
	*		Commands:
	*		t take a picture
	*		r	to reset saved WiFi configuration
  *		x to reset the device
	*		y=YYYY,MM,DD,HH,mm,ss to set time and date
	*		z to format SPIFFS
	*/
void socketServer(WiFiClient tcpClient) {
	comLedFlashStart(0.4);

	// Get data from client until he stops the connection or timeout occurs
	long timeoutStart = now();
	String req = "1234567890123456789012";
	String cmd;
	char inByte;
	byte index = 0;

	while (tcpClient.connected()) {
		if (tcpClient.available()) {
			inByte = tcpClient.read();
			req[index] = inByte;
			index++;
			if (index >= 21) break; // prevent buffer overflow
		}
		if (now() > timeoutStart + 3000) { // Wait a maximum of 3 seconds
			break; // End the while loop because of timeout
		}
	}
	req[index] = 0;

	// tcpClient.flush();
	// tcpClient.stop();

	if (req.length() < 1) { // No data received
		comLedFlashStop();
		tcpClient.flush();
		tcpClient.stop();
		return;
	}

	// Take a picture
  if (req.substring(0, 1) == "t"){
		sendRpiDebug("Request to take a picture", OTA_HOST);
		sendBroadCast(takeShot());
		// Switch on debug output
	} else if (req.substring(0, 1) == "d") {
		debugOn = !debugOn;
		if (debugOn) {
			sendRpiDebug("Debug over TCP is on", OTA_HOST);
		} else {
			sendRpiDebug("Debug over TCP is off", OTA_HOST);
		}
		// Delete saved WiFi configuration
	} else if (req.substring(0, 1) == "r") {
		sendRpiDebug("Delete WiFi credentials and reset device", OTA_HOST);
		wifiManager.resetSettings();
		// Reset the ESP
		delay(3000);
		ESP.reset();
		delay(5000);
		// Date/time received
	} else if (req.substring(0, 2) == "y=") {
		int nowYear = 0;
		int nowMonth = 0;
		int nowDay = 0;
		int nowHour = 0;
		int nowMinute = 0;
		int nowSecond = 0;

		if (isDigit(req.charAt(2))
		&& isDigit(req.charAt(3))
		&& isDigit(req.charAt(4))
		&& isDigit(req.charAt(5))
		&& isDigit(req.charAt(7))
		&& isDigit(req.charAt(8))
		&& isDigit(req.charAt(10))
		&& isDigit(req.charAt(11))
		&& isDigit(req.charAt(13))
		&& isDigit(req.charAt(14))
		&& isDigit(req.charAt(16))
		&& isDigit(req.charAt(17))
		&& isDigit(req.charAt(19))
		&& isDigit(req.charAt(20))) {
			cmd = req.substring(2, 6);
			int nowYear = cmd.toInt();
			cmd = req.substring(7, 9);
			int nowMonth = cmd.toInt();
			cmd = req.substring(10, 12);
			int nowDay = cmd.toInt();
			cmd = req.substring(13, 15);
			int nowHour = cmd.toInt();
			cmd = req.substring(16, 18);
			int nowMinute = cmd.toInt();
			cmd = req.substring(19, 21);
			int nowSecond = cmd.toInt();

			if (debugOn) {
				String debugMsg = "Changed time to " + String(nowYear) + "-" + String(nowMonth) + "-" + String(nowDay) + " " + String(nowHour) + ":" + String(nowMinute) + ":" + String(nowSecond);
				sendRpiDebug(debugMsg, OTA_HOST);
			}
			setTime(nowHour,nowMinute,nowSecond,nowDay,nowMonth,nowYear);
			gotTime = true;
		} else {
			String debugMsg = "Received wrong time format: " + req;
			sendRpiDebug(debugMsg, OTA_HOST);
		}
		// Reset device
	} else if (req.substring(0, 1) == "x") {
		sendRpiDebug("Reset device", OTA_HOST);
		tcpClient.flush();
		tcpClient.stop();
		// Reset the ESP
		delay(3000);
		ESP.reset();
		delay(5000);
		// Format SPIFFS
	} else if (req.substring(0, 1) == "z") {
		formatSPIFFS(OTA_HOST);
	}
	comLedFlashStop();
	tcpClient.flush();
	tcpClient.stop();
}
