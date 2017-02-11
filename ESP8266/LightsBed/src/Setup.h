#ifndef Setup_h
#define Setup_h

#include <Arduino.h>

#include <SPI.h>
#include <Wire.h>
#include <EEPROM.h>

#include <IRLibDecodeBase.h> // First include the decode base
#include <IRLib_P01_NEC.h>   // Now include only the protocols you wish
#include <IRLibCombo.h>     // After all protocols, include this
// All of the above automatically creates a universal decoder
// class called "IRdecode" containing only the protocols you want.
#include <IRLibRecv.h> // Include a receiver either this or IRLibRecvPCI or IRLibRecvLoop

#include <ELClient.h>
#include <ELClientSocket.h>
#include <ELClientCmd.h>

// Serial port to ESP8266
#include <SC16IS750.h>

/* globals.h contains defines and global variables */
#include "globals.h"
/* functions.h contains all function declarations */
#include "functions.h"

#endif
