#include <ESP8266WiFi.h>

//void messageReceived(String topic, String payload, char * bytes, unsigned int length);
void sendToMQTT();
void socketServer(WiFiClient tcpClient);
void getSPMStatus();
void getAC1Status();
void getAC2Status();
void getSEFStatus();
void getSEBStatus();
void sendCmd(IPAddress serverIP, String deviceCmd);
void getUDPbroadcast(int udpMsgLength);
void parseACpacket (JsonObject& jsonIn, String device);
void parseSecFrontPacket (JsonObject& jsonIn);
void parseSecBackPacket (JsonObject& jsonIn);
void parseSPMPacket (JsonObject& jsonIn);

void getHomeInfo(boolean all);
void getTemperature();
void makeInWeather();
void makeOutWeather();

void triggerGetStatus();
void triggerGetDHT();
void switchBackDisplay();

void updateDisplay(boolean all);
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
