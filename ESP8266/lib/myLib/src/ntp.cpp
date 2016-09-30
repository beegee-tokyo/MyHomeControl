#include "ntp.h"

/** Size of NTP time server packet */
const int NTP_PACKET_SIZE = 48; // NTP time is in the first 48 bytes of message
/** Buffer for data from NTP server */
byte packetBuffer[NTP_PACKET_SIZE]; //buffer to hold incoming & outgoing packets
/** Flag if getting the time from NTP was successfull */
bool gotTime = false;

/**
 * Prepares request to NTP server
 * @return <code>time_t</code>
 *			Current time as time_t structure or NULL if failed
 */
time_t getNtpTime()
{
	/** WiFiUDP class for creating UDP communication */
	WiFiUDP udpNTP;
	/** Definition of timezone */
	const int timeZone = 8;     // Philippine time (GMT + 8h)

	udpNTP.begin(5000);

	while (udpNTP.parsePacket() > 0) ; // discard any previously received packets
	Serial.println("Transmit NTP Request");
	sendNTPpacket(udpNTP);
	uint32_t beginWait = millis();
	while (millis() - beginWait < 1500) {
		int size = udpNTP.parsePacket();
		if (size >= NTP_PACKET_SIZE) {
			Serial.println("Receive NTP Response");
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
	Serial.println("No NTP Response :-(");
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

String digitalClockDisplay() {
	// digital clock display of the time as string
	String dateTime = String(hour()) + ":";
	dateTime += getDigits(minute()) + " ";
	dateTime += String(day()) + ".";
	dateTime += String(month()) + ".";
	dateTime += String(year());
	return dateTime;
}

String getDigits(int digits) {
	String digitsStr = "";
	// utility for digital clock display: prints preceding colon and leading 0
	if (digits < 10)
		digitsStr += "0";
	digitsStr += String(digits);
	return digitsStr;
}
