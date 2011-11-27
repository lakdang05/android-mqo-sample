package net.binzume.android.mqotest;

import javax.microedition.khronos.opengles.GL10;

import java.io.IOException;
import java.io.InputStream;
import java.nio.*;
import java.util.*;
import android.util.Log;

class OctreeNode {
	public int value = 0;
	public OctreeNode child[] = null;
	public int size;

	OctreeNode(int sz) {
		size = sz;
	}

	public void makeChildNodes() {
		if (size < 2)
			return;
		child = new OctreeNode[8];
		int half = size >> 1;
		for (int i = 0; i < 8; i++) {
			child[i] = new OctreeNode(half);
			child[i].value = value;
		}
	}

	public boolean optimize() {
		if (child != null) {
			boolean f = true;
			for (int i = 0; i < 8; i++) {
				f &= child[i].optimize();
			}
			if (!f)
				return false;
			int v = child[0].value;
			for (int i = 1; i < 8; i++) {
				if (child[i].value != v)
					return false;
			}
			value = v;
			child = null;
		}
		return true;
	}

	public int getValue(int x, int y, int z) {
		//Log.d("Octree","get "+x+","+y+","+z+" v:"+value+" s:"+size);
		if (x < 0 || x >= size || y < 0 || y >= size || z < 0 || z >= size)
			return -1;
		if (child == null)
			return value;
		int half = size >> 1;
		int i = 0;
		if (x >= half) {
			i |= 1;
			x -= half;
		}
		if (y >= half) {
			i |= 2;
			y -= half;
		}
		if (z >= half) {
			i |= 4;
			z -= half;
		}

		return child[i].getValue(x, y, z);
	}

	public void setValue(int x, int y, int z, int v) {
		//Log.d("Octree","set "+x+","+y+","+z+" v:"+v+" s:"+size);
		if (x < 0 || x >= size || y < 0 || y >= size || z < 0 || z >= size)
			return;
		if (size == 1) {
			value = v;
			return;
		}
		if (child == null) {
			if (value == v)
				return;
			makeChildNodes();
		}
		int half = size >> 1;
		int i = 0;
		if (x >= half) {
			i |= 1;
			x -= half;
		}
		if (y >= half) {
			i |= 2;
			y -= half;
		}
		if (z >= half) {
			i |= 4;
			z -= half;
		}
		child[i].setValue(x, y, z, v);

		for (i = 0; i < 8; i++) {
			if (child[i].child != null || child[i].value != v)
				return;
		}
		value = v;
		child = null;
		//Log.d("Octree","marge! "+x+","+y+","+z+" v:"+v+" s:"+size);
	}

	void unserialize(InputStream in) throws IOException {
		child = null;
		if (in.read() == 0) {
			value = in.read();
		} else {
			makeChildNodes();
			for (int i = 0; i < 8; i++) {
				child[i].unserialize(in);
			}
		}
	}

}

public class Octree implements GLDrawable {

	OctreeNode element;
	float element_size;
	float[] vart_array;
	float[] norm_array;
	FloatBuffer vart_buffer;
	FloatBuffer norm_buffer;
	int vart_num;
	HashMap<Integer, float[]> vart_map = new HashMap<Integer, float[]>(1024 * 10);

	Octree() {
		int sz = 32;
		element = new OctreeNode(sz);
		element_size = 2.0f / sz;
		element.value = 1;

		/*
				element.value = 1;
				element.setValue(3,3,3, 0);
				element.setValue(2,3,3, 0);
				element.setValue(1,3,3, 0);
				element.setValue(3,2,3, 0);
				element.setValue(2,2,3, 0);
				element.setValue(1,2,3, 0);

				element.setValue(3,3,0, 0);
				element.setValue(2,3,0, 0);
				element.setValue(1,3,0, 0);
				element.setValue(3,2,0, 0);
				element.setValue(2,2,0, 0);
				element.setValue(1,2,0, 0);
		*/
		/*
		for (int i=-sz/2;i<sz/2;i++) {
			for (int j=-sz/2;j<sz/2;j++) {
				for (int k=-sz/2;k<sz/2;k++) {
					if (i*i+j*j+k*k<(sz/2)*(sz/2)) {
						element.setValue(i+sz/2,j+sz/2,k+sz/2, 1);
					}
				}
			}
		}
		*/
		vart_array = new float[3 * 150000];
		vart_buffer = FloatBuffer.wrap(vart_array);

		norm_array = new float[3 * 150000];
		norm_buffer = FloatBuffer.wrap(norm_array);

		/*
		ByteBuffer vb = ByteBuffer.allocateDirect(4 * 3 * 150000);
		vb.order(ByteOrder.nativeOrder());
		vart_buffer = vb.asFloatBuffer();

		ByteBuffer nb = ByteBuffer.allocateDirect(4 * 3 * 150000);
		nb.order(ByteOrder.nativeOrder());
		norm_buffer = nb.asFloatBuffer();
		 */

		make_vartex();
	}

