#include <ESP8266WiFi.h>

/** SPI clock pin */
#define disSCLK 13
/** SPI data pin */
#define disMOSI 14
/** SPI chip select pin */
#define disCS 12
/** SPI address pin */
#define disA0 16
/** Definition of DHT sensor type */
#define DHTTYPE DHT11
/** Definition of data pin for DHT sensor */
#define dhtPin  5
/** Definition of button input */
#define buttonPin  4

/**********************************************
When doing breadboard test, enable this define
***********************************************/
//#define BREADBOARD

#ifdef BREADBOARD
	#define DEVICE_ID "monb" // ID for security in front yard
	#define OTA_HOST "monb" // Host name for OTA updates
#else
	#define DEVICE_ID "moni" // ID for security in front yard
	#define OTA_HOST "moni" // Host name for OTA updates
#endif

/** WiFi client class to receive messages from mqtt broker */
extern WiFiClient mqttReceiver;
/** MQTT client class to access mqtt broker */
extern MQTTClient mqttClient;
/** FTPServer class to create simple ftp server */
extern FtpServer ftpSrv;
/** WiFiUDP class for listening to UDP broadcasts */
extern WiFiUDP udpListener;
/** WiFiServer class to create TCP socket server on port 6000 */
extern WiFiServer tcpServer;

/** IP address of this module */
extern IPAddress ipAddr;

/** Instance of the display */
extern Ucglib_ILI9163_18x128x128_SWSPI ucg;

// Initialize DHT sensor
extern DHT dht;

/** Timer to collect temperature & humidity information */
extern Ticker getDHTTimer;
/** Timer to reset display to layout 0 */
extern Ticker resetDisplay;

/** Flag for request to switch back to display layout 0 */
extern boolean displayChange;
/** Flag for request to read status information from home devices */
extern boolean statusUpdated;
/** Flag for request to read out temperature & humidity information */
extern boolean dhtUpdated;
/** Flag for OTA update */
extern bool otaRunning;
/** Flag for TCP debugging */
extern bool debugOn;

/** Result of last temperature measurement */
extern float tempInside;
/** Result of last humidity measurement */
extern float humidInside;
/** Result of last heat index calculation */
extern float heatIndexIn;
/** Counter of measurements between reports */
extern int sensorReadings;

/** Outside temperature measurement */
extern float tempOutside;
/** Outside humidity measurement */
extern float humidOutside;
/** Outside heat index calculation */
extern float heatIndexOut;

/** Status of inside weather station */
extern String inWeatherStatus;
/** Status of outside weather station */
extern String outWeatherStatus;

/** Power consumption of the house from spMonitor */
extern double consPower;
/** Solar panel production from spMonitor */
extern double solarPower;

/** Power status of office aircon */
extern byte ac1On;
/** Modus of office aircon */
extern byte ac1Mode;
/** Auto status of office aircon */
extern byte ac1Auto;
/** Timer status of office aircon */
extern byte ac1Timer;
/** Fan speed status of office aircon */
extern byte ac1Speed;
/** Temp setting of office aircon */
extern byte ac1Temp;

/** Power status of living aircon */
extern byte ac2On;
/** Modus of living aircon */
extern byte ac2Mode;
/** Auto status of living aircon */
extern byte ac2Auto;
/** Timer status of living aircon */
extern byte ac2Timer;
/** Fan speed status of living aircon */
extern byte ac2Speed;
/** Temp setting of living aircon */
extern byte ac2Temp;

/** Alarm status of front yard security */
extern byte secFrontOn;
/** Light modus of front yard security */
extern byte secFrontLight;
/** Auto modus of front yard security */
extern byte secFrontAuto;
/** On time of front yard security */
extern byte secFrontOnTime;
/** Off time of front yard security */
extern byte secFrontOffTime;

/** Alarm status of back yard security */
extern byte secBackOn;
/** Light modus of back yard security */
extern byte secBackLight;
/** Auto modus of back yard security */
extern byte secBackAuto;
/** On time of back yard security */
extern byte secBackOnTime;
/** Off time of back yard security */
extern byte secBackOffTime;

/** Current display layout */
extern byte displayLayout;

/** Length of received UDP broadcast message */
extern int udpMsgLength;

/** Port for blue LED */
extern uint8_t comLED;
