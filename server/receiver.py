import argparse
import socket
import numpy as np
import time
import random
# import cv2


parser = argparse.ArgumentParser()
# 본인 컴퓨터의 ip 주소 리턴
myIP = socket.gethostbyname(socket.getfqdn())
parser.add_argument('--ip', required=False, default=myIP, help='device\'s IP address')
parser.add_argument('--port', required=False, default='8500', help='device\'s PORT number')
args = parser.parse_args()

cIP, cPORT = args.ip, int(args.port)
# cIP = '0.0.0.0'
socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
print('IP Address ', cIP, ':', cPORT)

socket.bind((cIP, cPORT))
socket.listen(5)
print("listening...")

x, y = 500, 800


try:
	c_socket, c_addr = socket.accept()
	print('Connected with ', c_addr)
	
	while True:
		# 최대 bufsize 바이트만큼의 데이터를 읽어온다.
		data = c_socket.recv(4734976)
		# img = cv2.imdecode(np.fromstring(data, np.uint8), 1)
		# img = (np.fromstring(data, np.uint8), 1)
		mat = np.frombuffer(data, dtype=np.int)

		# 클라이언트로부터 받은 데이터 크기
		print('get data', str(len(data)))
		# print('get',str(img.shape[1]) +" "+ str(img.shape[0]))

		x = x + random.randint(-50, 50)
		y = y + random.randint(-50, 50)
		click = 0
		cord = str(x) + '/' + str(y) + '/' + str(click) + '\n'

		# 문자열을 byte로 변환하여 클라이언트로 보냄
		c_socket.sendall(cord.encode('utf-8'))

		# 클라이언트로 보내는 좌표 String
		print('send data', cord)
		# model data
		# connect.sendall()
except Exception as e:
	print(e)
	print('Connecting Error')
	pass
finally:
	socket.close()
	print('End of the session')



	

