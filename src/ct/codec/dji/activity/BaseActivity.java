package ct.codec.dji.activity;

import java.util.ArrayList;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import com.hp.hpl.sparta.xpath.PositionEqualsExpr;

import ct.codec.dji.openCV.NativeController;
import ct.codec.dji.tracking.FlightController;
import ct.codec.dji.tracking.TrackingCore;
import ct.codec.dji_sdk_basicopencv.R;
import ct.codec.dji_sdk_basicopencv.VisualApplication;
import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import dji.log.DJILogHelper;
import dji.midware.media.DJIVideoDataRecver;
import dji.sdk.AirLink.DJILBAirLink.DJIOnReceivedVideoCallback;
import dji.sdk.Camera.DJICamera;
import dji.sdk.Camera.DJICamera.CameraReceivedVideoDataCallback;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIBaseProduct.Model;

@SuppressLint("ClickableViewAccessibility")
public class BaseActivity extends Activity implements OnTouchListener, SurfaceTextureListener, SurfaceHolder.Callback {
	
	public static final String TAG = BaseActivity.class.getName();
	
    private static final int INTERVAL_LOG = 300;
    private static long mLastTime = 0l;
    
    protected Dialog dialog;
    protected Button mOKBtn;
    protected Button mCancelBtn;
    protected RelativeLayout mRootView;
    protected LinearLayout mTrackingArea;
    protected RelativeLayout.LayoutParams layoutParams;
    
    protected Button mBtnTrack;
    protected RelativeLayout mLyTrack;
    protected Button mBtnOp;
    protected SeekBar mSeekBar;
    protected TextView mTvCofficient;
    
    private DJIBaseProduct mProduct;
    private DJICamera mCamera;
    
    protected TextView mConnectStatusTextView;
    
    protected TextureView mVideoSurface = null;
    protected CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;
    protected DJIOnReceivedVideoCallback mOnReceivedVideoCallback = null;
    protected DJICodecManager mCodecManager = null;
    
    protected SurfaceView mCannySurface = null;
    protected SurfaceHolder mSurfaceHolder=null;
    
    private final static int IMG_WIDTH = 1280;
    private final static int IMG_HEIGHT = 720;
    private Bitmap frameBitmap = null;
    private Bitmap outputBitmap = null;
    private Mat inputMat = new Mat(IMG_HEIGHT, IMG_WIDTH, CvType.CV_8UC4);
    private Mat grayMat = new Mat(IMG_HEIGHT, IMG_WIDTH, CvType.CV_8UC1);
    private Mat ouputMat = new Mat(IMG_HEIGHT, IMG_WIDTH, CvType.CV_8UC1);
    
    private int FPS = 0;
    PaintThread thread = new PaintThread();
    
    protected boolean trackingMode = false;
    protected boolean selectMode = false;
    protected boolean isTracking = false;
	Rect trackingRect = null;
	
	private float[] trackPosition = {0, 0, 0, 0};
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         
        IntentFilter filter = new IntentFilter();  
        filter.addAction(VisualApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);  
        
