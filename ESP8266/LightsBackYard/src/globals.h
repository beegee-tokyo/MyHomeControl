#include <ESP8266WiFi.h>

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
	#define DEVICE_ID "lyb" // ID for security in front yard
	#define OTA_HOST "lyb" // Host name for OTA updates
#else
	#define DEVICE_ID "ly1" // ID for security in front yard
	#define OTA_HOST "ly1" // Host name for OTA updates
#endif

/** Build time */
extern const char compileDate [];
/** WiFiServer class to create TCP socket server on port tcpComPort */
extern WiFiServer tcpServer;
/** IP address of this module */
extern IPAddress ipAddr;

/** Timer to switch off the relay */
extern Ticker relayOffTimer;

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
/** Bug capture trial year of last good NTP time received */
extern int lastKnownYear;

/** Flag for light switched off */
extern boolean lightOffTriggered;
/** Flag for manual switch on activity */
extern boolean lightOnByUser;

/** Flag for boot status */
extern boolean inSetup;
/** String with reboot reason */
extern String rebootReason;
/** String with last known reboot reason */
extern String lastRebootReason;

/** Flag for OTA update */
extern bool otaRunning;
