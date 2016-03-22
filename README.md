# MyHomeControl
Application to get information from and send commands to my home appliance control system devices

Accesses at the moment 3 devices in my home appliance control system
 * Solar panel monitor (Arduino Yun)
 * Security system (ESP8266)
 * Aircon control system (ESP8266)

## Solar panel monitor
Solar Panel Monitor with Arduino Yun, current sensors and WiFi module to transfer data to a PC or Android device<br />

 * Uses current sensor to measure output of solar panels.<br />
 * Optional additional measurement of luminosity.<br />
 * Optional additional measurement of in/output to electricity grid<br />

## Security
Security system based on ESP8266 module with Android devices as monitoring system<br />
 * Uses PIR sensor to detect measurement.<br />
 * Relay to switch on external light.<br />
 * Light on time 2 minutes, extended if PIR sensor is re-triggered<br />
 * Light on depending on environment light measured with LDR resistor<br />
 * Sends intruder alarm messages over local WiFi per UDP and over Internet per Google Cloud Messaging<br />

## MyAirCon
Remote control for air con using Adafruit HUZZAH ESP8266 modules<br />
 * Switch on aircon if solar panel production is exceeding house consumption by 200W to avoid sending back energy into the grid<br />
 * Enable control of the aircon with any Android device<br />
 
## Hardware
 * Schematics/Breadboard/PCB files for all devices are in the subfolder Hardware<br />
 
## Software
 * Source code for Android is in subfolder app<br />
 * Source code for Arduino Yun is in subfolder Arduino<br />
 * Source code for ESP8266 modules is in subfolder ESP8266<br />
