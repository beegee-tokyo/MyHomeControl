#ifndef wifiAPinfo_h
#define wifiAPinfo_h

#include <ESP8266WiFi.h>

/** SSID of local WiFi network */
static const char* ssid = "Teresa&Bernd";
/** Password for local WiFi network */
static const char* password = "teresa1963";

/** Gateway address of WiFi access point */
static IPAddress ipGateWay(192, 168, 0, 1);
/** Network mask of the local lan */
static IPAddress ipSubNet(255, 255, 255, 0);
/** Network address mask for UDP multicast messaging */
static IPAddress multiIP (192,	168, 0, 255);
/** Network address for the monitor */
static IPAddress ipMonitor (192,	168, 0, 149);
/** IP address of spMonitor module */
static IPAddress ipSPM(192, 168, 0, 140);
/** IP address of security front */
static IPAddress ipSecFront(192, 168, 0, 141);
/** IP address of office aircon */
static IPAddress ipAC1(192, 168, 0, 142);
/** IP address of living room aircon */
static IPAddress ipAC2(192, 168, 0, 143);
/** IP address of security back */
static IPAddress ipSecBack(192, 168, 0, 144);
/** IP address of spare */
static IPAddress ipSpare1(192, 168, 0, 145);
/** IP address of spare */
static IPAddress IPSpare2(192, 168, 0, 146);
/** IP address of spare */
static IPAddress ipSpare3(192, 168, 0, 147);
/** IP address of spare */
static IPAddress ipSpare4(192, 168, 0, 148);
/** Network address mask for TCP debug */
static IPAddress ipDebug (192, 168, 0, 121);

/** MQTT broker URL */
//const char * mqttBroker = "shiftr.io";
static const char * mqttBroker = "93.104.213.79";
/** MQTT connection id */
static const char * mqttID = "Monitor";
/** MQTT user name */
//const char * mqttUser = "home_server";
static const char * mqttUser = "home";
/** MQTT password */
//const char * mqttPwd = "de87786045fcffee";
static const char * mqttPwd = "teresa1963";

#endif
