package toolbox;

import org.joml.Matrix4f;

import Jama.Matrix;

public class Utils {

	public static void pl(Object obj) {
		System.out.println(obj);
	}

	public static void p(Object obj) {
		System.out.print(obj);
	}

	public static Matrix4f MatrixToMatrix4f(Matrix matrix) {
		Matrix4f matrix4f = new Matrix4f();
		matrix4f.m00((float) matrix.get(0, 0));
		matrix4f.m01((float) matrix.get(0, 1));
		matrix4f.m02((float) matrix.get(0, 2));
		matrix4f.m03((float) matrix.get(0, 3));
		matrix4f.m10((float) matrix.get(1, 0));
		matrix4f.m11((float) matrix.get(1, 1));
		matrix4f.m12((float) matrix.get(1, 2));
		matrix4f.m13((float) matrix.get(1, 3));
		matrix4f.m20((float) matrix.get(2, 0));
		matrix4f.m21((float) matrix.get(2, 1));
		matrix4f.m22((float) matrix.get(2, 2));
		matrix4f.m23((float) matrix.get(2, 3));
		matrix4f.m30((float) matrix.get(3, 0));
		matrix4f.m31((float) matrix.get(3, 1));
		matrix4f.m32((float) matrix.get(3, 2));
		matrix4f.m33((float) matrix.get(3, 3));
		return matrix4f;
	}

}
