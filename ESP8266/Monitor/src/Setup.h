#ifndef Setup_h
#define Setup_h

#include <Arduino.h>
#include <ArduinoOTA.h>
#include <ESP8266WiFi.h>
#include <WiFiUDP.h>
#include <MQTTClient.h>
#include <ESP8266mDNS.h>
#include <Ticker.h>
#include <ESP8266FtpServer.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <SPI.h>
#include <Ucglib.h>
#include <FS.h>
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

#endif
