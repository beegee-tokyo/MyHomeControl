#include "ntp.h"
#include "wifi.h"

/** Size of NTP time server packet */
const int NTP_PACKET_SIZE = 48; // NTP time is in the first 48 bytes of message
/** Buffer for data from NTP server */
byte packetBuffer[NTP_PACKET_SIZE]; //buffer to hold incoming & outgoing packets
/** Flag if getting the time from NTP was successfull */
bool gotTime = false;

/**
 * Try to get time from NTP server
 * @return <code>bool</code>
 *		true if time was updated
 */
bool tryGetTime(boolean debugOn) {
	time_t dayTime = 0;
	for (uint8_t trials = 0; trials < 10; trials++) {
		dayTime = getNtpTime();
		if ((dayTime != 0) && gotTime) {
			setTime(dayTime);
			if (debugOn) {
				String debugMsg = "Got time after " + String(trials) + " trials";
				sendDebug(debugMsg, "NTP");
			}
			return true;
		}
		if (dayTime == 0) {
			if (debugOn) {
				sendDebug("dayTime == 0", "NTP");
			}
		}
		if (!gotTime) {
			if (debugOn) {
				sendDebug("gotTime == false", "NTP");
			}
		}
	}
	return false;
}

/**
 * Prepares request to NTP server
 * @return <code>time_t</code>
 *			Current time as time_t structure or 0 if failed
 */
time_t getNtpTime()
{
	/** WiFiUDP class for creating UDP communication */
	WiFiUDP udpNTP;
	/** Definition of timezone */
	const int timeZone = 8;     // Philippine time (GMT + 8h)

	udpNTP.begin(5000);

	while (udpNTP.parsePacket() > 0) ; // discard any previously received packets
	// Serial.println("Transmit NTP Request");
	sendNTPpacket(udpNTP);
	uint32_t beginWait = millis();
	while (millis() - beginWait < 1500) {
		int size = udpNTP.parsePacket();
		if (size >= NTP_PACKET_SIZE) {
			// Serial.println("Receive NTP Response");
			udpNTP.read(packetBuffer, NTP_PACKET_SIZE);	// read packet into the buffer
			unsigned long secsSince1900;
			// convert four bytes starting at location 40 to a long integer
			secsSince1900 =	(unsigned long)packetBuffer[40] << 24;
			secsSince1900 |= (unsigned long)packetBuffer[41] << 16;
			secsSince1900 |= (unsigned long)packetBuffer[42] << 8;
			secsSince1900 |= (unsigned long)packetBuffer[43];
			udpNTP.stop();
			gotTime = true;
			return secsSince1900 - 2208988800UL + timeZone * SECS_PER_HOUR;
		}
	}
	// Serial.println("No NTP Response :-(");
	udpNTP.stop();
	gotTime = false;
	return 0; // return 0 if unable to get the time
}

/**
 * Send an NTP request to the time server at the given address
 * @param &address
 *			Pointer to address of NTP server
 */
void sendNTPpacket(WiFiUDP udpNTP)
{
	/** URL of NTP server */
	const char* timeServerURL = "time.nist.gov";

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
	udpNTP.beginPacket(timeServerURL, 123); //NTP requests are to port 123
	udpNTP.write(packetBuffer, NTP_PACKET_SIZE);
	udpNTP.endPacket();
}

/**
 * Generate a string with formatted time and date
 * @return <code>String</code>
 *			String with current time as hh:mm dd.mm.yy
 */
String digitalClockDisplay() {
	// digital clock display of the time as string
	String dateTime = String(hour()) + ":";
	dateTime += getDigits(minute()) + " ";
	dateTime += String(day()) + ".";
	dateTime += String(month()) + ".";
	dateTime += String(year());
	return dateTime;
}

/**
 * Generate a string from a integer with leading zero if integer is smaller than 10
 * @param int
 *			Integer to be converted
 * @return <code>String</code>
 *			Integer as String
 */
String getDigits(int digits) {
	if (digits < 10) {
		return "0" + String(digits);
	} else {
		return String(digits);
	}
}
