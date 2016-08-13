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
/** Timer to reset display to layout 0 */
Ticker resetDisplay;
/** Flag for request to switch back to display layout 0 */
boolean displayChange = false;

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
/** Definition of DHT sensor type */
#define DHTTYPE DHT11
/** Definition of data pin for DHT sensor */
#define DHTPIN  5
/** Definition of button input */
#define BUTTPIN  4


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

/** Outside temperature measurement */
float tempOutside = 0.0;
/** Outside humidity measurement */
float humidOutside = 0.0;
/** Outside heat index calculation */
float heatIndexOut = 0.0;

/** Power consumption of the house from spMonitor */
double consPower = 0;
/** Solar panel production from spMonitor */
double solarPower = 0;

/** Status of the solar panel */
String spmStatus = "";

/** Status of the office aircon */
String ac1Status = "";
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

/** Status of the living aircon */
String ac2Status = "";
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

/** Status of the front yard security */
String secFrontStatus = "";
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

/** Status of the back yard security */
String secBackStatus = "";
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

// NTP Servers stuff
/** URL of NTP server */
static const char* timeServerURL = "time.nist.gov";

/** Definition of timezone */
static const int timeZone = 8;     // Philippine time (GMT + 8h)

/** Size of NTP time server packet */
static const int NTP_PACKET_SIZE = 48; // NTP time is in the first 48 bytes of message
/** Buffer for data from NTP server */
byte packetBuffer[NTP_PACKET_SIZE]; //buffer to hold incoming & outgoing packets

/** Length of received UDP broadcast message */
int udpMsgLength = 0;

/** Current display layout */
byte displayLayout = 0;
/** Button status */
byte lastButtonStatus = HIGH;
/** Button debounce time value */
unsigned long lastButtonChange;
/** Flag for button accepted */
boolean displayChanged = false;
