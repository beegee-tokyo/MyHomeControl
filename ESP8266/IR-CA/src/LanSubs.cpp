#include <Setup.h>

/** WiFiUDP class for creating UDP communication */
WiFiUDP udpClientServer;

/**
	 sendBroadCast
	 send updated status over LAN
	 - by UTP broadcast over local lan
*/
void sendBroadCast() {
	comLedFlashStart(0.4);
	DynamicJsonBuffer jsonBuffer;
	// Prepare json object for the response
	JsonObject& root = jsonBuffer.createObject();
	//root["result"] = "fail";
	root["de"] = DEVICE_ID; //root["device"] = DEVICE_ID;

	//root["result"] = "success";
	// Display status of aircon
	if ((acMode & AC_ON) == AC_ON) {
		root["po"] = 1; //root["power"] = 1;
	} else {
		root["po"] = 0; //root["power"] = 0;
	}
	byte testMode = acMode & MODE_MASK;
	if (testMode == MODE_FAN) {
		root["mo"] = 0; //root["mode"] = 0;
	} else if (testMode == MODE_DRY) {
		root["mo"] = 1; //root["mode"] = 1;
	} else if (testMode == MODE_COOL) {
		root["mo"] = 2; //root["mode"] = 2;
	} else if (testMode == MODE_AUTO) {
		root["mo"] = 3; //root["mode"] = 3;
	}
	testMode = acMode & FAN_MASK;
	if (testMode == FAN_LOW) {
		root["sp"] = 0; //root["speed"] = 0;
	} else if (testMode == FAN_MED) {
		root["sp"] = 1; //root["speed"] = 1;
	} else if (testMode == FAN_HIGH) {
		root["sp"] = 2; //root["speed"] = 2;
	}
	testMode = acTemp & TEMP_MASK;
	root["te"] = testMode; //root["temp"] = testMode;

	// Display power cycle status
	root["st"] = powerStatus; //root["status"] = powerStatus;

	// Display status of auto control by power consumption
	if ((acMode & AUTO_ON) == AUTO_ON) {
		root["au"] = 1; //root["auto"] = 1;
	} else {
		root["au"] = 0; //root["auto"] = 0;
	}

	// Display timer status of aircon
	if ((acMode & TIM_ON) == TIM_ON) {
		root["ti"] = 1; //root["timer"] = 1;
	} else {
		root["ti"] = 0; //root["timer"] = 0;
	}

	// Display last timer on time
	root["ot"] = onTime; //root["onTime"] = onTime;

	// Display device id
	root["de"] = DEVICE_ID; //root["device"] = DEVICE_ID;

	// Set flag for restart
	if (inSetup) {
		root["bo"] = 1; //root["boot"] = 1;
	} else {
		root["bo"] = 0; //root["boot"] = 0;
	}

	root["dt"] = dayTime; //root["daytime"] = dayTime;
	String nowTime = String(hour()) + ":";
	if (minute() < 10) {
		nowTime += "0";
	}
	nowTime += String(minute());
	root["tm"] = nowTime; //root["time"] = nowTime;

	// Broadcast per UTP to LAN
	String broadCast;
	root.printTo(broadCast);
	udpClientServer.beginPacketMulticast(multiIP, udpBcPort, ipAddr);
	udpClientServer.print(broadCast);
	udpClientServer.endPacket();
	udpClientServer.stop();
	udpClientServer.beginPacket(ipMonitor,udpBcPort);
	udpClientServer.print(broadCast);
	udpClientServer.endPacket();
	udpClientServer.stop();

	comLedFlashStop();
}

/**
	socketServer
	answer request on tcp socket server
	* Commands:
  * 	c=00 AC on/off
  *		c=10 Mode Auto (ca1 only)
  *		c=11 Mode Cool
  * 	c=12 Mode Dry
  * 	c=13 Mode Fan
  * 	c=20 Fan High (fd1 only)
	* 	c=21 Fan Medium (fd1 only)
	* 	c=22 Fan Low (fd1 only)
	* 	c=23 Fan Speed (ca1 only)
	* 	c=30 Temp plus
	* 	c=31 Temp minus
	* 	c=40 Timer (fd1 only)
	* 	c=41 Sweep (ca1 only)
	* 	c=42 Turbo (ca1 only)
	* 	c=43 Ion (ca1 only)
	* 	c=70 Reset
	* 	c=71 AC init
	* 	c=80 Remote 00
	* 	c=81 Remote 01
	* 	c=82 Remote 02
	* 	c=98 Auto on
	* 	c=99 Auto off
	*		t=h Timer on time (1...9)(fd1 only)
	*		s	to get short status message
	*		d	to enable TCP debug
	*		i	AC init request
	*		r	to reset saved WiFi configuration
	*		x to reset the device
	*		y=YYYY,MM,DD,HH,mm,ss to set time and date
	*
	* @param httpClient
	*		Connected WiFi client
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

	tcpClient.flush();
	tcpClient.stop();
	if (req.length() < 1) { // No data received
		comLedFlashStop();
		return;
	}

	if (req.substring(0, 2) == "c=") { // command received
		if (isDigit(req.charAt(2))) {
			if (isDigit(req.charAt(3))) {
				cmd = req.substring(2, 4);
				irCmd = cmd.toInt();
				parseSocketCmd();
			} else {
				irCmd = 9999;
			}
		} else {
			irCmd = 9999;
		}
		// Timer duration received
	} else if (req.substring(0, 2) == "t=") {
		if (isDigit(req.charAt(2))) {
			cmd = req.substring(2, 3);
			onTime = cmd.toInt();
			if (debugOn) {
				String debugMsg = "Changed timer to " + String(onTime) + " hour";
				sendDebug(debugMsg, OTA_HOST);
			}
			writeStatus();
		}
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
				sendDebug(debugMsg, OTA_HOST);
			}
			setTime(nowHour,nowMinute,nowSecond,nowDay,nowMonth,nowYear);
			gotTime = true;
		} else {
			String debugMsg = "Received wrong time format: " + req;
			sendDebug(debugMsg, OTA_HOST);
		}
		// toggle debugging
	} else if (req.substring(0, 1) == "d"){
		debugOn = !debugOn;
		if (debugOn) {
			sendDebug("Debug over TCP is on", OTA_HOST);
		} else {
			sendDebug("Debug over TCP is off", OTA_HOST);
		}
		// send status
	} else if (req.substring(0, 1) == "s"){
		// Status sent after every TCP request!
		// initialization request received
	} else if (req.substring(0, 1) == "i") {
		irCmd = CMD_INIT_AC;
		// Delete saved WiFi configuration
	} else if (req.substring(0, 1) == "r") {
		sendDebug("Delete WiFi credentials and reset device", OTA_HOST);
		wifiManager.resetSettings();
		// Reset the ESP
		delay(3000);
		ESP.reset();
		delay(5000);
		// Reset device
	} else if (req.substring(0, 1) == "x") {
		sendDebug("Reset device", OTA_HOST);
		// Save last status before reset
		writeStatus();
		// Reset the ESP
		delay(3000);
		ESP.reset();
		delay(5000);
	}
	comLedFlashStop();
}
