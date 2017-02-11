#include <Setup.h>
#include <declarations.h>

void setup() {
	Serial.begin(115200);
	i2cuart.begin(9600);
  // declare led pin be an output:
  pinMode(led, OUTPUT);
  // declare pin 13 to be an output:
  pinMode(controlLed, OUTPUT);
  setPwmFrequency(led,1);
  myReceiver.enableIRIn(); // Start the receiver
  // set the initial brightness of led pin
  analogWrite(led, brightness);
  // show power on
  digitalWrite(controlLed, true);

	// Set flag for setup routine
	inSetup = true;

	// Reset ESP to make sure we start clean!
	cmd.EspRestart();
	delay(1000);

	// Sync-up with esp-link
	bool ok;
	do {
		ok = esp.Sync();			// sync up with esp-link, blocks for up to 2 seconds
	} while(!ok);

	// Sync-up with esp-link
	ok;
	do {
		ok = esp2.Sync();			// sync up with esp-link, blocks for up to 2 seconds
	} while(!ok);

	// // Set up the UDP socket client to send a short message to <udpServer> on port <udpPort>, this doesn't connect to that server,
	// // it just sets-up stuff on the esp-link side
	int err = udp.begin(udpServer, udpPort, SOCKET_UDP, udpCb);
	if (err < 0) {
		Serial.print(F("UDP socket setup failed, try again in 10 seconds after reboot"));
	}
	// Serial.print(F("UDP connection number = ")); Serial.println(err);
	// Serial.print(F("UDP server IP = ")); Serial.println(udpServer);
	// Serial.print(F("UDP server port = ")); Serial.println(udpPort);

	// Set up the TCP socket server to wait for a client on port <tcpPort>,
	// it just sets-up stuff on the esp-link side and waits until a client sends some data
	int tcpConnNum = tcp.begin(tcpServer, tcpPort, SOCKET_TCP_SERVER, tcpCb); // SOCKET_SERVER ==> accept connections
	if (tcpConnNum < 0) {
		Serial.println(F("TCP socket setup failed, try again in 10 seconds after reboot"));
	}
	// Serial.print(F("TCP connection number = ")); Serial.println(tcpConnNum);
	// Serial.print(F("TCP server IP = ")); Serial.println(tcpServer);
	// Serial.print(F("TCP server port = ")); Serial.println(tcpPort);

	// Get last saved dimmed brightness value
	readStatus();

	// Send status message
	sendBroadCast();
	// Reset flag for setup routine
	inSetup = false;
	// Initialize timer for frequent status updates
	lastTime = millis();
}
