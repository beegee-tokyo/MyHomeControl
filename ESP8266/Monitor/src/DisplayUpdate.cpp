#include "Setup.h"

/** Current line position for scrolling text */
byte currLine = 10;

/**
	updateDisplay
	Updates display for different layouts
*/
void updateDisplay(boolean all) {
	switch (displayLayout) {
		case 1: // Weather & time display layout
			updateWeather(all);
			break;
		case 2: // AC detail display layout
			updateAC(all);
			break;
		case 3: // Security detail display layout
			updateSecurity(all);
			break;
		case 4: // Solar data display layout
			updateSolar(all);
			break;
		default: // default display layout
			updateSolar(all);
			updateWeather(all);
			updateAC(all);
			updateSecurity(all);
			break;
	}
}

/**
	updateSolar
	Update solar values
*/
void updateSolar(boolean all) {
	/** String for some constructed text */
	String outText = "";
	if (displayLayout == 0) { // default display layout
		// Draw background boxes
		ucg.setColor(255, 255, 0);
		if (all) {
			ucg.drawBox(0, 0, 128, 22); // Solar production
		} else {
			ucg.drawBox(30, 0, 94, 22); // Solar production
		}
		if (consPower < 0.0) {
			ucg.setColor(0, 255, 0);
		} else {
			ucg.setColor(255, 0, 0);
		}
		ucg.drawBox(0, 22, 128, 22); // House consumption

		// Print fixed text for solar values
		ucg.setFont(ucg_font_helvB18_tr);
		ucg.setColor(0, 0, 0);
		if (all) {
			ucg.setPrintPos(0,20);
			ucg.print("S");
		}

		// Print solar values
		ucg.setFont(ucg_font_helvB18_tr);
		ucg_print_center(String(solarPower,0) + "W", 15, 20);
		ucg.setPrintPos(0,42);
		ucg.print("C");
		ucg_print_center(String(abs(consPower),0) + "W", 15, 42);
	} else if (displayLayout == 4) {
		// Draw background boxes
		ucg.setColor(255, 255, 0);
		if (all) {
			ucg.drawBox(0, 0, 128, 64); // Solar production
		} else {
			ucg.drawBox(30, 0, 98, 64); // Solar production
		}
		if (consPower < 0.0) {
			ucg.setColor(0, 255, 0);
		} else {
			ucg.setColor(255, 0, 0);
		}
		ucg.drawBox(0, 64, 128, 64); // House consumption

		// Print fixed text for solar values
		ucg.setFont(ucg_font_helvB24_tr);
		ucg.setColor(0, 0, 0);
		if (all) {
			ucg.setPrintPos(0,44);
			ucg.print("S");
		}

		// Print solar values
		ucg.setFont(ucg_font_helvB24_tr);
		ucg_print_center(String(solarPower,0) + "W", 20, 44);
		ucg.setPrintPos(0,108);
		ucg.print("C");
		ucg_print_center(String(abs(consPower),0) + "W", 20, 108);
	}
}

