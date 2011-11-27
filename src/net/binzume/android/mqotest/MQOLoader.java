package net.binzume.android.mqotest;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.util.Log;

public class MQOLoader {

	private static class Tokenizer {
		BufferedReader reader;
		String[] tokens = null;
		int pos = 0;
		public boolean stopLineEnd = false;

		public Tokenizer(InputStream is) {
			reader = new BufferedReader(new InputStreamReader(is));
		}

		public String getToken() {
			try {
				while (tokens == null || pos >= tokens.length && !stopLineEnd) {
					String line = reader.readLine();
					if (line == null) {
						return null;
					}
					tokens = line.trim().split("[\\s\\(\\)]+");
					pos = 0;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			if (tokens == null || pos >= tokens.length) {
				return null;
			}
			return tokens[pos++];
		}

		public String checkToken() {
			String t = getToken();
			if (t != null)
				pos--;
			return t;
		}

		public int getTokenInt(int defvalue) {
			String t = getToken();
			if (t == null) {
				return defvalue;
			}
			return Integer.parseInt(t);
		}

		public String getTokenString() {
			String t = getToken();
			if (t == null) {
				return null;
			}

			if (t.length() >= 2 && t.startsWith("\"")) {
				t = t.substring(1, t.length() - 1);
			}

			return t;
		}

	}

	public static class Material {
		public String name;
		public float r, g, b, a;
		public String tex;
		public int texId;

		public void prepare(GL10 gl) {

			if (tex == null) {
				return;
			}

			Bitmap bm = BitmapFactory.decodeFile("sdcard/mqo/" + tex);
			if (bm == null) {
				return;
			}

			// テクスチャ生成
			int[] texIds = new int[1];
			gl.glGenTextures(1, texIds, 0);
			texId = texIds[0];

			// Bitmapからテクスチャを作る
			gl.glBindTexture(GL10.GL_TEXTURE_2D, texId);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bm, 0);

			//テクスチャパラメータ
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

			Log.d("Texture", tex + " id:" + texId);
		}
	}

	public static class MaterialMesh extends GLObject {
		public int materialId;

		public float[] vertexArray;
		public float[] uvArray;
		public int vertexNum;

		public short[] indicesArray;
		public int indices;
		public int uvs;

		public MaterialMesh(int mat) {
			materialId = mat;
			indicesArray = new short[16 * 3];
			vertexArray = new float[16 * 3];
		}

		public void addTriangle(short[] idxs, int offset) {
			if (indicesArray.length <= indices + 3) {
				short[] indicesArray2 = new short[indicesArray.length * 3 / 2]; // size*1.5
				System.arraycopy(indicesArray, 0, indicesArray2, 0, indices);
				indicesArray = indicesArray2;
			}

			indicesArray[indices++] = idxs[0 + offset];
			indicesArray[indices++] = idxs[1 + offset];
			indicesArray[indices++] = idxs[2 + offset];
		}

		public void addTriangleV(float[] verts, int offset) {
			if (vertexArray.length <= vertexNum * 3 + 9) {
				float[] vertexArray2 = new float[vertexArray.length * 3 / 2 + 16]; // size*1.5
				System.arraycopy(vertexArray, 0, vertexArray2, 0, vertexNum * 3);
				vertexArray = vertexArray2;
			}

			System.arraycopy(verts, offset, vertexArray, vertexNum * 3, 3 * 3);
			vertexNum += 3;

		}

		public void makeBuffer() {
			indicesBuffer = ShortBuffer.wrap(indicesArray);
			vartBuffer = FloatBuffer.wrap(vertexArray);
			polyNum = indices / 3;
			vartBuffer = FloatBuffer.wrap(vertexArray);
			polyNum = vertexNum / 3;
			indicesBuffer.position(0);
			if (uvArray != null) {
				uvBuffer = FloatBuffer.wrap(uvArray);
				uvBuffer.position(0);
			}

		}

		public void addUv(float[] uv, int vertnum, int offset) {

			if (uvArray == null) {
				uvArray = new float[indicesArray.length * 2 * 3 / 2];
			}

			if (uvArray.length <= indicesArray.length * 2) {
				float[] uvArray2 = new float[indicesArray.length * 2 * 3 / 2]; // size*1.5
				System.arraycopy(uvArray, 0, uvArray2, 0, uvs * 2);
				uvArray = uvArray2;
			}

			System.arraycopy(uv, offset, uvArray, uvs * 2, vertnum * 2);
			uvs += vertnum;

		}

	}

