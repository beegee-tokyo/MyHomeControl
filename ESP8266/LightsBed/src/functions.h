// PWM functions
void setPwmFrequency(int pin, int divisor);

// Communication functions
void handleIR(unsigned long cmdToUse, bool isIR);
void handleTCP(String command);
void tcpCb(uint8_t resp_type, uint8_t client_num, uint16_t len, char *data);
void udpCb(uint8_t resp_type, uint8_t client_num, uint16_t len, char *data);
void sendBroadCast();

// Status functions
void writeStatus();
void readStatus();
