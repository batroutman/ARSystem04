package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Quaternion_F64;
import toolbox.Utils;
import types.Point3D;
import types.Pose;

public class MapOptimizer {

	protected Map map = null;

	public MapOptimizer() {

	}

	public MapOptimizer(Map map) {
		this.map = map;
	}

	public void mainloop() {
		Utils.pl("Starting map optimization loop...");
	}

	public void fullBundleAdjustment(int iterations) {
		List<Pose> cameras = new ArrayList<Pose>();
		List<Point3D> point3Ds = new ArrayList<Point3D>();
		List<List<Observation>> obsv = new ArrayList<List<Observation>>();

		// lock the map and load data
		synchronized (this.map) {
			// load camera list
			for (int i = 0; i < this.map.getKeyframes().size(); i++) {
				cameras.add(this.map.getKeyframes().get(i).getPose());
			}

			// go through map points and add point3Ds and observations
			for (int i = 0; i < this.map.getAllMapPoints().size(); i++) {

				// add point
				MapPoint mp = this.map.getAllMapPoints().get(i);
				Point3D point = mp.getPoint();
				if (point == null) {
					continue;
				}
				point3Ds.add(point);

				// initialize observation list
				List<Observation> observations = new ArrayList<Observation>();
				for (int j = 0; j < cameras.size(); j++) {
					observations.add(null);
				}

				// for each observation on this point, find its corresponding keyframe
				for (int j = 0; j < mp.getObservations().size(); j++) {
					Observation o = mp.getObservations().get(j);
					int index = -1;
					for (int k = 0; k < cameras.size() && index == -1; k++) {
						index = cameras.get(k) == o.getKeyframe().getPose() ? k : -1;
					}
					observations.set(index, o);
				}

				// add observation list to obsv
				obsv.add(observations);

			}

		}

		// perform BA
		SceneStructureMetric scene = BundleAdjustor.bundleAdjust(cameras, point3Ds, obsv, iterations);

		// lock the map and update the points
		synchronized (this.map) {

			// load points from scene back into input
			for (int i = 0; i < scene.getPoints().size(); i++) {
				point3Ds.get(i).setX(scene.getPoints().get(i).getX());
				point3Ds.get(i).setY(scene.getPoints().get(i).getY());
				point3Ds.get(i).setZ(scene.getPoints().get(i).getZ());
			}

			// load poses from scene back into input
			for (int viewID = 0; viewID < cameras.size(); viewID++) {
				Se3_F64 worldToView = scene.getViews().get(viewID).worldToView;
				Quaternion_F64 q = ConvertRotation3D_F64.matrixToQuaternion(worldToView.getR(), null);
				q.normalize();
				Vector3D_F64 t = worldToView.getTranslation();
				cameras.get(viewID).setQw(q.w);
				cameras.get(viewID).setQx(q.x);
				cameras.get(viewID).setQy(q.y);
				cameras.get(viewID).setQz(q.z);
				cameras.get(viewID).setT(t.x, t.y, t.z);
			}
		}
	}

	public void pairBundleAdjustment(Pose pose) {
		List<Pose> cameras = new ArrayList<Pose>();
		List<Point3D> point3Ds = new ArrayList<Point3D>();
		List<List<Observation>> obsv = new ArrayList<List<Observation>>();

		// lock the map and load data
		synchronized (this.map) {
			// load camera list
			for (int i = 0; i < this.map.getKeyframes().size(); i++) {
				cameras.add(this.map.getKeyframes().get(i).getPose());
			}

			// go through map points and add point3Ds and observations
			for (int i = 0; i < this.map.getAllMapPoints().size(); i++) {

				// add point
				MapPoint mp = this.map.getAllMapPoints().get(i);
				Point3D point = mp.getPoint();
				if (point == null) {
					continue;
				}
				point3Ds.add(point);

				// initialize observation list
				List<Observation> observations = new ArrayList<Observation>();
				for (int j = 0; j < cameras.size(); j++) {
					observations.add(null);
				}

				// for each observation on this point, find its corresponding keyframe
				for (int j = 0; j < mp.getObservations().size(); j++) {
					Observation o = mp.getObservations().get(j);
					int index = -1;
					for (int k = 0; k < cameras.size() && index == -1; k++) {
						index = cameras.get(k) == o.getKeyframe().getPose() ? k : -1;
					}
					observations.set(index, o);
				}

				// add observation list to obsv
				obsv.add(observations);

			}

		}

		// perform BA
		SceneStructureMetric scene = BundleAdjustor.bundleAdjust(cameras, point3Ds, obsv, 100);

		// lock the map and update the points
		synchronized (this.map) {

			// load points from scene back into input
			for (int i = 0; i < scene.getPoints().size(); i++) {
				point3Ds.get(i).setX(scene.getPoints().get(i).getX());
				point3Ds.get(i).setY(scene.getPoints().get(i).getY());
				point3Ds.get(i).setZ(scene.getPoints().get(i).getZ());
			}

			// load poses from scene back into input
			for (int viewID = 0; viewID < cameras.size(); viewID++) {
				Se3_F64 worldToView = scene.getViews().get(viewID).worldToView;
				Quaternion_F64 q = ConvertRotation3D_F64.matrixToQuaternion(worldToView.getR(), null);
				q.normalize();
				Vector3D_F64 t = worldToView.getTranslation();
				cameras.get(viewID).setQw(q.w);
				cameras.get(viewID).setQx(q.x);
				cameras.get(viewID).setQy(q.y);
				cameras.get(viewID).setQz(q.z);
				cameras.get(viewID).setT(t.x, t.y, t.z);
			}
		}
	}

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}

}
