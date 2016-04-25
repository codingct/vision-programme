package ct.codec.dji.tracking;

import android.util.Log;


public class TrackingCore {
	public static final String TAG = TrackingCore.class.getName();
	public static float[] position = {0, 0, 0, 0};
	public static float ScreenHeight = 0;
	public static float ScreenWidth = 0;
	private static float offset_x = 0;
	private static float offset_y = 0;
	private static float area = 0;
	private static float origin_area = -1;
	private static float area_change = 0;
	
	private static float pYaw, pThrottle, pPitch, pRoll = 0;
	
	
	public static void cal() {
		offset_x = position[0] - ScreenWidth / 2.0f;
		offset_y = position[1] - ScreenHeight / 2.0f;
		area = position[2] * position[3];
		if (origin_area == -1) {
			origin_area = area;
		}
		
		area_change = origin_area - area;
		if (Math.abs(area_change / area) > 0.5) {
			Log.e(TAG, "area Exception!");
			pRoll = 0;
		}
		
		if (Math.abs(area_change / area) > 0.1) {
			pRoll = area_change / area;
		} else {
			pRoll = 0;
		}
		
		pPitch = offset_y / ScreenHeight;
		pRoll += offset_x / ScreenWidth;
		
		FlightController.getInstance().setController(pYaw, pThrottle, pPitch, pRoll);
	}
	
	public static void reset() {
		ScreenHeight = 0;
		ScreenWidth = 0;
		offset_x = 0;
		offset_y = 0;
		area = 0;
		origin_area = -1;
		
		pYaw = 0;
		pThrottle = 0;
		pPitch = 0;
		pRoll = 0;
	}
	
	public static void LogDetail() {
//		Log.d(TAG, "ScreenWidth = " + ScreenWidth + "ScreenHeight = " + ScreenHeight);
		Log.d(TAG, " x--"+position[0] 
        		 + " y--" + position[1] 
        		 + " width--" + position[2]
        		 + " height--" + position[3]);
	}
	
}
