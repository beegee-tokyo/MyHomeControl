#include <ESP8266WiFi.h>

// Function definitions

// Timer interrupt functions
void triggerGetPower();
void triggerSendUpdate();
void triggerTimerEnd();

// Other utilities
String formatInt(int number);

// Status functions
bool writeStatus();
bool readStatus();

// Communication functions
void sendBroadCast();
void socketServer(WiFiClient tcpClient);
void getPowerVal(boolean doPowerCheck);
void parseCmd(JsonObject& root);
void parseSocketCmd();

// Aircon control functions
void sendCode(int repeat, unsigned int *rawCodes, int rawCount);
unsigned int getVal(byte testVal, byte maskVal);
void buildBuffer(unsigned int *newBuffer, byte *cmd);
void sendCmd();
void initAC();
boolean switchSlaveAC(IPAddress ipSlave, byte reqMode);
void checkPower();
void chkCmdCnt();
void handleCmd();
void restoreTempSetting();

// For debug over TCP
void sendStatusToDebug();
