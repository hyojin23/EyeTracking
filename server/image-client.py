import socket
import numpy as np
import cv2


def recvall(sock, count):
    buf = b''
    while count:
        # count만큼의 byte를 읽어온다. (16자리로 만든 String을 읽기 위해)
        newbuf = sock.recv(count)
        if not newbuf: return None
        buf += newbuf
        count -= len(newbuf)
    return buf


HOST = '127.0.0.1'
PORT = 9999

client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# 소켓 연결
client_socket.connect((HOST, PORT))

while True:

    message = '1'
    client_socket.send(message.encode())

    length = recvall(client_socket, 16)
    print(length)
    stringData = recvall(client_socket, int(length))
    print(int(length))
    # String 데이터를 넘파이 배열로 바꿈
    data = np.frombuffer(stringData, dtype='uint8')

    decimg = cv2.imdecode(data, 1)
    # print(decimg)
    cv2.imshow('Image', decimg)

    # ESC 누르면 종료
    key = cv2.waitKey(1)
    if key == 27:
        break

client_socket.close()