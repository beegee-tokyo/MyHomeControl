#ifndef leds_h
#define leds_h

#include <Arduino.h>
#include <Ticker.h>

/** Red LED on GPIO0 for visual signal if alarm is on or off */
#define actLED 0
/** Blue LED on GPIO2 for communication activities */
#define comLED 2

/** Timer for flashing red detection/alarm/activity LED */
extern Ticker actFlasher;
/** Timer for flashing blue communication LED */
extern Ticker comFlasher;
/** Timer for flashing both blue and red leds */
extern Ticker doubleFlasher;

void initLeds();
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
