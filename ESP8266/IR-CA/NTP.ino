/**
 * Prepares request to NTP server
 * @return <code>time_t</code>
 *			Current time as time_t structure or NULL if failed
 */
time_t getNtpTime()
{
	/** WiFiUDP class for creating UDP communication */
	WiFiUDP udpClientServer;

	for (int i=0; i<2; i++) { // Try at least 2 times to get the time from the NTP server
		udpClientServer.begin(5000);

		while (udpClientServer.parsePacket() > 0) ; // discard any previously received packets
		#ifdef DEBUG_OUT 
		Serial.println("Transmit NTP Request");
		#endif
		sendNTPpacket();
		uint32_t beginWait = millis();
		while (millis() - beginWait < 5000) {
			int size = udpClientServer.parsePacket();
			if (size >= NTP_PACKET_SIZE) {
				#ifdef DEBUG_OUT 
				Serial.println("Receive NTP Response");
				#endif
				udpClientServer.read(packetBuffer, NTP_PACKET_SIZE);	// read packet into the buffer
				unsigned long secsSince1900;
				// convert four bytes starting at location 40 to a long integer
				secsSince1900 =	(unsigned long)packetBuffer[40] << 24;
				secsSince1900 |= (unsigned long)packetBuffer[41] << 16;
				secsSince1900 |= (unsigned long)packetBuffer[42] << 8;
				secsSince1900 |= (unsigned long)packetBuffer[43];
				udpClientServer.stop();
				gotTime = true;
				return secsSince1900 - 2208988800UL + timeZone * SECS_PER_HOUR;
			}
		}
		#ifdef DEBUG_OUT 
		Serial.println("No NTP Response :-(");
		#endif
		udpClientServer.stop();
	}
	return 0; // return 0 if unable to get the time
}

/**
 * Send an NTP request to the time server at the given address
 * @param &address
 *			Pointer to address of NTP server
 */
void sendNTPpacket()
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
	udpClientServer.beginPacket(timeServerURL, 123); //NTP requests are to port 123
	udpClientServer.write(packetBuffer, NTP_PACKET_SIZE);
	udpClientServer.endPacket();
}
