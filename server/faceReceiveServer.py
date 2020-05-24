import argparse
import socket
import numpy as np
import random
import cv2



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


def recvAll(sock, count):
    buf = b''
    while count:
        # count만큼의 byte를 읽어온다. 한 프레임씩 읽어오기 위해
        newbuf = sock.recv(count)
        if not newbuf: return None
        buf += newbuf
        count -= len(newbuf)
    return buf


try:
    client_socket, c_addr = socket.accept()
    print('Connected with ', c_addr)

    while True:
        # 안드로이드에서 보낸 바이트 배열의 길이를 String으로 나타낸 것 ex) '101979____' _는 공백
        length = recvAll(client_socket, 10)
        print("이미지 파일의 크기: {} byte".format(int(length)))
        # 안드로이드 스크린 하나의 프레임 bytes
        one_frame_bytes = recvAll(client_socket, int(length))
        # np array로 변경하여 디코딩
        image = cv2.imdecode(np.frombuffer(one_frame_bytes, np.uint8), 1)
        # 이미지 크기 조절
        # resized_image = cv2.resize(image, (340, 600))
        # resized_image = cv2.resize(image, (480, 640))
        # 창 크기 조절
        # cv2.resizeWindow("AndroidScreen", 620, 1080)
        # 보여주기
        # cv2.imshow('AndroidScreen', resized_image)
        cv2.imshow('AndroidScreen', image)

        # ESC 누르면 종료
        key = cv2.waitKey(1)
        if key == 27:
            break


        # # 최대 bufsize 바이트만큼의 데이터를 읽어온다.
        # data = client_socket.recv(4734976)
        # # img = cv2.imdecode(np.fromstring(data, np.uint8), 1)
        # # img = (np.fromstring(data, np.uint8), 1)
        # mat = np.frombuffer(data, dtype=np.int)
        #
        # # 클라이언트로부터 받은 데이터 크기
        # print('get data', str(len(data)))
        # # print('get',str(img.shape[1]) +" "+ str(img.shape[0]))

        x = x + random.randint(-50, 50)
        y = y + random.randint(-50, 50)
        click = 0
        cord = str(x) + '/' + str(y) + '/' + str(click) + '\n'

        # 문자열을 byte로 변환하여 클라이언트로 보냄
        client_socket.sendall(cord.encode('utf-8'))

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
