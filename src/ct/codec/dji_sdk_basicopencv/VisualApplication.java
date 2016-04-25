package ct.codec.dji_sdk_basicopencv;

import ct.codec.dji.openCV.NativeController;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJISDKError;
import dji.sdk.base.DJIBaseComponent.DJIComponentListener;
import dji.sdk.base.DJIBaseProduct.DJIBaseProductListener;
import dji.sdk.base.DJIBaseProduct.DJIComponentKey;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class VisualApplication extends Application {
	
	private static final String TAG = VisualApplication.class.getName();
    
    public static final String FLAG_CONNECTION_CHANGE = "ct_codec_dji_connection_change";
    
    private static DJIBaseProduct mProduct;
    private Handler mHandler;
    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = null;
    private DJIBaseProductListener mDJIBaseProductListener = null;
    private DJIComponentListener mDJIComponentListener = null;
    
    public static synchronized DJIBaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getDJIProduct();
        }
        return mProduct;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "Visual Application onCreate");
        mHandler = new Handler(Looper.getMainLooper());
        initDJISDK();
        DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);
        initOpenCVLib();
    }
    
    private void initDJISDK() {
    	mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {

            @Override
            public void onGetRegisteredResult(DJISDKError error) {
                if(error == DJISDKError.REGISTRATION_SUCCESS) {
                    DJISDKManager.getInstance().startConnectionToProduct();
                } else {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "register sdk fails, check network is available", Toast.LENGTH_LONG).show();
                        }
                    });
                    
                }
                Log.e("TAG", error.toString());
            }

            @Override
            public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {

//                log(String.format("onProductChanged oldProduct:%s, newProduct:%s", oldProduct, newProduct));
                mProduct = newProduct;
                if(mProduct != null) {
                    mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
                }
                
                notifyStatusChange();
            }
        };
        
        mDJIBaseProductListener = new DJIBaseProductListener() {

            @Override
            public void onComponentChange(DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {

                if(newComponent != null) {
                    newComponent.setDJIComponentListener(mDJIComponentListener);
                }
//                log(String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s", key, oldComponent, newComponent));

                notifyStatusChange();
            }

            @Override
            public void onProductConnectivityChanged(boolean isConnected) {
                
//                log("onProductConnectivityChanged: " + isConnected);

                notifyStatusChange();
            }
            
        };
        
        mDJIComponentListener = new DJIComponentListener() {

            @Override
            public void onComponentConnectivityChanged(boolean isConnected) {
                notifyStatusChange();
            }
            
        };
    }
    
    private void initOpenCVLib() {
    	NativeController.loadLibrary();
    }
    
    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }
    
    private Runnable updateRunnable = new Runnable() {
        
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);  
            sendBroadcast(intent);
        }
    };
    
    public String getCurProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {

                return appProcess.processName;
            }
        }
        return null;
    }
    
}