/**
	updateWeather
	Update weather values
*/
void updateWeather(boolean all) {
	/** String for some constructed text */
	String outText = "";
	if (displayLayout == 0) {
		// Print fixed text for weather values & background boxes
		ucg.setFont(ucg_font_helvB10_tr);
		if (all) {
			ucg.setColor(0, 0, 0);
			ucg.drawBox(0, 44, 128, 21); // Weather text
			ucg.setColor(255, 255, 255);
			ucg.setPrintPos(10,61);
			outText = "1C";
			outText.setCharAt(0, 176);
			ucg.print(outText);
			ucg.setPrintPos(50,61);
			ucg.print("%h");
			ucg.setPrintPos(90,61);
			ucg.print("Feel");
			ucg.setColor(0, 0, 0);
		}

		// Clear weather values
		ucg.setColor(255, 255, 255);
		ucg.drawBox(0, 65, 128, 42);

		// Print inside weather values
		ucg.setColor(0, 0, 0);
		ucg.setFont(ucg_font_helvB10_tr);
		ucg.setPrintPos(1,80);
		ucg.print("I");
		ucg.setPrintPos(12,80);
		if (sensorReadings != 0) {
			ucg.print(String((tempInside/sensorReadings),1));
			ucg.setPrintPos(50,80);
			ucg.print(String((humidInside/sensorReadings),1));
			ucg.setPrintPos(90,80);
			ucg.print(String(dht.computeHeatIndex(tempInside/sensorReadings, humidInside/sensorReadings, false),1));
		} else {
			ucg.print(String((tempInside),1));
			ucg.setPrintPos(50,80);
			ucg.print(String((humidInside),1));
			ucg.setPrintPos(90,80);
			ucg.print(String(dht.computeHeatIndex(tempInside, humidInside, false),1));
		}

		// Print outside weather values
		ucg.setFont(ucg_font_helvB10_tr);
		ucg.setColor(0, 0, 0);
		ucg.setPrintPos(1,102);
		ucg.print("E");
		if (secBackOn == 2) {
			ucg.setColor(128, 128, 128);
			outText = "NA";
			ucg.setPrintPos(12,102);
			ucg.print(outText);
			ucg.setPrintPos(50,102);
			ucg.print(outText);
			ucg.setPrintPos(90,102);
			ucg.print(outText);
		} else {
			ucg.setPrintPos(12,102);
			ucg.print(String(tempOutside,1));
			ucg.setPrintPos(50,102);
			ucg.print(String(humidOutside,1));
			ucg.setPrintPos(90,102);
			ucg.print(String(heatIndexOut,1));
		}
	} else if (displayLayout == 1) {
		// Draw background boxes
		ucg.setColor(255, 255, 255);
		ucg.drawBox(0, 0, 128, 22); // Date & time
		ucg.setPrintPos(1,18);
		ucg.setFont(ucg_font_helvB12_tr);
		outText = digitalClockDisplay();
		ucg.setColor(0, 0, 0);
		ucg.print(outText);

		// Print fixed text for weather values & background boxes
		ucg.setFont(ucg_font_helvB14_tr);
		if (all) {
			ucg.drawBox(0, 22, 128, 22); // Location text
			ucg.drawBox(0, 44, 40, 84); // Weather text
			ucg.setColor(255, 255, 255);
			ucg.setPrintPos(5,65);
			outText = "1C";
			outText.setCharAt(0, 176);
			ucg.print(outText);
			ucg.setPrintPos(5,93);
			ucg.print("%h");
			ucg.setPrintPos(45,40);
			ucg.print("IN");
			ucg.setPrintPos(89,40);
			ucg.print("OUT");
			ucg.setPrintPos(5,121);
			ucg.setFont(ucg_font_helvB12_tr);
			ucg.print("Feel");
			ucg.setColor(0, 0, 0);
		}

		// Clear weather values
		ucg.setColor(255, 255, 255);
		ucg.drawBox(40, 44, 98, 94);

		// Print inside weather values
		ucg.setColor(0, 0, 0);
		ucg.setFont(ucg_font_helvB14_tr);
		ucg.setPrintPos(45,65);
		if (sensorReadings != 0) {
			ucg.print(String((tempInside/sensorReadings),1));
			ucg.setPrintPos(45,93);
			ucg.print(String((humidInside/sensorReadings),1));
			ucg.setPrintPos(45,121);
			ucg.print(String(dht.computeHeatIndex(tempInside/sensorReadings, humidInside/sensorReadings, false),1));
		} else {
			ucg.print(String((tempInside),1));
			ucg.setPrintPos(45,93);
			ucg.print(String((humidInside),1));
			ucg.setPrintPos(45,121);
			ucg.print(String(dht.computeHeatIndex(tempInside, humidInside, false),1));
		}

		// Print outside weather values
		if (secBackOn == 2) {
			ucg.setColor(128, 128, 128);
			outText = "NA";
			ucg.setFont(ucg_font_helvB14_tr);
			ucg.setPrintPos(89,65);
			ucg.print(outText);
			ucg.setPrintPos(89,93);
			ucg.print(outText);
			ucg.setPrintPos(89,121);
			ucg.print(outText);
		} else {
			ucg.setColor(0, 0, 0);
			ucg.setFont(ucg_font_helvB14_tr);
			ucg.setPrintPos(89,65);
			ucg.print(String(tempOutside,1));
			ucg.setPrintPos(89,93);
			ucg.print(String(humidOutside,1));
			ucg.setPrintPos(89,121);
			ucg.print(String(heatIndexOut,1));
		}
	}
}

