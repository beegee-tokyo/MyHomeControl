#include <ESP8266WiFi.h>

/** Input from PIR sensor */
#define pirPort 4
/** Output to activate Relay */
#define relayPort 5
/** Output to loudspeaker or piezo */
// #define speakerPin 15
#define speakerPin 12

/**********************************************
When doing breadboard test, enable this define
***********************************************/
//#define BREADBOARD

#ifdef BREADBOARD
	#define DEVICE_ID "sfb" // ID for security in front yard
	#define OTA_HOST "sfb" // Host name for OTA updates
#else
	#define DEVICE_ID "sf1" // ID for security in front yard
	#define OTA_HOST "sf1" // Host name for OTA updates
#endif

/** Build time */
extern const char compileDate [];
/** WiFiServer class to create TCP socket server on port 6000 */
extern WiFiServer tcpServer;
/** FTPServer class to create simple ftp server */
extern FtpServer ftpSrv;
/** IP address of this module */
extern IPAddress ipAddr;

/** Timer to switch off the relay */
extern Ticker relayOffTimer;
/** Timer for alarm siren */
extern Ticker alarmTimer;

/** Flag for alarm activity */
extern boolean alarmOn;
/** Flag if heart beat was triggered */
extern boolean heartBeatTriggered;
/** Flag if panic button was pressed */
extern boolean panicOn;
/** Flag for debugging */
extern bool debugOn;
/** Relay on delay time in seconds */
extern int onTime;
/** Flag for WiFi connection */
extern bool wmIsConnected;

/** Alarm melody */
extern long melody [];
/** Number of melody[] notes */
extern int melodyLenght;
/** Melody position pointer */
extern int melodyPoint;
/** Time to play a single tune in milliseconds */
extern int melodyTuneTime;

/** Flag if lights should be switched on after movement detection */
extern boolean switchLights;
/** Flag for PIR status change */
extern boolean pirTriggered;
/** Flag for request to read out LDR value from analog input */
extern boolean lightLDRTriggered;
/** Flag for detection status */
extern boolean hasDetection;
/** Flag for light switched off */
extern boolean lightOffTriggered;

/** Flag for boot status */
extern boolean inSetup;
/** String with reboot reason */
extern String rebootReason;
/** String with last known reboot reason */
extern String lastRebootReason;

/** Value read from LDR on analog input */
extern int ldrValue;

/** Flag for OTA update */
extern bool otaRunning;

/** Flag for automatic activation/deactivation of alarm */
extern boolean hasAutoActivation;
/** Hour for automatic activation of alarm (24h format) */
extern int autoActivOn;
/** Hour for automatic deactivation of alarm (24h format) */
extern int autoActivOff;
