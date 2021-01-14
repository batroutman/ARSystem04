package entities;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import Jama.Matrix;
import toolbox.Utils;

public class Camera {

	private Vector3f position = new Vector3f(0, 0, 0);
	private float pitch;
	private float yaw;
	private float roll;

	// extrinsic format
	float r00 = 1;
	float r01 = 0;
	float r02 = 0;
	float tx = 0;
	float r10 = 0;
	float r11 = 1;
	float r12 = 0;
	float ty = 0;
	float r20 = 0;
	float r21 = 0;
	float r22 = 1;
	float tz = 0;

	public Camera() {
	}

	public Vector3f getPosition() {
		return position;
	}

	public float getPitch() {
		return pitch;
	}

	public float getYaw() {
		return yaw;
	}

	public float getRoll() {
		return roll;
	}

	public void setMatrix(double r00, double r01, double r02, double r10, double r11, double r12, double r20,
			double r21, double r22, double tx, double ty, double tz) {
		this.r00 = (float) r00;
		this.r01 = (float) r01;
		this.r02 = (float) r02;
		this.r10 = (float) r10;
		this.r11 = (float) r11;
		this.r12 = (float) r12;
		this.r20 = (float) r20;
		this.r21 = (float) r21;
		this.r22 = (float) r22;
		this.tx = (float) tx;
		this.ty = (float) ty;
		this.tz = (float) tz;

	}

	public Matrix4f getViewMatrix() {

		Matrix Rx = Matrix.identity(4, 4);

		Rx.set(1, 1, Math.cos(Math.PI));
		Rx.set(2, 2, Math.cos(Math.PI));
		Rx.set(1, 2, -Math.sin(Math.PI));
		Rx.set(2, 1, Math.sin(Math.PI));

		Matrix Ry = Matrix.identity(4, 4);

		Ry.set(0, 0, Math.cos(Math.PI));
		Ry.set(2, 2, Math.cos(Math.PI));
		Ry.set(2, 0, -Math.sin(Math.PI));
		Ry.set(0, 2, Math.sin(Math.PI));

		Matrix mat = Matrix.identity(4, 4);
		mat.set(0, 0, r00);
		mat.set(0, 1, r01);
		mat.set(0, 2, r02);
		mat.set(1, 0, r10);
		mat.set(1, 1, r11);
		mat.set(1, 2, r12);
		mat.set(2, 0, r20);
		mat.set(2, 1, r21);
		mat.set(2, 2, r22);

		mat.set(0, 3, tx);
		mat.set(1, 3, ty);
		mat.set(2, 3, tz);

//		Matrix4f viewMatrix = Utils.MatrixToMatrix4f(mat);
		Matrix4f viewMatrix = Utils.MatrixToMatrix4f(Rx.times(mat));
//		Matrix4f viewMatrix = Utils.MatrixToMatrix4f(mat.times(Rx));
//		Utils.printMatrix(viewMatrix);
//		viewMatrix.transpose(viewMatrix);

		return viewMatrix;
	}

}
