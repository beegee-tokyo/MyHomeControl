// Function definitions

// Timer interrupt functions
void triggerGetPower();
void triggerSendUpdate();
void triggerTimerEnd();

// LED function
void redLedFlash();
void blueLedFlash();

// Other utilities
String formatInt(int number);

// Status functions
bool writeStatus();
bool readStatus();

// Communication functions
void connectWiFi();
void WiFiEvent(WiFiEvent_t event);
void sendBroadCast();
void replyClient(WiFiClient httpClient);
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
void resetFanMode ();
void handleCmd();

// NTP client functions
time_t getNtpTime();
void sendNTPpacket();

// For debug over TCP
void sendDebug(String debugMsg);
void sendStatusToDebug();