package posco_ai.e_con.threadClass;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class CoordinateReceiverTask extends AsyncTask<Integer, String, Boolean> {
    protected DatagramSocket socket;
    protected int x, y;
    private int sPORT;
    private View gazePointer;

    public CoordinateReceiverTask(int sPORT, View gazePointer) {
        this.sPORT = sPORT;
        this.gazePointer = gazePointer;
    }


    @Override
    public Boolean doInBackground(Integer... integers) {
        try {

            socket = new DatagramSocket(sPORT); // 소켓을 생성한다.
            DatagramPacket packet;
            byte[] buf;
            buf = new byte[20];
            packet = new DatagramPacket(buf, buf.length);


            while (true) {
                socket.receive(packet);
                String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
                Log.d("UDP", msg);

                publishProgress(msg);
            }

        } catch (Exception e) {
            Log.d("UDP", "s: Error", e);

        } finally {
            socket.disconnect();
            socket.close();
        }

        return null;
    }


    @Override
    public void onProgressUpdate(String... values) {
        String[] coordinates = values[0].split("/");
        x = Integer.parseInt(coordinates[0]);
        y = Integer.parseInt(coordinates[1]);
        gazePointer.setX(x);
        gazePointer.setY(y);
            if (x % 15 == 0){
//                EconUtils.gazeTouchMotion(webView,x,y, MotionEvent.ACTION_DOWN);
//                EconUtils.gazeTouchMotion(webView,x,y,MotionEvent.ACTION_UP);
            }
        super.onProgressUpdate(values);

    }


    @Override
    public void onPostExecute(Boolean aBoolean) {
        try {
            socket.disconnect();
            socket.close();
        }
        catch (Exception e) {
            Log.d("UDP", "s: Error", e);
        }
        super.onPostExecute(aBoolean);
    }
}


