import argparse
import socket
import numpy as np
import random
import cv2
import dlib
from math import hypot
from _thread import *

# ***** 안드로이드 기기의 전면 카메라가 찍은 이미지를 소켓으로 받아 보여줌 *****

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


detector = dlib.get_frontal_face_detector()
predictor = dlib.shape_predictor("shape_predictor_68_face_landmarks.dat")

left_eye_basepoints = [36, 37, 38, 39, 40, 41]
right_eye_basepoints = [42, 43, 44, 45, 46, 47]


def midpoint(p1 ,p2):
    return int((p1.x + p2.x)/2), int((p1.y + p2.y)/2)
font = cv2.FONT_HERSHEY_PLAIN


def get_blinking_ratio(eye_points, facial_landmarks):
    left_point = (facial_landmarks.part(eye_points[0]).x, facial_landmarks.part(eye_points[0]).y)
    right_point = (facial_landmarks.part(eye_points[3]).x, facial_landmarks.part(eye_points[3]).y)
    center_top = midpoint(facial_landmarks.part(eye_points[1]), facial_landmarks.part(eye_points[2]))
    center_bottom = midpoint(facial_landmarks.part(eye_points[5]), facial_landmarks.part(eye_points[4]))

    #hor_line = cv2.line(frame, left_point, right_point, (0, 255, 0), 2)
    #ver_line = cv2.line(frame, center_top, center_bottom, (0, 255, 0), 2)

    hor_line_lenght = hypot((left_point[0] - right_point[0]), (left_point[1] - right_point[1]))
    ver_line_lenght = hypot((center_top[0] - center_bottom[0]), (center_top[1] - center_bottom[1]))

    ratio = hor_line_lenght / ver_line_lenght
    return ratio


def get_gaze_ratio(eye_points, facial_landmarks):
    # Gaze detection
    left_eye_region = np.array([(facial_landmarks.part(eye_points[0]).x, facial_landmarks.part(eye_points[0]).y),
                                (facial_landmarks.part(eye_points[1]).x, facial_landmarks.part(eye_points[1]).y),
                                (facial_landmarks.part(eye_points[2]).x, facial_landmarks.part(eye_points[2]).y),
                                (facial_landmarks.part(eye_points[3]).x, facial_landmarks.part(eye_points[3]).y),
                                (facial_landmarks.part(eye_points[4]).x, facial_landmarks.part(eye_points[4]).y),
                                (facial_landmarks.part(eye_points[5]).x, facial_landmarks.part(eye_points[5]).y)], np.int32)
    # cv2.polylines(frame, [left_eye_region], True, (0, 0, 255), 2)

    height, width, _ = image.shape
    mask = np.zeros((height, width), np.uint8)
    cv2.polylines(image, [left_eye_region], True, 255, 2)
    cv2.fillPoly(mask, [left_eye_region], 255)
    eye = cv2.bitwise_and(gray, gray, mask=mask)

    # print(left_eye_region)
    min_x = np.min(left_eye_region[:, 0])
    max_x = np.max(left_eye_region[:, 0])

    min_y = np.min(left_eye_region[:, 1])
    max_y = np.max(left_eye_region[:, 1])

    gray_eye = eye[min_y: max_y, min_x: max_x]
    _, threshold_eye = cv2.threshold(gray_eye, 70, 255, cv2.THRESH_BINARY_INV)
    height, width = threshold_eye.shape

    left_side_threshold = threshold_eye[0: height, 0: int(width / 2)]
    left_side_white = cv2.countNonZero(left_side_threshold)

    right_side_threshold = threshold_eye[0: height, int(width / 2): width]
    right_side_white = cv2.countNonZero(right_side_threshold)

    upper_side_threshold = threshold_eye[0: int(height / 2), 0: width]
    upper_side_white = cv2.countNonZero(upper_side_threshold)

    down_side_threshold = threshold_eye[int(height / 2): height, 0: width]
    down_side_white = cv2.countNonZero(down_side_threshold)

    if left_side_white == 0:
        horizontal_gaze_ratio = 1
    elif right_side_white == 0:
        horizontal_gaze_ratio = 5
    else:
        horizontal_gaze_ratio = left_side_white / right_side_white

    if upper_side_white == 0:
        vertical_gaze_ratio = 1
    elif down_side_white == 0:
        vertical_gaze_ratio = 5
    else:
        vertical_gaze_ratio = upper_side_white / down_side_white


    # cv2.imshow("Down side eye", down_side_threshold)
    # threshold_txt = 'Downside white ratio %s' % upper_side_white
    # cv2.putText(frame, str(threshold_txt), (70, 140), font, 2, (0, 0, 255), 3)

    return horizontal_gaze_ratio, vertical_gaze_ratio


