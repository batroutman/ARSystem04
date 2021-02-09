package ARSystem04;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ejml.data.DMatrixRMaj;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.so.Quaternion_F64;
import runtimevars.CameraIntrinsics;
import toolbox.Utils;
import types.Correspondence2D2D;
import types.Pose;

public class Photogrammetry {

	public static Pose SFMFundamentalMatrixEstimate(List<Correspondence2D2D> correspondences) {

		// create point matrices
		List<Point> points0 = new ArrayList<Point>();
		List<Point> points1 = new ArrayList<Point>();
		for (int i = 0; i < correspondences.size(); i++) {
			Point point0 = new Point(correspondences.get(i).getX0(), correspondences.get(i).getY0());
			Point point1 = new Point(correspondences.get(i).getX1(), correspondences.get(i).getY1());
			points0.add(point0);
			points1.add(point1);
		}

		MatOfPoint2f points0Mat = new MatOfPoint2f();
		MatOfPoint2f points1Mat = new MatOfPoint2f();
		points0Mat.fromList(points0);
		points1Mat.fromList(points1);

		long start = System.currentTimeMillis();
//		Mat fundamentalMatrix = Calib3d.findFundamentalMat(points0Mat, points1Mat, Calib3d.FM_7POINT);
		Mat fundamentalMatrix = Calib3d.findFundamentalMat(points0Mat, points1Mat, Calib3d.FM_RANSAC, 2, 0.99, 500);
		long end = System.currentTimeMillis();
		Utils.pl("Fundamental matrix estimation time: " + (end - start) + "ms");

		// convert to essential matrix
		Mat K = CameraIntrinsics.getKMat();
		Mat Kt = new Mat();
		Core.transpose(K, Kt);
		Mat E = new Mat();
		Core.gemm(Kt, fundamentalMatrix, 1, new Mat(), 0, E, 0);
		Core.gemm(E, K, 1, new Mat(), 0, E, 0);

		// decompose essential matrix
		Mat R1Mat = new Mat();
		Mat R2Mat = new Mat();
		Mat tMat = new Mat();
		Calib3d.decomposeEssentialMat(E, R1Mat, R2Mat, tMat);

		Matrix R1 = Utils.MatToMatrix(R1Mat);
		Matrix R2 = Utils.MatToMatrix(R2Mat);
		Matrix t = Utils.MatToMatrix(tMat);

		// triangulate point and select correct solution (chirality)
		Matrix I = Matrix.identity(4, 4);
		Matrix R1t1 = Matrix.identity(4, 4);
		Matrix R1t2 = Matrix.identity(4, 4);
		Matrix R2t1 = Matrix.identity(4, 4);
		Matrix R2t2 = Matrix.identity(4, 4);
		Matrix[] possiblePoses = { R1t1, R1t2, R2t1, R2t2 };

		R1t1.setMatrix(0, 2, 0, 2, R1);
		R1t1.setMatrix(0, 2, 3, 3, t);
		R1t2.setMatrix(0, 2, 0, 2, R1);
		R1t2.setMatrix(0, 2, 3, 3, t.times(-1));
		R2t1.setMatrix(0, 2, 0, 2, R2);
		R2t1.setMatrix(0, 2, 3, 3, t);
		R2t2.setMatrix(0, 2, 0, 2, R2);
		R2t2.setMatrix(0, 2, 3, 3, t.times(-1));

		Random rand = new Random();
		int[] scores = { 0, 0, 0, 0 };

		for (int i = 0; i < 32 && i < correspondences.size(); i++) {
			int index = (int) (rand.nextDouble() * correspondences.size());
			Correspondence2D2D c = correspondences.get(index);

			// get triangulated 3D points
			Matrix point3DR1t1 = triangulate(R1t1, I, c);
			Matrix point3DR1t2 = triangulate(R1t2, I, c);
			Matrix point3DR2t1 = triangulate(R2t1, I, c);
			Matrix point3DR2t2 = triangulate(R2t2, I, c);

			// reproject points onto cameras
			Matrix point2DR1t1 = R1t1.times(point3DR1t1);
			Matrix point2DR1t2 = R1t2.times(point3DR1t2);
			Matrix point2DR2t1 = R2t1.times(point3DR2t1);
			Matrix point2DR2t2 = R2t2.times(point3DR2t2);

			int numSelected = 0;
			if (point3DR1t1.get(2, 0) > 0 && point2DR1t1.get(2, 0) > 0) {
				scores[0]++;
				numSelected++;
			}
			if (point3DR1t2.get(2, 0) > 0 && point2DR1t2.get(2, 0) > 0) {
				scores[1]++;
				numSelected++;
			}
			if (point3DR2t1.get(2, 0) > 0 && point2DR2t1.get(2, 0) > 0) {
				scores[2]++;
				numSelected++;
			}
			if (point3DR2t2.get(2, 0) > 0 && point2DR2t2.get(2, 0) > 0) {
				scores[3]++;
				numSelected++;
			}
			if (numSelected > 1) {
				Utils.pl(
						"UH OH! More than one pose passed acceptance criteria in fundamental matrix initialization! (Photogrammetry::SFMFundamentalMatrixEstimate()) ==> numSelected: "
								+ numSelected);
			}

		}

		// find highest scoring pose
		int highestInd = 0;
		for (int i = 1; i < scores.length; i++) {
			highestInd = scores[i] > scores[highestInd] ? i : highestInd;
		}

		// convert to quaternion and pose object
		Matrix selection = possiblePoses[highestInd];
		DMatrixRMaj R = new DMatrixRMaj(3, 3);
		R.add(0, 0, selection.get(0, 0));
		R.add(0, 1, selection.get(0, 1));
		R.add(0, 2, selection.get(0, 2));
		R.add(1, 0, selection.get(1, 0));
		R.add(1, 1, selection.get(1, 1));
		R.add(1, 2, selection.get(1, 2));
		R.add(2, 0, selection.get(2, 0));
		R.add(2, 1, selection.get(2, 1));
		R.add(2, 2, selection.get(2, 2));

		Quaternion_F64 q = ConvertRotation3D_F64.matrixToQuaternion(R, null);
		q.normalize();

		Pose pose = new Pose();
		pose.setQw(q.w);
		pose.setQx(q.x);
		pose.setQy(q.y);
		pose.setQz(q.z);

		pose.setT(selection.get(0, 3), selection.get(1, 3), selection.get(2, 3));

		return pose;

	}

