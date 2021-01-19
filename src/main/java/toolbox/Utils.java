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
		matrix4f.m01((float) matrix.get(1, 0));
		matrix4f.m02((float) matrix.get(2, 0));
		matrix4f.m03((float) matrix.get(3, 0));
		matrix4f.m10((float) matrix.get(0, 1));
		matrix4f.m11((float) matrix.get(1, 1));
		matrix4f.m12((float) matrix.get(2, 1));
		matrix4f.m13((float) matrix.get(3, 1));
		matrix4f.m20((float) matrix.get(0, 2));
		matrix4f.m21((float) matrix.get(1, 2));
		matrix4f.m22((float) matrix.get(2, 2));
		matrix4f.m23((float) matrix.get(3, 2));
		matrix4f.m30((float) matrix.get(0, 3));
		matrix4f.m31((float) matrix.get(1, 3));
		matrix4f.m32((float) matrix.get(2, 3));
		matrix4f.m33((float) matrix.get(3, 3));
		return matrix4f;
	}

	public static void printMatrix(Matrix4f mat) {
		pl("");
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				p(mat.getRowColumn(i, j) + "\t");
			}
			pl("");
		}
		pl("");
	}

	// quaternion multiplication, assuming column vector of format [qw, qx, qy,
	// qz].transpose() (q*r)
	public static Matrix quatMult(Matrix q, Matrix r) {

		Matrix t = new Matrix(4, 1);

		double q0 = q.get(0, 0);
		double q1 = q.get(1, 0);
		double q2 = q.get(2, 0);
		double q3 = q.get(3, 0);
		double r0 = r.get(0, 0);
		double r1 = r.get(1, 0);
		double r2 = r.get(2, 0);
		double r3 = r.get(3, 0);

		t.set(0, 0, r0 * q0 - r1 * q1 - r2 * q2 - r3 * q3);
		t.set(1, 0, r0 * q1 + r1 * q0 - r2 * q3 + r3 * q2);
		t.set(2, 0, r0 * q2 + r1 * q3 + r2 * q0 - r3 * q1);
		t.set(3, 0, r0 * q3 - r1 * q2 + r2 * q1 + r3 * q0);

		t = t.times(1 / t.normF());

		return t;
	}

}