try:
    client_socket, client_addr = socket.accept()
    # client_send_socket, c_send_addr = socket.accept()
    print('Connected with ', client_addr)
    # print('Connected with ', c_send_addr)

    horizontal_view_direction = "CENTER"
    vertical_view_direction = "CENTER"


    print("실행")

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

        new_frame = np.zeros((500, 500, 3), np.uint8)
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

        faces = detector(gray)
        for face in faces:
            # x, y = face.left(), face.top()
            # x1, y1 = face.right(), face.bottom()
            # cv2.rectangle(image, (x, y), (x1, y1), (0, 255, 0), 2)

            landmarks = predictor(gray, face)

            # Detect blinking
            left_eye_ratio = get_blinking_ratio(left_eye_basepoints, landmarks)
            right_eye_ratio = get_blinking_ratio(right_eye_basepoints, landmarks)
            blinking_ratio = (left_eye_ratio + right_eye_ratio) / 2

            if blinking_ratio > 5.7:
                # if blinking_ratio > 7:
                cv2.putText(image, "BLINKING", (50, 150), font, 7, (255, 0, 0))


            # Gaze detection
            horizontal_gaze_ratio_left_eye, vertical_gaze_ratio_left_eye = get_gaze_ratio(left_eye_basepoints,
                                                                                          landmarks)
            horizontal_gaze_ratio_right_eye, vertical_gaze_ratio_right_eye = get_gaze_ratio(right_eye_basepoints,
                                                                                            landmarks)
            horizontal_gaze_ratio = (horizontal_gaze_ratio_right_eye + horizontal_gaze_ratio_left_eye) / 2
            vertical_gaze_ratio = (vertical_gaze_ratio_right_eye + vertical_gaze_ratio_left_eye) / 2

            if horizontal_gaze_ratio < 0.8:
                horizontal_view_direction = '''LEFT %s''' % horizontal_gaze_ratio
                new_frame[:] = (0, 0, 255)
            elif 0.9 < horizontal_gaze_ratio < 1.2:
                horizontal_view_direction = '''CENTER %s''' % horizontal_gaze_ratio
            else:
                new_frame[:] = (255, 0, 0)
                horizontal_view_direction = '''RIGHT %s''' % horizontal_gaze_ratio

            if vertical_gaze_ratio < 1.35:
                vertical_view_direction = '''DOWN %s''' % vertical_gaze_ratio
                new_frame[:] = (0, 0, 255)
            elif 1.4 < vertical_gaze_ratio < 1.65:
                vertical_view_direction = '''CENTER %s''' % vertical_gaze_ratio
            else:
                new_frame[:] = (255, 0, 0)
                vertical_view_direction = '''UP %s''' % vertical_gaze_ratio

            # 눈동자 방향 정보를 안드로이드 기기에 보냄
            click = 0
            directions = str(horizontal_view_direction) + '/' + str(vertical_view_direction) + '/' + str(click) + '\n'

            # 문자열을 byte로 변환하여 클라이언트로 보냄
            client_socket.sendall(directions.encode('utf-8'))

            # 클라이언트로 보내는 방향 String
            print('send data: ', directions)

            # showing direction

            cv2.putText(image, str(horizontal_view_direction), (50, 100), font, 2, (0, 0, 255), 3)
            cv2.putText(image, str(vertical_view_direction), (50, 190), font, 2, (0, 0, 255), 3)

        cv2.imshow('AndroidScreen', image)

        # ESC 누르면 종료
        key = cv2.waitKey(1)
        if key == 27:
            break


except Exception as e:
    print(e)
    print('Connecting Error')
    pass
finally:
    socket.close()
    print('End of the session')
