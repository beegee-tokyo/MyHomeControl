#ifndef leds_h
#define leds_h

#include <Arduino.h>
#include <Ticker.h>

/** Timer for flashing red detection/alarm/activity LED */
extern Ticker actFlasher;
/** Timer for flashing blue communication LED */
extern Ticker comFlasher;
/** Timer for flashing both blue and red leds */
extern Ticker doubleFlasher;

void initLeds(uint8_t reqComLED = 2, uint8_t reqActLED = 0); // defaults to Adafruit Huzzah breakout ports 
void actLedFlashStart(float flashTime);
void comLedFlashStart(float flashTime);
void doubleLedFlashStart(float flashTime);
void actLedFlashStop();
void comLedFlashStop();
void doubleLedFlashStop();
void actLedFlash();
void comLedFlash();
void doubleLedFlash();

#endif
