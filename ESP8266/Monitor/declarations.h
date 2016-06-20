#include <ArduinoOTA.h>
#include <ESP8266WiFi.h>
#include <WiFiUDP.h>
#include <MQTTClient.h>
#include <ESP8266mDNS.h>
#include <Ticker.h>
#include <ESP8266FtpServer.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <SPI.h>
#include "Ucglib.h"
#include <FS.h>
#include <TimeLib.h> 

/** Build time */
const char compileDate[] = __DATE__ " " __TIME__;

//#define DEBUG_OUT

/** WiFi client class to receive messages from mqtt broker */
WiFiClient mqttReceiver;
/** MQTT client class to access mqtt broker */
MQTTClient mqttClient;
/** FTPServer class to create simple ftp server */
FtpServer ftpSrv;
/** WiFiUDP class for listening to UDP broadcasts */
WiFiUDP udpListener;

/** Timer for flashing red detection LED */
Ticker ledFlasher;
/** Timer to collect status information from home devices */
Ticker getStatusTimer;
/** Flag for request to read status information from home devices */
boolean statusUpdated = false;
/** Timer to collect temperature & humidity information */
Ticker getDHTTimer;
/** Timer for playing sound */
Ticker soundTimer;

/** Flag for request to read out temperature & humidity information */
boolean dhtUpdated = false;

/** Flag for OTA update */
bool otaRunning = false;
/** OTA update status */
unsigned int otaStatus = 0;

/** Blue LED on GPIO2 for communication activities */
#define COM_LED 2
/** Red LED on GPIO0 for control activities */
#define ACT_LED 0

/** SPI clock pin */
#define DIS_SCLK 13
/** SPI data pin */
#define DIS_MOSI 14
/** SPI chip select pin */
#define DIS_CS 12
/** SPI address pin */
#define DIS_AO 16
/** Speaker pin */
#define SPEAKER 4
/** Definition of DHT sensor type */
#define DHTTYPE DHT11
/** Definition of data pin for DHT sensor */
#define DHTPIN  5


/** Current line position for scrolling text */
byte currLine = 10;

/** Instance of the display */
Ucglib_ILI9163_18x128x128_SWSPI ucg(/*sclk=*/ DIS_SCLK, /*data=*/ DIS_MOSI, /*cd=*/ DIS_AO , /*cs=*/ DIS_CS); //, /*reset=*/ 4);

// Initialize DHT sensor 
DHT dht(DHTPIN, DHTTYPE, 11); // 11 works fine for ESP8266

/** Result of last temperature measurement */
float tempInside = 0.0;
/** Result of last humidity measurement */
float humidInside = 0.0;
/** Result of last heat index calculation */
float heatIndexIn = 0.0;
/** Counter of measurements between reports */
int sensorReadings = 0;

/** Status of inside weather station */
String inWeatherStatus = "";
/** Status of outside weather station */
String outWeatherStatus = "";

/** Power consumption of the house from spMonitor */
double consPower = 0;
/** Solar panel production from spMonitor */
double solarPower = 0;

/** Status of the solar panel */
String spmStatus = "";

/** Status of the office aircon */
String ac1Status = "";
/** Power status of office aircon */
byte ac1On = 0;
/** Modus of office aircon */
byte ac1Mode = 0;
/** Auto status of office aircon */
byte ac1Auto = 0;
/** Timer status of office aircon */
byte ac1Timer = 0;

/** Status of the living aircon */
String ac2Status = "";
/** Power status of living aircon */
byte ac2On = 0;
/** Modus of living aircon */
byte ac2Mode = 0;
/** Auto status of living aircon */
byte ac2Auto = 0;
/** Timer status of living aircon */
byte ac2Timer = 0;

/** Status of the front yard security */
String secFrontStatus = "";
/** Alarm status of front yard security */
byte secFrontOn = 0;
/** Light modus of front yard security */
byte secFrontLight = 0;

/** Status of the back yard security */
String secBackStatus = "";
/** Alarm status of back yard security */
byte secBackOn = 0;
/** Light modus of back yard security */
byte secBackLight = 0;
/** Light value from TSL2561 sensor on back yard security */
long lightValue = 0;

// NTP Servers stuff
/** URL of NTP server */
const char* timeServerURL = "time.nist.gov";

/** Definition of timezone */
const int timeZone = 8;     // Philippine time (GMT + 8h)

/** Size of NTP time server packet */
const int NTP_PACKET_SIZE = 48; // NTP time is in the first 48 bytes of message
/** Buffer for data from NTP server */
byte packetBuffer[NTP_PACKET_SIZE]; //buffer to hold incoming & outgoing packets

/** Length of received UDP broadcast message */
int udpMsgLength = 0;

/** Melody as delay time */
long melody[] = {1700,1700,1136,1136,1432,1915,1915,1700,1700,1136,1136,1700,1700,1915,1915,1432,1432,1700,1700,1136,1136,1915,1915,1700,1700,1136,1136,1432,1915,1915,1700,1700,1136,1136,1136,1136,1275,1275,1275,1275};

//long melody[] = {1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275, 1915, 1915, 1915, 1915, 1275, 1275, 1275, 1275};
//long melody[] = {3830, 3830, 3830, 3830, 2550, 2550, 2550, 2550, 3830, 3830, 3830, 3830, 2550, 2550, 2550, 2550, 3830, 3830, 3830, 3830, 2550, 2550, 2550, 2550, 3830, 3830, 3830, 3830, 2550, 2550, 2550, 2550, 3830, 3830, 3830, 3830, 2550, 2550, 2550, 2550};
/** Relation between values and notes */
//	1915	1700	1519	1432	1275	1136	1014	956
//	c		d		e		f		g		a		b		c
/** Melody position pointer */
int melodyPoint = 0;
/** Number of melody[] notes */
int melodyLenght = 40;
/** Time to play a single tune in milliseconds */
int melodyTuneTime = 175;

