#ifndef ntp_h
#define ntp_h

#include <TimeLib.h>
#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <WiFiUDP.h>

bool tryGetTime(boolean debugOn);
time_t getNtpTime();
void sendNTPpacket(WiFiUDP udpNTP);
String digitalClockDisplay();
String getDigits(int digits);

/** Flag if getting the time from NTP was successfull */
extern bool gotTime;

#endif
