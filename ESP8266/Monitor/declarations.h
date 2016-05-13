#include <ArduinoOTA.h>
#include <ESP8266WiFi.h>
#include <MQTTClient.h>
#include <ESP8266mDNS.h>
#include <Ticker.h>
#include <ESP8266FtpServer.h>
#include <Adafruit_Sensor_ESP.h>
#include <Adafruit_TSL2561_U_ESP.h>
#include <pgmspace.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <SPI.h>
#include "Ucglib.h"

/** Build time */
const char compileDate[] = __DATE__ " " __TIME__;

/** WiFi client class to receive messages from mqtt broker */
WiFiClient mqttReceiver;
/** MQTT client class to access mqtt broker */
MQTTClient mqttClient;

/** FTPServer class to create simple ftp server */
FtpServer ftpSrv;

/** Timer for flashing red detection LED */
Ticker ledFlasher;
/** Timer to collect status information from home devices */
Ticker getStatusTimer;
/** Flag for request to read status information from home devices */
boolean statusUpdated = false;
/** Timer to collect temperature & humidity information */
Ticker getDHTTimer;
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

/** Current line position for scrolling text */
byte currLine = 10;

/** Instance of the display */
Ucglib_ILI9163_18x128x128_SWSPI ucg(/*sclk=*/ DIS_SCLK, /*data=*/ DIS_MOSI, /*cd=*/ DIS_AO , /*cs=*/ DIS_CS); //, /*reset=*/ 4);

/** Data pin for I2C communication with light sensor */
#define sdaPin 4 // brown cable
/** Clock pin for I2C communication with light sensor */
#define sclPin 5 // green cable
// GND = blue cable
// Vin = red cable

/** Instance of the Adafruit TSL2561 sensor */
Adafruit_TSL2561_Unified tsl = Adafruit_TSL2561_Unified (TSL2561_ADDR_FLOAT, 1);
/** Currently used integration time for light sensor, 0 = 13.7ms, 1 = 101ms, 2 = 402ms */
int lightInteg = 2;
/** Result of last light measurement */
long lightValue = 0;

/** Definition of DHT sensor type */
#define DHTTYPE DHT11
/** Definition of data pin for DHT sensor */
#define DHTPIN  0

// Initialize DHT sensor 
DHT dht(DHTPIN, DHTTYPE, 11); // 11 works fine for ESP8266
/** Result of last temperature measurement */
float tempValue = 0.0;
/** Result of last humidity measurement */
float humidValue = 0.0;
/** Result of last heat index calculation */
float heatIndex = 0.0;
/** Counter of measurements between reports */
int sensorReadings = 0;

/** Status of the weather station */
String weatherStatus = "";

/** Power consumption of the house from spMonitor */
double consPower = 0;
/** Solar panel production from spMonitor */
double solarPower = 0;

/** Status of the solar panel */
String spmStatus = "";

/** Status of the office aircon */
String ac1Status = "";
/** Display status of the office aircon */
String ac1Display = "?? ";


/** Status of the front yard security */
String securityStatus = "";
/** Display status of the front yard security */
String securityDisplay = "?? ";
