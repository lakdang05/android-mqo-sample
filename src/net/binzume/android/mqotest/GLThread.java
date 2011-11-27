package net.binzume.android.mqotest;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLU;
import android.util.Log;
import android.content.res.Resources;

public class GLThread extends Thread {
	//�ｽI�ｽu�ｽW�ｽF�ｽN�ｽg�ｽ�ｽ�ｽﾊ子
	private String mName;
	//�ｽ�ｽﾊ管暦ｿｽ�ｽI�ｽu�ｽW�ｽF�ｽN�ｽg
	private View3D mView;
	//�ｽ�ｽ�ｽs�ｽ�ｽ�ｽ�ｽ�ｽt�ｽ�ｽ�ｽO
	private boolean mDone;

	//ES�ｽ�ｽ�ｽ�ｽ�ｽ_�ｽ�ｽ�ｽ�ｽ�ｽO�ｽR�ｽ�ｽ�ｽe�ｽL�ｽX�ｽg
	private EGLContext mEglContext;
	//ES�ｽf�ｽB�ｽX�ｽv�ｽ�ｽ�ｽC�ｽR�ｽl�ｽN�ｽV�ｽ�ｽ�ｽ�ｽ
	private EGLDisplay mEglDisplay;
	//ES�ｽT�ｽ[�ｽt�ｽF�ｽC�ｽX
	private EGLSurface mEglSurface;
	//ES�ｽR�ｽ�ｽ�ｽt�ｽB�ｽO
	private EGLConfig mEglConfig;
	public float[] accel = { 0, 0, 0 };
	public float[] orientation = null;
	public double[] position = { 0, 0 };
	private boolean mTouch;

	/**
	 * �ｽR�ｽ�ｽ�ｽX�ｽg�ｽ�ｽ�ｽN�ｽ^
	 * 
	 * @param view
	 */
	public GLThread(View3D view) {
		mView = view;
		mName = "GLThread";
		mDone = false;
	}

	/**
	 * �ｽX�ｽ�ｽ�ｽb�ｽh�ｽ�ｽ�ｽ�ｽ�ｽX�ｽ^�ｽ[�ｽg
	 */
	@Override
	public void run() {
		//OpenGL ES�ｽﾌ擾ｿｽ�ｽ�ｽ
		if (!initGLES()) {
			Log.e(mName, "OpenGL ES�ｽ�ｽ�ｽ�ｽ�ｽs");
			mDone = true;
		}

		//�ｽI�ｽ�ｽ�ｽv�ｽ�ｽ�ｽ�ｽ�ｽﾅゑｿｽﾜで繰�ｽ�ｽﾔゑｿｽ
		GL10 gl = (GL10) mEglContext.getGL();

		init(gl);
		mTouch = false;

		while (!mDone) {
			//�ｽ`�ｽ�ｽ
			drawFrame(gl);
			try {
				this.wait(1);
			} catch (Exception e) {
			}
		}

		//OpenGL ES�ｽﾌ片付�ｽ�ｽ
		endGLES();
	}

	public void touch(int x, int y) {
		mTouch = !mTouch;
	}

	public void setAccel(float x, float y, float z) {
		accel[0] = x;
		accel[1] = y;
		accel[2] = z;
	}

	public void setOrientation(float[] o) {
		orientation = o;
	}

	public void setPosition(double lat, double lon) {
		position[0] = lat;
		position[1] = lon;
	}

