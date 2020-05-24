import socket
import cv2
import numpy
from queue import Queue
from _thread import *

enclosure_queue = Queue()


# 쓰레드 함수. 접속한 클라이언트마다 새로운 쓰레드가 생성되어 통신. 큐에서 이미지를 꺼내 클라이언트에 전송하는 쓰레드
def threaded(client_socket, addr, queue):
    print('Connected by :', addr[0], ':', addr[1])

    while True:
        try:
            data = client_socket.recv(1024)
            if not data:
                print('Disconnected by ' + addr[0], ':', addr[1])
                break
            stringData = queue.get()
            client_socket.send(str(len(stringData)).ljust(16).encode())
            print("len(stringData): ", len(stringData))
            print("str(len(stringData)).ljust(16):", str(len(stringData)).ljust(16))
            print("str(len(stringData)).ljust(16).encode():", str(len(stringData)).ljust(16).encode())
            # 데이터 전송
            client_socket.send(stringData)
            print("stringData:", stringData)
        except ConnectionResetError as e:
            print('Disconnected by ' + addr[0], ':', addr[1])
            break

    client_socket.close()


def webcam(queue):
    capture = cv2.VideoCapture(0)

    while True:
        # frame이 없을 경우 ret는 false
        ret, frame = capture.read()

        if not ret:
            continue

        encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 90]
        # 프레임을 이미지로 인코딩
        result, imgencode = cv2.imencode('.jpg', frame, encode_param)

        # 인코딩된 이미지를 넘파이 배열로 변환
        data = numpy.array(imgencode)
        stringData = data.tostring()
        # 큐에 넣음
        queue.put(stringData)

        cv2.imshow('image', frame)
        # ESC 누르면 27을 리턴하여 종료됨
        key = cv2.waitKey(1)
        if key == 27:
            break


HOST = '127.0.0.1'
PORT = 9999

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server_socket.bind((HOST, PORT))
server_socket.listen()

print('server start')
# 웹캠 쓰레드 시작
start_new_thread(webcam, (enclosure_queue,))

while True:
    print('wait')
    # accept 함수에서 대기하다가 클라이언트가 접속하면 새로운 소켓을 리턴. addr은 사용하고 있는 IP와 포트 번호
    client_socket, addr = server_socket.accept()
    # 쓰레드 시작
    start_new_thread(threaded, (client_socket, addr, enclosure_queue,))

server_socket.close()