#include "leds.h"

/** Timer for flashing red detection/alarm/activity LED */
Ticker actFlasher;
/** Timer for flashing blue communication LED */
Ticker comFlasher;
/** Timer for flashing both blue and red leds */
Ticker doubleFlasher;

/** Port for blue LED */
uint8_t comLED;
/** Port for red LED */
uint8_t actLED;

/**
 * Initialize LED pins
 */
void initLeds(uint8_t reqComLED, uint8_t reqActLED) {
	comLED = reqComLED;
	actLED = reqActLED;
	
	pinMode(comLED, OUTPUT); // Communication LED blue
	pinMode(actLED, OUTPUT); // Communication LED red
	digitalWrite(comLED, HIGH); // Turn off blue LED
	digitalWrite(actLED, HIGH); // Turn off red LED
}

/**
 * Start flashing of red led
 */
void actLedFlashStart(float flashTime) {
	actFlasher.attach(flashTime, actLedFlash);
}

/**
 * Start flashing of blue led
 */
void comLedFlashStart(float flashTime) {
	comFlasher.attach(flashTime, comLedFlash);
}

/**
 * Start flashing of both led
 */
void doubleLedFlashStart(float flashTime) {
	digitalWrite(actLED, LOW); // Turn on red LED
	digitalWrite(comLED, HIGH); // Turn off blue LED
	doubleFlasher.attach(flashTime, doubleLedFlash);
}

/**
 * Start flashing of red led
 */
void actLedFlashStop() {
	digitalWrite(actLED, HIGH); // Turn off red LED
	actFlasher.detach();
}

/**
 * Start flashing of blue led
 */
void comLedFlashStop() {
	digitalWrite(comLED, HIGH); // Turn off blue LED
	comFlasher.detach();
}

/**
 * Start flashing of both led
 */
void doubleLedFlashStop() {
	digitalWrite(actLED, HIGH); // Turn off red LED
	digitalWrite(comLED, HIGH); // Turn off blue LED
	doubleFlasher.detach();
}

/**
 * Change status of red led on each call
 * called by Ticker actFlasher
 */
void actLedFlash() {
	int state = digitalRead(actLED);
	digitalWrite(actLED, !state);
}

/**
 * Change status of blue led on each call
 * called by Ticker comFlasher
 */
void comLedFlash() {
	int state = digitalRead(comLED);
	digitalWrite(comLED, !state);
}

/**
 * Change status of blue led on each call
 * called by Ticker comFlasher
 */
void doubleLedFlash() {
	int state = digitalRead(comLED);
	digitalWrite(comLED, !state);
	digitalWrite(actLED, state);
}
