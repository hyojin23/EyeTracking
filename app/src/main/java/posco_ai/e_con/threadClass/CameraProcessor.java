package posco_ai.e_con.threadClass;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
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
import android.webkit.WebView;
import android.widget.Toast;

import posco_ai.e_con.MainActivity;


public class CameraProcessor extends AsyncTask<String, String, Boolean> implements CameraBridgeViewBase.CvCameraViewListener2 {


    private Mat matResult;
    private Boolean previewVisible;
    protected int click;
    private View gazePointer;
    private static final String TAG = "CameraProcessor";
    private Socket socket;
    private String serverIP = "192.168.35.159";
    private int cameraPORT = 8500;
    private Context context;
    private WebView webView;
    int bottomTo = 0;
    int upTo = 0;

    public CameraProcessor(View gazePointer, Context context, WebView webView) {
        Log.d(TAG, "CameraProcessor: 실행");
        matResult = new Mat();
        previewVisible = false;
        this.gazePointer = gazePointer;
        this.context = context;
        this.webView = webView;

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
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
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


    // 서버로부터 눈동자 방향을 받는 소켓 연결 시도
    @Override
    protected Boolean doInBackground(String... strings) {
        try {
//            Thread.sleep(1000);

            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inStream = new DataInputStream(socket.getInputStream());
            // 데이터를 모아뒀다가 한줄씩 출력하기 위해 BufferedReader 사용
            BufferedReader br = new BufferedReader(new InputStreamReader(inStream));

            Log.d(TAG, "WHILE");
            while (true) {
                Thread.sleep(1000);

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
        String[] viewData = values[0].split("/");
        String horizontalViewData = viewData[0];
        String verticalViewData = viewData[1];
        Log.d(TAG, "onProgressUpdate: 수평방향 시선 정보: " + viewData[0]);
        Log.d(TAG, "onProgressUpdate: 수직방향 시선 정보: " + viewData[1]);

        String[] horizontalViewArray = horizontalViewData.split(" ");
        String[] verticalViewArray = verticalViewData.split(" ");

        // 수평으로 움직이는 시선 방향과 비율
        String horizontalDirection = horizontalViewArray[0];
        String horizontalRatio = horizontalViewArray[1];
        Log.d(TAG, "onProgressUpdate: horizontalDirection: " + horizontalDirection);
        Log.d(TAG, "onProgressUpdate: horizontalRatio: " + horizontalRatio);

        // 수직으로 움직이는 시선 방향과 비율
        String verticalDirection = verticalViewArray[0];
        String verticalRatio = verticalViewArray[1];
        Log.d(TAG, "onProgressUpdate: verticalDirection: " + verticalDirection);
        Log.d(TAG, "onProgressUpdate: verticalRatio: " + verticalRatio);

        // 소수 셋째 자리에서 반올림(둘째 자리까지 나타냄)
        double x_d = (Math.round(Double.parseDouble(horizontalRatio)*100)/100.0);
        // 소수 셋째 자리에서 반올림(둘째 자리까지 나타냄)
        double y_d = (Math.round(Double.parseDouble(verticalRatio)*100)/100.0);

//        click = Integer.parseInt(viewData[2]);
//        Log.d(TAG, "onProgressUpdate: x 좌표: " + x + " y 좌표: " + y + " click: " + click);

        // 500, 800이 화면의 중앙. x값이 작을수록 왼쪽, 클수록 오른쪽을 보는 것, y값이 작을수록 아래, 클수록 위를 보는 것
        int x = (int)(500 * x_d);
        int y = (int)(1200 * (1/y_d));

        gazePointer.setX(x);
        gazePointer.setY(y);
        Toast.makeText(context, verticalDirection + " 수평방향: " + x + " 수직방향: " + y, Toast.LENGTH_SHORT).show();

        // 시선방향이 위쪽일 경우
        if (verticalDirection.equals("UP")) {
            Log.d(TAG, "onClick: webView.getScrollY(): " + webView.getScrollY());

            // 현재 스크롤 위치에 따라 스크롤 도착지점을 다르게 만듦. 스크롤 도착지점이 마이너스가 되면
            // webView.getScrollY()이 마이너스가 되어 scroll down을 여러번해야 스크롤이 내려가게됨
            if (webView.getScrollY() > 850) {
                upTo = webView.getScrollY() - 850;
            } else {
                upTo = 0;
            }

            ObjectAnimator anim = ObjectAnimator.ofInt(webView, "scrollY", webView.getScrollY(), upTo);
            anim.setDuration(1000).start();
        // 시선방향이 아래일 경우
        } else {

            Log.d(TAG, "onClick: webView.getScrollY(): " + webView.getScrollY());
            bottomTo = webView.getScrollY() + 850;
            // ofInt(애니메이션 적용 대상, 애니메이션 종류, 애니메이션 시작점(스크롤 출발위치), 애니메이션 끝점(스크롤 도착위치))
            ObjectAnimator anim = ObjectAnimator.ofInt(webView, "scrollY", webView.getScrollY(), bottomTo);
            anim.setDuration(1000).start();

        }

        // 클릭
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
