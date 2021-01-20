package ARSystem04;

import java.util.ArrayList;
import java.util.Random;

import org.joml.Vector3f;
import org.opencv.core.Mat;

import Jama.Matrix;
import runtimevars.CameraIntrinsics;
import runtimevars.Parameters;

public class MockPointData {

	protected int HEIGHT = Parameters.height;
	protected int WIDTH = Parameters.width;
	protected long MAX_FRAMES = 400;
	protected int NUM_POINTS = 500;
	protected int START_FRAME = 0;
	protected int SEED = 1;
	protected Matrix K = new Matrix(3, 3);

	// Starting pose parameters
	// NOTE: translations should be negative (-C)
	protected Vector3f initialTranslation = new Vector3f(0f, 0f, -2f);
	protected double initialRotX = 0.0;
	protected double initialRotY = 0.0;
	protected double initialRotZ = 0.0;

	// Amount to update R and t by each frame
	// NOTE: translations should be negative (-C)
	// protected Vector3f translationVelocity = new Vector3f(0.00f, 0.002f,
	// -0.2f);
//	protected Vector3f translationVelocity = new Vector3f(0.7f, 0.0f, 0.0f);
//	protected double rotX = 0.000;
//	protected double rotY = -0.02;
//	protected double rotZ = -0.000;
	protected Vector3f translationVelocity = new Vector3f(-0.02f, 0.00f, 0.00f);
	protected double rotX = 0.000;
	protected double rotY = 0.005;
	protected double rotZ = 0.000;

	// List of homogeneous column vectors (4x1) corresponding to world
	// coordinates
	protected ArrayList<Matrix> worldCoordinates = new ArrayList<Matrix>();

	// fake descriptors for the world points
	protected Mat descriptors = null;

	public MockPointData() {
		this.init();
	}

	protected void init() {
		this.initK();
		this.initWorldCoordinates();
	}

	protected void initK() {
		K.set(0, 0, CameraIntrinsics.fx);
		K.set(0, 1, CameraIntrinsics.s);
		K.set(0, 2, CameraIntrinsics.cx);
		K.set(1, 0, 0.0);
		K.set(1, 1, CameraIntrinsics.fy);
		K.set(1, 2, CameraIntrinsics.cy);
		K.set(2, 0, 0.0);
		K.set(2, 1, 0.0);
		K.set(2, 2, 1.0);
	}

	protected void initWorldCoordinates() {
		String output = "";
//		double Z_SPAWN_MIN = -100;
//		double Z_SPAWN_MAX = 100;
//		double Y_SPAWN_MIN = -20;
//		double Y_SPAWN_MAX = 20;
//		double X_SPAWN_MIN = -30;
//		double X_SPAWN_MAX = 30;

		double Z_SPAWN_MIN = 1;
		double Z_SPAWN_MAX = 2;
		double Y_SPAWN_MIN = -1;
		double Y_SPAWN_MAX = 1;
		double X_SPAWN_MIN = -1;
		double X_SPAWN_MAX = 1;

		double Z_RANGE = Z_SPAWN_MAX - Z_SPAWN_MIN;
		double Y_RANGE = Y_SPAWN_MAX - Y_SPAWN_MIN;
		double X_RANGE = X_SPAWN_MAX - X_SPAWN_MIN;

		Random random = new Random(this.SEED);
		for (int i = 0; i < this.NUM_POINTS; i++) {
			Matrix point = new Matrix(4, 1);
			point.set(0, 0, random.nextDouble() * X_RANGE + X_SPAWN_MIN);
			point.set(1, 0, random.nextDouble() * Y_RANGE + Y_SPAWN_MIN);
			point.set(2, 0, random.nextDouble() * Z_RANGE + Z_SPAWN_MIN);
			point.set(3, 0, 1);
			this.worldCoordinates.add(point);
			output += point.get(0, 0) + ", " + point.get(1, 0) + ", " + point.get(2, 0) + "\n";
		}
		// System.out.println("true world coords:");
		// System.out.println(output);

	}

