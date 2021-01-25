package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Point3;

import Jama.Matrix;
import toolbox.Utils;
import types.Correspondence2D2D;
import types.Pose;

public class Tracker {

	protected Map map;

	public Tracker() {

	}

	public Tracker(Map map) {
		this.map = map;
	}

	public int trackMovement(MatOfKeyPoint keypoints, Mat descriptors, List<Correspondence2D2D> outCorrespondences,
			List<MapPoint> outCorrespondingMapPoints, List<Correspondence2D2D> outUntriangulatedCorrespondences,
			List<MapPoint> outUntriangulatedMapPoints, Pose outPose) {
		return this.trackMovementFromKeyframe(this.map.getCurrentKeyframe(), keypoints, descriptors, outCorrespondences,
				outCorrespondingMapPoints, outUntriangulatedCorrespondences, outUntriangulatedMapPoints, outPose);
	}

	// does full frame feature matching, generates correspondences, gets PnP pose,
	// and returns the number of matches
	public int trackMovementFromKeyframe(Keyframe keyframe, MatOfKeyPoint keypoints, Mat descriptors,
			List<Correspondence2D2D> outCorrespondences, List<MapPoint> outCorrespondingMapPoints,
			List<Correspondence2D2D> outUntriangulatedCorrespondences, List<MapPoint> outUntriangulatedMapPoints,
			Pose outPose) {

		outCorrespondences.clear();
		outCorrespondingMapPoints.clear();
		outUntriangulatedCorrespondences.clear();
		outUntriangulatedMapPoints.clear();

		List<KeyPoint> keypointList = keypoints.toList();
		List<KeyPoint> keyframeKeypointList = keyframe.getKeypoints().toList();

		for (int i = 0; i < descriptors.rows(); i++) {
			outCorrespondingMapPoints.add(null);
		}

		List<DMatch> matches = ORBMatcher.matchDescriptors(keyframe, descriptors);
		Utils.pl("number of features: " + descriptors.rows());
		Utils.pl("number of matches: " + matches.size());

		// generate correspondences, map point list, and input for PnP
		int numTracked = 0;
		List<Point3> point3s = new ArrayList<Point3>();
		List<Point> points = new ArrayList<Point>();
		for (int i = 0; i < matches.size(); i++) {

			// add correspondence
			Correspondence2D2D c = new Correspondence2D2D();
			c.setX0(keyframeKeypointList.get(matches.get(i).trainIdx).pt.x);
			c.setY0(keyframeKeypointList.get(matches.get(i).trainIdx).pt.y);
			c.setX1(keypointList.get(matches.get(i).queryIdx).pt.x);
			c.setY1(keypointList.get(matches.get(i).queryIdx).pt.y);
			outCorrespondences.add(c);

			// set map point in list
			MapPoint mp = keyframe.getMapPoints().get(matches.get(i).trainIdx);
			outCorrespondingMapPoints.set(matches.get(i).queryIdx, mp);

			// if it is already triangulated, extract point data for PnP
			if (mp.getPoint() == null) {
				outUntriangulatedCorrespondences.add(c);
				outUntriangulatedMapPoints.add(mp);
				continue;
			}

			numTracked++;

			Point3 point3 = new Point3(mp.getPoint().getX(), mp.getPoint().getY(), mp.getPoint().getZ());
			point3s.add(point3);
			Point point = keypointList.get(matches.get(i).queryIdx).pt;
			points.add(point);

		}
		Utils.pl("numTracked = " + numTracked);

		long start = System.currentTimeMillis();
		Matrix E = Photogrammetry.OpenCVPnP(point3s, points, new Mat(), new Mat(), false);
		long end = System.currentTimeMillis();
		Utils.pl("PnP time: " + (end - start) + "ms");
		outPose.setPose(Utils.matrixToPose(E));

		return matches.size();

	}

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}

}