/**
	updateAC
	Update AC values
*/
void updateAC(boolean all) {
	if (displayLayout == 0) {
		// Draw background boxes
		ucg.setColor(0, 0, 0);
		ucg.drawBox (0, 107, 71, 21);

		// Print AC 1 status
		ucg.setFont(ucg_font_helvB10_tr);
		if (ac1On == 2) {
			ucg.setColor(128, 128, 128);
			ucg.setPrintPos(0,124);
			ucg.print("A1 -");
		} else {
			if (ac1On == 1) {
				ucg.setColor(255, 0, 0);
			} else {
				ucg.setColor(0, 255, 255);
			}
			ucg.setPrintPos(0,124);
			ucg.print("A1");

			ucg.setFont(ucg_font_helvB08_tr);
			if (ac1On == 1) {
				ucg.setPrintPos(22,117);
				if (ac1Mode == 2) {
					ucg.setColor(0, 255, 255);
					ucg.print("C");
				} else if (ac1Mode == 1) {
					ucg.setColor(0, 255, 0);
					ucg.print("D");
				} else {
					ucg.setColor(0, 255, 255);
					ucg.print("F");
				}
			}

			ucg.setPrintPos(22,127);
			if (ac1Timer == 1) {
				ucg.setColor(0, 255, 255);
				ucg.print("T");
			} else {
				if (ac1Auto == 1) {
					ucg.setColor(0, 255, 0);
					ucg.print("A");
				} else {
					ucg.setColor(255, 0, 0);
					ucg.print("M");
				}
			}
		}

		// Print AC 2 status
		ucg.setFont(ucg_font_helvB10_tr);
		if (ac2On == 2) {
			ucg.setColor(128, 128, 128);
			ucg.setPrintPos(35,124);
			ucg.print("A2 -");
		} else {
			if (ac2On == 1) {
				ucg.setColor(255, 0, 0);
			} else {
				ucg.setColor(0, 255, 255);
			}
			ucg.setPrintPos(35,124);
			ucg.print("A2");

			ucg.setFont(ucg_font_helvB08_tr);
			if (ac2On == 1) {
				ucg.setPrintPos(57,117);
				if (ac2Mode == 2) {
					ucg.setColor(0, 255, 255);
					ucg.print("C");
				} else if (ac2Mode == 1) {
					ucg.setColor(0, 255, 0);
					ucg.print("D");
				} else {
					ucg.setColor(0, 255, 255);
					ucg.print("F");
				}
			}

			ucg.setPrintPos(57,127);
			if (ac2Timer == 1) {
				ucg.setColor(0, 0, 255);
				ucg.print("T");
			} else {
				if (ac2Auto == 1) {
					ucg.setColor(0, 255, 0);
					ucg.print("A");
				} else {
					ucg.setColor(255, 0, 0);
					ucg.print("M");
				}
			}
		}
	} else if (displayLayout == 2) {
		String outText;
		ucg.setFont(ucg_font_helvB10_tr);
		// Draw background boxes
		ucg.setColor(0, 0, 0);
		if (all) {
			ucg.drawBox (0, 0, 128, 23);
			ucg.drawBox (0, 23, 40, 125);
			ucg.setColor(255, 255, 255);
			ucg.setPrintPos(1,41);
			ucg.print("Stat");
			ucg.setPrintPos(1,62);
			ucg.print("Mode");
			ucg.setPrintPos(1,83);
			ucg.print("Auto");
			ucg.setPrintPos(1,104);
			ucg.print("Fan");
			ucg.setPrintPos(1,125);
			ucg.print("Temp");
			ucg.setFont(ucg_font_helvB10_tr);
			ucg.setPrintPos(40,17);
			ucg.print("Office");
			ucg.setPrintPos(84,17);
			ucg.print("Living");
		}
		ucg.setColor(255, 255, 255);
		ucg.drawBox (40, 23, 88, 105);

		// Print office AC status
		ucg.setFont(ucg_font_helvB18_tr);
		if (ac1On == 2) {
			ucg.setColor(128, 128, 128);
			outText = "NA";
			ucg.setPrintPos(42,43);
			ucg.print(outText);
			ucg.setPrintPos(42,64);
			ucg.print(outText);
			ucg.setPrintPos(42,85);
			ucg.print(outText);
			ucg.setPrintPos(42,106);
			ucg.print(outText);
			ucg.setPrintPos(42,127);
			ucg.print(outText);
		} else {
			ucg.setPrintPos(42,43);
			if (ac1On == 1) {
				ucg.setColor(255, 0, 0);
				ucg.print("On");
			} else {
				ucg.setColor(0, 0, 255);
				ucg.print("Off");
			}

			ucg.setPrintPos(42,64);
			if (ac1Mode == 2) {
				ucg.setColor(0, 0, 255);
				ucg.setFont(ucg_font_helvB14_tr);
				ucg.print("Cool");
			} else if (ac1Mode == 1) {
				ucg.setColor(0, 255, 0);
				ucg.print("Dry");
			} else {
				ucg.setColor(255, 0, 0);
				ucg.print("Fan");
			}
			ucg.setFont(ucg_font_helvB18_tr);

			ucg.setPrintPos(42,85);
			if (ac1Timer == 1) {
				ucg.setColor(0, 0, 255);
				ucg.print("Tim");
			} else {
				if (ac1Auto == 1) {
					ucg.setColor(0, 255, 0);
					ucg.setFont(ucg_font_helvB14_tr);
					ucg.print("Auto");
				} else {
					ucg.setColor(255, 0, 0);
					ucg.print("Man");
				}
			}
			ucg.setFont(ucg_font_helvB18_tr);

			ucg.setPrintPos(42,106);
			if (ac1Speed == 0) {
				ucg.setColor(0, 0, 255);
				ucg.print("Low");
			} else if (ac1Speed == 1) {
				ucg.setColor(0, 0, 255);
				ucg.print("Med");
			} else {
				ucg.setColor(0, 0, 255);
				ucg.setFont(ucg_font_helvB14_tr);
				ucg.print("High");
			}
			ucg.setFont(ucg_font_helvB18_tr);

			ucg.setPrintPos(42,127);
			outText = String(ac1Temp);
			ucg.setColor(0, 0, 255);
			ucg.print(outText);
		}

		// Print living room AC status
		ucg.setFont(ucg_font_helvB18_tr);
		if (ac2On == 2) {
			ucg.setColor(128, 128, 128);
			outText = "NA";
			ucg.setPrintPos(89,43);
			ucg.print(outText);
			ucg.setPrintPos(89,64);
			ucg.print(outText);
			ucg.setPrintPos(89,85);
			ucg.print(outText);
			ucg.setPrintPos(89,106);
			ucg.print(outText);
			ucg.setPrintPos(89,127);
			ucg.print(outText);
		} else {
			ucg.setPrintPos(89,43);
			if (ac2On == 1) {
				ucg.setColor(255, 0, 0);
				ucg.print("On");
			} else {
				ucg.setColor(0, 0, 255);
				ucg.print("Off");
			}

			ucg.setPrintPos(89,64);
			if (ac2Mode == 2) {
				ucg.setColor(0, 0, 255);
				ucg.setFont(ucg_font_helvB14_tr);
				ucg.print("Cool");
			} else if (ac2Mode == 1) {
				ucg.setColor(0, 255, 0);
				ucg.print("Dry");
			} else {
				ucg.setColor(255, 0, 0);
				ucg.print("Fan");
			}
			ucg.setFont(ucg_font_helvB18_tr);

			ucg.setPrintPos(89,85);
			if (ac2Timer == 1) {
				ucg.setColor(0, 0, 255);
				ucg.print("Tim");
			} else {
				if (ac2Auto == 1) {
					ucg.setColor(0, 255, 0);
					ucg.setFont(ucg_font_helvB14_tr);
					ucg.print("Auto");
				} else {
					ucg.setColor(255, 0, 0);
					ucg.print("Man");
				}
			}
			ucg.setFont(ucg_font_helvB18_tr);

			ucg.setPrintPos(89,106);
			if (ac2Speed == 0) {
				ucg.setColor(0, 0, 255);
				ucg.print("Low");
			} else if (ac2Speed == 1) {
				ucg.setColor(0, 0, 255);
				ucg.print("Med");
			} else {
				ucg.setColor(0, 0, 255);
				ucg.setFont(ucg_font_helvB14_tr);
				ucg.print("High");
			}
			ucg.setFont(ucg_font_helvB18_tr);

			ucg.setPrintPos(89,127);
			outText = String(ac2Temp);
			ucg.setColor(0, 0, 255);
			ucg.print(outText);
		}
	}
}

