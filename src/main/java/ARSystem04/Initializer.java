package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;

import toolbox.Utils;
import types.Correspondence2D2D;
import types.ImageData;

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
		} else if (frameNum == 44) {
			return this.initialize(imageData);
		}
		return null;
	}

	public List<Correspondence2D2D> initialize(ImageData imageData) {
		Utils.pl("Attempting to initialize...");

		// construct reference keyframe
		Keyframe referenceFrame = this.map.registerInitialKeyframe(this.referenceData);

		// get correspondences against reference keyframe (: List<MapPoint>)
		List<DMatch> matches = ORBMatcher.matchDescriptors(referenceFrame, imageData.getDescriptors());
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

		// triangulate all matched points

		// construct new keyframe

		// register keyframes with map and set current keyframe?

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
