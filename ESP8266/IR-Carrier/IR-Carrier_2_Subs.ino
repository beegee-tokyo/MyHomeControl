/**
	resetFanMode
	called by Ticker resetFanModeTimer
	resets flag for fan speed change mode
*/
void resetFanMode () {
	isInFanMode = false;
	resetFanModeTimer.detach();
}

/**
	triggerSendUpdate
	called by Ticker sendUpdateTimer
	sets flag sendUpdateTriggered to true for handling in loop()
	will initiate a call to sendBroadCast() from loop()
*/
void triggerSendUpdate() {
	sendUpdateTriggered = true;
}

/**
 * Change status of red led on each call
 * called by Ticker ledFlasher
 */
void redLedFlash() {
	int state = digitalRead(ACT_LED);
	digitalWrite(ACT_LED, !state);
}

void printDigits(int digits){
	// utility for digital clock display: prints preceding colon and leading 0
	Serial.print(":");
	if(digits < 10)
		Serial.print('0');
	Serial.print(digits);
}

void digitalClockDisplay(){
	// digital clock display of the time
	Serial.print(hour());
	printDigits(minute());
	printDigits(second());
	Serial.print(" ");
	Serial.print(day());
	Serial.print(".");
	Serial.print(month());
	Serial.print(".");
	Serial.print(year()); 
	Serial.println(); 
}

/*-------- NTP code ----------*/

IPAddress timeServer(129, 6, 15, 28); // time.nist.gov NTP server
const int timeZone = +8;		 // Philippine Standard Timer
WiFiUDP Udp;
unsigned int localPort = 8888;	// local port to listen for UDP packets

const int NTP_PACKET_SIZE = 48; // NTP time is in the first 48 bytes of message
byte packetBuffer[NTP_PACKET_SIZE]; //buffer to hold incoming & outgoing packets

time_t prevDisplay = 0; // when the digital clock was displayed

// send an NTP request to the time server at the given address
void sendNTPpacket(IPAddress &address)
{
	// set all bytes in the buffer to 0
	memset(packetBuffer, 0, NTP_PACKET_SIZE);
	// Initialize values needed to form NTP request
	// (see URL above for details on the packets)
	packetBuffer[0] = 0b11100011;	 // LI, Version, Mode
	packetBuffer[1] = 0;		 // Stratum, or type of clock
	packetBuffer[2] = 6;		 // Polling Interval
	packetBuffer[3] = 0xEC;	// Peer Clock Precision
	// 8 bytes of zero for Root Delay & Root Dispersion
	packetBuffer[12]	= 49;
	packetBuffer[13]	= 0x4E;
	packetBuffer[14]	= 49;
	packetBuffer[15]	= 52;
	// all NTP fields have been given values, now
	// you can send a packet requesting a timestamp:								 
	Udp.beginPacket(address, 123); //NTP requests are to port 123
	Udp.write(packetBuffer, NTP_PACKET_SIZE);
	Udp.endPacket();
}

time_t getNtpTime()
{
	while (Udp.parsePacket() > 0) ; // discard any previously received packets
	Serial.println("Transmit NTP Request");
	sendNTPpacket(timeServer);
	uint32_t beginWait = millis();
	while (millis() - beginWait < 1500) {
		int size = Udp.parsePacket();
		if (size >= NTP_PACKET_SIZE) {
			Serial.println("Receive NTP Response");
			Udp.read(packetBuffer, NTP_PACKET_SIZE);	// read packet into the buffer
			unsigned long secsSince1900;
			// convert four bytes starting at location 40 to a long integer
			secsSince1900 =	(unsigned long)packetBuffer[40] << 24;
			secsSince1900 |= (unsigned long)packetBuffer[41] << 16;
			secsSince1900 |= (unsigned long)packetBuffer[42] << 8;
			secsSince1900 |= (unsigned long)packetBuffer[43];
			return secsSince1900 - 2208988800UL + timeZone * SECS_PER_HOUR;
		}
	}
	Serial.println("No NTP Response :-(");
	return 0; // return 0 if unable to get the time
}

/**
 * Get current time from Google server
 * @return theDate
 *		String containing the current time
 */