/**
	updateSecurity
	Update Security values
*/
void updateSecurity(boolean all) {
	String outText;
	if (displayLayout == 0) {
		// Draw background boxes
		ucg.setColor(0, 0, 0);
		ucg.drawBox(71, 107, 57, 21);
		ucg.setFont(ucg_font_helvB10_tr);

		// Print Security status
		if (secFrontOn == 2) {
			ucg.setColor(128, 128, 128);
			ucg.setPrintPos(71,124);
			ucg.print("SF -");
		} else {
			if (secFrontOn == 1) {
				ucg.setColor(255, 0, 0);
			} else {
				if (secFrontAuto == 1) {
					ucg.setColor(0, 255, 0);
				} else {
					ucg.setColor(0, 255, 255);
				}
			}
			ucg.setPrintPos(71,124);
			ucg.print("SF");

			if (secFrontLight == 1) {
				ucg.setColor(255, 0, 0);
			} else {
				ucg.setColor(0, 255, 255);
			}
			ucg.setFont(ucg_font_helvB08_tr);
			ucg.setPrintPos(93,122);
			ucg.print("L");
		}

		ucg.setFont(ucg_font_helvB10_tr);
		if (secBackOn == 2) {
			ucg.setColor(128, 128, 128);
			ucg.setPrintPos(101,124);
			ucg.print("SB -");
		} else {
			if (secBackOn == 1) {
				ucg.setColor(255, 0, 0);
			} else {
				if (secBackAuto == 1) {
					ucg.setColor(0, 255, 0);
				} else {
					ucg.setColor(0, 255, 255);
				}
			}
			ucg.setPrintPos(101,124);
			ucg.print("SB");

			if (secBackLight == 1) {
				ucg.setColor(255, 0, 0);
			} else {
				ucg.setColor(0, 255, 255);
			}
			ucg.setFont(ucg_font_helvB08_tr);
			ucg.setPrintPos(123,122);
			ucg.print("L");
		}
	} else if (displayLayout == 3) {
		String outText;
		ucg.setFont(ucg_font_helvB10_tr);
		// Draw background boxes
		ucg.setColor(0, 0, 0);
		if (all) {
			ucg.drawBox (0, 0, 128, 23);
			ucg.drawBox (0, 23, 40, 125);
			ucg.setColor(255, 255, 255);
			ucg.setPrintPos(1,41);
			ucg.print("Alarm");
			ucg.setPrintPos(1,62);
			ucg.print("Light");
			ucg.setPrintPos(1,83);
			ucg.print("Auto");
			ucg.setPrintPos(1,104);
			ucg.print("On");
			ucg.setPrintPos(1,125);
			ucg.print("Off");
			ucg.setFont(ucg_font_helvB10_tr);
			ucg.setPrintPos(40,17);
			ucg.print("Front");
			ucg.setPrintPos(84,17);
			ucg.print("Back");
		}
		ucg.setColor(255, 255, 255);
		ucg.drawBox (40, 23, 88, 105);

		ucg.setFont(ucg_font_helvB18_tr);
		// Print Security front status
		if (secFrontOn == 2) {
			ucg.setColor(128, 128, 128);
			outText = "NA";
			ucg.setPrintPos(42,43);
			ucg.print(outText);
			ucg.setPrintPos(42,64);
			ucg.print(outText);
			ucg.setPrintPos(42,85);
			ucg.print(outText);
			ucg.setPrintPos(42,106);
			ucg.print(outText);
			ucg.setPrintPos(42,127);
			ucg.print(outText);
		} else {
			ucg.setPrintPos(42,43);
			if (secFrontOn == 1) {
				ucg.setColor(255, 0, 0);
				ucg.print("On");
			} else {
				ucg.setColor(0, 0, 255);
				ucg.print("Off");
			}

			ucg.setPrintPos(42,64);
			if (secFrontLight == 1) {
				ucg.setColor(255, 0, 0);
				ucg.print("On");
			} else {
				ucg.setColor(0, 0, 255);
				ucg.print("Off");
			}

			ucg.setPrintPos(42,85);
			if (secFrontAuto == 1) {
				ucg.setColor(255, 0, 0);
				ucg.print("On");
			} else {
				ucg.setColor(0, 0, 255);
				ucg.print("Off");
			}

			ucg.setPrintPos(42,106);
			outText = String(secFrontOnTime);
			ucg.setColor(0, 0, 255);
			ucg.print(outText);

			ucg.setPrintPos(42,127);
			outText = String(secFrontOffTime);
			ucg.setColor(0, 0, 255);
			ucg.print(outText);
		}

		// Print Security back status
		if (secBackOn == 2) {
			ucg.setColor(128, 128, 128);
			outText = "NA";
			ucg.setPrintPos(89,43);
			ucg.print(outText);
			ucg.setPrintPos(89,64);
			ucg.print(outText);
			ucg.setPrintPos(89,85);
			ucg.print(outText);
			ucg.setPrintPos(89,106);
			ucg.print(outText);
			ucg.setPrintPos(89,127);
			ucg.print(outText);
		} else {
			ucg.setPrintPos(89,43);
			if (secBackOn == 1) {
				ucg.setColor(255, 0, 0);
				ucg.print("On");
			} else {
				ucg.setColor(0, 0, 255);
				ucg.print("Off");
			}

			ucg.setPrintPos(89,64);
			if (secBackLight == 1) {
				ucg.setColor(255, 0, 0);
				ucg.print("On");
			} else {
				ucg.setColor(0, 0, 255);
				ucg.print("Off");
			}

			ucg.setPrintPos(89,85);
			if (secBackAuto == 1) {
				ucg.setColor(255, 0, 0);
				ucg.print("On");
			} else {
				ucg.setColor(0, 0, 255);
				ucg.print("Off");
			}

			ucg.setPrintPos(89,106);
			outText = String(secBackOnTime);
			ucg.setColor(0, 0, 255);
			ucg.print(outText);

			ucg.setPrintPos(89,127);
			outText = String(secBackOffTime);
			ucg.setColor(0, 0, 255);
			ucg.print(outText);
		}
	}
}

