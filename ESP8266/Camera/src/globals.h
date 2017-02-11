#include <ESP8266WiFi.h>

/** Build time */
extern const char compileDate[];

#ifdef BREADBOARD
	#define DEVICE_ID "cmb" // ID for security cam
	#define OTA_HOST "cmb" // Host name for OTA updates
#else
	#define DEVICE_ID "cm1" // ID for security cam
	#define OTA_HOST "cm1" // Host name for OTA updates
#endif

/** WiFiServer class to create TCP socket server on port 6000 */
extern WiFiServer tcpServer;
// /** FTPServer class to create simple ftp server */
extern FtpServer ftpSrv;
/** FTP client */
extern WiFiClient ftpClient;
/** External FTP server for data transfer*/
extern WiFiClient ftpDataClient;
/** External FTP server IP */
extern IPAddress ftpDataServer;
/** External FTP server port */
extern uint16_t ftpDataPort;

/** Buffer for received/sent data */
extern char ftpBuf[];
/** Counter for sent/received data */
extern char ftpCount;

/** IP address of this module */
extern IPAddress ipAddr;

/** Flag for boot status */
extern boolean inSetup;

/** Flag for OTA update running */
extern boolean otaRunning;
/** Flag for TCP debugging */
extern bool debugOn;
/** Last time NTP sync was performed */
extern time_t lastSyncTime;

/** Flag for broadcast status & consumption */
extern boolean sendUpdateTriggered;
/** Flag for broadcast status */
extern boolean sendUpdateTriggered;

// On ESP: camera TX connected to GPIO15, camera RX to GPIO13:
extern SoftwareSerial cameraconnection;
// Camera connection
extern Adafruit_VC0706 cam;
// Flag if camera was found
extern boolean hasCamera;

/** Flashlight LED output */
extern int flashLED;
/** Blinking LED output */
extern int blinkLED;