        frameBitmap = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);
        outputBitmap = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);
        
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
            mVideoSurface.setVisibility(View.GONE);
            mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);
            
            if (!VisualApplication.getProductInstance().getModel().equals(Model.UnknownAircraft)) {
	            mReceivedVideoDataCallBack = new CameraReceivedVideoDataCallback() {
	
	                @Override
	                public void onResult(byte[] videoBuffer, int size) {
	                    if(mCodecManager != null){
	                    	Log.d("DEBUGCT", "Recive data");
	                        mCodecManager.sendDataToDecoder(videoBuffer, size);
	                    } 
	                }
	            };  
            } else {
            	mOnReceivedVideoCallback = new DJIOnReceivedVideoCallback() {
					
					@Override
					public void onResult(byte[] videoBuffer, int size) {
						if(mCodecManager != null){
							Log.d("DEBUGCT", "Recive data");
	                        mCodecManager.sendDataToDecoder(videoBuffer, size);
	                    } 
					}
				};
            }
        }
        initPreviewer();
        Thread FpsTimer = new Thread(new Runnable() {
    		
    		@Override
    		public void run() {
    			while (true) {
    				FPS = 0;
    				try {
    					Thread.sleep(1000);
    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    				Log.d("FPS", "FPS = " + FPS);
    			}
    		}
    	});
        FpsTimer.start();

        
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            final long current = System.currentTimeMillis();
            if (current - mLastTime < INTERVAL_LOG) {
                DJILogHelper.getInstance().autoHandle();
                Log.d("", "click double");
                mLastTime = 0;
            } else {
                mLastTime = current;
                Log.d("", "click single");
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateTitleBar();
            onProductChange();
        }
        
    };
    
    protected void onProductChange() {
        
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        updateTitleBar();
        
    }
    
    @Override
    protected void onPause() {
        try {
            DJIVideoDataRecver.getInstance().setVideoDataListener(false, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        if(mCodecManager != null){
            mCodecManager.destroyCodec();
        }
        
        super.onPause();
    }
    
    
    
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
    
    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(BaseActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void initPreviewer() {
        try {
            mProduct = VisualApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
        }
        
        if (null == mProduct || !mProduct.isConnected()) {
            mCamera = null;
            showToast("Gimbal_Disconnect");
        } else {
        	if (!mProduct.getModel().equals(Model.UnknownAircraft)) {
	            mCamera = mProduct.getCamera();
	            if (mCamera != null){
	            	mCamera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);   
	        
	            }
        	} else {
        		if (null != mProduct.getAirLink()) {
        			if (null != mProduct.getAirLink().getLBAirLink()) {
        				mProduct.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(mOnReceivedVideoCallback);
        			}
        		}
        	}
        }
    }
    
    /**
     * @param surface
     * @param width
     * @param height
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureAvailable(android.graphics.SurfaceTexture,
     *      int, int)
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    /**
     * @param surface
     * @param width
     * @param height
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture,
     *      int, int)
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    	
    }

    /**
     * @param surface
     * @return
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureDestroyed(android.graphics.SurfaceTexture)
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCodecManager != null)
            mCodecManager.cleanSurface();
        return false;
    }

    /**
     * @param surface
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureUpdated(android.graphics.SurfaceTexture)
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    	if (mVideoSurface == null) {
    		return;
    	}
//    	long time0 = System.currentTimeMillis();
    	mVideoSurface.getBitmap(frameBitmap);
//        long time1 = System.currentTimeMillis();
        
        
        if (trackingMode) {
        	Utils.bitmapToMat(frameBitmap, inputMat);
        	TrackingCore.position = NativeController.Track(inputMat.nativeObj, grayMat.nativeObj, ouputMat.nativeObj);
//        	long time2 = System.currentTimeMillis();
            FPS++;
            TrackingCore.LogDetail();
            if (FlightController.getInstance().isTracking) {
            	TrackingCore.cal();
            }
//          Log.i("onFrame", "timing: getBmp= "  + (time1-time0) + " canny=" + (time2-time1));
            Utils.matToBitmap(inputMat, outputBitmap);
        }
        
        paintSurfaceView();
    }
    
/*    private boolean isProductConnected() {
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            return true;
        }
        return false; 
    }*/
    
    private void updateTitleBar() {
        log("updateTitleBar");
        if(mConnectStatusTextView == null) return;
        boolean ret = false;
        DJIBaseProduct product = VisualApplication.getProductInstance();
        if (product != null) {
            log("updateTitleBar product isconnected: " + product.isConnected());
            if(product.isConnected()) {
                mConnectStatusTextView.setText(VisualApplication.getProductInstance().getModel() + " Connected");
                ret = true;
            } else {
                if(product instanceof DJIAircraft) {
                    DJIAircraft aircraft = (DJIAircraft)product;
                    log("updateTitleBar rc: " + aircraft.getRemoteController());
                    if(aircraft.getRemoteController() != null) {
                        log("updateTitleBar rc connected: " + aircraft.getRemoteController().isConnected());
                    }
                    if(aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        mConnectStatusTextView.setText("only RC Connected");
                        ret = true;
                    }
                }
            }
        }
        
        if(!ret) {
            mConnectStatusTextView.setText("Disconnected"); 
        }
    }
    
    /**
     * @Description : RETURN BTN RESPONSE FUNCTION
     * @author : andy.zhao
     * @param view
     * @return : void
     */
    public void onReturn(View view) {
        this.finish();
    }
    
    private void log(String desc) {
        //DJILogHelper.getInstance().LOGD("sdk", desc, false, true);
    	Log.d(TAG, desc);
    }
    
    public ArrayList<String> makeListHelper(Object[] o) {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < o.length - 1; i++) {
            list.add(o[i].toString());
        }
        return list;
    }
    
    public void paintSurfaceView() {
    	thread.run();
    }
    
    private class PaintThread extends Thread{
        public void run(){
            Canvas canvas=null;
            canvas=mSurfaceHolder.lockCanvas();//获取目标画图区域，无参数表示锁定的是全部绘图区
            if (trackingMode) {
            	canvas.drawBitmap(outputBitmap, 0, 0, null);
            } else {
            	canvas.drawBitmap(frameBitmap, 0, 0, null);
            }
            mSurfaceHolder.unlockCanvasAndPost(canvas); //解除锁定并显示 
        }   
    }

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		paintSurfaceView();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		
	}
	
	protected float startX = 0;
	protected float startY = 0;
	protected int AreaWidth = 0;
	protected int AreaHeight = 0;
	protected float ratioX = 1.0f;
	protected float ratioY = 1.0f;
	

	@SuppressWarnings("deprecation")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (!selectMode) {
			return false;
		}
		switch (event.getAction()) { 
        case MotionEvent.ACTION_DOWN: 
        	if (mTrackingArea != null) {
        		mRootView.removeView(mTrackingArea);
        	}
        	startX = event.getX();
        	startY = event.getY();
        	layoutParams = new RelativeLayout.LayoutParams( 
        			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT); 
        	mTrackingArea = new LinearLayout(this); 
        	mTrackingArea.setVisibility(View.GONE);
        	mTrackingArea.setLayoutParams(layoutParams); 
        	mTrackingArea.setBackground(getResources().getDrawable(R.drawable.camera_switch_focus_real));
        	mRootView.addView(mTrackingArea);
        	Log.d("screen", "screen X: " + startX + " screen Y:" + startY);
            break;  
        case MotionEvent.ACTION_MOVE: 
        	AreaHeight = (int) (event.getY() - startY);
        	AreaWidth = (int) (event.getX() - startX);
        	mTrackingArea.setVisibility(View.VISIBLE);
        	layoutParams.setMargins((int)startX, (int)startY, 0, 0);
        	layoutParams.height = AreaHeight;
        	layoutParams.width = AreaWidth;
        	mTrackingArea.setLayoutParams(layoutParams);
            break;  
        case MotionEvent.ACTION_UP:
        	DisplayMetrics dm = new DisplayMetrics();
        	getWindowManager().getDefaultDisplay().getMetrics(dm);
        	TrackingCore.ScreenWidth = dm.widthPixels;
        	TrackingCore.ScreenHeight = dm.heightPixels; 
        	Log.d("screen", "screen width: " + dm.widthPixels + " screen height:" + dm.heightPixels);
        	ratioX = 1.0f * IMG_WIDTH / dm.widthPixels;
        	ratioY = 1.0f * IMG_HEIGHT / dm.heightPixels;
        	NativeController.TrackingArea((int)(startX * ratioX) , (int)(startY * ratioY), 
        			(int)(AreaWidth * ratioX), (int)(AreaHeight * ratioY));
        	trackingMode = true;
        	mTrackingArea.setVisibility(View.GONE);
        	mLyTrack.setVisibility(View.VISIBLE);
            break;
            
        default:
        	break;
        }  
        return true;
	}

}
