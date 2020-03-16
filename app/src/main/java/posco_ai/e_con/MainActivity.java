package posco_ai.e_con;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;

import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import posco_ai.e_con.threadClass.CameraProcessor;
import posco_ai.e_con.threadClass.CoordinateReceiverTask;

public class MainActivity extends AppCompatActivity {

    private final String startUrl = "https://www.autodraw.com/";
    private static final int REQUEST_CAMERA = 1;
    private static final String TAG = "MainActivity";

    /**
     * 화면 녹화에 사용되는 변수들
     **/
    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;
    private static final String SCREENCAP_NAME = "screencap";
    private static int IMAGES_PRODUCED;
    private static String STORE_DIRECTORY;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;
    private ImageReader mImageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private OrientationChangeCallback mOrientationChangeCallback;
    private Socket socket;

    //    String serverIP = "192.168.0.17";
    String serverIP = "192.168.35.74";
    int serverPORT = 9000;
    int PORT = 8500;
    int screenPORT = 8600;

    private WebView webView;
    private ImageView gazePointer;
    private FloatingActionButton fabMain, fabGaze, fabSetting, fabScreenShare;
    private CameraBridgeViewBase mOpenCvCameraView;

    private Animation fabOpen, fabClose;

    // 버튼 눌렀는지 안눌렀는지 판별하는데 쓰임 처음 값은 false
    private boolean pointerVisible, shareClicked;
    private boolean fabState;

    private CameraProcessor cameraProcessor;
    private CoordinateReceiverTask coordinateReceiverTask;

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        permissionValidation();

        initView();
        initVariables();
        cameraViewInit();
        setWeb(webView);

