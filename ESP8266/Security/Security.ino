/**
	Security Monitor frontyard

	Hardware
	Adafruit HUZZAH ESP8266 (ESP-12) module - https://www.adafruit.com/products/2471
	Adafruit PIR (motion) sensor - https://www.adafruit.com/products/189
	dfRobot Relay Modular V3.1 (10A/220V max) - http://www.dfrobot.com/index.php?route=product/product&product_id=64#.VmBYpXYrJpg

	Uses PIR sensor to detect measurement.
	Relay to switch on external light.
	Light on time 2 minutes, extended if PIR sensor is retriggered
	Light on depending on light value read from LDR
	Playing an alarm sound if enabled

	@author Bernd Giesecke
	@version 0.5 beta May, 2016.
*/

/* Security.h contains all includes, defines, variables and instance declarations */
#include "declarations.h"
/* Security_function.h contains all function declarations */
#include "functions.h"
