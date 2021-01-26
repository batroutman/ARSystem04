package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import types.Point3D;

public class MapPoint {

	protected Point3D point = null;
	protected List<Observation> observations = new ArrayList<Observation>();

	public MapPoint() {

	}

	public Point3D getPoint() {
		return point;
	}

	public void setPoint(Point3D point) {
		this.point = point;
	}

	public List<Observation> getObservations() {
		return observations;
	}

	public void setObservations(List<Observation> observations) {
		this.observations = observations;
	}

}
