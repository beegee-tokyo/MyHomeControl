#include <Setup.h>
/* codes.h contains the IR sequences for the aircon */
#include "codes.h"

/**
	sendCode
	Send IR command
*/
void sendCode(int repeat, unsigned int *rawCodes, int rawCount) {
	// Assume 38 KHz
	for (int i = 0; i < 3; i++) {
		My_Sender.IRsend::sendRaw(rawCodes, rawCount, 38);
		delay(170);
	}
}

/**
	getVal
	Returns correct duration value depending on bit value
*/
unsigned int getVal(byte testVal, byte maskVal) {
	if ((testVal & maskVal) == maskVal) {
		return 1700;
	} else {
		return 700;
	}
}

/**
	buildBuffer
	Converts bit stream into IR command made of durations
*/
void buildBuffer(unsigned int *newBuffer, byte *cmd) {
	for (int i = 0; i < 3; i++) {
		newBuffer[(i * 16) + 3]	= getVal(cmd[i], B10000000);
		newBuffer[(i * 16) + 5]	= getVal(cmd[i], B01000000);
		newBuffer[(i * 16) + 7]	= getVal(cmd[i], B00100000);
		newBuffer[(i * 16) + 9]	= getVal(cmd[i], B00010000);
		newBuffer[(i * 16) + 11] = getVal(cmd[i], B00001000);
		newBuffer[(i * 16) + 13] = getVal(cmd[i], B00000100);
		newBuffer[(i * 16) + 15] = getVal(cmd[i], B00000010);
		newBuffer[(i * 16) + 17] = getVal(cmd[i], B00000001);
	}
}

/**
	handleCmd
	Handles commands received over the webserver
*/
void handleCmd() {
	// Handle the different AC commands
	switch (irCmd) {
		case CMD_REMOTE_0: // Should only be received in slave AC
			if ((acMode & AC_ON) == AC_ON) { // AC is on
				irCmd = CMD_MODE_FAN;
				sendCmd();
				delay(1000);
				irCmd = CMD_ON_OFF;
				sendCmd();
			}
			irCmd = 9999;
			break;
		case CMD_REMOTE_1: // Should only be received in slave AC
			if ((acMode & AC_ON) != AC_ON) {
				irCmd = CMD_ON_OFF;
				sendCmd();
				delay(1000);
			}
			irCmd = CMD_MODE_FAN;
			sendCmd();
			irCmd = 9999;
			break;
		case CMD_REMOTE_2: // Should only be received in slave AC
			if ((acMode & AC_ON) != AC_ON) {
				irCmd = CMD_ON_OFF;
				sendCmd();
				delay(1000);
			}
			irCmd = CMD_MODE_AUTO;
			sendCmd();
			irCmd = 9999;
			break;
		case CMD_INIT_AC: // Initialize aircon
			initAC();
			irCmd = 9999;
			break;
		case CMD_RESET: // Reboot the ESP module
			if (debugOn) {
				sendDebug("Request to reset the module", OTA_HOST);
			}
			pinMode(16, OUTPUT); // Connected to RST pin
			digitalWrite(16,LOW); // Initiate reset
			ESP.reset(); // In case it didn't work
			break;
		case CMD_AUTO_ON: // Command to (re)start auto control
			// If AC is on, switch it to FAN low speed and then switch it off
			if ((acMode & AC_ON) == AC_ON) { // AC is on
				// Set mode to FAN
				irCmd = CMD_MODE_FAN;
				sendCmd();
				delay(1000);
				// Get last fan speed
				byte lastFanSpeed = acMode & FAN_MASK;
				// Set fan speed to LOW
				// If in high speed send command once to get into low speed mode
				if (lastFanSpeed == 2) {
					irCmd = CMD_FAN_SPEED;
					sendCmd();
					delay(1000);
				}
				// If in medium speed send command once again to get into low speed mode
				if (lastFanSpeed >= 1) {
					irCmd = CMD_FAN_SPEED;
					sendCmd();
					delay(1000);
				}
				// Switch AC off
				irCmd = CMD_ON_OFF;
				sendCmd();
			}
			irCmd = 9999;
			break;
		default: // All other commands
			sendCmd();
	}
	sendBroadCast(); // Inform about the change
}

