package ARSystem04;

import org.opencv.core.Point;

public class Observation {

	protected Keyframe keyframe = null;
	protected Point point = null;

	public Observation() {

	}

	public Observation(Keyframe keyframe, Point point) {
		this.keyframe = keyframe;
		this.point = point;
	}

	public Keyframe getKeyframe() {
		return keyframe;
	}

	public void setKeyframe(Keyframe keyframe) {
		this.keyframe = keyframe;
	}

	public Point getPoint() {
		return point;
	}

	public void setPoint(Point point) {
		this.point = point;
	}

}
