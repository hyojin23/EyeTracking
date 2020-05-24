package posco_ai.e_con.threadClass;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import android.graphics.Bitmap.CompressFormat;


public class CameraProcessor extends AsyncTask<String, String, Boolean> implements CameraBridgeViewBase.CvCameraViewListener2 {


    private Mat matResult;
    private Boolean previewVisible;
    protected int x, y, click;
    private View gazePointer;
    private static final String TAG = "CameraProcessor";
    private Socket socket;
    private String serverIP = "192.168.35.159";
    private int cameraPORT = 8500;

    public CameraProcessor(View gazePointer) {
        Log.d(TAG, "CameraProcessor: 실행");
        matResult = new Mat();
        previewVisible = false;
        this.gazePointer = gazePointer;


    }

    public void setPreviewVisible(Boolean previewVisible) {
        this.previewVisible = previewVisible;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, "onCameraViewStarted: 실행");
        /* 소켓 연결 */
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "onImageAvailable: 소켓 연결 대기중 IP: " + serverIP + " PORT: " + cameraPORT);
                    socket = new Socket(serverIP, cameraPORT);
                    Log.d(TAG, "onImageAvailable: 소켓 연결 성공 IP: " + serverIP + " PORT: " + cameraPORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.d(TAG, "onCameraFrame: 실행");
        // inputFrame을 matResult에 복사, 서버에 전송되는 이미지가 세로방향에 맞게 나오게 하기 위해 t()로 행렬 전치
        inputFrame.rgba().t().copyTo(matResult);
        // 이미지 크기 변환
        Size size = new Size(480, 640);
        Imgproc.resize(matResult, matResult, size);
        Log.d(TAG, "onCameraFrame: matResult: " + matResult);
        Log.d(TAG, "onCameraFrame: inputFrame: " + inputFrame);

        Imgproc.resize(matResult, matResult, size);
        // 세로방향에 맞게 나오게 하기 위해 이미지를 뒤집음. 수평으로는 양수, 수직으로 0, 모두 뒤집기는 음수
        Core.flip(matResult, matResult, -1);


        /* 서버에 전면카메라 이미지 바이트 배열 전송 */

        try {
            // 비트맵 생성
            Bitmap bitmap = Bitmap.createBitmap(matResult.width() , matResult.height(), Bitmap.Config.ARGB_8888);
            Log.d(TAG, "onCameraFrame: width: " + matResult.width() + "height: " + matResult.height());
            // mat 객체를 비트맵으로 변환
            Utils.matToBitmap(matResult, bitmap);
            // 이미지를 ByteArray로 바꾸기 위한 ByteArrayOutputStream
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            // 비트맵 이미지를 전달할 OutputStream
            OutputStream os = null;
            if (socket != null) {
                Log.d(TAG, "onCameraFrame: 소켓: " + socket);
                os = socket.getOutputStream();
            } else {
                Log.d(TAG, "onCameraFrame: 소켓: " + socket);
            }
            // 비트맵을 압축하고 ByteArrayOutputStream에 넣음
            bitmap.compress(CompressFormat.JPEG, 100, ba);
            // ByteArrayOutputStream을 바이트 배열로 바꾸고 배열의 길이 구함
            byte[] bytes = ba.toByteArray();
            // 바이트 배열의 길이 구함
            int len = bytes.length;
            Log.d(TAG, "onCameraFrame: 이미지의 바이트 길이: " + len);
            // 서버에 이미지 바이트의 길이를 제대로 전달하기 위해 숫자를 10자리 문자열로 바꾸고 전달 (숫자가 없는 부분은 공백으로 채움)
            StringBuilder stLen = new StringBuilder(String.valueOf(len));
            Log.d(TAG, "onCameraFrame: stLen.length(): " + stLen.length());
            int wantDigit = 10;
            int currentDigit = stLen.length();
            // 10자리를 맞추기 위해 나머지 부분은 공백으로 채움
            for (int i = 0; i < wantDigit - currentDigit; i++) {
                stLen.append(' ');
            }
            Log.d(TAG, "onCameraFrame: stLen: " + stLen);
            byte[] stLenBytes = stLen.toString().getBytes();
            // 바이트 배열의 길이를 스트림에서 출력하여 서버에 전송
            os.write(stLenBytes);
            Log.d(TAG, "onCameraFrame: ba: " + Arrays.toString(bytes));
            // 바이트 배열을 스트림에서 출력하여 서버에 전송
            ba.writeTo(os);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 뷰에서 전면카메라 이미지를 보려면 matResult를 반환하고 보이지 않게 하려면 null 반환
        if (previewVisible) {
            Log.d(TAG, "onCameraFrame: previewVisible: " + previewVisible);
//             return inputFrame.rgba().t();
            return matResult;
            // 480x640으로 이미지를 바꾸려면 return null로 해야 동작
//            return null;
        } else {
            Log.d(TAG, "onCameraFrame: previewVisible 없음: " + previewVisible);
            return null;
        }
    }


    @Override
    protected Boolean doInBackground(String... strings) {
        try {
            Thread.sleep(1000);
            Log.d(TAG, "doInBackground: 실행");

            String IP = strings[0];
            int PORT = Integer.parseInt(strings[1]);
            Log.d(TAG, IP + ":" + PORT);

            final int imgSize = (int) (matResult.total() * matResult.channels());
            Log.d(TAG, "SIZE : " + imgSize);

            byte[] outData = new byte[imgSize];
            byte[] inData = new byte[30];
            socket = new Socket(IP, PORT);
            Log.d(TAG, "doInBackground: 소켓 연결 성공: " + IP + " : " + PORT);

            Log.d(TAG, "STREAM");

            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inStream = new DataInputStream(socket.getInputStream());
            // 데이터를 모아뒀다가 한줄씩 출력하기 위해 BufferedReader 사용
            BufferedReader br = new BufferedReader(new InputStreamReader(inStream));


            Log.d(TAG, "WHILE");
            while (true) {
                Thread.sleep(1000);
                matResult.put(0, 0, outData);
                Log.d(TAG, "WRITE");

                outStream.write(outData);

                Log.d(TAG, "READ");
                // 서버에서 받은 데이터를 한줄씩 읽음
                String msg = br.readLine();
                publishProgress(msg);
                Log.d(TAG, "doInBackground: msg: " + msg);
            }

        } catch (Exception e) {

            Log.e("SERVER_TAG", String.valueOf(e));
            e.printStackTrace();

        } finally {
//            try {
//                Log.d(TAG, "doInBackground: finally에서 소켓 종료");
//                socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
        return null;
    }

    @Override
    public void onProgressUpdate(String... values) {
        Log.d(TAG, "onProgressUpdate: 실행");
        Log.d(TAG, "onProgressUpdate: values[0]: " + values[0]);
        String[] coordinates = values[0].split("/");
        Log.d(TAG, "onProgressUpdate: x값: " + coordinates[0]);
        Log.d(TAG, "onProgressUpdate: y값: " + coordinates[1]);
        x = Integer.parseInt(coordinates[0]);
        y = Integer.parseInt(coordinates[1]);
        click = Integer.parseInt(coordinates[2]);
        Log.d(TAG, "onProgressUpdate: x 좌표: " + x + " y 좌표: " + y + " click: " + click);
        gazePointer.setX(x);
        gazePointer.setY(y);
        if (click == 1) {
//                EconUtils.gazeTouchMotion(webView,x,y, MotionEvent.ACTION_DOWN);
//                EconUtils.gazeTouchMotion(webView,x,y,MotionEvent.ACTION_UP);
        }
        super.onProgressUpdate(values);
    }


    @Override
    public void onPostExecute(Boolean aBoolean) {
        Log.d(TAG, "onPostExecute: 실행");
//        try {
//            Log.d(TAG, "doInBackground: onPostExecute에서 소켓 종료");
//            socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        super.onPostExecute(aBoolean);
    }
}
