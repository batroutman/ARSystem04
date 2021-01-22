package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import types.ImageData;

public class Map {

	protected boolean initialized = false;

	protected List<Keyframe> keyframes = new ArrayList<Keyframe>();
	protected Keyframe currentKeyframe = null;
	protected Initializer initializer = new Initializer(this);

	public Map() {

	}

	public Keyframe registerInitialKeyframe(ImageData imageData) {
		Keyframe keyframe = new Keyframe();
		keyframe.setKeypoints(imageData.getKeypoints());
		keyframe.setDescriptors(imageData.getDescriptors());
		for (int i = 0; i < keyframe.getDescriptors().rows(); i++) {
			keyframe.getMapPoints().add(new MapPoint());
		}
		this.keyframes.add(keyframe);
		this.currentKeyframe = keyframe;
		return keyframe;
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

}