String getTime() {
	
	WiFiClient client;
	while (!!!client.connect("time.is", 80)) {
		Serial.println("connection failed, retrying...");
	}

	client.print("HEAD / HTTP/1.1\r\n\r\n");
 
	while(!!!client.available()) {
		 yield();
	}

	while(client.available()){
		if (client.read() == '\n') {		
			if (client.read() == 'D') {		
				if (client.read() == 'a') {		
					if (client.read() == 't') {		
						if (client.read() == 'e') {		
							if (client.read() == ':') {		
								client.read();
								String theDate = client.readStringUntil('\r');
								client.stop();

								// Sat, 19 Mar 2016 05:59:46 GMT
								String nowDay;
								String nowMonth;
								String nowYear;
								String nowTime;
								String nowHour;
								String nowMinute;
								String nowSecond;
								String partOfString[7];
								int index = 0;
								for (int i=0; i<theDate.length(); i++) {
									if (theDate[i]==' ') {
										index++;
									} else {
										partOfString[index] = partOfString[index] + theDate[i];
									}
								}
								nowDay = partOfString[1];
								nowMonth = partOfString[2];
								nowYear = partOfString[3];
								nowHour = partOfString[4].substring(0,2);
								nowMinute = partOfString[4].substring(3,5);
								nowSecond = partOfString[4].substring(6);
								
								for (int i=0; i<6; i++) {
									Serial.println(partOfString[i]);
								}
								return theDate;
							}
						}
					}
				}
			}
		}
	}
}

/**
	writeStatus
	writes current status into status.txt
*/
bool writeStatus() {
	// Open config file for writing.
	File statusFile = SPIFFS.open("/status.txt", "w");
	if (!statusFile)
	{
		Serial.println("Failed to open status.txt for writing");
		return false;
	}
	// Create current status as JSON
	DynamicJsonBuffer jsonBuffer;

	// Prepare json object for the response
	JsonObject& root = jsonBuffer.createObject();

	root["acMode"] = acMode;
	root["acTemp"] = acTemp;
	root["powerStatus"] = powerStatus;
	
	String jsonTxt;
	root.printTo(jsonTxt);
	
	Serial.println("writeStatus:");
	Serial.print("acMode = ");
	Serial.println(acMode);
	Serial.print("acTemp = ");
	Serial.println(acTemp);
	Serial.print("powerStatus = ");
	Serial.println(powerStatus);
	root.printTo(Serial);
	Serial.println();
	
	// Save status to file
	statusFile.println(jsonTxt);
	statusFile.close();
	return true;
}

/**
	readStatus
	reads current status from status.txt
	global variables are updated from the content
*/
bool readStatus() {
	Serial.println("readStatus:");
	// open file for reading.
	File statusFile = SPIFFS.open("/status.txt", "r");
	if (!statusFile)
	{
		Serial.println("Failed to open status.txt.");
		return false;
	}

	// Read content from config file.
	String content = statusFile.readString();
	statusFile.close();

	content.trim();

	// Create current status as from file as JSON
	DynamicJsonBuffer jsonBuffer;

	// Prepare json object for the response
	JsonObject& root = jsonBuffer.parseObject(content);

	// Parse JSON
	if (!root.success())
	{
		// Parsing fail
		Serial.println("Failed to parse status json");
		return false;
	}
	if (root.containsKey("acMode")) {
		acMode = root["acMode"];
	} else {
		Serial.println("Could not find acMode");
		return false;
	}
	if (root.containsKey("acTemp")) {
		acTemp = root["acTemp"];
	} else {
		Serial.println("Could not find acTemp");
		return false;
	}
	if (root.containsKey("powerStatus")) {
		powerStatus = root["powerStatus"];
	} else {
		Serial.println("Could not find powerStatus");
		return false;
	}
	Serial.print("acMode = ");
	Serial.println(acMode);
	Serial.print("acTemp = ");
	Serial.println(acTemp);
	Serial.print("powerStatus = ");
	Serial.println(powerStatus);
	root.printTo(Serial);
	Serial.println();
	
	return true;
}