/**
	chkCmdCnt
	Carrier aircon requires a different code when same
	button is pressed several times (e.g. raise temperature)
*/
void chkCmdCnt() {
	if (irCmd == lastCmd) {
		cmdCnt ++;
		if (cmdCnt == 3) {
			cmdCnt = 0;
		}
	} else {
		cmdCnt = 0;
		lastCmd = irCmd;
	}
}

/**
	sendCmd
	Prepares and sends IR command to aircon depending on
	requested command irCmd
*/
void sendCmd() {
	actLedFlashStart(0.1);
	boolean isValidCmd = false;
	byte lastTemp;
	switch (irCmd) {
		case CMD_ON_OFF: // On-Off
			if ((acMode & AC_ON) == AC_ON) { // AC is on
				acMode = acMode & AC_CLR; // set power bit to 0
				acMode = acMode | AC_OFF;
			} else {
				acMode = acMode & AC_CLR; // set power bit to 0
				acMode = acMode | AC_ON;
			}

			chkCmdCnt();
			switch (cmdCnt) {
				case 0:
					buildBuffer(&sendBuffer[0], &POWER_0[0]);
					break;
				case 1:
					buildBuffer(&sendBuffer[0], &POWER_1[0]);
					break;
				case 2:
					buildBuffer(&sendBuffer[0], &POWER_2[0]);
					break;
			}
			isValidCmd = true;
			break;
		case CMD_MODE_AUTO: // Auto mode
			if ((acMode & AC_ON) == AC_ON) {
				acMode = acMode & MODE_CLR; // set mode bits to 0
				acMode = acMode | MODE_AUTO; // set mode bits to AUTO mode
				chkCmdCnt();
				switch (cmdCnt) {
					case 0:
						buildBuffer(&sendBuffer[0], &AUTO_0[0]);
						break;
					case 1:
						buildBuffer(&sendBuffer[0], &AUTO_1[0]);
						break;
					case 2:
						buildBuffer(&sendBuffer[0], &AUTO_2[0]);
						break;
				}
				isValidCmd = true;
			}
			break;
		case CMD_MODE_COOL: // Cool
			if ((acMode & AC_ON) == AC_ON) {
				acMode = acMode & MODE_CLR; // set mode bits to 0
				acMode = acMode | MODE_COOL; // set mode bits to COOL mode
				chkCmdCnt();
				switch (cmdCnt) {
					case 0:
						buildBuffer(&sendBuffer[0], &COOL_0[0]);
						break;
					case 1:
						buildBuffer(&sendBuffer[0], &COOL_1[0]);
						break;
					case 2:
						buildBuffer(&sendBuffer[0], &COOL_2[0]);
						break;
				}
				isValidCmd = true;
			}
			break;
		case CMD_MODE_DRY: // Dry
			if ((acMode & AC_ON) == AC_ON) {
				acMode = acMode & MODE_CLR; // set mode bits to 0
				acMode = acMode | MODE_DRY; // set mode bits to DRY mode
				chkCmdCnt();
				switch (cmdCnt) {
					case 0:
						buildBuffer(&sendBuffer[0], &DRY_0[0]);
						break;
					case 1:
						buildBuffer(&sendBuffer[0], &DRY_1[0]);
						break;
					case 2:
						buildBuffer(&sendBuffer[0], &DRY_2[0]);
						break;
				}
				isValidCmd = true;
			}
			break;
		case CMD_MODE_FAN: // Fan
			if ((acMode & AC_ON) == AC_ON) {
				acMode = acMode & MODE_CLR; // set mode bits to 0
				acMode = acMode | MODE_FAN; // set mode bits to FAN mode
				chkCmdCnt();
				switch (cmdCnt) {
					case 0:
						buildBuffer(&sendBuffer[0], &FAN_0[0]);
						break;
					case 1:
						buildBuffer(&sendBuffer[0], &FAN_1[0]);
						break;
					case 2:
						buildBuffer(&sendBuffer[0], &FAN_2[0]);
						break;
				}
				isValidCmd = true;
			}
			break;
		case CMD_FAN_SPEED: // Fan speed change
			if ((acMode & AC_ON) == AC_ON) {
				byte currentSpeed = acMode & FAN_MASK;
				if (fanSpeedUp) {
					currentSpeed += 1;
					if (currentSpeed == 2) {
						fanSpeedUp = false;
					}
				} else {
					currentSpeed -= 1;
					if (currentSpeed == 0) {
						fanSpeedUp = true;
					}
				}
				acMode = acMode & FAN_CLR;
				acMode = acMode | currentSpeed;
				chkCmdCnt();
				switch (cmdCnt) {
					case 0:
						buildBuffer(&sendBuffer[0], &SPEED_0[0]);
						break;
					case 1:
						buildBuffer(&sendBuffer[0], &SPEED_1[0]);
						break;
					case 2:
						buildBuffer(&sendBuffer[0], &SPEED_2[0]);
						break;
				}
				if (!isInFanMode) {
					isInFanMode = true;
					// Send command 1 time to switch to fan speed mode
					sendCode(0, &sendBuffer[0], 51); // Send command 1 time to switch mode
					chkCmdCnt();
					switch (cmdCnt) {
						case 0:
							buildBuffer(&sendBuffer[0], &SPEED_0[0]);
							break;
						case 1:
							buildBuffer(&sendBuffer[0], &SPEED_1[0]);
							break;
						case 2:
							buildBuffer(&sendBuffer[0], &SPEED_2[0]);
							break;
					}
					resetFanModeTimer.attach(5, resetFanMode);
				} else {
					resetFanModeTimer.detach();
					resetFanModeTimer.attach(5, resetFanMode);
				}
				isValidCmd = true;
			}
			break;
		case CMD_TEMP_PLUS: // Temp +
			if ((acMode & AC_ON) == AC_ON) {
				lastTemp = acTemp & TEMP_MASK;
				if (lastTemp < MAX_TEMP) {
					lastTemp++;
					acTemp = acTemp & TEMP_CLR;
					acTemp = acTemp | lastTemp;
					chkCmdCnt();
					switch (cmdCnt) {
						case 0:
							buildBuffer(&sendBuffer[0], &PLUS_0[0]);
							break;
						case 1:
							buildBuffer(&sendBuffer[0], &PLUS_1[0]);
							break;
						case 2:
							buildBuffer(&sendBuffer[0], &PLUS_2[0]);
							break;
					}
					isValidCmd = true;
				}
				isValidCmd = true;
			}
			break;
		case CMD_TEMP_MINUS: // Temp -
			if ((acMode & AC_ON) == AC_ON) {
				lastTemp = acTemp & TEMP_MASK;
				if (lastTemp > MIN_TEMP) {
					lastTemp--;
					acTemp = acTemp & TEMP_CLR;
					acTemp = acTemp | lastTemp;
				}
				chkCmdCnt();
				switch (cmdCnt) {
					case 0:
						buildBuffer(&sendBuffer[0], &MINUS_0[0]);
						break;
					case 1:
						buildBuffer(&sendBuffer[0], &MINUS_1[0]);
						break;
					case 2:
						buildBuffer(&sendBuffer[0], &MINUS_2[0]);
						break;
				}
				isValidCmd = true;
			}
			break;
		case CMD_OTHER_TIMER: // Timer
			if ((acMode & TIM_ON) == TIM_ON) { // TIMER is already on
				timerEndTimer.detach(); // Stop timer
				timerEndTriggered = false;
				acMode = acMode & TIM_CLR; // set timer bit to 0
				// Switch off the aircon
				if ((acMode & AC_ON) == AC_ON) { // // Switch off the aircon if still on
					// Set mode to FAN
					irCmd = CMD_MODE_FAN;
					sendCmd();
					delay(1000);
					// Set fan speed to LOW
					irCmd = CMD_FAN_LOW;
					sendCmd();
					delay(1000);
					// Switch AC off
					irCmd = CMD_ON_OFF;
					sendCmd();
					delay(1000);
					if (debugOn) {
						String debugMsg = "Timer stopped manually, switch off AC (" + String(hour()) + ":" + getDigits(minute()) + ")";
						sendDebug(debugMsg, OTA_HOST);
					}
				} else {
					if (debugOn) {
						String debugMsg = "Timer stopped manually, AC was already off (" + String(hour()) + ":" + getDigits(minute()) + ")";
						sendDebug(debugMsg, OTA_HOST);
					}
				}
				powerStatus = 0;
			} else { // Timer is not yet on
				acMode = acMode & TIM_CLR; // set timer bit to 0
				acMode = acMode | TIM_ON; // set timer bit to 1 (on)
				// Switch on the aircon
				if (!((acMode & AC_ON) == AC_ON)) { // // Switch on the aircon if off
					// Switch AC on
					irCmd = CMD_ON_OFF;
					sendCmd();
					delay(1000);
				}
				// Set fan speed to LOW
				irCmd = CMD_FAN_LOW;
				sendCmd();
				delay(1000);
				// Set mode to cooling
				irCmd = CMD_MODE_COOL;
				sendCmd();
				powerStatus = 0;
				// Start timer to switch off the aircon after "onTime" (default = 1 hour = 60mx60s+1000ms=3600000 milliseconds)
				// Because of 32bit limit (== max 71min) the timer is triggered
				// every hour and a counter is used if timer time is 2 hours or more
				timerEndTimer.attach_ms(3600000, triggerTimerEnd);
				if (debugOn) {
					String debugMsg = "Start of timer, switch on AC (" + String(hour()) + ":" + getDigits(minute()) + ") for " + onTime + "hours";
					sendDebug(debugMsg, OTA_HOST);
				}
				isValidCmd = true;
			}
			break;
		case CMD_OTHER_TURBO: // Turbo
			if ((acMode & AC_ON) == AC_ON) {
				if ((acTemp & TUR_ON) == TUR_ON) { // Turbo is on
					acTemp = acTemp & TUR_CLR; // set turbo bit to 0
					acTemp = acTemp | TUR_OFF;
				} else {
					acTemp = acTemp & TUR_CLR; // set turbo bit to 1
					acTemp = acTemp | TUR_ON;
				}
				chkCmdCnt();
				switch (cmdCnt) {
					case 0:
						buildBuffer(&sendBuffer[0], &TURBO_0[0]);
						break;
					case 1:
						buildBuffer(&sendBuffer[0], &TURBO_1[0]);
						break;
					case 2:
						buildBuffer(&sendBuffer[0], &TURBO_2[0]);
						break;
				}
				isValidCmd = true;
			}
			break;
		case CMD_OTHER_ION: // Ion
			if ((acMode & AC_ON) == AC_ON) {
				if ((acTemp & ION_ON) == ION_ON) { // Ion is on
					acTemp = acTemp & ION_CLR; // set ion bit to 0
					acTemp = acTemp | ION_OFF;
				} else {
					acTemp = acTemp & ION_CLR; // set ion bit to 1
					acTemp = acTemp | ION_ON;
				}
				chkCmdCnt();
				switch (cmdCnt) {
					case 0:
						buildBuffer(&sendBuffer[0], &ION_0[0]);
						break;
					case 1:
						buildBuffer(&sendBuffer[0], &ION_1[0]);
						break;
					case 2:
						buildBuffer(&sendBuffer[0], &ION_2[0]);
						break;
				}
				isValidCmd = true;
			}
			break;
	}
	// Send the command
	if (isValidCmd) {
		writeStatus();
		sendCode(0, &sendBuffer[0], 51);
	}

	// Reset irCmd
	irCmd = 9999;
	actLedFlashStop();
}

