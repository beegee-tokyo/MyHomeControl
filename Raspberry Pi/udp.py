import select, socket, subprocess, json

port = 9997  # where do you expect to get a msg?
bufferSize = 1024 # whatever you need
# print ("Start listening")
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.bind(('', port))
s.setblocking(0)

while True:
	result = select.select([s],[],[]) # get UDP message
	msg = result[0][0].recv(bufferSize) # extract JSON part
	parsedJson = json.loads(msg) # parse JSON
	# print ("Received UDP")
	# print "Message: %s" % msg
	if parsedJson["de"] == 'sf1': # check if message is from security device
		# print ("Device is sf1")
		if parsedJson["al"] == 1: # check if there is a detection
			# print ("Alarm was 1")
			if parsedJson["ao"] == 1: # check if there the alarm is active
				# print ("Alarm_on was 1")
				subprocess.call(["amixer", "cset", "numid=3", "1"])
				subprocess.call(["mplayer", "-volume", "100", "-loop", "3", "/media/TERESA/dog.mp3"])
				subprocess.call(["amixer", "cset", "numid=3", "2"])