	public static class MQOObject implements GLDrawable.MeshObject {

		LinkedList<GLDrawable> mObjects = new LinkedList<GLDrawable>();
		List<MaterialMesh> meshlist = new ArrayList<MaterialMesh>();
		List<Material> materials;

		boolean prepared = false;

		public void draw(GL10 gl) {
			if (!prepared) {
				for (Material material : materials) {
					material.prepare(gl);
				}
				for (MaterialMesh mesh : meshlist) {
					if (mesh.materialId >= 0 && materials.get(mesh.materialId) != null)
						mesh.texId = materials.get(mesh.materialId).texId;
				}
				prepared = true;
			}

			for (GLDrawable object : meshlist) {
				//Log.d("", " v:" + object.vartNum + " p:" + object.polyNum);
				object.draw(gl);
			}
		}

		public void makeNormal() {
			for (GLDrawable object : meshlist) {
				((GLDrawable.MeshObject) object).makeNormal();
			}

		}

		public void add(GLObject object) {
			mObjects.add(object);
		}

	}

	private final static int MODE_NONE = 0;
	private final static int MODE_SCENE = 1;
	private final static int MODE_OBJECT = 3;

	public static GLDrawable load(InputStream is) {
		Tokenizer tokenizer = new Tokenizer(is);

		MQOObject mqo = new MQOObject();
		GLObject object = null;
		String s;
		int depth = 0;
		int mode = MODE_NONE;
		while ((s = tokenizer.getToken()) != null) {
			//Log.d("mqo test", "token: " + s);

			if ("Object".equals(s)) {
				object = new GLObject();
				tokenizer.getToken();
				mqo.add(object);
				mode = MODE_OBJECT;
			} else if ("Material".equals(s)) {
				mqo.materials = readMaterial(tokenizer);
			} else if ("Eof".equals(s)) {
				mode = MODE_NONE;
				object = null;
				break;
			} else if ("vertex".equals(s)) {
				readVertex(object, tokenizer);
			} else if ("face".equals(s)) {
				List<MaterialMesh> meshlist = readFace(object, tokenizer);
				for (MaterialMesh mesh : meshlist) {
					mesh.makeBuffer();
					if (mesh.materialId >= 0) {
						Material mat = mqo.materials.get(mesh.materialId);
						mesh.color(mat.r, mat.g, mat.b);
						if (mat.tex == null) {
							//mesh.makeNormal();
						}
					} else {
						mesh.color(object.mColor[0], object.mColor[1], object.mColor[2]);
						//mesh.makeNormal();
					}

					mqo.meshlist.add(mesh);
				}

				//opt = tokenizer.getToken();
				//mode = MODE_FACE;
			}

			if (object != null && "color".equals(s)) {
				float r = Float.parseFloat(tokenizer.getToken());
				float g = Float.parseFloat(tokenizer.getToken());
				float b = Float.parseFloat(tokenizer.getToken());
				object.color(r, g, b);
			}

			if ("}".equals(s)) {
				depth--;
				continue;
			}
			if ("{".equals(s)) {
				depth++;
			}

		}

		return mqo;
	}

	private static void readVertex(GLObject object, Tokenizer tokenizer) {
		int n = tokenizer.getTokenInt(-1);
		tokenizer.getToken(); // {

		ByteBuffer vb = ByteBuffer.allocateDirect(4 * n * 3);
		vb.order(ByteOrder.nativeOrder());
		FloatBuffer vartBuffer = vb.asFloatBuffer();
		String ns;
		for (int i = 0; i < n * 3; i++) {
			if ((ns = tokenizer.getToken()) == null)
				return;
			vartBuffer.put(Float.parseFloat(ns));
		}
		vartBuffer.position(0);
		object.vartBuffer = vartBuffer;
		object.vartNum = n;

		ns = tokenizer.getToken(); // }
		Log.d("MQOLoader", "vart ok :" + n + " " + ns);

	}

