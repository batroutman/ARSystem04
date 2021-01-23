package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import runtimevars.CameraIntrinsics;
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

		Mat fundamentalMatrix = Calib3d.findFundamentalMat(points0Mat, points1Mat, Calib3d.FM_7POINT);

		// convert to essential matrix
		Mat K = CameraIntrinsics.getKMat();
		Mat Kt = new Mat();
		Core.transpose(K, Kt);
		Mat E = new Mat();
		Core.gemm(Kt, fundamentalMatrix, 1, new Mat(), 0, E, 0);
		Core.gemm(E, K, 1, new Mat(), 0, E, 0);

		// decompose essential matrix
		Mat R1 = new Mat();
		Mat R2 = new Mat();
		Mat t = new Mat();
		Calib3d.decomposeEssentialMat(E, R1, R2, t);

		// triangulate point and select correct solution (chirality)

		return null;

	}

}
