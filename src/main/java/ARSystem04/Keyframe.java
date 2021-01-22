package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import types.Pose;

public class Keyframe {

	Pose pose = new Pose();
	MatOfKeyPoint keypoints = null;
	Mat descriptors = null;
	List<MapPoint> mapPoints = new ArrayList<MapPoint>();

	public Keyframe() {

	}

	public Pose getPose() {
		return pose;
	}

	public void setPose(Pose pose) {
		this.pose = pose;
	}

	public MatOfKeyPoint getKeypoints() {
		return keypoints;
	}

	public void setKeypoints(MatOfKeyPoint keypoints) {
		this.keypoints = keypoints;
	}

	public Mat getDescriptors() {
		return descriptors;
	}

	public void setDescriptors(Mat descriptors) {
		this.descriptors = descriptors;
	}

	public List<MapPoint> getMapPoints() {
		return mapPoints;
	}

	public void setMapPoints(List<MapPoint> mapPoints) {
		this.mapPoints = mapPoints;
	}

}
