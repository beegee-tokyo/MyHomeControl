#ifndef declarations_h
#define declarations_h

#include <Arduino.h>
#include <ESP8266WiFi.h>

/** Build time */
const char compileDate[] = __DATE__ " " __TIME__;

/** WiFiServer class to create TCP socket server on port tcpComPort */
WiFiServer tcpServer(tcpComPort);

/** IP address of this module */
#ifdef BREADBOARD
	IPAddress ipAddr = ipSpare;
#else
	IPAddress ipAddr = ipByardLight;
#endif

/** Timer to switch off the relay */
Ticker relayOffTimer;

/** Flag if heart beat was triggered */
boolean heartBeatTriggered = false;
/** Flag if panic button was pressed */
boolean panicOn = false;

/** Flag for light switched off */
boolean lightOffTriggered = false;
/** Flag for manual switch on activity */
boolean lightOnByUser = false;

/** Relay on delay time in seconds */
int onTime = 300;
/** Bug capture trial year of last good NTP time received */
int lastKnownYear = 0;

/** Flag for boot status */
boolean inSetup = true;
/** String with reboot reason */
String rebootReason = "unknown";
/** String with last known reboot reason */
String lastRebootReason = "unknown";

/** Flag for OTA update */
bool otaRunning = false;
/** Flag for debugging */
bool debugOn = false;

#endif
