void messageReceived(String topic, String payload, char * bytes, unsigned int length);
void sendToMQTT();

void connectWiFi();

void getHomeInfo(boolean all);
void configureSensor ();
void getLight ();
void getTemperature();
void makeWeather();
void getSPMStatus();
void getACStatus();
void getSECStatus();

void redLedFlash();
void blueLedFlash();
void triggerGetStatus();
void triggerGetDHT();

char* cryptMessage(String message);

void updateScreen(boolean all);
void updateWeather();
void showMQTTerrorScreen();
void ucg_print_ln(String text, boolean center);
void ucg_print_center(String text, int xPos, int yPos);