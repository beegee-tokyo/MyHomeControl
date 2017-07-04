/** Build time */
const char compileDate[] = __DATE__ " " __TIME__;

/**********************************************
When doing breadboard test, enable this define
***********************************************/
//#define BREADBOARD

#ifdef BREADBOARD
	IPAddress ipAddr = ipSpare;
#else
	IPAddress ipAddr = ipAC1;
#endif

/** WiFiClient class to create TCP communication */
WiFiClient tcpClientOut;
/** WiFiServer class to create TCP socket server on port tcpComPort */
WiFiServer tcpServer(tcpComPort);
/** FTPServer class to create simple ftp server */
FtpServer ftpSrv;

/** Received command (from lan or serial connection) */
int irCmd = 9999;
/** Last processed command (from lan or serial connection) */
int lastCmd = 99;
/** Counter to check if command is repeated */
byte cmdCnt = -1;

/** Flag for fan speed change up or down */
boolean fanSpeedUp = true;
/* Flag to detect if we are in fan speed change mode */
boolean isInFanMode = false;
/** Timer to switch off fan speed change mode after 10 seconds */
Ticker resetFanModeTimer;

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
byte acMode =		B00000000;
byte acTemp =		B00000000;

/** Timer to contact spMonitor server to get current consumption */
Ticker getPowerTimer;
/** Timer to broadcast status & consumption every 10 minutes */
Ticker sendUpdateTimer;
/** Timer for aircon 1 hour timer */
Ticker timerEndTimer;

/** Flag for request to contact spMonitor server to get current consumption */
boolean powerUpdateTriggered = false;
/** Flag for broadcast status & consumption */
boolean sendUpdateTriggered = false;
/** Flag for timer has reached 1 hour */
boolean timerEndTriggered = false;
/** Flag for boot status */
boolean inSetup = true;
/** Flag for day time */
boolean dayTime = false;
/** Start of day time */
int startOfDay = 8;
/** End of day time (hour - 1) */
int endOfDay = 17;
/** Time in hours for timer function */
uint32_t onTime = 1; // default 1 hour
/** Timer off time as hh:mm */
String timerEnd = "";
/** Up counter for timer function */
uint32_t timerCounter = 0;

/** Flag for OTA update running */
boolean otaRunning = false;
/** Flag for TCP debugging */
bool debugOn = false;

/** Instance of the IR sender */
IRsend My_Sender(IR_LED_OUT);

/* Status of aircon related to power consumption value of the house
 * If consumption is negative => production higher than consumption => switch on aircon
 * If consumption is positive => production lower than consumption => switch off aircon
 * Tresholds:
 *		off                         => status 0
 *    off and  +75W  => fan mode  => status 1
 *    fan and  +300W => cool mode => status 3
 *		fan and  -200W => off       => status 0
 *    cool and -400W => dry mode  => status 2
 *    dry and  -200W => fan mode  => status 1
 * After a change the average consumption is cleared and it takes at least 10 minutes before next change is possible
 * Check every minute to switch aircon on, if on, check every 10 minutes before changing to another level
 */
byte powerStatus = 0;
/** Power consumption of the house from spMonitor */
double consPower = 0;

/** Collected power consumption of last 10 minutes of the house from spMonitor */
double avgConsPower[10] = {0,0,0,0,0,0,0,0,0,0};
/** Last temperature set by user before switching to DRY mode */
byte savedAcTemp = 0;
/** Last temperature saved by switching to DRY mode */
bool savedAcTempByDry = false;
