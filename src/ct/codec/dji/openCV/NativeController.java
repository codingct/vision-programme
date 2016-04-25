package ct.codec.dji.openCV;

import android.util.Log;

public class NativeController {
	private final static String TAG = NativeController.class.getName();
	static {
        try {
        	
            Log.d(TAG, "try to load jni_visual.so");
            System.loadLibrary("jni_visual");
        }
        catch (UnsatisfiedLinkError e) {
        	e.printStackTrace();
            Log.e(TAG, "Couldn't load lib");
        }
    }
    
    public static void loadLibrary() { }
    
    public native static void TrackingArea(int startX, int startY, int width, int height);
	
    public native static float[] Track(long matAddrSource, long matAddrGr, long matAddrRgba);
    
    
    
}
