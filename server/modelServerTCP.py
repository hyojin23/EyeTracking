import argparse
import socket

parser = argparse.ArgumentParser()
# 본인 컴퓨터의 ip 주소 리턴
myIP = socket.gethostbyname(socket.getfqdn())
parser.add_argument('--ip', required=False, default=myIP, help='device\'s IP address')
parser.add_argument('--port', required=False, default='8000', help='device\'s PORT number')
args = parser.parse_args()

cIP, cPORT = args.ip, int(args.port)
# cIP = '0.0.0.0'

try:
	socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	print('IP Address ', cIP, ':', cPORT)

	socket.bind((cIP, cPORT))
	socket.listen(1)
	print("listening...")
	connect, addr = socket.accept()
	print('Connected with ', addr)
	
	while True:
		packet = connect.recv(40000)
		print(data.len())
		data = packet.decode()
		print('get')
		# model data
		# connect.sendall()

		if data == 'END':
			connect.close()
			print('Disconnected with', cIP)
			break
except Exception as e:
	print(e)
	print('Connecting Error')
	pass
finally :
	print('End of the session')