	/**
	 * �ｽ`�ｽ�ｽ
	 * 
	 * @param gl OpenGL�ｽ�ｽ�ｽ�ｽn�ｽ�ｽ�ｽh�ｽ�ｽ
	 */
	private void drawFrame(GL10 gl) {

		{
			//�ｽ�ｽ�ｽf�ｽ�ｽ�ｽr�ｽ�ｽ�ｽ[�ｽs�ｽ�ｽﾌ設抵ｿｽ
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			//�ｽP�ｽﾊ行�ｽ�ｽ
			gl.glLoadIdentity();
			//�ｽJ�ｽ�ｽ�ｽ�ｽ�ｽﾌ設抵ｿｽ
			GLU.gluLookAt(gl,
				0.0f, 0.0f, 3.0f,
				0.0f, 0.0f, 0.0f,
				0.0f, 1.0f, 0.0f);
		}

		//�ｽ`�ｽ�ｽ
		draw(gl);

		//�ｽ�ｽﾊに出�ｽﾍゑｿｽ�ｽ�ｽo�ｽb�ｽt�ｽ@�ｽﾌ切ゑｿｽﾖゑｿｽ
		EGL10 egl = (EGL10) EGLContext.getEGL();
		egl.eglSwapBuffers(mEglDisplay, mEglSurface);
	}

	/**
	 * OpenGL ES�ｽ�ｽ�ｽ�ｽ
	 * 
	 * @return �ｽ�ｽ�ｽ�ｽI�ｽ�ｽ�ｽﾈゑｿｽ^�ｽA�ｽG�ｽ�ｽ�ｽ[�ｽﾈゑｿｽU
	 */
	private boolean initGLES() {
		//GL ES�ｽ�ｽ�ｽ�ゑｿｽW�ｽ�ｽ�ｽ[�ｽ�ｽ�ｽ謫ｾ 
		EGL10 egl = (EGL10) EGLContext.getEGL();

		{
			//�ｽf�ｽB�ｽX�ｽv�ｽ�ｽ�ｽC�ｽR�ｽl�ｽN�ｽV�ｽ�ｽ�ｽ�ｽ�ｽ�ｬ
			mEglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
			if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
				Log.e(mName, "�ｽf�ｽB�ｽX�ｽv�ｽ�ｽ�ｽC�ｽR�ｽl�ｽN�ｽV�ｽ�ｽ�ｽ�ｽ�ｽ�ｬ�ｽ�ｽ�ｽs");
				return false;
			}

			//�ｽf�ｽB�ｽX�ｽv�ｽ�ｽ�ｽC�ｽR�ｽl�ｽN�ｽV�ｽ�ｽ�ｽ�ｽ�ｽ�ｽ�ｽ�ｽ
			int[] version = new int[2];
			if (!egl.eglInitialize(mEglDisplay, version)) {
				Log.e(mName, "�ｽf�ｽB�ｽX�ｽv�ｽ�ｽ�ｽC�ｽR�ｽl�ｽN�ｽV�ｽ�ｽ�ｽ�ｽ�ｽ�ｽ�ｽ�ｽ�ｽs");
				return false;
			}
		}

		{
			//�ｽR�ｽ�ｽ�ｽt�ｽB�ｽO�ｽﾝ抵ｿｽ
			int[] configSpec = {
					//EGL10.EGL_ALPHA_SIZE, 8,	//�ｽA�ｽ�ｽ�ｽt�ｽ@�ｽ`�ｽ�ｽ�ｽ�ｽ�ｽl�ｽ�ｽ�ｽF8�ｽr�ｽb�ｽg
					EGL10.EGL_RED_SIZE, 5,
					EGL10.EGL_GREEN_SIZE, 6,
					EGL10.EGL_BLUE_SIZE, 5,
					EGL10.EGL_DEPTH_SIZE, 16, //�ｽ[�ｽx�ｽo�ｽb�ｽt�ｽ@�ｽF16�ｽr�ｽb�ｽg
					EGL10.EGL_NONE //�ｽI�ｽ[�ｽﾉゑｿｽEGL_NONE�ｽ�ｽ�ｽ�ｽ�ｽ
			};
			EGLConfig[] configs = new EGLConfig[1];
			int[] numConfigs = new int[1];
			if (!egl.eglChooseConfig(mEglDisplay, configSpec, configs, 1, numConfigs)) {
				Log.e(mName, "�ｽR�ｽ�ｽ�ｽt�ｽB�ｽO�ｽﾝ定失�ｽs");
				return false;
			}
			mEglConfig = configs[0];
		}

