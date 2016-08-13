// /**************************************************************************/
// /*
		// Adafruit sensor routines taken from "sensorapi.pde"
// */
// /**************************************************************************/
// /**
 // * Configures the gain and integration time for the TSL2561
 // */
// void configureSensor () {
	// /* You can also manually set the gain or enable auto-gain support */
// //	tsl.enableAutoRange ( true );				/* Auto-gain ... switches automatically between 1x and 16x */
	// tsl.setGain(TSL2561_GAIN_16X);

	// /* Changing the integration time gives you better sensor resolution (402ms = 16-bit data) */
	// tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_402MS); /* 16-bit data but slowest conversions */
// }

// // /**
 // // * Get current light measurement.
 // // * Function makes 3 measurements and returns the average value.
 // // * Function adapts integration time in case of sensor overload
 // // * Result is stored in global variable lightValue
 // // */
// void getLight () {
	// /** Sensor event reads value from the sensor */
	// sensors_event_t event;

	// /** Result of light measurement for this call */
	// long currLightValue = 0;
	// tsl.getEvent (&event);
	// for (int i = 0; i < 3; i++) { // do 3 runs, in case we get saturation
		// wdt_reset();
		// /* Display the results (light is measured in lux) */
		// if (tsl.getEvent (&event)) {
			// /** Int value read from AD conv for sun measurement */
			// currLightValue = event.light;
			// // Serial.println("Light result = " + String(currLightValue) + " lux Integration = " + String(lightInteg));
			// wdt_reset();
			// if (lightInteg == 1) { /* we are at medium integration time, try a higher one */
				// tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_402MS); /* 16-bit data but slowest conversions */
				// /* Test new integration time */
				// //tsl.getEvent (&event);

				// wdt_reset();
				// if (!tsl.getEvent (&event)) {
					// /* Satured, switch back to medium integration time */
					// tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_101MS); /* medium resolution and speed	*/
				// } else {
					// lightInteg = 2;
					// // Serial.println("Light result = " + String(currLightValue) + " lux switch to Integration = " + String(lightInteg));
				// }
			// } else if (lightInteg == 0) { /* we are at lowest integration time, try a higher one */
				// tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_101MS); /* medium resolution and speed	*/
				// /* Test new integration time */
				// //tsl.getEvent (&event);
				// wdt_reset();
				// if (!tsl.getEvent (&event)) {
					// /* Satured, switch back to low integration time */
					// tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_13MS); /* fast but low resolution */
				// } else {
					// lightInteg = 1;
					// // Serial.println("Light result = " + String(currLightValue) + " lux switch to Integration = " + String(lightInteg));
				// }
			// }
		// } else {
			// /* If event.light = 0 lux the sensor is probably saturated and no reliable data could be generated! */
			// // Serial.println("Light result = saturated Integration = " + String(lightInteg));
			// if (lightInteg == 2) { /* we are at highest integration time, try a lower one */
				// tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_101MS); /* medium resolution and speed	*/
				// //tsl.getEvent (&event);
				// wdt_reset();
				// if (!tsl.getEvent (&event)) { /* Still saturated? */
					// lightInteg = 0;
					// tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_13MS); /* fast but low resolution */
					// //tsl.getEvent (&event);
					// wdt_reset();
					// if (tsl.getEvent (&event)) { /* Got a result now? */
						// currLightValue = event.light;
						// // Serial.println("Light result = " + String(currLightValue) + " lux switch to Integration = " + String(lightInteg));
					// } else {
						// currLightValue = 65536; // Still saturated at lowest integration time, assume max level of light
					// }
				// } else {
					// // Serial.println("Light result = saturated Integration = " + String(lightInteg));
					// lightInteg = 1;
					// currLightValue = event.light;
				// }
			// } else if (lightInteg == 1) { /* we are at medium integration time, try a lower one */
				// lightInteg = 0;
				// tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_13MS); /* fast but low resolution */
				// //tsl.getEvent (&event);
				// wdt_reset();
				// if (tsl.getEvent (&event)) { /* Got a result now? */
					// currLightValue = event.light;
					// // Serial.println("Light result = " + String(currLightValue) + " lux switch to Integration = " + String(lightInteg));
				// } else {
					// lightInteg = 0;
					// tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_13MS); /* fast but low resolution */
					// currLightValue = 65536; // Still saturated at lowest integration time, assume max level of light
				// }
			// } else {
					// lightInteg = 0;
					// tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_13MS); /* fast but low resolution */
					// currLightValue = 65536; // Still saturated at lowest integration time, assume max level of light
			// }
		// }
	// }
	// lightValue = currLightValue;
// }

// // void getLight() {
	// // /** Sensor event reads value from the sensor */
	// // sensors_event_t event;
	// // int thisLightInteg = 402;
	// // int thisGain = 16;

	// // // Start with 402ms and 16x gain
	// // tsl.setGain(TSL2561_GAIN_16X);
	// // tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_402MS); /* 16-bit data but slowest conversions */

	// // if (tsl.getEvent (&event)) { // 402ms 16x ok
		// // lightValue = event.light;
	// // } else { // change to 402ms 1x
		// // tsl.setGain(TSL2561_GAIN_1X); // change to 1x
		// // if (tsl.getEvent (&event)) { // 402ms 1x ok
			// // thisGain = 1;
			// // thisLightInteg = 402;
			// // lightValue = event.light;
		// // } else { // change to 101ms 16x
			// // tsl.setGain(TSL2561_GAIN_16X); // change to 16x
			// // tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_101MS); // change to 101ms
			// // if (tsl.getEvent (&event)) { // 101ms 16x ok
				// // thisGain = 16;
				// // thisLightInteg = 101;
				// // lightValue = event.light;
			// // } else { // change to 101ms 1x
				// // tsl.setGain(TSL2561_GAIN_1X); // change to 1x
				// // if (tsl.getEvent (&event)) { // 101ms 1x ok
					// // thisGain = 1;
					// // thisLightInteg = 101;
					// // lightValue = event.light;
				// // } else { // change to 13ms 16x
					// // tsl.setGain(TSL2561_GAIN_16X); // change to 16x
					// // tsl.setIntegrationTime (TSL2561_INTEGRATIONTIME_13MS); // change to 13ms
					// // if (tsl.getEvent (&event)) { // 13ms 16x ok
						// // thisGain = 16;
						// // thisLightInteg = 13;
						// // lightValue = event.light;
					// // } else { // change to 13ms 1x
						// // tsl.setGain(TSL2561_GAIN_1X); // change to 1x
						// // if (tsl.getEvent (&event)) { // 13ms 1x ok
							// // thisGain = 1;
							// // thisLightInteg = 13;
							// // lightValue = event.light;
						// // } else {
							// // thisGain = -1;
							// // thisLightInteg = -1;
							// // lightValue = 65536;
						// // }
					// // }
				// // }
			// // }
		// // }
	// // }
// // String debugMsg = "Light value = "+String(lightValue)+" measured with "+String(thisLightInteg)+"ms and a gain of "+String(thisGain);
// // sendDebug(debugMsg);
	// // uint16_t broadLight = 0;
	// // uint16_t irLight = 0;
	// // // uint32_t calculatedLight = 0;
	// // tsl.getLuminosity(&broadLight, &irLight);
	// // lightValue = broadLight;
	// // calculatedLight = tsl.calculateLux(broadLight, irLight);
// // debugMsg = "Light value = "+String(broadLight)+" IR value = "+String(irLight)+" calc. light = "+String(calculatedLight);
// // sendDebug(debugMsg);
// // }
