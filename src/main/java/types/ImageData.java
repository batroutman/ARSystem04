package types;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

public class ImageData {

	protected int numOctaves = 4;
	protected List<Mat> octaves = new ArrayList<Mat>(this.numOctaves);
	protected List<MatOfKeyPoint> keypoints = new ArrayList<MatOfKeyPoint>(this.numOctaves);
	protected List<KeyPoint> normalizedKeypoints = new ArrayList<KeyPoint>();

	public ImageData() {
		this.preloadLists();
	}

	public ImageData(int numOctaves) {
		this.numOctaves = numOctaves;
		this.octaves = new ArrayList<Mat>(this.numOctaves);
		this.keypoints = new ArrayList<MatOfKeyPoint>(this.numOctaves);
		this.preloadLists();
	}

	private void preloadLists() {
		for (int i = 0; i < this.numOctaves; i++) {
			this.octaves.add(null);
			this.keypoints.add(null);
		}
	}

	public List<KeyPoint> generateNormalizedKeypoints() {

		this.normalizedKeypoints = new ArrayList<KeyPoint>();

		for (int i = 0; i < this.keypoints.size(); i++) {
			List<KeyPoint> listKeypoints = this.keypoints.get(i).toList();

			// normalize each keypoint in this octave
			for (int j = 0; j < listKeypoints.size(); j++) {
				KeyPoint keypoint = listKeypoints.get(j);
				keypoint.pt.x = keypoint.pt.x * Math.pow(2, i);
				keypoint.pt.y = keypoint.pt.y * Math.pow(2, i);
				keypoint.size = (float) (keypoint.size * Math.pow(2, i));
			}

			// add the octave of keypoints to the full list
			this.normalizedKeypoints.addAll(listKeypoints);

		}

		return this.normalizedKeypoints;

	}

	public int getNumOctaves() {
		return numOctaves;
	}

	public void setNumOctaves(int numOctaves) {
		this.numOctaves = numOctaves;
	}

	public List<Mat> getOctaves() {
		return octaves;
	}

	public void setOctaves(List<Mat> octaves) {
		this.octaves = octaves;
	}

	public List<MatOfKeyPoint> getKeypoints() {
		return keypoints;
	}

	public void setKeypoints(List<MatOfKeyPoint> keypoints) {
		this.keypoints = keypoints;
	}

	public List<KeyPoint> getNormalizedKeypoints() {
		return normalizedKeypoints;
	}

	public void setNormalizedKeypoints(List<KeyPoint> normalizedKeypoints) {
		this.normalizedKeypoints = normalizedKeypoints;
	}

}