/**
	parseCmd
	Parse the received command
*/
void parseCmd(JsonObject& root) {
	String statResponse;
	// The following commands are followed independant of AC on or off
	switch (irCmd) {
		case CMD_REMOTE_0: // Auto mode controlled off
			if ((acMode & AUTO_ON) != AUTO_ON) {
				irCmd = 9999;
				root["result"] = "auto_off";
			} else {
				root["result"] = "success";
				root["cmd"] = CMD_REMOTE_0;
				powerStatus = 0;
				writeStatus();
			}
			break;
		case CMD_REMOTE_1: // Auto mode controlled fan mode
			if ((acMode & AUTO_ON) != AUTO_ON) {
				irCmd = 9999;
				root["result"] = "auto_off";
			} else {
				root["result"] = "success";
				root["cmd"] = CMD_REMOTE_1;
				powerStatus = 1;
				writeStatus();
			}
			break;
		case CMD_REMOTE_2: // Auto mode controlled cool mode
			if ((acMode & AUTO_ON) != AUTO_ON) {
				irCmd = 9999;
				root["result"] = "auto_off";
			} else {
				root["result"] = "success";
				root["cmd"] = CMD_REMOTE_2;
				powerStatus = 2;
				writeStatus();
			}
			break;
		case CMD_AUTO_ON: // Command to (re)start auto control
			acMode = acMode & AUTO_CLR;
			acMode = acMode | AUTO_ON;
			root["result"] = "success";
			root["cmd"] = CMD_AUTO_ON;
			writeStatus();
			break;
		case CMD_AUTO_OFF: // Command to stop auto control
			acMode = acMode & AUTO_CLR;
			acMode = acMode | AUTO_OFF;
			root["result"] = "success";
			root["cmd"] = CMD_AUTO_OFF;
			writeStatus();
			break;
		case CMD_RESET: // Command to reset device
			root["result"] = "success";
			root["cmd"] = CMD_RESET;
			writeStatus();
			break;
		default: // Handle other commands
			if ((acMode & AC_ON) == AC_ON) { // AC is on
				root["result"] = "success";
				switch (irCmd) {
					case CMD_ON_OFF: // Switch aircon On/Off
						root["cmd"] = CMD_ON_OFF;
						break;
					case CMD_MODE_AUTO: // Switch to Auto
						root["cmd"] = CMD_MODE_AUTO;
						break;
					case CMD_MODE_COOL: // Switch to Cool
						root["cmd"] = CMD_MODE_COOL;
						break;
					case CMD_MODE_DRY: // Switch to Dry
						root["cmd"] = CMD_MODE_DRY;
						break;
					case CMD_MODE_FAN: // Switch to Fan
						root["cmd"] = CMD_MODE_FAN;
						break;
					case CMD_FAN_HIGH: // Switch to High Fan
						root["cmd"] = CMD_FAN_HIGH;
						break;
					case CMD_FAN_MED: // Switch to Medium Fan
						root["cmd"] = CMD_FAN_MED;
						break;
					case CMD_FAN_LOW: // Switch to Low Fan
						root["cmd"] = CMD_FAN_LOW;
						break;
					case CMD_FAN_SPEED: // Switch to next fan speed
						root["cmd"] = CMD_FAN_SPEED;
						break;
					case CMD_TEMP_PLUS: // Temp +
						root["cmd"] = CMD_TEMP_PLUS;
						break;
					case CMD_TEMP_MINUS: // Temp -
						root["cmd"] = CMD_TEMP_MINUS;
						break;
					case CMD_OTHER_TIMER: // Switch to Timer
						root["cmd"] = CMD_OTHER_TIMER;
						break;
					case CMD_OTHER_SWEEP: // Switch on/off sweep
						root["cmd"] = CMD_OTHER_SWEEP;
						break;
					case CMD_OTHER_TURBO: // Switch on/off turbo
						if (((acMode & MODE_COOL) == MODE_COOL) || ((acMode & MODE_AUTO) == MODE_AUTO)) {
							root["cmd"] = CMD_OTHER_TURBO;
						} else {
							root["result"] = "fail - AC not in cool mode";
							irCmd = 9999;
						}
						break;
					case CMD_OTHER_ION: // Switch on/off ion
						root["cmd"] = CMD_OTHER_ION;
						break;
					default:
						root["result"] = "fail - unknown command";
						irCmd = 9999;
						break;
				}
			} else { // AC is off
				root["result"] = "success";
				switch (irCmd) {
					case CMD_ON_OFF: // Switch aircon On/Off
						root["cmd"] = CMD_ON_OFF;
						break;
					default:
						root["result"] = "fail - AC is off";
						irCmd = 9999;
						break;
				}
			}
			break;
	}
}

