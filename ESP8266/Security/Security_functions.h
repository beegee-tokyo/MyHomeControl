//Function definitions

// GCM functions
boolean writeRegIds();
boolean getRegisteredDevices();
boolean addRegisteredDevice(String newDeviceID);
boolean delRegisteredDevice();
boolean delRegisteredDevice(String delRegId);
boolean delRegisteredDevice(int delRegIndex);
boolean gcmSendOut(String data);
boolean gcmSendMsg(JsonArray& pushMessageIds, JsonArray& pushMessages);
boolean gcmSendMsg(JsonObject& pushMessages);

// TSL2561 (light sensor) functions
void configureSensor ();
void getLight ();

// UI functions
void redLedFlash();
void blueLedFlash();
void playAlarmSound();

// Timer trigger functions
void triggerGetLight();
void triggerGetLDR();
void triggerHeartBeat();

// Sensor functions
boolean getLDR();
void pirTrigger();
void buttonTrig();

// Actuator functions
void relayOff();

// Device status functions
void createStatus(JsonObject& root, boolean makeShort);
bool writeStatus();
bool writeRebootReason(String message);
bool readStatus();

// WiFi connection functions
void connectWiFi();
void WiFiEvent(WiFiEvent_t event);
int32_t getRSSI();

// Communication functions
void sendAlarm(boolean doGCM);
void replyClient(WiFiClient httpClient);

// NTP client functions
time_t getNtpTime();
void sendNTPpacket();