		{
			//�ｽ�ｽ�ｽ�ｽ�ｽ_�ｽ�ｽ�ｽ�ｽ�ｽO�ｽR�ｽ�ｽ�ｽe�ｽL�ｽX�ｽg�ｽ�ｬ
			mEglContext =
				egl.eglCreateContext(mEglDisplay, mEglConfig, EGL10.EGL_NO_CONTEXT, null);
			if (mEglContext == EGL10.EGL_NO_CONTEXT) {
				Log.e(mName, "�ｽ�ｽ�ｽ�ｽ�ｽ_�ｽ�ｽ�ｽ�ｽ�ｽO�ｽR�ｽ�ｽ�ｽe�ｽL�ｽX�ｽg�ｽ�ｬ�ｽ�ｽ�ｽs");
				return false;
			}
		}

		{
			//�ｽT�ｽ[�ｽt�ｽF�ｽC�ｽX�ｽ�ｬ(�ｽ�ｽ�ｽﾆで包ｿｽ�ｽ�ｽ�ｽ�ｽﾌで別�ｿｽ�ｽ\�ｽb�ｽh)
			if (!createSurface()) {
				Log.e(mName, "�ｽT�ｽ[�ｽt�ｽF�ｽC�ｽX�ｽ�ｬ�ｽ�ｽ�ｽs");
				return false;
			}
		}

