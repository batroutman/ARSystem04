package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;

import Jama.Matrix;
import toolbox.Utils;
import types.Correspondence2D2D;
import types.ImageData;
import types.Point3D;
import types.Pose;

public class Initializer {

	ImageData referenceData = null;
	Map map = null;

	public Initializer() {

	}

	public Initializer(Map map) {
		this.map = map;
	}

	public List<Correspondence2D2D> registerData(long frameNum, ImageData imageData) {
		if (frameNum == 0) {
			this.referenceData = imageData;
		} else if (frameNum == 29) {
			return this.initialize(imageData);
		}
		return ORBMatcher.matchDescriptors(this.referenceData.getKeypoints().toList(),
				this.referenceData.getDescriptors(), imageData.getKeypoints().toList(), imageData.getDescriptors());

	}

	public List<Correspondence2D2D> initialize(ImageData imageData) {
		Utils.pl("Attempting to initialize...");

		// construct reference keyframe
		Keyframe referenceFrame = this.map.registerInitialKeyframe(this.referenceData);

		// get correspondences against reference keyframe (: List<MapPoint>)
		long start = System.currentTimeMillis();
		List<DMatch> matches = ORBMatcher.matchDescriptors(referenceFrame, imageData.getDescriptors());
		long end = System.currentTimeMillis();
		Utils.pl("Matching time: " + (end - start) + "ms");
		List<MapPoint> matchedMapPoints = new ArrayList<MapPoint>();
		for (int i = 0; i < imageData.getDescriptors().rows(); i++) {
			matchedMapPoints.add(null);
		}
		for (int i = 0; i < matches.size(); i++) {
			matchedMapPoints.set(matches.get(i).queryIdx, referenceFrame.getMapPoints().get(matches.get(i).trainIdx));
		}

		// construct correspondence list (List<Correspondence2D2D>)
		List<Correspondence2D2D> correspondences = this.inferCorrespondences(referenceFrame, imageData, matches);

		// get sfm estimate from correspondences
		Pose pose = Photogrammetry.SFMFundamentalMatrixEstimate(correspondences);

		// triangulate all matched points
		Matrix E = pose.getHomogeneousMatrix();
		Matrix I = Matrix.identity(4, 4);
		synchronized (this.map) {
			for (int i = 0; i < correspondences.size(); i++) {
				Matrix point = Photogrammetry.triangulate(E, I, correspondences.get(i));
//				Utils.pl(point.get(0, 0) + ", " + point.get(1, 0) + ", " + point.get(2, 0));
				Point3D point3D = new Point3D(point.get(0, 0), point.get(1, 0), point.get(2, 0));
				MapPoint mapPoint = matchedMapPoints.get(matches.get(i).queryIdx);
				mapPoint.setPoint(point3D);
				this.map.registerMapPoint(mapPoint);
				this.map.registerPoint(point3D);
			}
		}

		// construct new keyframe
		this.map.registerNewKeyframe(pose, imageData.getKeypoints(), imageData.getDescriptors(), matchedMapPoints);

		this.map.setInitialized(true);

		return correspondences;

	}

	public List<Correspondence2D2D> inferCorrespondences(Keyframe referenceFrame, ImageData currentData,
			List<DMatch> currentMatches) {

		List<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();

		List<KeyPoint> referenceKeypoints = referenceFrame.getKeypoints().toList();
		List<KeyPoint> currentKeypoints = currentData.getKeypoints().toList();

		for (DMatch match : currentMatches) {
			Correspondence2D2D c = new Correspondence2D2D();
			c.setX0(referenceKeypoints.get(match.trainIdx).pt.x);
			c.setY0(referenceKeypoints.get(match.trainIdx).pt.y);
			c.setX1(currentKeypoints.get(match.queryIdx).pt.x);
			c.setY1(currentKeypoints.get(match.queryIdx).pt.y);
			correspondences.add(c);
		}

		return correspondences;

	}

	public ImageData getReferenceData() {
		return referenceData;
	}

	public void setReferenceData(ImageData referenceData) {
		this.referenceData = referenceData;
	}

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}

}
