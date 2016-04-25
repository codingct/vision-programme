package ct.codec.dji.activity;

import java.lang.ref.WeakReference;

import ct.codec.dji.activity.BaseActivity;
import ct.codec.dji.tracking.FlightController;
import ct.codec.dji.tracking.TrackingCore;
import ct.codec.dji_sdk_basicopencv.R;
import dji.midware.media.DJIVideoDataRecver;
import dji.sdk.Camera.DJICamera.CameraReceivedVideoDataCallback;
import dji.sdk.Codec.DJICodecManager;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class PreviewActivity extends BaseActivity implements View.OnClickListener,OnSeekBarChangeListener{

	private static final String TAG = PreviewActivity.class.getName();
	public static final int MSG_UPDATE = 0x0001;

	private DJICodecManager mCodecManager = null;
	public static UIHandler mhandler = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_FULLSCREEN
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		setContentView(R.layout.activity_main);

		mhandler = new UIHandler(this);
		
		mRootView = (RelativeLayout) findViewById(R.id.root_view);
		mRootView.setOnTouchListener(this);
		
		mVideoSurface = (TextureView) findViewById(R.id.preview_texture);
		mVideoSurface.setSurfaceTextureListener(this);
		mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);
		
		mLyTrack = (RelativeLayout)findViewById(R.id.ly_track);
		mBtnOp = (Button)findViewById(R.id.btn_operate);
		mBtnTrack = (Button)findViewById(R.id.btn_track);
		mBtnOp.setOnClickListener(this);
		mBtnTrack.setOnClickListener(this);
		
		mSeekBar = (SeekBar)findViewById(R.id.seekbar);
		mSeekBar.setMax(200);
		mSeekBar.setOnSeekBarChangeListener(this);
		mTvCofficient = (TextView)findViewById(R.id.tv_coefficient);
		mTvCofficient.setText("" + coefficient);
		
		mReceivedVideoDataCallBack = new CameraReceivedVideoDataCallback() {

			@Override
			public void onResult(byte[] videoBuffer, int size) {

				if (mCodecManager != null) {
					mCodecManager.sendDataToDecoder(videoBuffer, size);
				}
			}
		};
		
		mCannySurface = (SurfaceView) findViewById(R.id.preview_surface);
		mSurfaceHolder = mCannySurface.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setFixedSize(1280, 720);
	}

	@Override
	protected void onResume() {
		super.onResume();
		FlightController.getInstance().onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		FlightController.getInstance().onDestroy();
		try {
			DJIVideoDataRecver.getInstance().setVideoDataListener(false, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (mCodecManager != null) {
			mCodecManager.destroyCodec();
		}

		kill();
		super.onDestroy();
	}
	
	

	/**
	 * @Description : RETURN BTN RESPONSE FUNCTION
	 */
	public void onReturn(View view) {
		Log.d(TAG, "onReturn");
		this.finish();
	}

	private void kill() {
		System.exit(0);
		ActivityManager am = (ActivityManager) this
				.getSystemService(Context.ACTIVITY_SERVICE);
		am.killBackgroundProcesses(getPackageName());

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_operate:
			if (!FlightController.getInstance().isTracking) {
				FlightController.getInstance().enableController();
			} else {
				FlightController.getInstance().disableController();
			}
			TrackingCore.reset();
			break;
		case R.id.btn_track:
			if (!selectMode) {
				selectMode = true;
				mBtnTrack.setText("Cancel");
			} else {
				selectMode = false;
				mBtnTrack.setText("Track");
				if (trackingMode) {
					trackingMode = false;
				}
				mLyTrack.setVisibility(View.GONE);
			}
			break;
		}
		
	}

	private float coefficient = 0;
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		coefficient = progress / 200f;
		FlightController.getInstance().setCoefficient(coefficient);
		mTvCofficient.setText(String.format("%.2f", coefficient));
	}
		
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		
	}
	
	public static class UIHandler extends Handler {
		private final WeakReference<PreviewActivity> mOutCls;
		public UIHandler(final PreviewActivity pa) {
			super(Looper.getMainLooper());
	        mOutCls = new WeakReference<PreviewActivity>(pa);
	    }
		
		@Override
		public void handleMessage(Message msg) {
			final PreviewActivity pa = mOutCls.get();
			switch(msg.what) {
			case MSG_UPDATE:
				if (FlightController.getInstance().isTracking) {
					pa.mBtnOp.setText("Stop");
				} else {
					pa.mBtnOp.setText("Go");
				}
				break;
			}
	    }
	}
}
