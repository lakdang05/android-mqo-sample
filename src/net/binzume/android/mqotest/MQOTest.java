package net.binzume.android.mqotest;

import java.util.*;
import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;
import android.view.MotionEvent;
import android.widget.Toast;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

public class MQOTest extends Activity implements SensorEventListener, LocationListener {
	View3D view;
	float[] accel = null;
	float[] magnet = null;
	float[] inR = new float[16];
	float[] outR = new float[16];
	float[] I = new float[16];
	Location location = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = new View3D(this);
		setContentView(view);

	}

	@Override
	protected void onResume() {
		super.onResume();
		boolean faccel = false, fmagnet = false;

		SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensors;
		sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
		for (Sensor sensor : sensors) {
			if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				// 加速度センサー
				sensorManager.registerListener(
						this, sensor,
						SensorManager.SENSOR_DELAY_GAME);
				faccel = true;
			} else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				// 磁界センサー
				sensorManager.registerListener(
						this, sensor,
						SensorManager.SENSOR_DELAY_GAME);
				fmagnet = true;
			}
		}

		if (!faccel)
			Toast.makeText(this, "ACCELEROMETER *NOT* Found", Toast.LENGTH_LONG).show();

		if (!fmagnet)
			Toast.makeText(this, "MAGNETIC_FIELD *NOT* Found", Toast.LENGTH_LONG).show();

		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

	}

	@Override
	protected void onPause() {
		super.onPause();

		SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorManager.unregisterListener(this);

		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.removeUpdates(this);

	}

	public void onAccuracyChanged(Sensor sensor, int value) {
	}

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			// event.values[0] ～ event.values[2] に　X,Y,Z
			accel = event.values.clone();
			if (view.mGLThread != null)
				view.mGLThread.setAccel(event.values[0], event.values[1], event.values[2]);
		}
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			magnet = event.values.clone();
		}

		//this.location = location;
		if (accel != null && magnet != null) {
			SensorManager.getRotationMatrix(inR, I, accel, magnet);
			SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
			float[] orientation = new float[3];

			SensorManager.getOrientation(outR, orientation);
			if (view.mGLThread != null)
				view.mGLThread.setOrientation(inR);
		}

	}

	public void onLocationChanged(Location location) {
		double lat = location.getLatitude();
		double lon = location.getLongitude();

		if (this.location == null) {
			//Toast.makeText(this, "la "+lat+"lo "+lon, Toast.LENGTH_LONG).show();
			view.mGLThread.setPosition(lat, lon);
		}

	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

}

class View3D extends SurfaceView implements Callback {
	//描画管理スレッド
	public GLThread mGLThread;

	/**
	 * コンストラクタ
	 * 
	 * @param context アプリケーションコンテキスト
	 */
	public View3D(MQOTest context) {
		super(context);

		//コールバックインターフェース設定
		getHolder().addCallback(this);

		//OpenGL ESを使う際はサーフェイスタイプをGPUにする
		getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);
		setFocusable(true);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mGLThread.touch((int) event.getX(), (int) event.getY());
		return super.onTouchEvent(event);
	}

	//@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		//画面サイズ変更処理
	}

	//@Override
	public void surfaceCreated(SurfaceHolder holder) {
		//GLThreadを動かす
		mGLThread = new GLThread(this);
		mGLThread.start();
	}

	//@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		//GLThreadに終了要求を出す
		mGLThread.RequestExitAndWait();
	}
}
