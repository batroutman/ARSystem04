package toolbox;

import org.joml.Matrix4f;
import org.opencv.core.Mat;

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
	// qz].transpose() (q1*q2)
	public static Matrix quatMult(Matrix q1, Matrix q2) {

		Matrix t = new Matrix(4, 1);

		double q1w = q1.get(0, 0);
		double q1x = q1.get(1, 0);
		double q1y = q1.get(2, 0);
		double q1z = q1.get(3, 0);

		double q2w = q2.get(0, 0);
		double q2x = q2.get(1, 0);
		double q2y = q2.get(2, 0);
		double q2z = q2.get(3, 0);

		t.set(1, 0, q1x * q2w + q1y * q2z - q1z * q2y + q1w * q2x);
		t.set(2, 0, -q1x * q2z + q1y * q2w + q1z * q2x + q1w * q2y);
		t.set(3, 0, q1x * q2y - q1y * q2x + q1z * q2w + q1w * q2z);
		t.set(0, 0, -q1x * q2x - q1y * q2y - q1z * q2z + q1w * q2w);

		t = t.times(1 / t.normF());

		return t;
	}

	public static Matrix MatToMatrix(Mat mat) {
		double[] buffer = new double[mat.rows() * mat.cols()];
		mat.get(0, 0, buffer);
		Matrix matrix = new Matrix(mat.rows(), mat.cols());
		for (int i = 0; i < matrix.getRowDimension(); i++) {
			for (int j = 0; j < matrix.getColumnDimension(); j++) {
				matrix.set(i, j, buffer[i * mat.cols() + j]);
			}
		}

		return matrix;
	}

}
