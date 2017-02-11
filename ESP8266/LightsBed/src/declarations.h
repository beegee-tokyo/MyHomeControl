#include "Setup.h"

/** IP address for tcp server */
char * const tcpServer PROGMEM = (char *) "192.168.0.151";
/** Port for TCP socket server */
uint16_t const tcpPort PROGMEM = 6000;
/** IP address for UDP client */
char * const udpServer PROGMEM = (char *) "192.168.0.255"; // Broadcast to given network ip mask
/** Port for UDP client */
uint16_t const udpPort PROGMEM = 5000;

/** Serial port to ESP8266 */
SC16IS750 i2cuart = SC16IS750(SC16IS750_PROTOCOL_I2C,SC16IS750_ADDRESS_AA);
/** Connection to esp-link using the I2Cuart chip of the Arduino Uno WiFi */
ELClient esp(&i2cuart, &Serial);
/** Connection to esp-link using the I2Cuart chip of the Arduino Uno WiFi */
ELClient esp2(&i2cuart, &Serial);
/** TCP socket server on the connection to esp-link */
ELClientSocket tcp(&esp);
/** UDP client on the connection to esp-link */
ELClientSocket udp(&esp2);
// Initialize CMD client
ELClientCmd cmd(&esp);

/** Instance of IR decoder */
IRdecode myDecoder;
/** Instance of IR receiver */
IRrecv myReceiver(2);  //pin number for the receiver

/** Flag for boot status */
boolean inSetup = true;

/** Last IR command received used for repeating command */
long lastReceived = 0;
/** PWM output for LED bulb control */
int led = 9;
/** Control LED port */
int controlLed = 3;
/** Brightness of LED bulbs */
int brightness = 140; // Limit max brightness because of heat
/** Dimmed brightness of LED bulbs */
int dimValue = 200;

/** Array of commands */
const unsigned int cmdArray[12] PROGMEM = {0x3BC4,0x39C6,0x01FE,0x817E,0xBB44,0x31CE,0x9966,0x837C,0x916E,0x21DE,0x9B64,0x6996};

/** Timervalue for frequent status updates */
long lastTime;
