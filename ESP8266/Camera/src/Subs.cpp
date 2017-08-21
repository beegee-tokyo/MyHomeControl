#include "Setup.h"

/**
 * Sets flag heartBeatTriggered to true for handling in loop()
 * called by Ticker heartBeatTimer
 * will initiate sending out a status message from loop()
 */
void triggerHeartBeat() {
	heartBeatTriggered = true;
}

bool formatSPIFFS() {
	String debugMsg;
	uint32_t startTime = millis();
	sendRpiDebug("SPIFFS format started", OTA_HOST);
	if (SPIFFS.format()){
		sendRpiDebug("SPIFFS formatted", OTA_HOST);
	} else {
		sendRpiDebug("SPIFFS format failed", OTA_HOST);
	}
	uint32_t endTime = millis();
	debugMsg = "Fromatting took " + String(endTime-startTime) + "ms";
	sendRpiDebug(debugMsg, OTA_HOST);
}
