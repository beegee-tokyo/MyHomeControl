/**
	Security Monitor

	Hardware
	Adafruit HUZZAH ESP8266 (ESP-12) module - https://www.adafruit.com/products/2471
	Adafruit PIR (motion) sensor - https://www.adafruit.com/products/189
	optional (not used) Adafruit TSL2561 Digital Luminosity/Lux/Light Sensor - http://www.adafruit.com/products/439
	dfRobot Relay Modular V3.1 (10A/220V max) - http://www.dfrobot.com/index.php?route=product/product&product_id=64#.VmBYpXYrJpg

	Uses PIR sensor to detect measurement.
	Relay to switch on external light.
	Light on time 2 minutes, extended if PIR sensor is retriggered
	Light on depending on light value read from LDR
	Playing an alarm sound if enabled

	@author Bernd Giesecke
	@version 0.4 beta March, 2016.
*/

/* Security.h contains all includes, defines, variables and instance declarations */
#include "Security_declarations.h"
/* Security_function.h contains all function declarations */
#include "Security_functions.h"
