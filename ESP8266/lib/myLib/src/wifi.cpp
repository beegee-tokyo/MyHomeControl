#include <wifi.h>

// WiFiManager instance
WiFiManager wifiManager;
// Static IP address set on first call to connectWiFi
IPAddress lastStatIP;
// Gateway IP address set on first call to connectWiFi
IPAddress lastStatGateWay;
// Netmask IP address set on first call to connectWiFi
IPAddress lastStatNetMask;
// SSID set on first call to connectWiFi
const char* lastSsidName;
// Flag if original connection was with static or dynamic IP address
bool wasStatic = false;
/**
	connectWiFi
	Connect to WiFi AP or start captive portal if no WiFi credentials are saved
	if WiFi is not found or no credentials are entered for 3 minutes
	module is restarted
	This method variant is for static IP address
*/
IPAddress connectWiFi(IPAddress statIP, IPAddress statGateWay, IPAddress statNetMask, const char* ssidName) {
	doubleLedFlashStart(1);

	// Prepare WiFiManager for connection with fixed IP
	wifiManager.setSTAStaticIPConfig(statIP, statGateWay, statNetMask);
	// Set a timeout of 180 seconds before returning
	wifiManager.setConfigPortalTimeout(180);
	// Try to connect to known network
	if (!wifiManager.autoConnect(ssidName)) {
		// If timeout occured try to reset the ESP
		delay(3000);
		ESP.reset();
		delay(5000);
	}
	doubleLedFlashStop();
	// Save ip, gateway, netmask and ssid name for reconnection
	lastStatIP = statIP;
	lastStatGateWay = statGateWay;
	lastStatNetMask = statNetMask;
	lastSsidName = ssidName;
	// Remember that we connected with static IP address
	wasStatic = true;
	return WiFi.localIP();
}

/**
	connectWiFi
	Connect to WiFi AP or start captive portal if no WiFi credentials are saved
	if WiFi is not found or no credentials are entered for 3 minutes
	module is restarted
	This method variant is for dynamic IP address
*/
IPAddress connectWiFi(const char* ssidName) {
	doubleLedFlashStart(1);

	// Set a timeout of 180 seconds before returning
	wifiManager.setConfigPortalTimeout(180);
	// Try to connect to known network
	if (!wifiManager.autoConnect(ssidName)) {
		// If timeout occured try to reset the ESP
		delay(3000);
		ESP.reset();
		delay(5000);
	}
	doubleLedFlashStop();
	// Save ssid name for reconnection
	lastSsidName = ssidName;
	return WiFi.localIP();
}

/**
	resetWiFiCredentials
	Delete saved WiFi credentials to force captive portal to be opened
	after next reboot
*/
void resetWiFiCredentials() {
	wifiManager.resetSettings();
	delay(3000);
	ESP.reset();
	delay(5000);
}

/**
	reConnectWiFi
	Reconnect to WiFi AP
	if no WiFi is found for 60 seconds
	module is restarted
*/
void reConnectWiFi() {
	if (wasStatic) {
		connectWiFi(lastStatIP, lastStatGateWay, lastStatNetMask, lastSsidName);
	} else {
		connectWiFi(lastSsidName);
	}
}

/**
 * Return signal strength or 0 if target SSID not found
 *
 * @return <code>int32_t</code>
 *              Signal strength as unsinged int or 0
 */
int32_t getRSSI() {
	/** Number of retries */
	byte retryNum = 0;
	/** Number of available networks */
	byte available_networks;
	/** The SSID we are connected to */
	String target_ssid(ssid);

	while (retryNum <= 3) {
		retryNum++;
		available_networks= WiFi.scanNetworks();
		if (available_networks == 0) { // Retryone time
			available_networks = WiFi.scanNetworks();
		}

		for (int network = 0; network < available_networks; network++) {
			if (WiFi.SSID(network).equals(target_ssid)) {
				return WiFi.RSSI(network);
			}
		}
	}
	return 0;
}

// For debug over TCP
void sendDebug(String debugMsg, String senderID) {
	doubleLedFlashStart(0.5);
	/** WiFiClient class to create TCP communication */
	WiFiClient tcpClient;

	const int httpPort = 9999;
	if (!tcpClient.connect(ipDebug, httpPort)) {
		Serial.println("connection to Debug PC " + String(ipDebug[0]) + "." + String(ipDebug[1]) + "." + String(ipDebug[2]) + "." + String(ipDebug[3]) + " failed");
		tcpClient.stop();
		doubleLedFlashStop();
		return;
	}

	// String sendMsg = OTA_HOST;
	debugMsg = senderID + " " + debugMsg;
	tcpClient.print(debugMsg);

	tcpClient.stop();
	doubleLedFlashStop();
}
