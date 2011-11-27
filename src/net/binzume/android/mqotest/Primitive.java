package net.binzume.android.mqotest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Primitive {

	public static class Plane implements GLDrawable {
		public float size;
		FloatBuffer vart_buffer;
		FloatBuffer norm_buffer;
		int vart_num;

		/**
		 * @param size 大きさ
		 */
		public Plane(float size) {
			create(size);
		}

		protected void create(float size) {
			this.size = size;

			ByteBuffer vb = ByteBuffer.allocateDirect(4 * 3 * 36);
			vb.order(ByteOrder.nativeOrder());
			vart_buffer = vb.asFloatBuffer();

			ByteBuffer nb = ByteBuffer.allocateDirect(4 * 3 * 36);
			nb.order(ByteOrder.nativeOrder());
			norm_buffer = nb.asFloatBuffer();

			float[][] sq_vart = {
					{ size, size, 0 },
					{ -size, size, 0 },
					{ size, -size, 0 },
					{ -size, -size, 0 },
			};
			int v[] = { 0, 1, 2, 2, 1, 3 };
			float n[][] = { { 0, 0, 1 }, };
			for (int i = 0; i < v.length; i++) {
				norm_buffer.put(n[i / 6]);
				vart_buffer.put(sq_vart[v[i]]);
			}
			vart_num = vart_buffer.position() / 3;
			vart_buffer.position(0);
			norm_buffer.position(0);
		}

		public void draw(GL10 gl) {
			// 頂点配列
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vart_buffer);

			// 法線配列
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
			gl.glNormalPointer(GL10.GL_FLOAT, 0, norm_buffer);

			// 描画
			gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vart_num);

			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		}
	}

	public static class Box implements GLDrawable {
		public float size;
		FloatBuffer vart_buffer;
		FloatBuffer norm_buffer;
		int vart_num;

		/**
		 * @param size 立方体の大きさ
		 */
		Box(float size) {
			create(size);
		}

		Box() {
			create(1.0f);
		}

		protected void create(float size) {
			this.size = size;

			ByteBuffer vb = ByteBuffer.allocateDirect(4 * 3 * 36);
			vb.order(ByteOrder.nativeOrder());
			vart_buffer = vb.asFloatBuffer();

			ByteBuffer nb = ByteBuffer.allocateDirect(4 * 3 * 36);
			nb.order(ByteOrder.nativeOrder());
			norm_buffer = nb.asFloatBuffer();

			float[][] sq_vart = {
					{ size, size, size },
					{ -size, size, size },
					{ size, -size, size },
					{ -size, -size, size },
					{ size, size, -size },
					{ -size, size, -size },
					{ size, -size, -size },
					{ -size, -size, -size },
			};
			int v[] = { 0, 1, 2, 2, 1, 3, 5, 4, 6, 5, 6, 7, 1, 0, 4, 1, 4, 5,
					2, 3, 6, 6, 3, 7, 0, 2, 4, 4, 2, 6, 3, 1, 5, 3, 5, 7 };
			float n[][] = { { 0, 0, 1 }, { 0, 0, -1 }, { 0, 1, 0 }, { 0, -1, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, };
			for (int i = 0; i < v.length; i++) {
				norm_buffer.put(n[i / 6]);
				vart_buffer.put(sq_vart[v[i]]);
			}
			vart_num = vart_buffer.position() / 3;
			vart_buffer.position(0);
			norm_buffer.position(0);
		}

		public void draw(GL10 gl) {
			// 頂点配列
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vart_buffer);

			// 法線配列
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
			gl.glNormalPointer(GL10.GL_FLOAT, 0, norm_buffer);

			// 描画
			gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vart_num);

			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		}
	}

	public static class Sphere implements GLDrawable {
		public float radius;
		protected int div;
		FloatBuffer vart_buffer;
		FloatBuffer norm_buffer;
		int vart_num;

		/**
		 * @param r 半径
		 * @param n 分割数
		 */
		Sphere(float r, int n) {
			create(r, n);
		}

		Sphere() {
			create(1.0f, 20);
		}

		protected void create(float r, int n) {
			radius = r;
			div = n;

			ByteBuffer vb = ByteBuffer.allocateDirect(4 * 3 * div * div * 2);
			vb.order(ByteOrder.nativeOrder());
			vart_buffer = vb.asFloatBuffer();

			ByteBuffer nb = ByteBuffer.allocateDirect(4 * 3 * div * div * 2);
			nb.order(ByteOrder.nativeOrder());
			norm_buffer = nb.asFloatBuffer();

			float z1, z2, nz1, nz2, s1, s2;
			for (int j = 0; j < div; j++) {
				s1 = (float) Math.sin(j * Math.PI * 2 / n);
				s2 = (float) Math.sin((j + 1) * Math.PI * 2 / n);
				nz1 = (float) Math.cos(j * Math.PI * 2 / n);
				nz2 = (float) Math.cos((j + 1) * Math.PI * 2 / n);
				z1 = nz1 * r;
				z2 = nz2 * r;
				for (int i = 0; i < div; i++) {
					float x = (float) Math.cos(i * Math.PI * 2 / n);
					float y = (float) Math.sin(i * Math.PI * 2 / n);
					float vart[] = { x * r * s1, y * r * s1, z1, x * r * s2,
							y * r * s2, z2 };
					float norm[] = { x * s1, y * s1, nz1, x * s2, y * s2, nz2 };
					vart_buffer.put(vart);
					norm_buffer.put(norm);
				}
			}
			vart_num = vart_buffer.position() / 3;
			vart_buffer.position(0);
			norm_buffer.position(0);
		}

		public void draw(GL10 gl) {
			// 頂点配列
			gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vart_buffer);

			// 法線配列
			gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
			gl.glNormalPointer(GL10.GL_FLOAT, 0, norm_buffer);

			// 描画
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vart_num);

			gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		}
	}

	public static class Cylinder implements GLDrawable {

		public void draw(GL10 gl) {
		}
	}

}
