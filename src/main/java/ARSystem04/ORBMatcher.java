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

import toolbox.Utils;
import types.Correspondence2D2D;

public class ORBMatcher {

	public ORBMatcher() {
		this.init();
	}

	protected void init() {

	}

	public List<Correspondence2D2D> matchDescriptors(List<KeyPoint> referenceKeypoints, List<Mat> referenceDescriptors,
			List<KeyPoint> currentKeypoints, List<Mat> currentDescriptors) {

		List<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();

		int distanceThreshold = 40;
		double loweRatio = 1;
		double searchBoxRadius = 50;

		// generate reference frame table
		// this list will store the indices of referenceDescriptors (which must be the
		// same as referenceKeypoints) based on the Hamming weight of the descriptor
		List<List<Integer>> referenceIndices = new ArrayList<List<Integer>>();
		for (int i = 0; i < 256; i++) {
			referenceIndices.add(new ArrayList<Integer>());
		}
		for (int i = 0; i < referenceDescriptors.size(); i++) {
			referenceIndices.get((int) Core.norm(referenceDescriptors.get(i), Core.NORM_HAMMING)).add(i);
		}

		// evaluate matches for each descriptor in the currentDescriptor set
		for (int i = 0; i < currentDescriptors.size(); i++) {
			int hammingWeight = (int) Core.norm(currentDescriptors.get(i), Core.NORM_HAMMING);
			int min = hammingWeight >= distanceThreshold ? hammingWeight - 10 : 0;
			int max = hammingWeight <= 255 - distanceThreshold ? hammingWeight + 10 : 255;

			int bestIndex = -1;
			int secondBestIndex = -1;
			int bestDist = 256;
			int secondBestDist = 256;

			// sample from reference descriptors with similar Hamming weights and evaluate
			// hamming distances
			for (int j = min; j <= max; j++) {
				for (int k = 0; k < referenceIndices.get(j).size(); k++) {

					int index = referenceIndices.get(j).get(k);

					// check that keypoint is within search box radius
					if (Math.abs(referenceKeypoints.get(index).pt.x - currentKeypoints.get(i).pt.x) > searchBoxRadius
							|| Math.abs(referenceKeypoints.get(index).pt.y
									- currentKeypoints.get(i).pt.y) > searchBoxRadius) {
						continue;
					}

					Mat referenceDescriptor = referenceDescriptors.get(index);
					int dist = (int) Core.norm(referenceDescriptor, currentDescriptors.get(i), Core.NORM_HAMMING);

					if (dist < bestDist) {
						secondBestDist = bestDist;
						bestDist = dist;
						secondBestIndex = bestIndex;
						bestIndex = index;
					} else if (dist < secondBestDist) {
						secondBestDist = dist;
						secondBestIndex = index;
					}

				}
			}

			if (bestDist > distanceThreshold) {
				continue;
			}

			// lowe ratio
			if (bestDist < loweRatio * secondBestDist) {
				// we have a sufficient match. create correspondence.
				Correspondence2D2D c = new Correspondence2D2D();
				c.setX0(referenceKeypoints.get(bestIndex).pt.x);
				c.setY0(referenceKeypoints.get(bestIndex).pt.y);
				c.setX1(currentKeypoints.get(i).pt.x);
				c.setY1(currentKeypoints.get(i).pt.y);
				correspondences.add(c);
			}

		}

		return correspondences;

	}

	public List<Correspondence2D2D> matchDescriptors2(List<KeyPoint> referenceKeypoints, Mat referenceDescriptors,
			List<KeyPoint> currentKeypoints, Mat currentDescriptors) {

		List<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();

		DescriptorMatcher matcher = BFMatcher.create(Core.NORM_HAMMING, true);
		MatOfDMatch matches = new MatOfDMatch();

		// tries to find a match for each query (currentDescriptor) against the already
		// existing train (referenceDescriptor) set
		matcher.match(currentDescriptors, referenceDescriptors, matches);

		Utils.pl("referenceDescriptors.rows(): " + referenceDescriptors.rows());
		Utils.pl("currentDescriptors.rows(): " + currentDescriptors.rows());
		Utils.pl("matches.rows(): " + matches.rows());

		List<DMatch> listMatches = matches.toList();
		for (int i = 0; i < listMatches.size(); i++) {
			DMatch dmatch = listMatches.get(i);
			Utils.pl("first match ==> distance: " + listMatches.get(i).distance + ", imgIdx: "
					+ listMatches.get(i).imgIdx + ", queryIdx: " + listMatches.get(i).queryIdx + ", trainIdx: "
					+ listMatches.get(i).trainIdx);
			if (dmatch.distance < 10) {
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

}
