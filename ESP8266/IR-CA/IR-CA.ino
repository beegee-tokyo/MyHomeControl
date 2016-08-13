/**
	Web controlled IR remote for Carrier aircon

	Hardware
	Adafruit HUZZAH ESP8266 (ESP-12) module - https://www.adafruit.com/products/2471
	S9014 NPN transistor
	330 Ohm resistor
	2x IR led Vishay TSUS4300 IR LED https://ph.rs-online.com/web/p/ir-leds/7082835/

	Receives commands for the aircon through WiFi.
	Checks production of solar panels
	Switch on/off the aircon depending on command or production of solar panels.

	@author Bernd Giesecke
	@version 0.3 beta April, 2016.
*/

/* Security.h contains all includes, defines, variables and instance declarations */
#include "declarations.h"
/* Security_function.h contains all function declarations */
#include "functions.h"
