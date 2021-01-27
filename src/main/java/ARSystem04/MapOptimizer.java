package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;

import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.ScaleSceneStructure;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.cameras.BundlePinhole;
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.FactoryMultiView;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Quaternion_F64;
import runtimevars.CameraIntrinsics;
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

		SceneStructureMetric scene = new SceneStructureMetric(false);
		SceneObservations observations = new SceneObservations();

		// load data into these neat lists
		List<Pose> cameras = new ArrayList<Pose>();
		List<Point3D> point3Ds = new ArrayList<Point3D>();
		List<List<Observation>> obsv = new ArrayList<List<Observation>>();

		// lock the map and load data
		synchronized (this.map) {

			//////////////////////
			// load camera list //
			//////////////////////
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
				List<Observation> observationsList = new ArrayList<Observation>();
				for (int j = 0; j < cameras.size(); j++) {
					observationsList.add(null);
				}

				// for each observation on this point, find its corresponding keyframe
				for (int j = 0; j < mp.getObservations().size(); j++) {
					Observation o = mp.getObservations().get(j);
					int index = -1;
					for (int k = 0; k < cameras.size() && index == -1; k++) {
						index = cameras.get(k) == o.getKeyframe().getPose() ? k : -1;
					}
					observationsList.set(index, o);
				}

				// add observation list to obsv
				obsv.add(observationsList);

			}

			///////////////////////////
			// load lists into scene //
			///////////////////////////
			scene.initialize(cameras.size(), cameras.size(), point3Ds.size());
			observations.initialize(cameras.size());

			// load camera poses into scene
			BundlePinhole camera = new BundlePinhole();
			camera.fx = CameraIntrinsics.fx;
			camera.fy = CameraIntrinsics.fy;
			camera.cx = CameraIntrinsics.cx;
			camera.cy = CameraIntrinsics.cy;
			camera.skew = CameraIntrinsics.s;
			for (int i = 0; i < cameras.size(); i++) {
				Se3_F64 worldToCameraGL = new Se3_F64();
				ConvertRotation3D_F64.quaternionToMatrix(cameras.get(i).getQw(), cameras.get(i).getQx(),
						cameras.get(i).getQy(), cameras.get(i).getQz(), worldToCameraGL.R);
				worldToCameraGL.T.x = cameras.get(i).getTx();
				worldToCameraGL.T.y = cameras.get(i).getTy();
				worldToCameraGL.T.z = cameras.get(i).getTz();
				scene.setCamera(i, true, camera);
				scene.setView(i, cameras.get(i).isFixed(), worldToCameraGL);
				scene.connectViewToCamera(i, i);
			}

			// load projected observations into observations variable
			for (int pointID = 0; pointID < obsv.size(); pointID++) {
				for (int cameraID = 0; cameraID < obsv.get(pointID).size(); cameraID++) {
					if (obsv.get(pointID).get(cameraID) == null) {
						continue;
					}
					float pixelX = (float) obsv.get(pointID).get(cameraID).getPoint().x;
					float pixelY = (float) obsv.get(pointID).get(cameraID).getPoint().y;
					observations.getView(cameraID).add(pointID, pixelX, pixelY);
				}
			}

			// load 3D points into scene
			for (int i = 0; i < point3Ds.size(); i++) {
				float x = (float) point3Ds.get(i).getX();
				float y = (float) point3Ds.get(i).getY();
				float z = (float) point3Ds.get(i).getZ();

				scene.setPoint(i, x, y, z);
			}

		}

		// perform BA
		this.bundleAdjustScene(scene, observations, iterations);

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

	public void bundleAdjustScene(SceneStructureMetric scene, SceneObservations observations, int maxIterations) {

		ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
		configLM.dampeningInitial = 1e-3;
		configLM.hessianScaling = true;

		ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
		configSBA.configOptimizer = configLM;
		BundleAdjustment<SceneStructureMetric> bundleAdjustment = FactoryMultiView.bundleSparseMetric(configSBA);

		// debug
		bundleAdjustment.setVerbose(System.out, 0);

		// Specifies convergence criteria
		bundleAdjustment.configure(1e-12, 1e-12, maxIterations);

		// Scaling each variable type so that it takes on a similar numerical
		// value. This aids in optimization
		// Not important for this problem but is for others
		ScaleSceneStructure bundleScale = new ScaleSceneStructure();
		bundleScale.applyScale(scene, observations);
		bundleAdjustment.setParameters(scene, observations);

		// Runs the solver. This will take a few minutes. 7 iterations takes
		// about 3 minutes on my computer
		long startTime = System.currentTimeMillis();
		double errorBefore = bundleAdjustment.getFitScore();
		Utils.pl("error before: " + errorBefore);
		if (!bundleAdjustment.optimize(scene)) {
			// throw new RuntimeException("Bundle adjustment failed?!?");
			Utils.pl("\n\n\n\n***************************  ERROR  ****************************");
			Utils.pl("NOTE: Bundle Adjustment failed!");
			Utils.pl("fit score: " + bundleAdjustment.getFitScore());
			bundleScale.undoScale(scene, observations);

			Utils.pl("****************************************************************\n\n\n\n");

		}

		// Print out how much it improved the model
		System.out.println();
		System.out.printf("Error reduced by %.1f%%\n", (100.0 * (errorBefore / bundleAdjustment.getFitScore() - 1.0)));
		System.out.println((System.currentTimeMillis() - startTime) / 1000.0);

		// Return parameters to their original scaling. Can probably skip this
		// step.
		bundleScale.undoScale(scene, observations);

	}

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}

}
