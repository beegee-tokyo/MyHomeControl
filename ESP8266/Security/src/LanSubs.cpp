#include "Setup.h"

/** Local name of server */
static const char* host = "secf";

/**
 * Send broadcast message over UDP into local network
 *
 * @param makeShort
 *		If true send short status, else send long status
 */
void sendAlarm(boolean makeShort) {
	comLedFlashStart(0.2);
	if (debugOn) {
		sendDebug("sendAlarm", OTA_HOST);
	}
	/** Buffer for Json object */
	DynamicJsonBuffer jsonBuffer;

	// Prepare json object for the response
	/* Json object with the alarm message */
	JsonObject& root = jsonBuffer.createObject();

	if (debugOn) {
		sendDebug("Create status", OTA_HOST);
	}

	// Create status
	createStatus(root, makeShort);

	/** WiFiUDP class for creating UDP communication */
	WiFiUDP udpClientServer;

	// Start UDP client for sending broadcasts
	udpClientServer.begin(udpBcPort);

	int connectionOK = udpClientServer.beginPacketMulticast(multiIP, udpBcPort, ipAddr);
	if (connectionOK == 0) { // Problem occured!
		comLedFlashStop();
		udpClientServer.stop();
		if (debugOn) {
			sendDebug("UDP write multicast failed", OTA_HOST);
		}
		// wmIsConnected = false;
		return;
	}
	String broadCast;
	root.printTo(broadCast);
	udpClientServer.print(broadCast);
	udpClientServer.endPacket();
	udpClientServer.stop();

	udpClientServer.beginPacket(ipMonitor,5000);
	udpClientServer.print(broadCast);
	udpClientServer.endPacket();
	udpClientServer.stop();

	comLedFlashStop();
}

/**
 * Answer request on tcp socket server
 * Commands:
 * 	a=0 to switch off alarm
 *	a=1 to switch on alarm
 *	a=2 to switch on the defined hour on/off alarm
 * 	a=3 to switch off the defined hour on/off alarm
 * 	a=4 to switch on automatic light
 * 	a=5 to switch off automatic light
 *		s	to get short status message
 *		p	to switch on alarm sound (panic button function)
 *		i	to get detailed status information
 *		b	to switch on the lights for 5 minutes
 *		r	to reset saved WiFi configuration
 *		d	to enable TCP debug
 *		x to reset the device
 *		y=YYYY,MM,DD,HH,mm,ss to set time and date
 *
 * @param httpClient
 *		Connected WiFi client
 */
void socketServer(WiFiClient tcpClient) {

	// Get data from client until he stops the connection or timeout occurs
	long timeoutStart = now();
	String req = "123456789012345678901";
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
		if (debugOn) {
			sendDebug("Socket server - no data received", OTA_HOST);
		}
		return;
	}

	if (debugOn) {
		String debugMsg = "TCP cmd = " + req;
		sendDebug(debugMsg, OTA_HOST);
	}
	// Switch on/off the alarm
	if (req.substring(0, 2) == "a=") {
		if (req.substring(2, 3) == "0") { // Alarm off
			alarmOn = false;
			actLedFlashStop();
		} else if (req.substring(2, 3) == "1") { // Alarm on
			alarmOn = true;
			actLedFlashStart(1);
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
			digitalWrite(speakerPin, HIGH); // Switch off piezo
			digitalWrite(relayPort, LOW); // Switch off lights
			panicOn = false;
		} else {
			digitalWrite(speakerPin, LOW); // Switch on piezo
			digitalWrite(relayPort, HIGH); // Switch on lights
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
	// Switch lights on for 5 minutes
	} else if (req.substring(0, 1) == "b") {
		// Switch on lights for 5 minutes
		relayOffTimer.detach();
		relayOffTimer.once(onTime, relayOff);
		digitalWrite(relayPort, HIGH);
		// Send back status over UDP
		sendAlarm(true);
		return;
	// Enable debugging
	} else if (req.substring(0, 1) == "d") {
		// toggle debug flag
		debugOn = !debugOn;
		if (debugOn) {
			sendDebug("Debug over TCP is on", OTA_HOST);
		} else {
			sendDebug("Debug over TCP is off", OTA_HOST);
		}
		writeStatus();
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
			String cmd = req.substring(2, 6);
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
		// Reset device
	} else if (req.substring(0, 1) == "x") {
		sendDebug("Reset device", OTA_HOST);
		writeStatus();
		// Reset the ESP
		delay(3000);
		ESP.reset();
		delay(5000);
	}
}

/**
	sendLightStatus
	Send auto light on/off command to backyard security modules
*/
void sendLightStatus(boolean switchOn) {
	comLedFlashStart(0.1);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	if (debugOn) {
		sendDebug("sendLightStatus", OTA_HOST);
	}

	if (!tcpClient.connect(ipSecBack, tcpComPort)) {
		Serial.println("connection to backyard security " + String(ipSecBack[0]) + "." + String(ipSecBack[1]) + "." + String(ipSecBack[2]) + "." + String(ipSecBack[3]) + " failed");
		tcpClient.stop();
		comLedFlashStop();
		return;
	}

	if (switchOn) {
		tcpClient.print("a=4");
	} else {
		tcpClient.print("a=5");
	}

	tcpClient.flush();
	tcpClient.stop();
	comLedFlashStop();
}

void triggerPic() {
	comLedFlashStart(0.1);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	if (debugOn) {
		sendDebug("triggerPic", OTA_HOST);
	}

	if (!tcpClient.connect(ipCam1, tcpComPort)) {
		Serial.println("connection to frontyard camera " + String(ipCam1[0]) + "." + String(ipCam1[1]) + "." + String(ipCam1[2]) + "." + String(ipCam1[3]) + " failed");
		tcpClient.stop();
		comLedFlashStop();
		return;
	}

	tcpClient.print("t");

	tcpClient.flush();
	tcpClient.stop();
	comLedFlashStop();
}

void triggerVid(int cameraNum) {
	comLedFlashStart(0.1);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	if (debugOn) {
		sendDebug("triggerVid", OTA_HOST);
	}

	if (!tcpClient.connect(ipMonitor, tcpComPort)) {
		if (debugOn) {
			String debugMsg = "connection to frontyard camera " + String(ipMonitor[0]) + "." + String(ipMonitor[1]) + "." + String(ipMonitor[2]) + "." + String(ipMonitor[3]) + " failed";
			sendDebug(debugMsg, OTA_HOST);
		}
		tcpClient.stop();
		comLedFlashStop();
		return;
	}

	tcpClient.print("c");

	tcpClient.flush();
	tcpClient.stop();
	comLedFlashStop();
}
