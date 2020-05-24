import socket
import numpy as np
import cv2
# 안드로이드 스크립 캡쳐 이미지를 소켓으로 받아 보여줌

def recvAll(sock, count):
    buf = b''
    while count:
        # count만큼의 byte를 읽어온다. socket이 이미지 파일의 bytes를 한번에 다 못 읽어오므로
        newbuf = sock.recv(count)
        if not newbuf: return None
        buf += newbuf
        count -= len(newbuf)
    return buf


# 본인 컴퓨터 IP 주소
IP = socket.gethostbyname(socket.getfqdn())
PORT = 8600
print('IP Address ', IP, ':', PORT)

# 소켓 객체를 생성합니다.
# 주소 체계(address family)로 IPv4, 소켓 타입으로 TCP 사용합니다.
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

# bind 함수는 소켓을 특정 네트워크 인터페이스와 포트 번호에 연결하는데 사용됩니다.
# HOST는 hostname, ip address, 빈 문자열 ""이 될 수 있습니다.
# 빈 문자열이면 모든 네트워크 인터페이스로부터의 접속을 허용합니다.
# PORT는 1-65535 사이의 숫자를 사용할 수 있습니다.
server_socket.bind((IP, PORT))

# 서버가 클라이언트의 접속을 허용하도록 합니다.
server_socket.listen()
print("listening...")
try:
    # accept 함수에서 대기하다가 클라이언트가 접속하면 새로운 소켓을 리턴합니다.
    client_socket, addr = server_socket.accept()
    print('Connected with ', addr)
    # 무한루프를 돌면서
    count = 0
    while True:
        # 안드로이드에서 보낸 바이트 배열의 길이를 String으로 나타낸 것 ex) '101979____' _는 공백
        length = recvAll(client_socket, 10)
        print("이미지 파일의 크기: {} byte".format(int(length)))
        # 안드로이드 스크린 하나의 프레임 bytes
        one_frame_bytes = recvAll(client_socket, int(length))
        # np array로 변경하여 디코딩
        image = cv2.imdecode(np.frombuffer(one_frame_bytes, np.uint8), 1)
        # 이미지 크기 조절
        resized_image = cv2.resize(image, (340, 600))
        # 창 크기 조절
        # cv2.resizeWindow("AndroidScreen", 620, 1080)
        # 보여주기
        cv2.imshow('AndroidScreen', resized_image)

        # ESC 누르면 종료
        key = cv2.waitKey(1)
        if key == 27:
            break


except Exception as e:
    print(e)
    print('Connecting Error')
    pass
finally:
    print('End of the session')

