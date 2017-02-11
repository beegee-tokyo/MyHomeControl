#!/usr/bin/python
import socket
import sys
sys.path.insert(0, '/usr/lib/python2.7/bridge/') 
from bridgeclient import BridgeClient as bridgeclient

values = bridgeclient() 

UDP_IP_BC = "192.168.0.255"   #Local network broadcast mask
UDP_IP_MONITOR = "192.168.0.149"   #Local network monitor IP
UDP_PORT = 5000
CONSUMPTION = values.get('C')
SOLAR = values.get('S')
# {\"device\":\"spm\",\"s\":100,\"c\":300}	
MESSAGE = '{"device":"spm","s":' + str(SOLAR) + ',"c":' + str(CONSUMPTION) + '}'
	
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) # UDP
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.sendto(bytes(MESSAGE), (UDP_IP_MONITOR, UDP_PORT))
sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
sock.sendto(bytes(MESSAGE), (UDP_IP_BC, UDP_PORT))
sock.close()
quit()
