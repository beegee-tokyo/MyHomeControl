/* Includes from libraties */
#include <ESP8266WiFi.h>
#include <WiFiUdp.h>
#include <WiFiClient.h>
#include <ArduinoOTA.h>
#include <Ticker.h>
#include <Wire.h>
#include <Adafruit_Sensor_ESP.h>
#include <Adafruit_TSL2561_U_ESP.h>
#include <pgmspace.h>
#include <ArduinoJson.h>
#include <FS.h>
#include <TimeLib.h> 
#include <ESP8266FtpServer.h>

/** Build time */
const char compileDate[] = __DATE__ " " __TIME__;

/* wifiAPinfo.h contains wifi SSID and password */
/* file content looks like: */
/* Begin of file:
	const char* ssid = "YOUR_WIFI_SSID";
	const char* password = "YOUR_WIFI_PASSWORD";
	End of file 
*/
#include "wifiAPinfo.h"

/* gcmInfo.h contains the API key for Google cloud messaging */
#include "gcmInfo.h"

/** Red LED on GPIO0 for visual signal if alarm is on or off */
#define alarmLED 0
/** Blue LED on GPIO2 for communication activities */
#define comLED 2
/** Input from PIR sensor */
#define pirPort 4
/** Output to activate Relay */
#define relayPort 5
/** Data pin for I2C communication with light sensor */
#define sdaPin 13
/** Clock pin for I2C communication with light sensor */
#define sclPin 12
/** Output to loudspeaker or piezo */
#define speakerPin 15
/** Input from push button */
#define pushButton 14

/**********************************************
When doing breadboard test, enable this define
***********************************************/
//#define BREADBOARD

#ifdef BREADBOARD
	#define DEVICE_ID "sfb" // ID for security in front yard
	#define OTA_HOST "secb" // Host name for OTA updates
#else
	#define DEVICE_ID "sf1" // ID for security in front yard
	#define OTA_HOST "sec" // Host name for OTA updates
#endif

/** WiFiClient class to create TCP communication */
WiFiClient tcpClient;

/** WiFiUDP class for creating UDP communication */
WiFiUDP udpClientServer;

/** WiFiServer class to create simple web server */
WiFiServer server(80);

/** FTPServer class to create simple ftp server */
FtpServer  ftpSrv;

/** IP address of this module */
#ifdef BREADBOARD
	IPAddress ipAddr(192, 168, 0, 148);
#else
	IPAddress ipAddr(192, 168, 0, 141);
#endif
/** Gateway address of WiFi access point */
IPAddress ipGateWay(192, 168, 0, 1);
/** Network mask of the local lan */
IPAddress ipSubNet(255, 255, 255, 0);
/** Network address mask for UDP multicast messaging */
IPAddress multiIP (192,	168, 0, 255);

/** MAC address of this module = unique id on the LAN */
String localMac = "";

/** Timer for flashing red detection LED */
Ticker ledFlasher;
/** Timer for flashing blue communication LED */
Ticker comFlasher;
/** Timer to switch off the relay */
Ticker relayOffTimer;
/** Timer to collect light information from TSL2561 sensor */
Ticker getLightTimer;
/** Timer to collect light information from LDR */
Ticker getLDRTimer;
/** Timer for alarm siren */
Ticker alarmTimer;
/** Timer for heart beat */
Ticker heartBeatTimer;

/** Flag for alarm activity */
boolean alarmOn = true;
/** Holds the current button state. */
volatile int state;
/** Holds the last time debounce was evaluated (in millis). */
volatile long lastDebounceTime = 0;
/** The delay threshold for debounce checking (in millis). */
const int debounceDelay = 500;
/** Flag for button pressed */
boolean buttonWasPressed = false;
/** Flag if heart beat was triggered */
boolean heartBeatTriggered = false;
/** Flag if WiFi connection is ongoing */
boolean wifiConnecting = false;

/** Melody as delay time */
//long melody[] = {1700,1700,1136,1136,1432,1915,1915,1700,1700,1136,1136,1700,1700,1915,1915,1432,1432,1700,1700,1136,1136,1915,1915,1700,1700,1136,1136,1432,1915,1915,1700,1700,1136,1136,1136,1136,1275,1275,1275,1275};
long melody[] = {1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275};

/** Relation between values and notes */
//	1915	1700	1519	1432	1275	1136	1014	956
//	c		d		e		f		g		a		b		c

/** Melody position pointer */
int melodyPoint = 0;
/** Number of melody[] notes */
int melodyLenght = 40;
/** Time to play a single tune in milliseconds */
int melodyTuneTime = 175;

/** Relay on delay time in seconds */
int onTime = 120;
/** Counter for relay switch off timing */
long offDelay = 0;
/** Flag if lights should be switched on after movement detection */
boolean switchLights = false;
/** Flag for PIR status change */
boolean pirTriggered = false;
/** Flag for request to read out light sensor */
boolean lightUpdateTriggered = false;
/** Flag for request to read out LDR value from analog input */
boolean lightLDRTriggered = false;
/** Flag for detection status */
boolean hasDetection = false;
/** Flag for light switched off */
boolean lightOffTriggered = false;

/** Flag for boot status */
boolean inSetup = true;
/** String with reboot reason */
String rebootReason = "unknown";
/** String with last known reboot reason */
String lastRebootReason = "unknown";

/** Instance of the Adafruit TSL2561 sensor */
Adafruit_TSL2561_Unified tsl = Adafruit_TSL2561_Unified ( TSL2561_ADDR_FLOAT, 1 );
/** Currently used integration time for light sensor, 0 = 13.7ms, 1 = 101ms, 2 = 402ms */
int lightInteg = 2;
/** Result of last light measurement */
long lightValue = 0;

/** Value read from LDR on analog input */
int ldrValue = 0;

/** Counter for "I am alive" led blink */
int liveCnt = 0;

// NTP Servers stuff
/** URL of NTP server */
const char* timeServerURL = "time.nist.gov";

/** Definition of timezone */
const int timeZone = 8;     // Philippine time (GMT + 8h)

/** Port for connection to NTP server */
unsigned int localPort = 8888;  // local port to listen for UDP packets

/** Size of NTP time server packet */
const int NTP_PACKET_SIZE = 48; // NTP time is in the first 48 bytes of message
/** Buffer for data from NTP server */
byte packetBuffer[NTP_PACKET_SIZE]; //buffer to hold incoming & outgoing packets

/** Flag for automatic activation/deactivation of alarm */
boolean hasAutoActivation = false;
/** Hour for automatic activation of alarm (24h format) */
int autoActivOn = 22;
/** Hour for automatic deactivation of alarm (24h format) */
int autoActivOff = 8;

