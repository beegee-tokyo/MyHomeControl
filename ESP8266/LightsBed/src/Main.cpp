#include <Setup.h>

void loop() {
	if (myReceiver.getResults()){
		if (myDecoder.decode()){
			handleIR(myDecoder.value, true);
			sendBroadCast();
		}
	}
	myReceiver.enableIRIn();      //Restart receiver

	// process any callbacks coming from esp_link
	esp.Process();

	// Send status every 60 seconds
	if ((millis() - lastTime) > 60000) {
		sendBroadCast();
		lastTime = millis();
	}
}
