/**
 * Prepares request to NTP server
 * @return <code>time_t</code>
 *			Current time as time_t structure or old time if failed
 */
time_t getNtpTime() {
	digitalWrite(COM_LED, LOW);
	IPAddress ipSlave = (192, 168, 0, 141);
	const int httpPort = 80;
	if (!tcpClient.connect(ipSlave, httpPort)) {
		Serial.println("connection to slave AC " + String(ipSPM[0]) + "." + String(ipSPM[1]) + "." + String(ipSPM[2]) + "." + String(ipSPM[3]) + " failed");
		tcpClient.stop();
		digitalWrite(COM_LED, HIGH);
		return now();
	}

	tcpClient.print("GET /?t HTTP/1.0\r\n\r\n");

	String line = "";
	int waitTimeOut = 0;
	while (tcpClient.connected()) {
		line = tcpClient.readStringUntil('\r');
		delay(1);
		waitTimeOut++;
		if (waitTimeOut > 2000) { // If no more response for 2 seconds return
			tcpClient.stop();
			return now();
		}
	}
	tcpClient.stop();
	/** Buffer for JSON string */
	DynamicJsonBuffer jsonBuffer;
	char json[line.length()];
	line.toCharArray(json, line.length() + 1);
	JsonObject& root = jsonBuffer.parseObject(json);
	if (!root.success()) {
		Serial.println("parseObject() failed");
		digitalWrite(COM_LED, HIGH);
		return now();
	}

	if (root.containsKey("time")) {
		digitalWrite(COM_LED, HIGH);
		return root["time"];
	}
	digitalWrite(COM_LED, HIGH);
	return now();
}