		return true;
	}

	/**
	 * �ｽT�ｽ[�ｽt�ｽF�ｽC�ｽX�ｽ�ｬ
	 * �ｽT�ｽ[�ｽt�ｽF�ｽC�ｽX�ｽ�ｽ�ｽ�ｬ�ｽ�ｽ�ｽﾄ、�ｽ�ｽ�ｽ�ｽ�ｽ_�ｽ�ｽ�ｽ�ｽ�ｽO�ｽR�ｽ�ｽ�ｽe�ｽL�ｽX�
	 * ｽg�ｽﾆ鯉ｿｽ�ｽﾑつゑｿｽ�ｽ�ｽ
	 * 
	 * @return �ｽ�ｽ�ｽ�ｽI�ｽ�ｽ�ｽﾈゑｿｽ^�ｽA�ｽG�ｽ�ｽ�ｽ[�ｽﾈゑｿｽU
	 */
	private boolean createSurface() {
		EGL10 egl = (EGL10) EGLContext.getEGL();

		{
			//�ｽT�ｽ[�ｽt�ｽF�ｽC�ｽX�ｽ�ｬ
			mEglSurface =
				egl.eglCreateWindowSurface(mEglDisplay, mEglConfig, mView.getHolder(), null);
			if (mEglSurface == EGL10.EGL_NO_SURFACE) {
				Log.e(mName, "�ｽT�ｽ[�ｽt�ｽF�ｽC�ｽX�ｽ�ｬ�ｽ�ｽ�ｽs");
				return false;
			}
		}

		{
			//�ｽT�ｽ[�ｽt�ｽF�ｽC�ｽX�ｽﾆ�ｿｽ�ｽ�ｽ�ｽ_�ｽ�ｽ�ｽ�ｽ�ｽO�ｽR�ｽ�ｽ�ｽe�ｽL�ｽX�ｽg�ｽ�ｽ�ｽﾑつゑｿｽ
			if (!egl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
				Log.e(mName, "�ｽ�ｽ�ｽ�ｽ�ｽ_�ｽ�ｽ�ｽ�ｽ�ｽO�ｽR�ｽ�ｽ�ｽe�ｽL�ｽX�ｽg�ｽﾆの鯉ｿｽ�ｽﾑつゑｿｽ�ｽ�ｽ�ｽs");
				return false;
			}
		}

		return true;
	}

	/**
	 * OpenGL ES�ｽﾐ付�ｽ�ｽ
	 */
	private void endGLES() {
		EGL10 egl = (EGL10) EGLContext.getEGL();

		//�ｽT�ｽ[�ｽt�ｽF�ｽC�ｽX�ｽj�ｽ�ｽ
		if (mEglSurface != null) {
			//�ｽ�ｽ�ｽ�ｽ�ｽ_�ｽ�ｽ�ｽ�ｽ�ｽO�ｽR�ｽ�ｽ�ｽe�ｽL�ｽX�ｽg�ｽﾆの鯉ｿｽ�ｽﾑつゑｿｽ�ｽﾍ会ｿｽ�ｽ�ｽ
			egl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);

			egl.eglDestroySurface(mEglDisplay, mEglSurface);
			mEglSurface = null;
		}

		//�ｽ�ｽ�ｽ�ｽ�ｽ_�ｽ�ｽ�ｽ�ｽ�ｽO�ｽR�ｽ�ｽ�ｽe�ｽL�ｽX�ｽg�ｽj�ｽ�ｽ
		if (mEglContext != null) {
			egl.eglDestroyContext(mEglDisplay, mEglContext);
			mEglContext = null;
		}

		//�ｽf�ｽB�ｽX�ｽv�ｽ�ｽ�ｽC�ｽR�ｽl�ｽN�ｽV�ｽ�ｽ�ｽ�ｽ�ｽj�ｽ�ｽ
		if (mEglDisplay != null) {
			egl.eglTerminate(mEglDisplay);
			mEglDisplay = null;
		}
	}

	/**
	 * �ｽX�ｽ�ｽ�ｽb�ｽh�ｽI�ｽ�ｽ�ｽv�ｽ�ｽ
	 * �ｽX�ｽ�ｽ�ｽb�ｽh�ｽﾉ終�ｽ�ｽ�ｽv�ｽ�ｽ�ｽ�ｽ�ｽo�ｽ�ｽ�ｽﾄ、�ｽ�ｽ~�ｽ�ｽ�ｽ�ｽﾌゑｿｽﾒゑｿｽ
	 */
	public void RequestExitAndWait() {
		synchronized (this) {
			//�ｽI�ｽ�ｽ�ｽv�ｽ�ｽ�ｽ�ｽ�ｽo�ｽ�ｽ
			mDone = true;
		}

		try {
			//�ｽX�ｽ�ｽ�ｽb�ｽh�ｽI�ｽ�ｽ�ｽ�ｽﾒゑｿｽ
			join();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private float t = 0;

	private Bullet bullet = new Bullet(0, 0, 0);
	private GLDrawable grand = new Primitive.Plane(60.0f);
	private GLDrawable girl = MQOLoader.load("/sdcard/mqo/miku.mqo");

	class Bullet extends Primitive.Box {
		public float x, y, z;
		private int time = -1;
		private float rot = 4.0f;
		public float[] vec = { 0, 0, 0 };

		public Bullet(float x, float y, float z) {
			super(0.05f);
			this.x = x;
			this.y = y;
			this.z = z;
			time = -1;
		}

		void tick() {
			x += vec[0];
			y += vec[1];
			z += vec[2];
			rot += 10.0f;
			time++;
		}

		public void draw(GL10 gl) {
			gl.glPushMatrix();
			gl.glTranslatef(x, y, z);
			gl.glRotatef(rot, 0, 1.0f, 1.0f);
			super.draw(gl);
			gl.glPopMatrix();
		}
	}

	public void init(GL10 gl) {
		float[] lightAmbient = new float[] { 0.4f, 0.4f, 0.4f, 1.0f };//�ｽ�ｽ�ｽ�ｽ�ｽA�ｽ�ｽ�ｽr�ｽG�ｽ�ｽ�ｽg
		float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };//�ｽ�ｽ�ｽ�ｽ�ｽf�ｽB�ｽt�ｽ�ｽ�ｽ[�ｽY
		float[] lightPos = new float[] { 10, 0, 10, 0 }; //�ｽ�ｽ�ｽ�ｽ�ｽﾊ置
		float[] matAmbient = new float[] { 0.4f, 0.4f, 0.4f, 1.0f };//�ｽ}�ｽe�ｽ�ｽ�ｽA�ｽ�ｽ�ｽA�ｽ�ｽ�ｽr�ｽG�ｽ�ｽ�ｽg
		float[] matDiffuse = new float[] { 0.6f, 0.6f, 0.6f, 1.0f };//�ｽ}�ｽe�ｽ�ｽ�ｽA�ｽ�ｽ�ｽf�ｽB�ｽt�ｽ�ｽ�ｽ[�ｽY

		gl.glEnable(GL10.GL_DEPTH_TEST);

		gl.glEnable(GL10.GL_LIGHTING);
		gl.glEnable(GL10.GL_LIGHT0);
		gl.glEnable(GL10.GL_COLOR_MATERIAL);
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, matAmbient, 0);
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, matDiffuse, 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, lightAmbient, 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, lightDiffuse, 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPos, 0);

		gl.glEnable(GL10.GL_NORMALIZE);
		gl.glLightModelx(GL10.GL_LIGHT_MODEL_TWO_SIDE, 1);

		//�ｽﾐ面ス�ｽ�ｽ�ｽ[�ｽY�ｽV�ｽF�ｽ[�ｽf�ｽB�ｽ�ｽ�ｽO�ｽﾌ指�ｽ�ｽ
		gl.glEnable(GL10.GL_CULL_FACE);
		//gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glCullFace(GL10.GL_BACK);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();

		int w = mView.getWidth(), h = mView.getHeight();
		GLU.gluPerspective(gl, 45.0f, (float) w / h, 0.2f, 30.0f);
		gl.glViewport(0, 0, w, h);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glClearColor(0.2f, 0.4f, 0.8f, 1.0f);

	}

	public void draw(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		gl.glTranslatef(0, 0, -10.0f);

		if (orientation != null) {
			//gl.glRotatef(-70.0f, 1.0f, 0, 0);
			//gl.glRotatef(220.0f, 0, 0, 1.0f);

			gl.glMultMatrixf(orientation, 0);
		}

		//gl.glTranslatef(0, -5.0f, 0);

		// �ｽ�ｽp
		//        double dx = 139.786882- position[1]; //139.7857
		//        double dy = 35.688069 - position[0]; //356763
		double dx = 139.691719 - position[1]; //139.7857
		double dy = 35.68951 - position[0]; //356763
		double len = Math.sqrt(dx * dx + dy * dy);
		dx /= len;
		dy /= len;
		dx *= 15.0f;
		dy *= 15.0f;

		// �ｽe�ｽ�ｽ
		if (bullet.time < 0 && mTouch) {
			bullet.time = 0;
			bullet.vec[0] = -orientation[2] * 0.1f;
			bullet.vec[1] = -orientation[6] * 0.1f;
			bullet.vec[2] = -orientation[10] * 0.1f;
			bullet.x = 0;
			bullet.y = 0;
			bullet.z = 0;
			mTouch = false;
		}
		if (bullet.time >= 0) {

			bullet.tick();

			if (bullet.time > 200) {
				bullet.time = -1;
				mTouch = false;
			}

			bullet.draw(gl);
		}

		//gl.glColor4f(0.2f, 1.0f, 0.1f, 1.0f);
		//gl.glTranslatef(0, 0, -1.0f);
		//	grand.draw(gl);
		//gl.glTranslatef(0, 0, 1.0f);

		//gl.glTranslatef((float) dx, (float) dy, 0);

		//if (mTouch) rot+=1.0f;
		//gl.glRotatef(rot, 0, 0, 1.0f);
		//gl.glColor4f(0.8f, 0.8f, 0.8f, 1.0f);

		t += 0.5;
		//*
		gl.glTranslatef(0, 0, -4.0f);

		//gl.glRotatef(t, 0, 0, 1.0f);
		gl.glRotatef(90.0f, 1.0f, 0, 0);
		gl.glScalef(0.02f, 0.02f, -0.02f);
		girl.draw(gl);
		//*/
	}

}
