package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import types.ImageData;
import types.Point3D;
import types.Pose;

public class Map {

	protected boolean initialized = false;

	protected List<Keyframe> keyframes = new ArrayList<Keyframe>();
	protected Keyframe currentKeyframe = null;
	protected Initializer initializer = new Initializer(this);

	protected List<Point3D> allPoints = new ArrayList<Point3D>();
	protected List<MapPoint> allMapPoints = new ArrayList<MapPoint>();

	public Map() {

	}

	public Keyframe registerInitialKeyframe(ImageData imageData) {
		synchronized (this) {
			Keyframe keyframe = new Keyframe();
			Pose pose = new Pose();
			pose.setFixed(true);
			keyframe.setPose(pose);
			keyframe.setKeypoints(imageData.getKeypoints());
			keyframe.setDescriptors(imageData.getDescriptors());
			for (int i = 0; i < keyframe.getDescriptors().rows(); i++) {
				MapPoint mp = new MapPoint();
				keyframe.getMapPoints().add(mp);
				this.allMapPoints.add(mp);

			}
			keyframe.registerObservations();
			this.keyframes.add(keyframe);

			this.currentKeyframe = keyframe;

			return keyframe;
		}

	}

	// given a pose, keypoints, descriptors, and a list of the map points that are
	// associated with the corresponding descriptors/keypoints, construct a new
	// keyframe and add it to the map. NOTE: preExistingMapPoints may contain null
	// values
	public Keyframe registerNewKeyframe(Pose pose, MatOfKeyPoint keypoints, Mat descriptors,
			List<MapPoint> preExistingMapPoints) {
		synchronized (this) {
			// fill the map point list
			for (int i = 0; i < preExistingMapPoints.size(); i++) {
				if (preExistingMapPoints.get(i) == null) {
					MapPoint mp = new MapPoint();
					preExistingMapPoints.set(i, mp);
					this.allMapPoints.add(mp);

				}
			}

			Keyframe keyframe = new Keyframe();
			keyframe.setPose(pose);
			keyframe.setKeypoints(keypoints);
			keyframe.setDescriptors(descriptors);
			keyframe.setMapPoints(preExistingMapPoints);
			keyframe.registerObservations();

			this.keyframes.add(keyframe);

			this.currentKeyframe = keyframe;

			return keyframe;
		}

	}

	public List<Pose> getCameras() {
		List<Pose> cameras = new ArrayList<Pose>();
		for (int i = 0; i < this.keyframes.size(); i++) {
			cameras.add(this.keyframes.get(i).getPose());
		}
		return cameras;
	}

	public void registerMapPoint(MapPoint mp) {
		this.allMapPoints.add(mp);
	}

	public void registerPoint(Point3D point) {
		this.allPoints.add(point);
	}

	public List<Keyframe> getKeyframes() {
		return keyframes;
	}

	public void setKeyframes(List<Keyframe> keyframes) {
		this.keyframes = keyframes;
	}

	public Keyframe getCurrentKeyframe() {
		return currentKeyframe;
	}

	public void setCurrentKeyframe(Keyframe currentKeyframe) {
		this.currentKeyframe = currentKeyframe;
	}

	public Initializer getInitializer() {
		return initializer;
	}

	public void setInitializer(Initializer initializer) {
		this.initializer = initializer;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	public List<Point3D> getAllPoints() {
		return allPoints;
	}

	public void setAllPoints(List<Point3D> allPoints) {
		this.allPoints = allPoints;
	}

	public List<MapPoint> getAllMapPoints() {
		return allMapPoints;
	}

	public void setAllMapPoints(List<MapPoint> allMapPoints) {
		this.allMapPoints = allMapPoints;
	}

}
