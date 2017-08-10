#ifndef functions_h
#define functions_h

#include <ArduinoJson.h>
#include <ESP8266WiFi.h>

//Function definitions

// Timer trigger functions
void triggerHeartBeat();

// Actuator functions
void relayOff();

// Device status functions
void createStatus(JsonObject& root, boolean makeShort);
bool writeStatus();
bool writeRebootReason(String message);
bool readStatus();

// Communication functions
void sendAlarm(boolean makeShort);
void socketServer(WiFiClient tcpClient);
#endif
