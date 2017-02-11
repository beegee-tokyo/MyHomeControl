#include <Setup.h>

void handleTCP(String command) {
	int cmd = 0;
	// Brightness value received
	if (command.substring(0, 2) == "b=") {
		if (isDigit(command.charAt(2))) {
			if (isDigit(command.charAt(3))) {
				if (isDigit(command.charAt(4))) {
					cmd = command.substring(2, 5).toInt();
					if ((cmd >= 140) & (cmd <= 255)) {
						brightness = cmd;
						// set the brightness of led pin:
						analogWrite(led, brightness);
						if (brightness >= 222) {
							digitalWrite(controlLed, true);
						} else {
							digitalWrite(controlLed, false);
						}
					}
					sendBroadCast();
				}
			}
		}
		// Command received
	} else if (command.substring(0, 2) == "c=") {
		if (isDigit(command.charAt(2))) {
			if (isDigit(command.charAt(3))) {
				cmd = command.substring(2, 4).toInt();
				handleIR(0x80BF0000+cmdArray[cmd], false);
			}
		}
		// Preferred dimmed brightness value received
	} else if (command.substring(0, 2) == "d=") {
		if (isDigit(command.charAt(2))) {
			if (isDigit(command.charAt(3))) {
				if (isDigit(command.charAt(4))) {
					cmd = command.substring(2, 5).toInt();
					if ((cmd >= 140) & (cmd <= 255)) {
						dimValue = cmd;
						writeStatus();
					}
					sendBroadCast();
				}
			}
		}
		// Send status as UDP broadcast
	} else if (command.substring(0, 1) == "s") {
		sendBroadCast();
		// delay(10000);
		// sendTCPResponse();
	}
}

// Callback for TCP & UDP sockets, called if data was sent or received
// Receives socket client number, can be reused for all initialized TCP socket connections
void tcpCb(uint8_t resp_type, uint8_t client_num, uint16_t len, char *data) {
	// Serial.println("tcpCb connection #"+String(client_num));
	if (resp_type == USERCB_SENT) {
		// Serial.println("\tSent " + String(len) + " bytes over client#" + String(client_num));
	} else if (resp_type == USERCB_RECV) {
		// char recvData[len+1]; // Prepare buffer for the received data
		// memcpy(recvData, data, len); // Copy received data into the buffer
		// recvData[len] = '\0'; // Terminate the buffer with 0 for proper printout!
		// handleTCP(recvData);
		handleTCP(data);
	}
}

// Callback for TCP & UDP sockets, called if data was sent or received
// Receives socket client number, can be reused for all initialized TCP socket connections
void udpCb(uint8_t resp_type, uint8_t client_num, uint16_t len, char *data) {
	// Serial.println("udpCb connection #"+String(client_num));
	if (resp_type == USERCB_SENT) {
		// Serial.println("\tSent " + String(len) + " bytes over client#" + String(client_num));
	} else if (resp_type == USERCB_RECV) {
		// Serial.println("\tReceived " + String(len) + " bytes over client#" + String(client_num));
	}
}

/**
sendBroadCast
send updated status over LAN
by UTP broadcast
*/
void sendBroadCast() {
	char broadCast[38] = "{\"de\":\"lb1\",\"bo\":1,\"br\":160,\"di\":160}";
	broadCast[37] = 0;
	if (inSetup) {
		broadCast[17] = '1';
	} else {
		broadCast[17] = '0';
	}

	String currValue = String(brightness);
	broadCast[24] = '0';
	broadCast[25] = '0';
	broadCast[26] = '0';
	if (brightness >= 100) {
		broadCast[24] = currValue[0];
		broadCast[25] = currValue[1];
		broadCast[26] = currValue[2];
	} else if (brightness >= 10) {
		broadCast[25] = currValue[0];
		broadCast[26] = currValue[1];
	} else {
		broadCast[26] = currValue[0];
	}

	currValue = String(dimValue);
	broadCast[33] = '0';
	broadCast[34] = '0';
	broadCast[35] = '0';
	if (dimValue >= 100) {
		broadCast[33] = currValue[0];
		broadCast[34] = currValue[1];
		broadCast[35] = currValue[2];
	} else if (dimValue >= 10) {
		broadCast[34] = currValue[0];
		broadCast[35] = currValue[1];
	} else {
		broadCast[35] = currValue[0];
	}
	// Broadcast per UTP to LAN
	udp.send((const char *)&broadCast[0],38);
}
