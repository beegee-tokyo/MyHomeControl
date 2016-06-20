void connectWiFi();

void messageReceived(String topic, String payload, char * bytes, unsigned int length);
void sendToMQTT();
void getSPMStatus();
void getAC1Status();
void getAC2Status();
void getSEFStatus();
void getSERStatus();
void getUDPbroadcast(int udpMsgLength);
void parseACpacket (JsonObject& jsonIn);
void parseSecFrontPacket (JsonObject& jsonIn);
void parseSecBackPacket (JsonObject& jsonIn);
void parseSPMPacket (JsonObject& jsonIn);
void sendDebug(String debugMsg);

time_t getNtpTime();
void sendNTPpacket();
void digitalClockDisplay();
String getDigits(int digits);

void getHomeInfo(boolean all);
void getTemperature();
void makeWeather();

void redLedFlash();
void blueLedFlash();
void triggerGetStatus();
void triggerGetDHT();
void playSound();

void updateSolar(boolean all);
void updateWeather(boolean all);
void updateAC(boolean all);
void updateSecurity(boolean all);
void showMQTTerrorScreen();
void ucg_print_ln(String text, boolean center);
void ucg_print_center(String text, int xPos, int yPos);

uint8_t bmReadByte(void);
void bmReadBuf(uint8_t *buf, uint16_t len);
uint8_t bmOpen(const char *name);
void bmClose(void);
uint8_t bmReadHeader(void);
uint8_t bmReadWritePixel(void);
void bmDraw(String fileName, uint16_t xPos, uint16_t yPos);