	// Returns homogeneous 4x4 matrix of JUST rotation parameters
	public Matrix getR(long frameNumber) {

		// Calculate this frame's rotation parameters
		float gamma = (float) -(this.initialRotX + this.rotX * (frameNumber + this.START_FRAME));
		float beta = (float) -(this.initialRotY + this.rotY * (frameNumber + this.START_FRAME));
		float alpha = (float) -(this.initialRotZ + this.rotZ * (frameNumber + this.START_FRAME));

		Matrix Rx = Matrix.identity(4, 4);
		Rx.set(1, 1, Math.cos(gamma));
		Rx.set(2, 2, Math.cos(gamma));
		Rx.set(1, 2, -Math.sin(gamma));
		Rx.set(2, 1, Math.sin(gamma));

		Matrix Ry = Matrix.identity(4, 4);
		Ry.set(0, 0, Math.cos(beta));
		Ry.set(2, 2, Math.cos(beta));
		Ry.set(2, 0, -Math.sin(beta));
		Ry.set(0, 2, Math.sin(beta));

		Matrix Rz = Matrix.identity(4, 4);
		Rz.set(0, 0, Math.cos(alpha));
		Rz.set(1, 1, Math.cos(alpha));
		Rz.set(0, 1, -Math.sin(alpha));
		Rz.set(1, 0, Math.sin(alpha));

		return Rz.times(Ry).times(Rx);
	}

	// Returns homogeneous 4x4 matrix of WORLD translation parameters (this uses
	// negative C)
	public Matrix getIC(long frameNumber) {
		// Calculate C
		Matrix C = Matrix.identity(4, 4);

		C.set(0, 3, -(this.initialTranslation.x + this.translationVelocity.x * (frameNumber + this.START_FRAME)));
		C.set(1, 3, -(this.initialTranslation.y + this.translationVelocity.y * (frameNumber + this.START_FRAME)));
		C.set(2, 3, -(this.initialTranslation.z + this.translationVelocity.z * (frameNumber + this.START_FRAME)));

		return C;
	}

	public Matrix getQuaternion(long frameNumber) {

		// Calculate this frame's rotation parameters
		float gamma = (float) -(this.initialRotX + this.rotX * (frameNumber + this.START_FRAME));
		float beta = (float) -(this.initialRotY + this.rotY * (frameNumber + this.START_FRAME));
		float alpha = (float) -(this.initialRotZ + this.rotZ * (frameNumber + this.START_FRAME));

		double cx = Math.cos(gamma * 0.5);
		double cy = Math.cos(beta * 0.5);
		double cz = Math.cos(alpha * 0.5);
		double sx = Math.sin(gamma * 0.5);
		double sy = Math.sin(beta * 0.5);
		double sz = Math.sin(alpha * 0.5);

		double qw = cx * cy * cz + sx * sy * sz;
		double qx = sx * cy * cz - cx * sy * sz;
		double qy = cx * sy * cz + sx * cy * sz;
		double qz = cx * cy * sz - sx * sy * cz;

		Matrix q = new Matrix(4, 1);
		q.set(0, 0, qw);
		q.set(1, 0, qx);
		q.set(2, 0, qy);
		q.set(3, 0, qz);

		q = q.times(1 / q.normF());

		return q;

	}

	public byte[] getImageBufferRGB(long frameNum) {
		byte[] buffer = new byte[this.WIDTH * this.HEIGHT * 3];

		// for each world coordinate, project onto image and add to buffer (if it should
		// be one the image)
		for (int i = 0; i < this.worldCoordinates.size(); i++) {
			Matrix projCoord = this.K.times(this.getR(frameNum).times(this.getIC(frameNum)).getMatrix(0, 2, 0, 3))
					.times(this.worldCoordinates.get(i));
			if (projCoord.get(2, 0) < 0) {
				continue;
			}
			projCoord = projCoord.times(1 / projCoord.get(2, 0));
			int x = (int) projCoord.get(0, 0);
			int y = (int) projCoord.get(1, 0);
			if (x >= 0 && y >= 0 && x < this.WIDTH && y < this.HEIGHT) {
				buffer[3 * (y * this.WIDTH + x)] = (byte) 255;
				buffer[3 * (y * this.WIDTH + x) + 1] = (byte) 255;
				buffer[3 * (y * this.WIDTH + x) + 2] = (byte) 255;
			}
		}

		return buffer;
	}

