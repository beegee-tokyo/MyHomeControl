/** IP address for tcp server */
extern char * const tcpServer;
/** Port for TCP socket server */
extern uint16_t const tcpPort;
/** IP address for UDP client */
extern char * const udpServer;
/** Port for UDP client */
extern uint16_t const udpPort;

/** Serial port to ESP8266 */
extern SC16IS750 i2cuart;
/** Connection to esp-link using the I2Cuart chip of the Arduino Uno WiFi */
extern ELClient esp;
/** Connection to esp-link using the I2Cuart chip of the Arduino Uno WiFi */
extern ELClient esp2;
/** TCP socket client on the connection to esp-link */
extern ELClientSocket tcp;
/** UDP client on the connection to esp-link */
extern ELClientSocket udp;
// Initialize CMD client
extern ELClientCmd cmd;

/** Instance of IR decoder */
extern IRdecode myDecoder;
/** Instance of IR receiver */
extern IRrecv myReceiver;

/** Flag for boot status */
extern boolean inSetup;

/** Last IR command received used for repeating command */
extern long lastReceived;
/** PWM output for LED bulb control */
extern int led;
/** Control LED port */
extern int controlLed;
/** Brightness of LED bulbs */
extern int brightness;
/** Dimmed brightness of LED bulbs */
extern int dimValue;
/** Array of commands */
extern const unsigned int cmdArray[];

/** Timervalue for frequent status updates */
extern long lastTime;

// SkyBox Digibox codes:
#define irOn_Off	0x80BF3BC4
#define irMute		0x80BF39C6
#define ir1			0x80BF49B6
#define ir2			0x80BFC936
#define ir3			0x80BF33CC
#define ir4			0x80BF718E
#define ir5			0x80BFF10E
#define ir6			0x80BF13EC
#define ir7			0x80BF51AE
#define ir8			0x80BFD12E
#define ir9			0x80BF23DC
// #define irChScan	0x80BF11EE
#define ir0			0x80BFE11E
// #define irRecall	0x80BF41BE
#define irCHp		0x80BF01FE
#define irCHm		0x80BF817E
#define irUpUp		0x80BFBB44
#define irDownDown	0x80BF31CE
#define irVp		0x80BFA15E
#define irVm		0x80BF619E
#define irUp		0x80BF53AC
#define irLeft		0x80BF9966
#define irRight		0x80BF837C
#define irDown		0x80BF4BB4
// #define irMenu		0x80BFA956
// #define irExit		0x80BFA35C
#define irStatus	0x80BF916E
#define irAudio		0x80BF21DE
#define irInfo		0x80BF9B64
#define irEPG		0x80BF6996
// #define irMail		0x80BF5BA4
// #define irGames		0x80BFC13E
// #define irLock		0x80BFC33C
// #define irFAV		0x80BF6B94
// #define irUnused1	0x80BFB34C
// #define irUnused2	0x80BFB14E
// #define irSet		0x80BF8976
// #define irBack		0x80BF0BF4
// #define irUnused3	0x80BF936C
// #define irUnused4	0x80BFF30C
// #define irUnused5	0x80BF19E6
// #define irUnused6	0x80BFB946
#define irRepeat	0xFFFFFFFF
