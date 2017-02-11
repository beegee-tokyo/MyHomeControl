#ifndef declarations_h
#define declarations_h

#include <Arduino.h>
#include <ESP8266WiFi.h>

/** Build time */
const char compileDate[] = __DATE__ " " __TIME__;

/** WiFiServer class to create TCP socket server on port 6000 */
WiFiServer tcpServer(6000);

/** FTPServer class to create simple ftp server */
FtpServer ftpSrv;

/** IP address of this module */
#ifdef BREADBOARD
	IPAddress ipAddr = ipSpare4;
#else
	IPAddress ipAddr = ipSecBack;
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

/** Melody as delay time */
//long melody[] = {1700, 1700, 1136, 1136, 1432, 1915, 1915, 1700 ,1700 ,1136 ,1136 ,1700 ,1700 ,1915 ,1915 ,1432 ,1432 ,1700 ,1700 ,1136 ,1136 ,1915 ,1915 ,1700 ,1700 ,1136 ,1136 ,1432 ,1915 ,1915 ,1700 ,1700 ,1136 ,1136 ,1136 ,1136 ,1275 ,1275 ,1275 ,1275};
/** Number of melody[] notes */
//int melodyLenght = 40;
// Bido Bido sound
//long melody[] = {1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275};
/** Number of melody[] notes */
//int melodyLenght = 40;
/** Alarm melody Martinshorn */
long melody[] PROGMEM = {NOTE_C4, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_A3, NOTE_A3, NOTE_A3, NOTE_A3, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_A3 ,NOTE_A3, NOTE_A3, NOTE_A3, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_A3, NOTE_A3, NOTE_A3, NOTE_A3, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_A3, NOTE_A3, NOTE_A3, NOTE_A3, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_A3, NOTE_A3, NOTE_A3, NOTE_A3, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_A3, NOTE_A3, NOTE_A3, NOTE_A3, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_C4, NOTE_A3, NOTE_A3, NOTE_A3, NOTE_A3};
/** Number of melody[] notes */
int melodyLenght PROGMEM = 56;
// long melody[] = {2272, 2272, 2272, 2272, 3400, 3400, 3400, 3400, 2272, 2272, 2272, 2272, 1700, 1700, 1700, 1700};
// /** Number of melody[] notes */
// int melodyLenght = 16;

/** Melody position pointer */
int melodyPoint = 0;
/** Time to play a single tune in milliseconds */
int melodyTuneTime PROGMEM = 125;

/** Flag if lights should be switched on after movement detection */
boolean switchLights = false;
/** Flag for PIR status change */
boolean pirTriggered = false;
/** Flag for request to read out light sensor */
boolean weatherUpdateTriggered = false;
/** Flag for detection status */
boolean hasDetection = false;
/** Flag for light switched off */
boolean lightOffTriggered = false;
/** Relay on delay time in seconds */
int onTime = 120;

/** Flag for boot status */
boolean inSetup = true;
/** String with reboot reason */
String rebootReason = "unknown";
/** String with last known reboot reason */
String lastRebootReason = "unknown";

/** Instance of the DHT sensor */
DHT dht(DHTPIN, DHTTYPE, 11); // 11 works fine for ESP8266
/** Result of last temperature measurement */
float tempValue = 0.0;
/** Result of last humidity measurement */
float humidValue = 0.0;

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
