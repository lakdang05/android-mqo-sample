package net.binzume.android.mqotest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.util.Log;

public class GLObject implements GLDrawable.MeshObject {
	public FloatBuffer vartBuffer;
	public FloatBuffer normBuffer;
	public int vartNum;
	public ShortBuffer indicesBuffer;
	public FloatBuffer uvBuffer;
	public int polyNum;
	public int vbo[];
	public float mColor[];
	public int texId = 1;

	public GLObject() {

	}

	public void dispose() {

	}

	public void makeVbo_(GL11 gl) {
		vbo = new int[3];
		gl.glGenBuffers(3, vbo, 0);

		gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, vbo[0]);
		gl.glBufferData(GL11.GL_ARRAY_BUFFER, 4 * vartNum, vartBuffer, GL11.GL_STATIC_DRAW);

		gl.glBindBuffer(GL11.GL_ARRAY_BUFFER, vbo[1]);
		gl.glBufferData(GL11.GL_ARRAY_BUFFER, 2 * vartNum, normBuffer, GL11.GL_STATIC_DRAW);

		gl.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, vbo[2]);
		gl.glBufferData(GL11.GL_ELEMENT_ARRAY_BUFFER, 4 * polyNum, indicesBuffer, GL11.GL_STATIC_DRAW);

		gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);

	}

	public static float[] getNormal(float[] p1, float[] p2, float[] p3) {
		float[] c = {
				(p1[1] - p2[1]) * (p3[2] - p2[2]) - (p1[2] - p2[2]) * (p3[1] - p2[1]),
				(p1[2] - p2[2]) * (p3[0] - p2[0]) - (p1[0] - p2[0]) * (p3[2] - p2[2]),
				(p1[0] - p2[0]) * (p3[1] - p2[1]) - (p1[1] - p2[1]) * (p3[0] - p2[0])
		};
		float l = (float) Math.sqrt(c[0] * c[0] + c[1] * c[1] + c[2] * c[2]);
		c[0] /= l;
		c[1] /= l;
		c[2] /= l;
		return c;
	}

	public void makeNormal() {
		// 法線

		int count[] = new int[vartNum];
		short indices[] = new short[3];
		float vart[][] = { new float[3], new float[3], new float[3] };
		float norm[][] = new float[vartNum][];
		for (int i = 0; i < polyNum; i++) {
			indicesBuffer.get(indices);
			//Log.d("indices", "" + indices[0] + "," + indices[1] + "," + indices[2] + "   " + vartNum);
			vartBuffer.position(indices[0] * 3);
			vartBuffer.get(vart[0]);
			vartBuffer.position(indices[1] * 3);
			vartBuffer.get(vart[1]);
			vartBuffer.position(indices[2] * 3);
			vartBuffer.get(vart[2]);
			for (int j = 0; j < 3; j++) {
				float n[] = getNormal(vart[(j + 2) % 3], vart[j], vart[(j + 1) % 3]);
				if (norm[indices[j]] == null) {
					norm[indices[j]] = n;
				} else {
					norm[indices[j]][0] += n[0];
					norm[indices[j]][1] += n[1];
					norm[indices[j]][2] += n[2];
				}
				count[indices[j]]++;
			}
		}

		ByteBuffer vb = ByteBuffer.allocateDirect(4 * vartNum * 3);
		vb.order(ByteOrder.nativeOrder());
		FloatBuffer normBuffer = vb.asFloatBuffer();
		for (int i = 0; i < vartNum; i++) {
			float v[] = norm[i];
			if (v == null)
				continue;
			float l = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
			v[0] /= l;
			v[1] /= l;
			v[2] /= l;
			normBuffer.put(v);
		}
		this.normBuffer = normBuffer;

		normBuffer.position(0);
		vartBuffer.position(0);
		indicesBuffer.position(0);
	}

	public void color(float r, float g, float b) {
		mColor = new float[] { r, g, b, 1.0f };
	}

	public void draw(GL10 gl) {
		if (uvBuffer != null) {
			gl.glEnable(GL10.GL_TEXTURE_2D);
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		} else {
			gl.glDisable(GL10.GL_TEXTURE_2D);
			if (mColor != null) {
				gl.glColor4f(mColor[0], mColor[1], mColor[2], mColor[3]);
			}
		}

		// 頂点配列
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vartBuffer);

		if (uvBuffer != null) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, texId);
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, uvBuffer);

		}

		if (normBuffer != null) {
			// 法線配列
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
			gl.glNormalPointer(GL10.GL_FLOAT, 0, normBuffer);
		}

		// 描画
		//gl.glDrawArrays(GL10.GL_TRIANGLES, 0, polyNum);
		gl.glDrawElements(GL10.GL_TRIANGLES, polyNum * 3, GL10.GL_UNSIGNED_SHORT, indicesBuffer);

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);

	}

}