        // start capture handling thread
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();

    }

    public void permissionValidation() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permissionCheck == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
    }

    private void initView() {
        webView = findViewById(R.id.webView);
        gazePointer = findViewById(R.id.gazePointer);
        fabMain = findViewById(R.id.fabMain);
        fabGaze = findViewById(R.id.fabGaze);
        fabSetting = findViewById(R.id.fabSetting);
        fabScreenShare = findViewById(R.id.fabScreenShare);
        mOpenCvCameraView = findViewById(R.id.activity_surface_view);
    }

    private void initVariables() {

        pointerVisible = false;
        shareClicked = false;
        fabState = true;

        fabOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);

        fabViewAni();
        Log.d(TAG, "initVariables: 실행");
        cameraProcessor = new CameraProcessor(gazePointer);

    }

    private void cameraViewInit() {
        mOpenCvCameraView.setCvCameraViewListener(cameraProcessor);
        mOpenCvCameraView.setCameraIndex(1);
        mOpenCvCameraView.setCameraPermissionGranted();
//        mOpenCvCameraView.setMaxFrameSize(resolutionWidth ,resolutionHeight);
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setVisibility(View.INVISIBLE);
    }


    private void setWeb(WebView webView) {
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float tmpx = event.getX();
                float tmpy = event.getY();
                Toast.makeText(MainActivity.this, "Coordi :" + tmpx + "/" + tmpy, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(startUrl);
    }

    // 버튼 클릭 이벤트
    public void onClick(View v) {
        switch (v.getId()) {
            // 플러스 버튼
            case R.id.fabMain:
                fabViewAni();
                break;
            // eye-tracking 버튼
            case R.id.fabGaze:
                // 버튼 한번 더 누를 경우 eye-tracking 종료
                if (pointerVisible) {
                    gazePointer.setVisibility(View.INVISIBLE);
//                coordinateReceiverTask.onPostExecute(true);
                    Snackbar.make(v, "Gaze Tracking Deactivation", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    mOpenCvCameraView.disableView();
                    mOpenCvCameraView.setVisibility(View.GONE);
                    cameraProcessor.onPostExecute(true);
//                cameraProcessor.

                    fabGaze.setImageResource(R.drawable.eye_fb_icon);
                    pointerVisible = !pointerVisible;
                }
                // 버튼 누를 경우 eye-tracking 시작
                else {
                    gazePointer.setVisibility(View.VISIBLE);
//                coordinateReceiverTask = new CoordinateReceiverTask(PORT, gazePointer);
//                coordinateReceiverTask.execute();

                    Snackbar.make(v, "Gaze Tracking Activation", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();


                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setVisibility(View.VISIBLE);
                    cameraProcessor.execute(serverIP, PORT + "");
                    Log.d(TAG, "onClick: serverIP: " + serverIP + " PORT: " + PORT);

                    fabGaze.setImageResource(R.drawable.touch_fb_icon);
                    pointerVisible = !pointerVisible;
                }
                break;
            // setting 버튼
            case R.id.fabSetting:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                // EconUtils.getLocalIpAddress(): 디바이스의 IP 주소 리턴
                builder.setTitle("IP Address").setMessage(EconUtils.getLocalIpAddress() + ":" + PORT).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                }).show();
                break;
            // 화면 공유 버튼
            case R.id.fabScreenShare:
                // 두번째로 누를 때
                if (shareClicked) {
                    Toast.makeText(getApplicationContext(), "녹화 중지 버튼 누름", Toast.LENGTH_SHORT).show();
                    fabScreenShare.setImageResource(R.drawable.ic_screen_share_white_24dp);
                    if (mediaProjection != null) {
                        // 녹화 종료
                        stopProjection();
                    }
                    // 처음 누를 때
                } else {
                    // 녹화 권한 요청
                    startProjection();
                    Toast.makeText(this, "녹화 버튼 누름", Toast.LENGTH_SHORT).show();
                }
                shareClicked = !shareClicked;
                break;
        }
    }


    // 녹화 권한 요청에 사용자가 응답한 후 호출됨 (취소 resultCode: 0, 시작하기 resultCode: -1)
    // TODO: onActivityResult
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 화면 녹화 요청이 맞으면
        if (requestCode == CAPTURE_PERMISSION_REQUEST_CODE) {
            Log.d(TAG, "onActivityResult: 화면 녹화");

            if (resultCode != Activity.RESULT_OK) {
                // 취소를 눌렀다면 함수 종료
                Log.d(TAG, "onActivityResult: 취소");
                return;
            }
            // 시작하기를 눌렀으면 버튼 아이콘 변경
            fabScreenShare.setImageResource(R.drawable.ic_stop_screen_share_white_24dp);
            // mediaProjection 객체 생성
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);

            /* 이 부분은 스크린 캡쳐한 것을 디바이스에 저장할 때 사용 */
            if (mediaProjection != null) {
//                File externalFilesDir = getExternalFilesDir(null);
//                if (externalFilesDir != null) {
//                    // 파일이 저장될 경로 지정
//                    STORE_DIRECTORY = externalFilesDir.getAbsolutePath() + "/screenshots/";
//                    Log.d(TAG, "onActivityResult: 경로: " + STORE_DIRECTORY);
//                    File storeDirectory = new File(STORE_DIRECTORY);
//                    if (!storeDirectory.exists()) {
//                        boolean success = storeDirectory.mkdirs();
//                        if (!success) {
//                            Log.e(TAG, "파일 저장 디렉토리 생성 실패");
//                            return;
//                        }
//                    }
//                } else {
//                    Log.e(TAG, "파일 저장 디렉토리 생성 실패, getExternalFilesDir is null.");
//                    return;
//                }

                // 소켓 생성
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d(TAG, "onImageAvailable: 소켓 연결 대기중 IP: " + serverIP + " PORT: " + screenPORT);
                            socket = new Socket(serverIP, screenPORT);
                            Log.d(TAG, "onImageAvailable: 소켓 연결 성공 IP: " + serverIP + " PORT: " + screenPORT);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            // display metrics. 디바이스 가로, 세로 길이와 dpi를 구함
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            mDensity = metrics.densityDpi;
            mDisplay = getWindowManager().getDefaultDisplay();

            // 가상 디스플레이 생성
            createVirtualDisplay();

            // mOrientationChangeCallback 등록
            mOrientationChangeCallback = new OrientationChangeCallback(this);
            if (mOrientationChangeCallback.canDetectOrientation()) {
                mOrientationChangeCallback.enable();
            }

            // MediaProjectionStopCallback 등록
            mediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);

        }

    }

    // 가상 디스플레이 생성
    private void createVirtualDisplay() {
        // get width and height
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x;
        mHeight = size.y;

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        // 이름, 가로, 세로, dpi, flag, surface
        mVirtualDisplay = mediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
        // 준비되면 onImageAvailable() 실행
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
    }

    // TODO: ImageAvailableListener
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;

            try {

                // 가장 최근 이미지 객체를 가져옴
                image = reader.acquireLatestImage();
                if (image != null) {
                    // 이미지 객체의 정보들을 가져옴
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    // 비트맵 생성
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    /** 스크립 캡쳐한 것을 디바이스에 저장할 때 사용 */
                    // bitmap 파일을 보낼 스트림 생성
                    // fos = new FileOutputStream(STORE_DIRECTORY + "/myscreen_" + IMAGES_PRODUCED + ".png");
                    // bitmap을 압축하고 해당 스트림에 보내 저장
                    // bitmap.compress(CompressFormat.JPEG, 100, fos);

                    // 이미지를 ByteArray로 바꾸기 위한 ByteArrayOutputStream
                    ByteArrayOutputStream ba = new ByteArrayOutputStream();
                    // 비트맵 이미지를 전달할 OutputStream
                    OutputStream os = socket.getOutputStream();
                    // 비트맵을 압축하고 ByteArrayOutputStream에 넣음
                    bitmap.compress(CompressFormat.JPEG, 100, ba);
                    // ByteArrayOutputStream을 바이트 배열로 바꾸고 배열의 길이 구함
                    byte[] bytes = ba.toByteArray();
                    int len = bytes.length;
                    Log.d(TAG, "onImageAvailable: 이미지의 바이트 길이: " + len);
                    // 서버에 이미지 바이트의 길이를 제대로 전달하기 위해 숫자를 10자리 문자열로 바꾸고 전달 (숫자가 없는 부분은 공백으로 채움)
                    StringBuilder stLen = new StringBuilder(String.valueOf(len));
                    Log.d(TAG, "onImageAvailable: stLen.length(): " + stLen.length());
                    int wantDigit = 10;
                    int currentDigit = stLen.length();
                    // 10자리를 맞추기 위해 나머지 부분은 공백으로 채움
                    for (int i = 0; i < wantDigit-currentDigit; i ++ ) {
                        stLen.append(' ');
                    }
                    Log.d(TAG, "onImageAvailable: stLen: " + stLen);
                    byte[] stLenBytes = stLen.toString().getBytes();
                    // 바이트 배열의 길이를 스트림에서 출력하여 서버에 전송
                    os.write(stLenBytes);
                    Log.d(TAG, "onImageAvailable: ba: " + Arrays.toString(bytes));
                    Log.d(TAG, "onImageAvailable: ");
                    // 바이트 배열을 스트림에서 출력하여 서버에 전송
                    ba.writeTo(os);

                    // 이름이 중복되지 않게 하기 위해 숫자를 1씩 증가시켜 이름에 붙여줌
                    IMAGES_PRODUCED++;
                    Log.e(TAG, "captured image: " + IMAGES_PRODUCED);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }

                if (bitmap != null) {
                    bitmap.recycle();
                }

                if (image != null) {
                    image.close();
                }
            }
        }
    }

    // 디바이스 방향이 바뀌었을때
    private class OrientationChangeCallback extends OrientationEventListener {

        OrientationChangeCallback(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            final int rotation = mDisplay.getRotation();
            if (rotation != mRotation) {
                mRotation = rotation;
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null)
                        mImageReader.setOnImageAvailableListener(null, null);

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // MediaProjection 정지 콜백
    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.e("ScreenCapture", "stopping projection.");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null)
                        mImageReader.setOnImageAvailableListener(null, null);
                    if (mOrientationChangeCallback != null)
                        mOrientationChangeCallback.disable();
                    mediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }

    // 화면 녹화 데이터를 인코딩하기 위한 미디어 코덱 만들기
