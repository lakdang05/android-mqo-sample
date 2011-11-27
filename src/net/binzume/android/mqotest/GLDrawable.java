package net.binzume.android.mqotest;

import javax.microedition.khronos.opengles.GL10;

public interface GLDrawable {
	public void draw(GL10 gl);

	public interface MeshObject extends GLDrawable {
		public void makeNormal();
	}
}