/**
	initAC
	Initialize aircon to fan mode, low fan speed, temperature 25 deg C
	Call this only once after power on and make sure aircon is off when
	ESP8266 is powered off!
	This will fail if aircon is already on and there is no (easy) way
	to detect if the aircon is already on
*/
void initAC() {
	doubleLedFlashStart(0.1);

	/* FIRST: switch on AC */
	acMode = acMode & AC_CLR; // set power bit to 0
	acMode = acMode | AC_ON;
	buildBuffer(&sendBuffer[0], &POWER_0[0]);
	sendCode(0, &sendBuffer[0], 51);
	delay(2000); // Wait 2 seconds to make sure the aircon is on
	/* SECOND: switch to COOL mode */
	acMode = acMode & MODE_CLR; // set mode bits to 0
	acMode = acMode | MODE_COOL; // set mode bits to COOL mode
	buildBuffer(&sendBuffer[0], &COOL_0[0]);
	sendCode(0, &sendBuffer[0], 51);
	delay(1000); // Wait 1 second to make sure the aircon mode is switched
	/* THIRD: switch to LOW FAN speed (Not possible for Carrier aircon, as it just goes up and down) */
	/* FORTH: set temperature to 25 deg Celsius */
	acTemp = acTemp & TEMP_CLR; // set temperature bits to 0
	acTemp = acTemp + 25; // set temperature bits to 25
	/* We do not know the temperature setting of the aircon
			therefor we first raise 17 times (to set 32 degrees)
			then we lower 7 times to get back to 25 degrees
	*/
	irCmd = 99;
	for (int i = 0; i < 16; i++) {
			chkCmdCnt();
			switch (cmdCnt) {
				case 0:
					buildBuffer(&sendBuffer[0], &PLUS_0[0]);
					break;
				case 1:
					buildBuffer(&sendBuffer[0], &PLUS_1[0]);
					break;
				case 2:
					buildBuffer(&sendBuffer[0], &PLUS_2[0]);
					break;
			}
		sendCode(0, &sendBuffer[0], 51);
		delay(1000); // Wait 1 second to make sure the aircon mode is switched
	}
	for (int i = 0; i < 7; i++) {
			chkCmdCnt();
			switch (cmdCnt) {
				case 0:
					buildBuffer(&sendBuffer[0], &MINUS_0[0]);
					break;
				case 1:
					buildBuffer(&sendBuffer[0], &MINUS_1[0]);
					break;
				case 2:
					buildBuffer(&sendBuffer[0], &MINUS_2[0]);
					break;
			}
		sendCode(0, &sendBuffer[0], 51);
		delay(1000); // Wait 1 second to make sure the aircon mode is switched
	}
	/* FIFTH: switch to FAN mode */
	acMode = acMode & MODE_CLR; // set mode bits to 0
	acMode = acMode | MODE_FAN; // set mode bits to FAN mode
	buildBuffer(&sendBuffer[0], &FAN_0[0]);
	sendCode(0, &sendBuffer[0], 51);
	delay(1000); // Wait 1 second to make sure the aircon mode is switched
	/* SIXTH: switch AC off */
	acMode = acMode & AC_CLR; // set status bits to 0
	acMode = acMode | AC_OFF; // set status to aircon off
	buildBuffer(&sendBuffer[0], &POWER_0[0]);
	sendCode(0, &sendBuffer[0], 51);
	writeStatus();
	doubleLedFlashStop();
}
