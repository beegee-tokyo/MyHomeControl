//Function definitions

// GCM functions
/**
 * Writes regAndroidIds[] to file gcm.txt as JSON object
 *
 * @return <code>boolean</code>
 *              True if successful
 *              False if file error
 */
boolean writeRegIds();
/**
 * Reads registered device IDs from file gcm.txt
 * saves found devices in global variable regAndroidIds[]
 * sets global variable regDevNum to number of devices found
 *
 * @return <code>boolean</code>
 *              True if successful
 *              False if file error or JSON parse error occured
 */
boolean getRegisteredDevices();
/**
 * Adds a new device to the file gcm.txt
 * saves found devices in global variable regAndroidIds[]
 * sets global variable regDevNum to number of devices found
 *
 * @param newDeviceID
 *              String new device ID
 * @return <code>boolean</code>
 *              True if successful
 *              False if file error occured or ID is invalid
 */
boolean addRegisteredDevice(String newDeviceID);
/**
 * Deletes all registration IDs from the list
 *
 * @return <code>boolean</code>
 *              True if successful
 *              False if file error occured
 */
boolean delRegisteredDevice();
/**
 * Deletes a registration id from the list
 *
 * @param delRegId
 *              Registration id to be deleted
 * @return <code>boolean</code>
 *              True if successful
 *              False if a file error occured or ID is invalid or not registered
 */
boolean delRegisteredDevice(String delRegId);
/**
 * Deletes a registration id from the list
 *
 * @param delRegIndex
 *              Index of id to be deleted
 * @return <code>boolean</code>
 *              True if successful
 *              False if a file error occured or index is invalid
 */
boolean delRegisteredDevice(int delRegIndex);
/**
 * Sends message to https://android.googleapis.com/gcm/send to
 * request a push notification to registered Android devices
 *
 * @param data
 *              String with the Json object containing the reg IDs and data
 * @return <code>boolean</code>
 *              True if successful
 *              False if an error occured
 */
boolean gcmSendOut(String data);
/**
 * Prepares the JSON object holding the registration IDs and data
 * calls gcmSendOut to forward the request to the GCM server
 *
 * @param pushMessageIds
 *              Json array with the keys (aka names) of the messages
 * @param pushMessages
 *              Json array with the messages
 * @return <code>boolean</code>
 *              True if successful
 *              False if an error occured
 */
boolean gcmSendMsg(JsonArray& pushMessageIds, JsonArray& pushMessages);
/**
 * Prepares the JSON object holding the registration IDs and data
 * calls gcmSendOut to forward the request to the GCM server
 *
 * @param pushMessages
 *              Json object with the data for the push notification
 * @return <code>boolean</code>
 *              True if successful
 *              False if an error occured
 */
boolean gcmSendMsg(JsonObject& pushMessages);

// TSL2561 (light sensor) functions
/**
 * Configures the gain and integration time for the TSL2561
 */
void configureSensor ();
/**
 * Get current light measurement.
 * Function makes 5 measurements and returns the average value.
 * Function adapts integration time in case of sensor overload
 * Result is stored in global variable lightValue
 */
void getLight ();

// Generic subfunctions
/**
 * Change status of red led on each call
 * called by Ticker ledFlasher
 */
void redLedFlash();
/**
 * Change status of blue led on each call
 * called by Ticker comFlasher
 */
void blueLedFlash();
 /**
  * Sets flag lightUpdateTriggered to true for handling in loop()
  * called by Ticker updateLightTimer
  * will initiate a call to getLight() from loop()
  */
void triggerGetLight();
/**
 * Sets flag lightLDRTriggered to true for handling in loop()
 * called by Ticker updateLDRTimer
 * will initiate a call to getLDR() from loop()
 */
void triggerGetLDR();
/**
 * Sets flag heartBeatTriggered to true for handling in loop()
 * called by Ticker heartBeatTimer
 * will initiate a call to getLDR() from loop()
 */
void triggerHeartBeat();
/**
 * Reads analog input where LDR is connected
 * sets flag switchLights if value is lower than 850
 *
 * @return <code>boolean</code>
 *		true if status changed
 *		false if status is the same	
 */
boolean getLDR();
/**
 * Return signal strength or 0 if target SSID not found
 * calls gcmSendOut to forward the request to the GCM server
 *
 * @return <code>int32_t</code>
 *              Signal strength as unsinged int or 0
 */
int32_t getRSSI();
/**
 * Counts up until offDelay reaches onTime, then
 * switch off the relay
 * turn off the alarm sound
 * called by relayOffTimer
 */
void relayOff();
/**
 * Create status JSON object
 *
 * @param root
 *              Json object to be filled with the status
 */
void createStatus(JsonObject& root, boolean makeShort);
/**
 * Write status to file
 *
 * @return <code>boolean</code>
 *              True if status was saved
 *              False if file error occured
 */
bool writeStatus();
/**
 * Write reboot reason to file
 *
 * @param message
 *              Reboot reason as string
 * @return <code>boolean</code>
 *              True if reboot reason was saved
 *              False if file error occured
 */
bool writeRebootReason(String message);
/**
 * Reads current status from status.txt
 * global variables are updated from the content
 *
 * @return <code>boolean</code>
 *              True if status could be read
 *              False if file error occured
 */
bool readStatus();
/**
 * Connect to WiFi AP
 * if no WiFi is found for 60 seconds
 * module is restarted
 */
void connectWiFi();
/**
 * Called if there is a change in the WiFi connection
 *
 * @param event
 *              Event that happened
 */
void WiFiEvent(WiFiEvent_t event);
/**
 * Send broadcast message over UDP into local network
 *
 * @param doGCM
 *              Flag if message is pushed over GCM as well
 */
void sendAlarm(boolean doGCM);
/**
 * Plays the tune defined with melody[] endless until ticker is detached
 */
void playAlarmSound();
/**
 * Interrupt routine called if status of PIR detection status changes
 */
void pirTrigger();
/**
 * Triggered when push button is pushed
 * enables/disables alarm sound
 */
void buttonTrig();
/**
 * Answer request on http server
 * send last measured light value to requester
 *
 * @param httpClient
 *              Connected WiFi client
 */
void replyClient(WiFiClient httpClient);

// NTP time server functions
/**
 * Prepares request to NTP server
 * @return <code>time_t</code>
 *			Current time as time_t structure or NULL if failed
 */
time_t getNtpTime();
/**
 * Send an NTP request to the time server at the given address
 * @param &address
 *			Pointer to address of NTP server
 */
void sendNTPpacket(IPAddress &address);