	private float[] adjust_vart(int x, int y, int z, int[] ff, int offset) {

		int n = x + (offset % 3) + (y + (offset / 3) % 3) * 1024 + (z + (offset / 9)) * 1024 * 1024;
		float[] pp = vart_map.get(n);
		if (pp != null) {
			return pp;
		}

		float e = element_size * 0.11f;
		int a, b;
		short d[] = { 0, 1, 3, 4, 9, 10, 12, 13 };
		for (int i = 0; i < 8; i++) {
			int dd = offset + d[i];
			if (ff[dd] < 0) {
				ff[dd] = element.getValue(x + (dd % 3), y + (dd / 3) % 3, z + (dd / 9) % 3) > 0 ? 1 : 0;
			}
		}

		float[] p = { (x + (offset % 3)) * element_size + element_size * 0.5f,
				(y + (offset / 3) % 3) * element_size + element_size * 0.5f,
				(z + (offset / 9)) * element_size + element_size * 0.5f };

		// x
		a = ff[offset + 0] + ff[offset + 3] + ff[offset + 9] + ff[offset + 12];
		b = ff[offset + 1] + ff[offset + 4] + ff[offset + 10] + ff[offset + 13];
		if (a > b) {
			p[0] += e * (a + b - 4);
		} else if (a < b) {
			p[0] -= e * (a + b - 4);
		}

		// y
		a = ff[offset + 0] + ff[offset + 1] + ff[offset + 9] + ff[offset + 10];
		b = ff[offset + 3] + ff[offset + 4] + ff[offset + 12] + ff[offset + 13];
		if (a > b) {
			p[1] += e * (a + b - 4);
		} else if (a < b) {
			p[1] -= e * (a + b - 4);
		}

		// z
		a = ff[offset + 0] + ff[offset + 1] + ff[offset + 3] + ff[offset + 4];
		b = ff[offset + 9] + ff[offset + 10] + ff[offset + 12] + ff[offset + 13];
		if (a > b) {
			p[2] += e * (a + b - 4);
		} else if (a < b) {
			p[2] -= e * (a + b - 4);
		}
		vart_map.put(n, p.clone());
		return p;
	}

	private float[] cross(float[] a, float[] b) {
		float[] c = {
				a[1] * b[2] - a[2] * b[1],
				a[2] * b[0] - a[0] * b[2],
				a[0] * b[1] - a[1] * b[0]
		};
		return c;
	}

