#include <ESP8266WiFi.h>

// Function definitions

// Timer interrupt functions
void triggerGetPower();
void triggerSendUpdate();
void triggerTimerEnd();

// Status functions
bool writeStatus();
bool readStatus();

// Communication functions
void sendBroadCast();
void socketServer(WiFiClient tcpClient);
void getPowerVal(boolean doPowerCheck);
void parseCmd(JsonObject& root);

// Aircon control functions
void initAC();
boolean switchSlaveAC(IPAddress ipSlave, byte reqMode);
void checkPower();
void chkCmdCnt();
void handleCmd();
void restoreTempSetting();

// For debug over TCP
void sendStatusToDebug();
