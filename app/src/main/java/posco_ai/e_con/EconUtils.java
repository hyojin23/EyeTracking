package posco_ai.e_con;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class EconUtils {
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void gazeTouchMotion(View view, float x, float y, int action){
        MotionEvent motionEvent = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis()+500,
                action,
                x,
                y,
                0
        );

        view.dispatchTouchEvent(motionEvent);
    }

}