	private float[] norm(float[] p1, float[] p2, float[] p3) {
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

	private void make_vartex(OctreeNode elem, int x, int y, int z) {
		if (elem.child != null) {
			int half = elem.size >> 1;
			for (int i = 0; i < 8; i++) {
				int dx = 0, dy = 0, dz = 0;
				if ((i & 1) != 0)
					dx = half;
				if ((i & 2) != 0)
					dy = half;
				if ((i & 4) != 0)
					dz = half;
				make_vartex(elem.child[i], x + dx, y + dy, z + dz);
			}
			return;
		}
		if (elem.value == 0)
			return;

		int sz = elem.size;
		float[][] sq_vart = { null, null, null, null };
		int[] ff = new int[27];
		int[] vn = { 0, 1, 2, 3, 2, 1 };
		int[][] offset_array = {
				{ 0, 1, 3, 4 },
				{ 0, 3, 1, 4 },
				{ 0, 3, 9, 12 },
				{ 0, 9, 3, 12 },
				{ 0, 9, 1, 10 },
				{ 0, 1, 9, 10 },
		};

		for (int j = 0; j < sz; j++) {
			for (int i = 0; i < sz; i++) {
				if (vart_num > 149000)
					continue;
				// z+
				if (element.getValue(x + i, y + j, z + sz) <= 0) {
					//Log.d("Octree","draw "+x+","+y+","+z+" "+(x+i)+","+(y+j)+","+(z+sz));

					int[] offset = offset_array[0];
					for (int k = 0; k < 27; k++) {
						ff[k] = -1;
					}
					for (int k = 0; k < 4; k++) {
						sq_vart[k] = adjust_vart(x + i - 1, y + j - 1, z + sz - 1, ff, offset[k]);
					}

					float[] n = norm(sq_vart[2], sq_vart[1], sq_vart[0]);
					for (int k = 0; k < 6; k++) {
						vart_array[vart_num * 3 + 0] = sq_vart[vn[k]][0];
						vart_array[vart_num * 3 + 1] = sq_vart[vn[k]][1];
						vart_array[vart_num * 3 + 2] = sq_vart[vn[k]][2];
						norm_array[vart_num * 3 + 0] = n[0];
						norm_array[vart_num * 3 + 1] = n[1];
						norm_array[vart_num * 3 + 2] = n[2];
						vart_num++;
					}
				}

				// z-
				if (element.getValue(x + i, y + j, z - 1) <= 0) {

					int[] offset = offset_array[1];
					for (int k = 0; k < 27; k++) {
						ff[k] = -1;
					}
					for (int k = 0; k < 4; k++) {
						sq_vart[k] = adjust_vart(x + i - 1, y + j - 1, z - 1, ff, offset[k]);
					}

					float[] n = norm(sq_vart[2], sq_vart[1], sq_vart[0]);
					for (int k = 0; k < 6; k++) {
						vart_array[vart_num * 3 + 0] = sq_vart[vn[k]][0];
						vart_array[vart_num * 3 + 1] = sq_vart[vn[k]][1];
						vart_array[vart_num * 3 + 2] = sq_vart[vn[k]][2];
						norm_array[vart_num * 3 + 0] = n[0];
						norm_array[vart_num * 3 + 1] = n[1];
						norm_array[vart_num * 3 + 2] = n[2];
						vart_num++;
					}
				}

				// x+
				if (element.getValue(x + sz, y + i, z + j) <= 0) {

					int[] offset = offset_array[2];
					for (int k = 0; k < 27; k++) {
						ff[k] = -1;
					}
					for (int k = 0; k < 4; k++) {
						sq_vart[k] = adjust_vart(x + sz - 1, y + i - 1, z + j - 1, ff, offset[k]);
					}

					float[] n = norm(sq_vart[2], sq_vart[1], sq_vart[0]);
					for (int k = 0; k < 6; k++) {
						vart_array[vart_num * 3 + 0] = sq_vart[vn[k]][0];
						vart_array[vart_num * 3 + 1] = sq_vart[vn[k]][1];
						vart_array[vart_num * 3 + 2] = sq_vart[vn[k]][2];
						norm_array[vart_num * 3 + 0] = n[0];
						norm_array[vart_num * 3 + 1] = n[1];
						norm_array[vart_num * 3 + 2] = n[2];
						vart_num++;
					}
				}

				// x-
				if (element.getValue(x - 1, y + i, z + j) <= 0) {

					int[] offset = offset_array[3];
					for (int k = 0; k < 27; k++) {
						ff[k] = -1;
					}
					for (int k = 0; k < 4; k++) {
						sq_vart[k] = adjust_vart(x - 1, y + i - 1, z + j - 1, ff, offset[k]);
					}

					float[] n = norm(sq_vart[2], sq_vart[1], sq_vart[0]);
					for (int k = 0; k < 6; k++) {
						vart_array[vart_num * 3 + 0] = sq_vart[vn[k]][0];
						vart_array[vart_num * 3 + 1] = sq_vart[vn[k]][1];
						vart_array[vart_num * 3 + 2] = sq_vart[vn[k]][2];
						norm_array[vart_num * 3 + 0] = n[0];
						norm_array[vart_num * 3 + 1] = n[1];
						norm_array[vart_num * 3 + 2] = n[2];
						vart_num++;
					}
				}

				// y+
				if (element.getValue(x + j, y + sz, z + i) <= 0) {
					//Log.d("Octree","draw "+x+","+y+","+z+" "+(x+i)+","+(y+j)+","+(z+sz));

					int[] offset = offset_array[4];
					for (int k = 0; k < 27; k++) {
						ff[k] = -1;
					}
					for (int k = 0; k < 4; k++) {
						sq_vart[k] = adjust_vart(x + j - 1, y + sz - 1, z + i - 1, ff, offset[k]);
					}

					float[] n = norm(sq_vart[2], sq_vart[1], sq_vart[0]);
					for (int k = 0; k < 6; k++) {
						vart_array[vart_num * 3 + 0] = sq_vart[vn[k]][0];
						vart_array[vart_num * 3 + 1] = sq_vart[vn[k]][1];
						vart_array[vart_num * 3 + 2] = sq_vart[vn[k]][2];
						norm_array[vart_num * 3 + 0] = n[0];
						norm_array[vart_num * 3 + 1] = n[1];
						norm_array[vart_num * 3 + 2] = n[2];
						vart_num++;
					}
				}

				// y-
				if (element.getValue(x + i, y - 1, z + j) <= 0) {

					int[] offset = offset_array[5];
					for (int k = 0; k < 27; k++) {
						ff[k] = -1;
					}
					for (int k = 0; k < 4; k++) {
						sq_vart[k] = adjust_vart(x + i - 1, y - 1, z + j - 1, ff, offset[k]);
					}

					float[] n = norm(sq_vart[2], sq_vart[1], sq_vart[0]);
					for (int k = 0; k < 6; k++) {
						vart_array[vart_num * 3 + 0] = sq_vart[vn[k]][0];
						vart_array[vart_num * 3 + 1] = sq_vart[vn[k]][1];
						vart_array[vart_num * 3 + 2] = sq_vart[vn[k]][2];
						norm_array[vart_num * 3 + 0] = n[0];
						norm_array[vart_num * 3 + 1] = n[1];
						norm_array[vart_num * 3 + 2] = n[2];
						vart_num++;
					}
				}

			}
		}

	}

	public void make_vartex() {
		vart_num = 0;
		vart_map.clear();
		make_vartex(element, 0, 0, 0);
	}

	public void setValue(int x, int y, int z, int v) {
		element.setValue(x, y, z, v);
	}

	public int getValue(int x, int y, int z) {
		return element.getValue(x, y, z);
	}

	public void getPos(int[] pos, float x, float y, float z) {
		pos[0] = (int) (x / element_size) + element.size / 2;
		pos[1] = (int) (y / element_size) + element.size / 2;
		pos[2] = (int) (z / element_size) + element.size / 2;
	}

	public void scrapeSphere(int[] pos, int r) {
		int xx, yy, zz;
		int x = pos[0], y = pos[1], z = pos[2];
		for (zz = 0; zz < r; zz++) {
			for (yy = 0; yy < r; yy++) {
				for (xx = 0; xx < r; xx++) {
					if (xx * xx + yy * yy + zz * zz < r * r) {
						element.setValue(x + xx, y + yy, z + zz, 0);
						element.setValue(x - xx, y + yy, z + zz, 0);
						element.setValue(x + xx, y - yy, z + zz, 0);
						element.setValue(x - xx, y - yy, z + zz, 0);
						element.setValue(x + xx, y + yy, z - zz, 0);
						element.setValue(x - xx, y + yy, z - zz, 0);
						element.setValue(x + xx, y - yy, z - zz, 0);
						element.setValue(x - xx, y - yy, z - zz, 0);
					}
				}
			}
		}
	}

	void unserialize(InputStream in) throws IOException {
		element.size = in.read();
		element.unserialize(in);
	}

	public void draw(GL10 gl) {

		//頂点バッファ設定
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vart_buffer);

		//法線配列の指定
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
		gl.glNormalPointer(GL10.GL_FLOAT, 0, norm_buffer);

		//描画
		gl.glPushMatrix();
		gl.glTranslatef(-element_size * element.size / 2, -element_size * element.size / 2, -element_size * element.size / 2);
		gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vart_num);
		gl.glPopMatrix();

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);

	}

}
