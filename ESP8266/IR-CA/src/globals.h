#include <ESP8266WiFi.h>

/** Build time */
extern const char compileDate[];

#ifdef BREADBOARD
	#define DEVICE_ID "cab" // ID for Carrier Aircon
	#define OTA_HOST "cab" // Host name for OTA updates
#else
	#define DEVICE_ID "ca1" // ID for Carrier Aircon
	#define OTA_HOST "ca1" // Host name for OTA updates
#endif

/** WiFiClient class to create TCP communication */
extern WiFiClient tcpClientOut;
/** WiFiServer class to create TCP socket server on port 6000 */
extern WiFiServer tcpServer;
/** FTPServer class to create simple ftp server */
extern FtpServer ftpSrv;

/** IP address of this module */
extern IPAddress ipAddr;

/** Received command (from lan or serial connection) */
extern int irCmd;

/** Definition of available commands */
#define CMD_ON_OFF		00

#define CMD_MODE_AUTO	10
#define CMD_MODE_COOL	11
#define CMD_MODE_DRY		12
#define CMD_MODE_FAN		13

#define CMD_FAN_HIGH		20
#define CMD_FAN_MED		21
#define CMD_FAN_LOW		22
#define CMD_FAN_SPEED	23

#define CMD_TEMP_PLUS	30
#define CMD_TEMP_MINUS	31

#define CMD_OTHER_TIMER 40
#define CMD_OTHER_SWEEP 41
#define CMD_OTHER_TURBO 42
#define CMD_OTHER_ION	43

#define CMD_RESET			70
#define CMD_INIT_AC		71

#define CMD_REMOTE_0		80
#define CMD_REMOTE_1		81
#define CMD_REMOTE_2		82

#define CMD_AUTO_ON		98
#define CMD_AUTO_OFF		99

/** Mode status of aircon (only guess, as human could have used remote)
		acMode acTemp
		MSB ... LSB (2x8bit)
		satp mmff Titt tttt
		s = sweep enabled:	0 sweep off
							1 sweep on
		a = auto mode:		0 power control disabled
							1 power control enabled
		t = timer:			0 timer is off
							1 timer is running
		p = status:			0 off
							1 on
		mm = mode:			00 fan
							01 dry
							10 cool
							11 auto
		ff = fan speed:		00 low
							01 medium
							10 high
		----------------------------------------------
		T = turbo enabled:	0 turbo off
							1 turbo on
		i = ion enabled: 	0 ion off
							1 ion on
		tttttt = temperature:	010000 16 deg C
								010001 17 deg C
								010010 18 deg C
								010011 19 deg C
								010100 20 deg C
								010101 21 deg C
								010110 22 deg C
								010111 23 deg C
								011000 24 deg C
								011001 25 deg C
								011010 26 deg C
								011011 27 deg C
								011100 28 deg C
								011101 29 deg C
								011110 30 deg C
								011111 31 deg C
								100000 32 deg C
*/
extern byte acMode;
extern byte acTemp;

#define SWP_OFF	B00000000
#define SWP_ON		B10000000
#define SWP_CLR	B01111111
#define SWP_MASK	B10000000

#define AUTO_OFF	B00000000
#define AUTO_ON	B01000000
#define AUTO_CLR	B10111111
#define AUTO_MASK	B01000000

#define TIM_OFF	B00000000
#define TIM_ON		B00100000
#define TIM_CLR	B11011111
#define TIM_MASK	B00100000

#define AC_OFF		B00000000
#define AC_ON		B00010000
#define AC_CLR		B11101111
#define AC_MASK	B00010000

#define MODE_FAN	B00000000
#define MODE_DRY	B00000100
#define MODE_COOL	B00001000
#define MODE_AUTO	B00001100
#define MODE_CLR	B11110011
#define MODE_MASK	B00001100

#define FAN_LOW	B00000000
#define FAN_MED	B00000001
#define FAN_HIGH	B00000010
#define FAN_CLR	B11111100
#define FAN_MASK	B00000011

#define TEMP_CLR	B11000000
#define TEMP_MASK	B00111111
#define TUR_OFF	B00000000
#define TUR_ON		B10000000
#define TUR_CLR	B01111111
#define TUR_MASK	B10000000
#define ION_OFF	B00000000
#define ION_ON		B01000000
#define ION_CLR	B10111111
#define ION_MASK	B01000000

/** Max selectable temperature */
#define MAX_TEMP 29
/** Min selectable temperature */
#define MIN_TEMP 18

/** IR LED on GPIO13 for communciation with aircon */
#define IR_LED_OUT 13

/** Timer to contact spMonitor server to get current consumption */
extern Ticker getPowerTimer;
/** Timer to broadcast status & consumption every 10 minutes */
extern Ticker sendUpdateTimer;
/** Timer for aircon 1 hour timer */
extern Ticker timerEndTimer;
/** Timer to switch off fan speed change mode after 10 seconds */
extern Ticker resetFanModeTimer;

/** Flag for request to contact spMonitor server to get current consumption */
extern boolean powerUpdateTriggered;
/** Flag for broadcast status & consumption */
extern boolean sendUpdateTriggered;
/** Flag for timer has reached 1 hour */
extern boolean timerEndTriggered;
/** Flag for boot status */
extern boolean inSetup;
/** Flag for day time */
extern boolean dayTime;
/** Start of day time */
extern int startOfDay;
/** End of day time (hour - 1) */
extern int endOfDay;
/** Time in hours for timer function */
extern uint32_t onTime;
/** Last processed command (from lan or serial connection) */
extern int lastCmd;
/** Counter to check if command is repeated */
extern byte cmdCnt;
/** Flag for fan speed change up or down */
extern boolean fanSpeedUp;
/* Flag to detect if we are in fan speed change mode */
extern boolean isInFanMode;

/** Flag for OTA update running */
extern boolean otaRunning;
/** Flag for TCP debugging */
extern bool debugOn;

/** Instance of the IR sender */
extern IRsend My_Sender;

/** Status of aircon related to power consumption value of the house */
extern byte powerStatus;
/** Power consumption of the house from spMonitor */
extern double consPower;

/** Collected power consumption of last 10 minutes of the house from spMonitor */
extern double avgConsPower[];
/** Last temperature set by user before switching to DRY mode */
extern byte savedAcTemp;
