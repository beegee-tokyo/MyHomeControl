#ifndef declarations_h
#define declarations_h

#include <ESP8266WiFi.h>

/** WiFi client class to receive messages from mqtt broker */
WiFiClient mqttReceiver;
/** MQTT client class to access mqtt broker */
MQTTClient mqttClient;
/** FTPServer class to create simple ftp server */
FtpServer ftpSrv;
/** WiFiUDP class for listening to UDP broadcasts */
WiFiUDP udpListener;
/** WiFiServer class to create TCP socket server on port 6000 */
WiFiServer tcpServer(6000);

//#define BREADBOARD

/** IP address of this module */
#ifdef BREADBOARD
	IPAddress ipAddr = IPSPARE4;
#else
	IPAddress ipAddr = ipMonitor;
#endif
/** Instance of the display */
Ucglib_ILI9163_18x128x128_SWSPI ucg(/*sclk=*/ disSCLK, /*data=*/ disMOSI, /*cd=*/ disA0 , /*cs=*/ disCS); //, /*reset=*/ 4);

// Initialize DHT sensor
DHT dht(dhtPin, DHTTYPE, 11); // 11 works fine for ESP8266

/** Timer to collect temperature & humidity information */
Ticker getDHTTimer;
/** Timer to reset display to layout 0 */
Ticker resetDisplay;

/** Flag for request to switch back to display layout 0 */
boolean displayChange = false;
/** Flag for request to read status information from home devices */
boolean statusUpdated = false;
/** Flag for request to read out temperature & humidity information */
boolean dhtUpdated = false;
/** Flag for OTA update */
bool otaRunning = false;
/** Flag for TCP debugging */
bool debugOn = false;

/** Result of last temperature measurement */
float tempInside = 0.0;
/** Result of last humidity measurement */
float humidInside = 0.0;
/** Result of last heat index calculation */
float heatIndexIn = 0.0;
/** Counter of measurements between reports */
int sensorReadings = 0;

/** Outside temperature measurement */
float tempOutside = 0.0;
/** Outside humidity measurement */
float humidOutside = 0.0;
/** Outside heat index calculation */
float heatIndexOut = 0.0;

/** Status of inside weather station */
String inWeatherStatus = "";
/** Status of outside weather station */
String outWeatherStatus = "";

/** Power consumption of the house from spMonitor */
double consPower = 0;
/** Solar panel production from spMonitor */
double solarPower = 0;

/** Power status of office aircon */
byte ac1On = 2;
/** Modus of office aircon */
byte ac1Mode = 0;
/** Auto status of office aircon */
byte ac1Auto = 0;
/** Timer status of office aircon */
byte ac1Timer = 0;
/** Fan speed status of office aircon */
byte ac1Speed = 0;
/** Temp setting of office aircon */
byte ac1Temp = 0;

/** Power status of living aircon */
byte ac2On = 2;
/** Modus of living aircon */
byte ac2Mode = 0;
/** Auto status of living aircon */
byte ac2Auto = 0;
/** Timer status of living aircon */
byte ac2Timer = 0;
/** Fan speed status of living aircon */
byte ac2Speed = 0;
/** Temp setting of living aircon */
byte ac2Temp = 0;

/** Alarm status of front yard security */
byte secFrontOn = 2;
/** Light modus of front yard security */
byte secFrontLight = 0;
/** Auto modus of front yard security */
byte secFrontAuto = 0;
/** On time of front yard security */
byte secFrontOnTime = 22;
/** Off time of front yard security */
byte secFrontOffTime = 8;

/** Alarm status of back yard security */
byte secBackOn = 2;
/** Light modus of back yard security */
byte secBackLight = 0;
/** Auto modus of back yard security */
byte secBackAuto = 0;
/** On time of back yard security */
byte secBackOnTime = 22;
/** Off time of back yard security */
byte secBackOffTime = 8;

/** Current display layout */
byte displayLayout = 0;

/** Length of received UDP broadcast message */
int udpMsgLength = 0;

#endif