	private static List<Material> readMaterial(Tokenizer tokenizer) {
		int matnum = tokenizer.getTokenInt(-1);
		tokenizer.getToken(); // {

		List<Material> list = new ArrayList<Material>();
		for (int i = 0; i < matnum; i++) {
			Material mat = new Material();
			mat.name = tokenizer.getTokenString();
			Log.d("MQOLoader", "material :" + mat.name);

			tokenizer.stopLineEnd = true;
			for (;;) {
				String s = tokenizer.getToken();
				if (s == null) {
					break;
				}
				if ("tex".equals(s)) {
					mat.tex = tokenizer.getTokenString();
				}
				if ("col".equals(s)) {
					mat.r = Float.parseFloat(tokenizer.getToken());
					mat.g = Float.parseFloat(tokenizer.getToken());
					mat.b = Float.parseFloat(tokenizer.getToken());
					mat.a = Float.parseFloat(tokenizer.getToken());
				}
			}
			tokenizer.stopLineEnd = false;

			list.add(mat);
		}

		String dummy = tokenizer.getToken(); // }
		Log.d("MQOLoader", "mat ok :" + matnum + " " + dummy);

		return list;
	}

	private static List<MaterialMesh> readFace(GLObject object, Tokenizer tokenizer) {

		int faces = tokenizer.getTokenInt(-1);

		tokenizer.getToken(); // {

		List<MaterialMesh> list = new ArrayList<MaterialMesh>();
		MaterialMesh meshArray[] = new MaterialMesh[100];
		MaterialMesh defmesh = null;

		short index[] = new short[5];
		float uv[] = new float[20];
		float verts[] = new float[15];
		String ns;
		for (int i = 0; i < faces; i++) {
			if ((ns = tokenizer.getToken()) == null)
				return null;

			int n = Integer.parseInt(ns);

			// read index
			tokenizer.getToken(); // V
			//Log.d("mqo test", "v: " + ns);
			for (int j = 0; j < n; j++) {
				int idx = tokenizer.getTokenInt(0);
				//Log.d("mqo test", "idx: " + ns);
				index[j] = (short) idx;

				verts[j * 3 + 0] = object.vartBuffer.get(idx * 3 + 0);
				verts[j * 3 + 1] = object.vartBuffer.get(idx * 3 + 1);
				verts[j * 3 + 2] = object.vartBuffer.get(idx * 3 + 2);
			}

			// select material
			ns = tokenizer.checkToken();
			MaterialMesh mesh = defmesh;
			if ("M".equals(ns)) {
				tokenizer.getToken();
				int mat = tokenizer.getTokenInt(0);
				mesh = meshArray[mat];
				if (mesh == null) {
					mesh = new MaterialMesh(mat);
					meshArray[mat] = mesh;
					list.add(mesh);

					mesh.vartBuffer = object.vartBuffer;
					mesh.vartNum = object.vartNum;
				}
			}
			if (mesh == null) {
				defmesh = new MaterialMesh(-1);
				list.add(defmesh);
				mesh = defmesh;
				mesh.vartBuffer = object.vartBuffer;
				mesh.vartNum = object.vartNum;
			}

			if (n >= 3) {
				//mesh.addTriangle(index, 0);
				mesh.addTriangle(new short[] { (short) mesh.indices, (short) (mesh.indices + 1), (short) (mesh.indices + 2) }, 0);
				mesh.addTriangleV(verts, 0);
				if (n == 4) { // square
					mesh.addTriangle(new short[] { (short) mesh.indices, (short) (mesh.indices + 1), (short) (mesh.indices + 2) }, 0);
					index[4] = index[0];
					verts[4 * 3 + 0] = verts[0];
					verts[4 * 3 + 1] = verts[1];
					verts[4 * 3 + 2] = verts[2];
					mesh.addTriangleV(verts, 6);

					//mesh.addTriangle(index, 2);
				}
			}

			ns = tokenizer.checkToken();
			if ("UV".equals(ns)) {
				tokenizer.getToken();
				for (int j = 0; j < n * 2; j++) {
					uv[j] = Float.parseFloat(tokenizer.getToken());
				}

				if (n >= 3) {
					mesh.addUv(uv, 3, 0);
					if (n == 4) {
						uv[8] = uv[0];
						uv[9] = uv[1];
						mesh.addUv(uv, 3, 4); // todo:offset
					}
				}
			}

			//Log.d("MQOLoader", "face " + mesh.materialId + "  v:" + mesh.indices + " uv:" + mesh.uvs);
		}

		ns = tokenizer.getToken(); // }
		Log.d("MQOLoader", "face ok :" + faces + " " + ns);

		return list;
	}

	public static GLDrawable load(String path) {
		try {
			return load(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

}
