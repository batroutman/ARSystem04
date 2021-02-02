package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.DescriptorMatcher;

import types.Correspondence2D2D;

public class ORBMatcher {

	static DescriptorMatcher matcher = BFMatcher.create(Core.NORM_HAMMING, true);
	static int MATCH_THRESHOLD = 0; // mock
//	static int MATCH_THRESHOLD = 15; // real

	public ORBMatcher() {
		this.init();
	}

	protected void init() {

	}

	public static List<Correspondence2D2D> matchDescriptors(List<KeyPoint> referenceKeypoints, Mat referenceDescriptors,
			List<KeyPoint> currentKeypoints, Mat currentDescriptors) {

		List<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();

		DescriptorMatcher matcher = BFMatcher.create(Core.NORM_HAMMING, true);
		MatOfDMatch matches = new MatOfDMatch();

		// tries to find a match for each query (currentDescriptor) against the already
		// existing train (referenceDescriptor) set
		matcher.match(currentDescriptors, referenceDescriptors, matches);

//		Utils.pl("referenceDescriptors.rows(): " + referenceDescriptors.rows());
//		Utils.pl("currentDescriptors.rows(): " + currentDescriptors.rows());
//		Utils.pl("matches.rows(): " + matches.rows());

		List<DMatch> listMatches = matches.toList();
		for (int i = 0; i < listMatches.size(); i++) {
			DMatch dmatch = listMatches.get(i);
//			Utils.pl("first match ==> distance: " + listMatches.get(i).distance + ", imgIdx: "
//					+ listMatches.get(i).imgIdx + ", queryIdx: " + listMatches.get(i).queryIdx + ", trainIdx: "
//					+ listMatches.get(i).trainIdx);
			if (dmatch.distance < MATCH_THRESHOLD) {
				Correspondence2D2D c = new Correspondence2D2D();
				c.setX0(referenceKeypoints.get(dmatch.trainIdx).pt.x);
				c.setY0(referenceKeypoints.get(dmatch.trainIdx).pt.y);
				c.setX1(currentKeypoints.get(dmatch.queryIdx).pt.x);
				c.setY1(currentKeypoints.get(dmatch.queryIdx).pt.y);
				correspondences.add(c);
			}
		}

		return correspondences;

	}

	public static List<DMatch> matchDescriptors(Keyframe referenceFrame, Mat currentDescriptors) {

		MatOfDMatch matches = new MatOfDMatch();

		// tries to find a match for each query (currentDescriptor) against the already
		// existing train (referenceDescriptor) set
		matcher.match(currentDescriptors, referenceFrame.getDescriptors(), matches);

		// initialize output list of matched map points
		List<MapPoint> matchedMapPoints = new ArrayList<MapPoint>();
		for (int i = 0; i < currentDescriptors.rows(); i++) {
			matchedMapPoints.add(null);
		}

		List<DMatch> matchList = matches.toList();
		List<DMatch> filteredMatches = new ArrayList<DMatch>();
		for (int i = 0; i < matchList.size(); i++) {
			if (matchList.get(i).distance <= MATCH_THRESHOLD) {
				filteredMatches.add(matchList.get(i));
			}
		}

		return filteredMatches;

	}

}
