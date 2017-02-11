#include "Setup.h"

/** Local name of server */
static const char* host = "secb";

/**
 * Send broadcast message over UDP into local network
 *
 * @param makeShort
 *		If true send short status, else send long status
 */
void sendAlarm(boolean makeShort) {
	comLedFlashStart(0.2);
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
	udpClientServer.begin(5000);

	int connectionOK = udpClientServer.beginPacketMulticast(multiIP, 5000, ipAddr);
	if (connectionOK == 0) { // Problem occured!
		comLedFlashStop();
		udpClientServer.stop();
		if (debugOn) {
			sendDebug("UDP write multicast failed", OTA_HOST);
		}
		// reConnectWiFi();
		// connectWiFi();
		wmIsConnected = false;
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
 *		b	to switch on the lights for 2 minutes
 *		r	to reset saved WiFi configuration
 *		d	to enable TCP debug
 *
 * @param httpClient
 *		Connected WiFi client
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
			// alarmTimer.detach();
			// analogWrite(speakerPin, 0);
			// digitalWrite(speakerPin, LOW); // Switch off speaker
			digitalWrite(speakerPin, HIGH); // Switch off piezo
			digitalWrite(relayPort, LOW); // Switch off lights
			panicOn = false;
		} else {
			// melodyPoint = 0; // Reset melody pointer to 0
			// alarmTimer.attach_ms(melodyTuneTime, playAlarmSound); // Start alarm sound
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
	// Switch lights on for 2 minutes
	} else if (req.substring(0, 1) == "b") {
		// Switch on lights for 2 minutes
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
	}
}

void triggerPic() {
	comLedFlashStart(0.1);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	const int httpPort = 6000;
	if (!tcpClient.connect(ipSpare2, httpPort)) {
		Serial.println("connection to backyard camera " + String(ipSpare2[0]) + "." + String(ipSpare2[1]) + "." + String(ipSpare2[2]) + "." + String(ipSpare2[3]) + " failed");
		tcpClient.stop();
		comLedFlashStop();
		wmIsConnected = false;
		return;
	}

	tcpClient.print("t");

	tcpClient.flush();
	tcpClient.stop();
	comLedFlashStop();
}