	public byte[] getImageBufferGrey(long frameNum) {
		byte[] buffer = new byte[this.WIDTH * this.HEIGHT];

		// for each world coordinate, project onto image and add to buffer (if it should
		// be one the image)
		for (int i = 0; i < this.worldCoordinates.size(); i++) {
			Matrix projCoord = this.K.times(this.getR(frameNum).times(this.getIC(frameNum)).getMatrix(0, 2, 0, 3))
					.times(this.worldCoordinates.get(i));
			if (projCoord.get(2, 0) < 0) {
				continue;
			}
			projCoord = projCoord.times(1 / projCoord.get(2, 0));
			int x = (int) projCoord.get(0, 0);
			int y = (int) projCoord.get(1, 0);
			if (x >= 0 && y >= 0 && x < this.WIDTH && y < this.HEIGHT) {
				buffer[y * this.WIDTH + x] = (byte) 255;
			}
		}

		return buffer;
	}

	public ArrayList<Matrix> getWorldCoordinates() {
		return worldCoordinates;
	}

	public void setWorldCoordinates(ArrayList<Matrix> worldCoordinates) {
		this.worldCoordinates = worldCoordinates;
	}

	public int getHEIGHT() {
		return HEIGHT;
	}

	public void setHEIGHT(int hEIGHT) {
		HEIGHT = hEIGHT;
	}

	public int getWIDTH() {
		return WIDTH;
	}

	public void setWIDTH(int wIDTH) {
		WIDTH = wIDTH;
	}

	public long getMAX_FRAMES() {
		return MAX_FRAMES;
	}

	public void setMAX_FRAMES(long mAX_FRAMES) {
		MAX_FRAMES = mAX_FRAMES;
	}

	public int getNUM_POINTS() {
		return NUM_POINTS;
	}

	public void setNUM_POINTS(int nUM_POINTS) {
		NUM_POINTS = nUM_POINTS;
	}

	public Matrix getK() {
		return K;
	}

	public void setK(Matrix k) {
		K = k;
	}

	public Vector3f getInitialTranslation() {
		return initialTranslation;
	}

	public void setInitialTranslation(Vector3f initialTranslation) {
		this.initialTranslation = initialTranslation;
	}

	public double getInitialRotX() {
		return initialRotX;
	}

	public void setInitialRotX(double initialRotX) {
		this.initialRotX = initialRotX;
	}

	public double getInitialRotY() {
		return initialRotY;
	}

	public void setInitialRotY(double initialRotY) {
		this.initialRotY = initialRotY;
	}

	public double getInitialRotZ() {
		return initialRotZ;
	}

	public void setInitialRotZ(double initialRotZ) {
		this.initialRotZ = initialRotZ;
	}

	public Vector3f getTranslationVelocity() {
		return translationVelocity;
	}

	public void setTranslationVelocity(Vector3f translationVelocity) {
		this.translationVelocity = translationVelocity;
	}

	public double getRotX() {
		return rotX;
	}

	public void setRotX(double rotX) {
		this.rotX = rotX;
	}

	public double getRotY() {
		return rotY;
	}

	public void setRotY(double rotY) {
		this.rotY = rotY;
	}

	public double getRotZ() {
		return rotZ;
	}

	public void setRotZ(double rotZ) {
		this.rotZ = rotZ;
	}

	public Mat getDescriptors() {
		return descriptors;
	}

	public void setDescriptors(Mat descriptors) {
		this.descriptors = descriptors;
	}
}