//    private MediaCodec buildMediaCodec(DisplayMetrics dm) throws IOException {
//
//        int width = dm.widthPixels;
//        int height = dm.heightPixels;
//        String mimeType = "video/avc";
//
//        MediaFormat format = MediaFormat.createVideoFormat(mimeType, width, height);
//        // MediaFormat 속성 설정
//        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//        format.setInteger(MediaFormat.KEY_BIT_RATE, 3000000);
//        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
//        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10); // 1 seconds between I-frames
//
//        // Create a MediaCodec encoder and configure it. Get a Surface we can use for recording into.
//        MediaCodec mediaCodec = MediaCodec.createEncoderByType(mimeType);
//        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//        return mediaCodec;
//    }

    /**
     * 입력은 했으므로 출력만 필요
     **/
//    private void dataEncoding(MediaCodec codec) {
//        try {
//            MediaFormat outputFormat; // option B
//            codec.start();
//            while (true) {
//                long timeoutUs = 60 * 1000;
    // 데이터 입력
//                Log.d(TAG, "dataEncoding: 데이터 입력");
//                int inputBufferIndex = codec.dequeueInputBuffer(500000);
//                Log.d(TAG, "dataEncoding: 실행");
//                // Buffer index가 -1 보다 큰 경우여야 실제 코덱의 큐에 들어온 데이터를 인코딩할수 있음
//                if (inputBufferIndex >= 0) {
//                    // inputBuffer를 채워넣는다
//                    ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferIndex);
//                    Log.d(TAG, "dataEncoding: inputBuffer: " + inputBuffer);
//                    codec.queueInputBuffer(inputBufferIndex, 0, 655360, 60 * 1000, 0);
//                }
    // 데이터 출력
