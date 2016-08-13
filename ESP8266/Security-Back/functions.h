//Function definitions

// TSL2561 (light sensor) functions
//void configureSensor ();
//void getLight ();

// DHT11 functions
void getTemperature();

// UI functions
void redLedFlash();
void blueLedFlash();
void playAlarmSound();

// Timer trigger functions
void triggerGetWeather();
void triggerGetDHT();
void triggerHeartBeat();

// Sensor functions
void pirTrigger();

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
void sendAlarm(boolean makeShort);
void replyClient(WiFiClient httpClient);
void socketServer(WiFiClient tcpClient);
void sendDebug(String debugMsg);

// NTP client functions
time_t getNtpTime();
void sendNTPpacket(WiFiUDP udpClientServer);