/**
	showMQTTerrorScreen
	Shows error screen when connection
	to MQTT broker gets disconnected
*/
void showMQTTerrorScreen() {
	ucg.clearScreen();
	ucg.setFont(ucg_font_helvB14_tr);
	ucg.setRotate180();
	ucg.setColor(255, 128, 0);
	ucg.drawBox(0, 0, 128, 128);
	ucg.setColor(0, 0, 0);
	ucg_print_center("MQTT ERR", 0, 34);
	ucg_print_center("disconnected", 0, 59);
	ucg_print_center("from", 0, 84);
	ucg_print_center(mqttBroker, 0, 109);
	if (debugOn) {
		sendDebug("Lost MQTT connection", OTA_HOST);
	}
}

/**
	ucg_print_ln
	Prints text and puts cursor to next line
*/
void ucg_print_ln(String text, boolean center) {
	int xPos=0;
	if (center) {
		char textChar[sizeof(text)];
		text.toCharArray(textChar, sizeof(textChar));
		xPos = (64 - (ucg.getStrWidth(textChar)/2));
	}
	ucg.setPrintPos(xPos,currLine);
	ucg.print(text);
	currLine = currLine + ucg.getFontAscent() - ucg.getFontDescent();
	currLine = currLine + 1;
	if (currLine > 128) {
		// TODO find out how to scroll the whole display
	}
}

/**
	ucg_print_center
	Prints a text centered between
	<yPos> and end of screen
*/
void ucg_print_center(String text, int xPos, int yPos) {
	char textChar[sizeof(text)];
	text.toCharArray(textChar, sizeof(textChar));
	int textCenter = ucg.getStrWidth(textChar)/2;
	ucg.setPrintPos(64 + (xPos/2) - textCenter,yPos);
	ucg.print(text);
}