//                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//                int outputBufferIndex = codec.dequeueOutputBuffer(info, timeoutUs);
//                // Buffer index가 -1 보다 큰 경우여야 실제 코덱의 큐에 들어온 데이터를 인코딩할수 있음
//                if (outputBufferIndex >= 0) {
//                    ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferIndex);
//                    Log.d(TAG, "dataEncoding: outputBuffer: " + outputBuffer);
//                    codec.releaseOutputBuffer(outputBufferIndex, false);
//                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                    Log.d(TAG, "dataEncoding: INFO_OUTPUT_FORMAT_CHANGED");
//                    // Subsequent data will conform to new format.
//                    // Can ignore if using getOutputFormat(outputBufferId)
//                    outputFormat = codec.getOutputFormat(); // option B
//                    Log.d(TAG, "dataEncoding: outputFormat: " + outputFormat.toString());
//                }
//            }
//        } catch (Exception e) {
//            Log.d(TAG, "dataEncoding: 예외");
//            e.printStackTrace();
//        }
//    }
    private void stopScreenCapture(MediaCodec codec) {
//        Log.d(TAG, "dataEncoding: 종료");
//        codec.stop();
//        codec.release();
    }

    /****************************************** UI Widget Callbacks *******************************/
    private void startProjection() {
        mediaProjectionManager =
                (MediaProjectionManager) getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        assert mediaProjectionManager != null;
        // 녹화 권한 요청
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }

    private void stopProjection() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaProjection != null) {
                    mediaProjection.stop();
                }
            }
        });
    }

    // 버튼 열고 닫기
    private void fabViewAni() {
        // 버튼이 보일 때
        if (fabState) {
            fabGaze.startAnimation(fabClose);
            fabSetting.startAnimation(fabClose);
            fabScreenShare.startAnimation(fabClose);

            fabGaze.setClickable(false);
            fabSetting.setClickable(false);
            fabScreenShare.setClickable(false);

            fabMain.setImageResource(R.drawable.open_fb_icon);
        // 버튼이 안보일 때
        } else {
            fabGaze.startAnimation(fabOpen);
            fabSetting.startAnimation(fabOpen);
            fabScreenShare.startAnimation(fabOpen);

            fabGaze.setClickable(true);
            fabSetting.setClickable(true);
            fabScreenShare.setClickable(true);

            fabMain.setImageResource(R.drawable.close_fb_icon);
        }
        fabState = !fabState;
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.CAMERA)) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {

                        } else {
                            Toast.makeText(this, "Should have camera permission to run", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                }
                break;
        }
    }


}
