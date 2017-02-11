#!/usr/bin/python
import socket
import sys
import sched, time
sys.path.insert(0, '/usr/lib/python2.7/bridge/') 
from bridgeclient import BridgeClient as bridgeclient

values = bridgeclient() 

s = sched.scheduler(time.time, time.sleep)
def sendBroadCast(sc): 
	UDP_IP = "192.168.0.255"   #Local network broadcast mask
	UDP_PORT = 5000
	#MESSAGE = str(sys.argv[1])
	CONSUMPTION = values.get('C')
	print CONSUMPTION
	SOLAR = values.get('S')
	print SOLAR
	# {\"device\":\"spm\",\"s\":100,\"c\":300}	
	MESSAGE = '{"device":"spm","s":' + str(SOLAR) + ',"c":' + str(CONSUMPTION) + '}'
	print MESSAGE
		
	sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
	sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
	sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
	sock.sendto(bytes(MESSAGE), (UDP_IP, UDP_PORT))
	sock.close()
	sc.enter(60, 1, sendBroadCast, (sc,))

s.enter(60, 1, sendBroadCast, (s,))
s.run()
