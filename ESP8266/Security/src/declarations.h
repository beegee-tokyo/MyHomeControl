#ifndef declarations_h
#define declarations_h

#include <Arduino.h>
#include <ESP8266WiFi.h>

/** Build time */
const char compileDate[] = __DATE__ " " __TIME__;

/** WiFiServer class to create TCP socket server on port tcpComPort */
WiFiServer tcpServer(tcpComPort);

/** FTPServer class to create simple ftp server */
FtpServer ftpSrv;

/** IP address of this module */
#ifdef BREADBOARD
	IPAddress ipAddr = ipSpare4;
#else
	IPAddress ipAddr = ipSecFront;
#endif

/** Timer to switch off the relay */
Ticker relayOffTimer;
/** Timer for alarm siren */
Ticker alarmTimer;

/** Flag for alarm activity */
boolean alarmOn = true;
/** Flag if heart beat was triggered */
boolean heartBeatTriggered = false;
/** Flag if panic button was pressed */
boolean panicOn = false;

/** Flag if lights should be switched on after movement detection */
boolean switchLights = false;
/** Flag for PIR status change */
boolean pirTriggered = false;
/** Flag for request to read out LDR value from analog input */
boolean lightLDRTriggered = false;
/** Flag for detection status */
boolean hasDetection = false;
/** Flag for light switched off */
boolean lightOffTriggered = false;

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

/** Value read from LDR on analog input */
int ldrValue = 0;

/** Flag for OTA update */
bool otaRunning = false;
/** Flag for debugging */
bool debugOn = false;

/** Flag for automatic activation/deactivation of alarm */
boolean hasAutoActivation = false;
/** Hour for automatic activation of alarm (24h format) */
int autoActivOn = 22;
/** Hour for automatic deactivation of alarm (24h format) */
int autoActivOff = 8;

#endif
