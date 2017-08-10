#ifndef Setup_h
#define Setup_h

#include <Arduino.h>
#include <ArduinoOTA.h>
#include <Ticker.h>
#include <ESP8266WiFi.h>
#include "ESP8266mDNS.h"
#include <WiFiUdp.h>
#include <IRremoteESP8266.h>
#include <IRremoteInt.h>
// All of the above automatically creates a universal decoder
// class called "IRdecode" containing only the protocols you want.
#include <ArduinoJson.h>
#include <WiFiClient.h>
#include <FS.h>
#include <ESP8266FtpServer.h>
#include <TimeLib.h>

#include <DNSServer.h>
#include <ESP8266WebServer.h>
#include <WiFiManager.h>

/* Common private libraries */
#include <ntp.h>
#include <leds.h>
#include <wifi.h>
#include <wifiAPinfo.h>

/* globals.h contains defines and global variables */
#include "globals.h"
/* functions.h contains all function declarations */
#include "functions.h"
/* codes.h contains the IR sequences for the aircon */
#include "codes.h"

#endif
