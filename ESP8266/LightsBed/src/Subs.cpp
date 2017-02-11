#include <Setup.h>

/**
 * Divides a given PWM pin frequency by a divisor.
 *
 * The resulting frequency is equal to the base frequency divided by
 * the given divisor:
 *   - Base frequencies:
 *      o The base frequency for pins 3, 9, 10, and 11 is 31250 Hz.
 *      o The base frequency for pins 5 and 6 is 62500 Hz.
 *   - Divisors:
 *      o The divisors available on pins 5, 6, 9 and 10 are: 1, 8, 64,
 *        256, and 1024.
 *      o The divisors available on pins 3 and 11 are: 1, 8, 32, 64,
 *        128, 256, and 1024.
 *
 * PWM frequencies are tied together in pairs of pins. If one in a
 * pair is changed, the other is also changed to match:
 *   - Pins 5 and 6 are paired on timer0
 *   - Pins 9 and 10 are paired on timer1
 *   - Pins 3 and 11 are paired on timer2
 *
 * Note that this function will have side effects on anything else
 * that uses timers:
 *   - Changes on pins 3, 5, 6, or 11 may cause the delay() and
 *     millis() functions to stop working. Other timing-related
 *     functions may also be affected.
 *   - Changes on pins 9 or 10 will cause the Servo library to function
 *     incorrectly.
 *
 * Thanks to macegr of the Arduino forums for his documentation of the
 * PWM frequency divisors. His post can be viewed at:
 *   http://forum.arduino.cc/index.php?topic=16612#msg121031
 */
void setPwmFrequency(int pin, int divisor) {
  byte mode;
  if(pin == 5 || pin == 6 || pin == 9 || pin == 10) {
    switch(divisor) {
      case 1: mode = 0x01; break;
      case 8: mode = 0x02; break;
      case 64: mode = 0x03; break;
      case 256: mode = 0x04; break;
      case 1024: mode = 0x05; break;
      default: return;
    }
    if(pin == 5 || pin == 6) {
      TCCR0B = (TCCR0B & 0b11111000) | mode;
    } else {
      TCCR1B = (TCCR1B & 0b11111000) | mode;
    }
  } else if(pin == 3 || pin == 11) {
    switch(divisor) {
      case 1: mode = 0x01; break;
      case 8: mode = 0x02; break;
      case 32: mode = 0x03; break;
      case 64: mode = 0x04; break;
      case 128: mode = 0x05; break;
      case 256: mode = 0x06; break;
      case 1024: mode = 0x07; break;
      default: return;
    }
    TCCR2B = (TCCR2B & 0b11111000) | mode;
  }
}

void handleIR(unsigned long cmdToUse, bool isIR) {
	if (isIR) {
		if (cmdToUse != irRepeat) {
			lastReceived = myDecoder.value;
		}
		cmdToUse = lastReceived;
	}
	// bool validCmd = true;
	switch(cmdToUse) {
		case ir0:
			brightness = 222;
			break;
		case ir1:
			brightness = 212;
			break;
		case ir2:
			brightness = 203;
			break;
		case ir3:
			brightness = 194;
			break;
		case ir4:
			brightness = 185;
			break;
		case ir5:
			brightness = 176;
			break;
		case ir6:
			brightness = 167;
			break;
		case ir7:
			brightness = 158;
			break;
		case ir8:
			brightness = 149;
			break;
		case ir9:
			brightness = 140;
			break;
		case irStatus:
			brightness = 212;
			break;
		case irAudio:
			brightness = 203;
			break;
		case irInfo:
			brightness = 194;
			break;
		case irEPG:
			brightness = 185;
			break;
		case irMute:
			brightness = dimValue;
			break;
		case irOn_Off:
			if (brightness >= 222) {// LED bulbs are off
				brightness = 140; // switch them on
			} else { // LED bulbs are on
				brightness = 255; // switch them off
			}
			break;
		case irCHp:
		case irVp:
		case irUp:
			if (brightness >= 141) {
				brightness -= 1;
		} else {
			brightness = 140;
		}
		break;
		case irRight:
		if (brightness >= 150) {
			brightness -= 10;
		} else {
			brightness = 140;
		}
		break;
		case irUpUp:
		if (brightness >= 165) {
			brightness -= 25;
		} else {
			brightness = 140;
			}
			break;
		case irCHm:
		case irVm:
		case irDown:
			brightness += 1;
			if (brightness > 222) brightness = 222;
			break;
		case irLeft:
			brightness += 10;
			if (brightness > 222) brightness = 222;
			break;
		case irDownDown:
			brightness += 25;
			if (brightness > 222) brightness = 222;
			break;
		default:
			// validCmd = false;
			return;
	}
	// if (validCmd) {
		// set the brightness of led pin:
		analogWrite(led, brightness);
		if (brightness >= 222) {
			digitalWrite(controlLed, true);
		} else {
			digitalWrite(controlLed, false);
		}
	// }
	if (isIR) delay(200); //try to ditch false repeat commands
}

/**
	writeStatus
	writes current status into EEPROM
*/
void writeStatus() {
// Serial.print("Writing Status: DimValue = "); Serial.println(dimValue);
	// Save status in EEPROM
	EEPROM.put(0,dimValue);
}

/**
	readStatus
	reads current status from EEPROM
	global variables are updated from the content
	or set to default values if nothing is saved in the EEPROM
*/
void readStatus() {
	// Try to read status from EEPROM
	EEPROM.get(0,dimValue);

	if ((dimValue < 140) || (dimValue > 222)) {
		dimValue = 200; // Wrong or no values in EEPROM, use default value
// Serial.println("Read Status failed to get dimValue");
	}
// Serial.print("Read Status: DimValue = "); Serial.println(dimValue);
}
