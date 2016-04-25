package ct.codec.dji.tracking;

import ct.codec.dji.activity.PreviewActivity;
import ct.codec.dji_sdk_basicopencv.VisualApplication;
import android.util.Log;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.FlightController.DJIFlightControllerDataType.DJIVirtualStickFlightControlData;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJIBaseComponent.DJICompletionCallback;

public class FlightController {
	private static final String TAG = FlightController.class.getName();
	private volatile static FlightController instance = null;
	private DJIAircraft mProduct;
	private DJIFlightController mFlightController;
	private float yaw = 0.0f;
	private float pitch = 0.0f;
	private float roll = 0.0f;
	private float throttle = 0.0f;
	private static float coefficient = 1.0f;
	public boolean isTracking = false;
	
	private FlightController() {
		Log.d(TAG, "FlightController init");
	}
	
	public static FlightController getInstance() {
		if (instance == null) {
			instance = new FlightController();
		}
		return instance;
	}
	
	public void init() {
		if (mFlightController == null) {
			try {
	            mProduct = (DJIAircraft) VisualApplication.getProductInstance();
	            mFlightController = mProduct.getFlightController();
	        } catch(Exception exception) {
	            Log.e(TAG, exception.getMessage());
	        }
		}
	}
	
	public void onResume() {
		Log.d(TAG, "onResume");
	}
	
	public void onDestroy() {
		
	}
	
	public void enableController() {
		if (mFlightController == null) {
			try {
	            mProduct = (DJIAircraft) VisualApplication.getProductInstance();
	            mFlightController = mProduct.getFlightController();
	        } catch(Exception exception) {
	            Log.e(TAG, exception.getMessage());
	        }
		}
		mFlightController.enableVirtualStickControlMode(new DJICompletionCallback() {

            @Override
            public void onResult(DJIError arg0) {
                if (null == arg0) {
                	isTracking = true;
                	Log.d(TAG, "enable FlightController");
                	PreviewActivity.mhandler.sendEmptyMessage(PreviewActivity.MSG_UPDATE);
                } else {
                	Log.e(TAG, arg0.getDescription());
                }
            }
        });
	}
	
	public void disableController() {
		if (mFlightController == null) {
			try {
	            mProduct = (DJIAircraft) VisualApplication.getProductInstance();
	            mFlightController = mProduct.getFlightController();
	        } catch(Exception exception) {
	            Log.e(TAG, exception.getMessage());
	        }
		}
		mFlightController.disableVirtualStickControlMode(new DJICompletionCallback() {
            
            @Override
            public void onResult(DJIError arg0) {
                if (null == arg0) {
                	isTracking = false;
                	Log.d(TAG, "disable FlightController");
                	PreviewActivity.mhandler.sendEmptyMessage(PreviewActivity.MSG_UPDATE);
                } else {
                	Log.e(TAG, arg0.getDescription());
                }
            }
        });
	}
	
	public void setCoefficient(float i) {
		coefficient = i;
	}
	
	public void setController(float pYaw, float pThrottle, float pPitch, float pRoll) {
		float JoyControlMaxSpeedY = DJIFlightControllerDataType.DJIVirtualStickVerticalControlMaxVelocity;
        float JoyControlMaxSpeedX = DJIFlightControllerDataType.DJIVirtualStickYawControlMaxAngularVelocity;
        
        if (Math.abs(pYaw) < 0.02 ) {
        	pYaw = 0;
        }
        if (Math.abs(pThrottle) < 0.02 ) {
        	pThrottle = 0;
        }
        if (Math.abs(pPitch) < 0.02 ) {
        	pPitch = 0;
        }
        if (Math.abs(pRoll) < 0.02 ) {
        	pRoll = 0;
        }
        // left X
        yaw = (float)(JoyControlMaxSpeedX * pYaw * coefficient);
        // left Y
        throttle = (float)(JoyControlMaxSpeedY * pThrottle * coefficient);
        // right X
        roll = (float)(JoyControlMaxSpeedX * pPitch * coefficient);
        // right Y
        pitch = (float)(JoyControlMaxSpeedY * pRoll * coefficient);

        
        if (null != mFlightController) {
            mFlightController.sendVirtualStickFlightControlData(new DJIVirtualStickFlightControlData(pitch,roll,yaw,throttle), new DJICompletionCallback() {
                
                @Override
                public void onResult(DJIError arg0) {
                    Log.d(TAG, "joystick--" + " yaw:" + yaw + " throttle:" + throttle + " roll:" + roll + " pitch:" + pitch);
                }
            });
        }
	}
}
