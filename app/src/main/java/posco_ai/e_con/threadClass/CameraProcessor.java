package posco_ai.e_con.threadClass;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class CameraProcessor extends AsyncTask<String, String, Boolean> implements CameraBridgeViewBase.CvCameraViewListener2 {


    private Mat matResult;
    private Boolean previewVisible;
    protected int x, y ,click;
    private View gazePointer;
    private Socket socket;
    private static final String TAG = "CameraProcessor";

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

    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.d(TAG, "onCameraFrame: 실행");
        inputFrame.rgba().copyTo(matResult);

//        return inputFrame.rgba().t();
        if (previewVisible)
            return inputFrame.rgba().t();
        else
            return null;
    }


    @Override
    protected Boolean doInBackground(String... strings) {
        try {
            Thread.sleep(1000);
            Log.d(TAG, "doInBackground: 실행");
            String IP = strings[0];
            int PORT = Integer.parseInt(strings[1]);
            Log.d("SERVER_TAG", IP + ":" + PORT);

            final int imgSize = (int) (matResult.total() * matResult.channels());
            Log.d("SERVER_TAG", "SIZE : " +imgSize);

            byte[] outData = new byte[imgSize];
//            byte[] inData = new byte[30];

            Log.d("SERVER_TAG", "socket");
            socket = new Socket(IP, PORT); //android - Client  connect with Server socket(python)

            Log.d("SERVER_TAG", "STREAM");

            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream inStream = new DataInputStream(socket.getInputStream());
            // 데이터를 모아뒀다가 한줄씩 출력하기 위해 BufferedReader 사용
            BufferedReader br = new BufferedReader(new InputStreamReader(inStream));


            Log.d("SERVER_TAG", "WHILE");
            while (true) {
                Thread.sleep(1000);
                matResult.put(0, 0, outData);
                Log.d("SERVER_TAG", "WRITE");

                outStream.write(outData);

                Log.d("SERVER_TAG", "READ");
                // 서버에서 받은 데이터를 한줄씩 읽음
                String msg = br.readLine();
                publishProgress(msg);
                Log.d(TAG, "doInBackground: msg: " + msg);
            }

        } catch (Exception e ) {

            Log.e("SERVER_TAG", String.valueOf(e));
            e.printStackTrace();

        }
        finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        if (click==1) {
//                EconUtils.gazeTouchMotion(webView,x,y, MotionEvent.ACTION_DOWN);
//                EconUtils.gazeTouchMotion(webView,x,y,MotionEvent.ACTION_UP);
        }
        super.onProgressUpdate(values);
    }


    @Override
    public void onPostExecute(Boolean aBoolean) {
        Log.d(TAG, "onPostExecute: 실행");
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onPostExecute(aBoolean);
    }
}