	public static Matrix triangulate(Matrix secondaryPose, Matrix primaryPose, Correspondence2D2D c) {

//		Matrix Pprime = E.times(pose);
		Matrix Pprime = CameraIntrinsics.getK4x4().times(secondaryPose);

		Matrix P = CameraIntrinsics.getK4x4().times(primaryPose);

		// compute A matrix for Ax = 0
		Matrix row0 = P.getMatrix(2, 2, 0, 3).times(c.getX0()).minus(P.getMatrix(0, 0, 0, 3));
		Matrix row1 = P.getMatrix(2, 2, 0, 3).times(c.getY0()).minus(P.getMatrix(1, 1, 0, 3));
		Matrix row2 = Pprime.getMatrix(2, 2, 0, 3).times(c.getX1()).minus(Pprime.getMatrix(0, 0, 0, 3));
		Matrix row3 = Pprime.getMatrix(2, 2, 0, 3).times(c.getY1()).minus(Pprime.getMatrix(1, 1, 0, 3));

		Matrix A = new Matrix(4, 4);
		A.setMatrix(0, 0, 0, 3, row0);
		A.setMatrix(1, 1, 0, 3, row1);
		A.setMatrix(2, 2, 0, 3, row2);
		A.setMatrix(3, 3, 0, 3, row3);

		SingularValueDecomposition svd = A.svd();
		Matrix X = svd.getV().getMatrix(0, 3, 3, 3);
		X = X.times(1.0 / X.get(3, 0));
		// System.out.println("X");
		// X.print(5, 4);
		return X;
	}

	public static Matrix OpenCVPnP(List<Point3> point3s, List<Point> points, Mat rvec, Mat tvec,
			boolean useInitialGuess) {

		MatOfPoint3f objectPoints = new MatOfPoint3f();
		objectPoints.fromList(point3s);
		MatOfPoint2f imagePoints = new MatOfPoint2f();
		imagePoints.fromList(points);
		Mat cameraMatrix = CameraIntrinsics.getKMat();

		Mat inliers = new Mat();
		Calib3d.solvePnPRansac(objectPoints, imagePoints, cameraMatrix, new MatOfDouble(), rvec, tvec, useInitialGuess,
				100, 1, 0.99, inliers);

		Utils.pl("Initial num of object points: " + objectPoints.rows());
		Utils.pl("Num inliers: " + inliers.rows());

		// if theres enough inliers, refine the estimate
		if (inliers.rows() > 10 && true) {
			// load inliers
			List<Point3> objectPointInliers = new ArrayList<Point3>();
			List<Point> imagePointInliers = new ArrayList<Point>();
			int[] inlierBuffer = new int[inliers.rows()];
			inliers.get(0, 0, inlierBuffer);
			for (int i = 0; i < inlierBuffer.length; i++) {
				objectPointInliers.add(point3s.get(inlierBuffer[i]));
				imagePointInliers.add(points.get(inlierBuffer[i]));
			}

			objectPoints.fromList(objectPointInliers);
			imagePoints.fromList(imagePointInliers);

			Calib3d.solvePnP(objectPoints, imagePoints, cameraMatrix, new MatOfDouble(), rvec, tvec, true,
					Calib3d.SOLVEPNP_ITERATIVE);
		}

		Mat RMat = new Mat();
		Calib3d.Rodrigues(rvec, RMat);
		double[] RBuffer = new double[9];
		RMat.get(0, 0, RBuffer);
		double[] tBuffer = new double[3];
		tvec.get(0, 0, tBuffer);

		Matrix E = Matrix.identity(4, 4);
		E.set(0, 0, RBuffer[0]);
		E.set(0, 1, RBuffer[1]);
		E.set(0, 2, RBuffer[2]);
		E.set(1, 0, RBuffer[3]);
		E.set(1, 1, RBuffer[4]);
		E.set(1, 2, RBuffer[5]);
		E.set(2, 0, RBuffer[6]);
		E.set(2, 1, RBuffer[7]);
		E.set(2, 2, RBuffer[8]);
		E.set(0, 3, tBuffer[0]);
		E.set(1, 3, tBuffer[1]);
		E.set(2, 3, tBuffer[2]);

		return E;
	}

}
