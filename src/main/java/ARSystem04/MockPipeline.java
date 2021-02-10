package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import Jama.Matrix;
import buffers.Buffer;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
import runtimevars.CameraIntrinsics;
import toolbox.Utils;
import types.Correspondence2D2D;
import types.FramePack;
import types.ImageData;
import types.PipelineOutput;
import types.Point3D;
import types.Pose;

public class MockPipeline extends PoseEstimator {

	long targetFrametime = 30;
	int frameNum = 0;
	MockPointData mock = new MockPointData();

	Map map = new Map();
	Tracker tracker = new Tracker(this.map);
	MapOptimizer mapOptimizer = new MapOptimizer(this.map);

	// quaternion orientation
	Matrix orientation = new Matrix(4, 1);

	ArrayList<Pose> keyframes = new ArrayList<Pose>();

	Thread poseEstimationThread = new Thread() {
		@Override
		public void run() {
			try {
				mainloop();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	public MockPipeline() {
		super(new QueuedBuffer<FramePack>(), new SingletonBuffer<PipelineOutput>());
	}

	public MockPipeline(Buffer<FramePack> inputBuffer, Buffer<PipelineOutput> outputBuffer) {
		super(inputBuffer, outputBuffer);
		this.orientation.set(0, 0, 1);
	}

	@Override
	public void start() {
		this.poseEstimationThread.start();
	}

	@Override
	public void stop() {
		this.poseEstimationThread.interrupt();
	}

	public void mainloop() throws Exception {
		boolean keepGoing = true;
		while (keepGoing) {

			long start = System.currentTimeMillis();
			if (this.mock.getMAX_FRAMES() <= this.frameNum) {
				keepGoing = false;
				continue;
			}

			Utils.pl("================  FRAME " + this.frameNum + " ================");

			// processing the image and extracting features
			ImageData processedImage = this.mock.getImageData(this.frameNum);

			List<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();
			List<Correspondence2D2D> prunedCorrespondences = new ArrayList<Correspondence2D2D>();
			Pose pose = new Pose();
			List<MapPoint> mapPointPerDescriptor = new ArrayList<MapPoint>();
			List<MapPoint> prunedCorrespondenceMapPoints = new ArrayList<MapPoint>();
			List<Correspondence2D2D> untriangulatedCorrespondences = new ArrayList<Correspondence2D2D>();
			List<MapPoint> untriangulatedMapPoints = new ArrayList<MapPoint>();

			if (!this.map.isInitialized()) {

				// register keypoints and descriptors with initializer until it can initialize
				// the map
				correspondences = this.map.getInitializer().registerData(this.frameNum, processedImage);

				// if it initialized this frame, get the map's new keyframe as the current pose
				if (this.map.isInitialized()) {
					pose = new Pose(this.map.getCurrentKeyframe().getPose());
					this.mapOptimizer.fullBundleAdjustment(10);
				}

			} else {

				// perform routine PnP pose estimation
				int numMatches = tracker.trackMovement(processedImage.getKeypoints(), processedImage.getDescriptors(),
						correspondences, prunedCorrespondences, prunedCorrespondenceMapPoints, mapPointPerDescriptor,
						untriangulatedCorrespondences, untriangulatedMapPoints, pose);

				// pair-wise BA
				Utils.pl("pair BA");
				this.mapOptimizer.pairBundleAdjustment(pose, this.map.getCurrentKeyframe().getPose(),
						prunedCorrespondenceMapPoints, prunedCorrespondences, 1);

				// if poses are far enough away, triangulate untriangulated points
				Utils.pl("pose.getDistanceFrom(this.map.getCurrentKeyframe().getPose()): "
						+ pose.getDistanceFrom(this.map.getCurrentKeyframe().getPose()));
				if (pose.getDistanceFrom(this.map.getCurrentKeyframe().getPose()) >= 1
						&& untriangulatedCorrespondences.size() > 0) {

					Utils.pl("Triangulating map points: " + untriangulatedMapPoints.size());

					// prune correspondences with epipolar search
					Photogrammetry.epipolarPrune(untriangulatedCorrespondences, untriangulatedMapPoints, pose,
							this.map.getCurrentKeyframe().getPose());

					// triangulate remaining correspondences
					this.triangulateUntrackedMapPoints(pose, untriangulatedCorrespondences, untriangulatedMapPoints);

					// pair-wise BA
					Utils.pl("pair BA (triangulate)");
					this.mapOptimizer.pairBundleAdjustment(pose, this.map.getCurrentKeyframe().getPose(),
							prunedCorrespondenceMapPoints, prunedCorrespondences, 1);

				}

				// if starting to lose tracking, generate new keyframe
				if (numMatches < processedImage.getKeypoints().rows() * 0.75) { // this needs to be tuned
																				// when moving back to
																				// true pipeline
					Utils.pl("--- registering  new keyframe ---");
					this.map.registerNewKeyframe(pose, processedImage.getKeypoints(), processedImage.getDescriptors(),
							mapPointPerDescriptor);
					Utils.pl("full BA");
					this.mapOptimizer.fullBundleAdjustment(10);
				}

			}

			if (correspondences == null) {
				correspondences = new ArrayList<Correspondence2D2D>();
			}

			Utils.pl("estimated: ");
			pose.getHomogeneousMatrix().print(15, 5);
			Utils.pl("ground truth: ");
			this.mock.getR(this.frameNum).times(this.mock.getIC(this.frameNum)).print(15, 5);

			// pipeline output
			PipelineOutput po = new PipelineOutput();
			po.frameNum = this.frameNum++;
			po.pose = pose;

			// image data
			po.rawFrameBuffer = this.mock.getImageBufferRGB(frameNum);
			po.processedFrameBuffer = this.mock.getImageBufferGrey(frameNum);
			po.cameras = this.map.getCameras();
			po.points = this.map.getAllPoints();

			po.features = processedImage.getKeypoints().toList();
			po.numFeatures = po.features.size();
			po.correspondences = correspondences;

			long end = System.currentTimeMillis();
			po.fps = 1000 / (end - start + 1);
			Utils.pl("frametime: " + (end - start) + "ms");
			Utils.pl("framerate: " + po.fps);

			try {
				long sleepTime = (end - start) < this.targetFrametime ? this.targetFrametime - (end - start) : 0;
				Thread.sleep(sleepTime);
			} catch (Exception e) {
			}

			outputBuffer.push(po);

		}
	}

	public void triangulateUntrackedMapPoints(Pose currentPose, List<Correspondence2D2D> untriangulatedCorrespondences,
			List<MapPoint> untriangulatedMapPoints) {

		Utils.pl("*******************************************************************");
		Utils.pl("**********************  TRIANGULATING...   ************************");
		Utils.pl("*******************************************************************");

		// the sum of average reprojection errors over which the triangulation will be
		// discarded
		double AVG_THRESHOLD = 5;

		double INDIVIDUAL_THRESHOLD = 10;

		// get point triangulations
		List<Point3D> newPoints = new ArrayList<Point3D>();
		for (int i = 0; i < untriangulatedCorrespondences.size(); i++) {
			Correspondence2D2D c = untriangulatedCorrespondences.get(i);
			Matrix pointMatrix = Photogrammetry.triangulate(currentPose.getHomogeneousMatrix(),
					this.map.getCurrentKeyframe().getPose().getHomogeneousMatrix(), c);
			Point3D point3D = new Point3D(pointMatrix.get(0, 0), pointMatrix.get(1, 0), pointMatrix.get(2, 0));
			newPoints.add(point3D);
		}

		// evaluate error
		double avgErrCurrent = 0;
		double avgErrKeyframe = 0;
		List<Double> currentErrors = new ArrayList<Double>();
		List<Double> keyframeErrors = new ArrayList<Double>();
		for (int i = 0; i < newPoints.size(); i++) {
			Matrix point = new Matrix(4, 1);
			point.set(0, 0, newPoints.get(i).getX());
			point.set(1, 0, newPoints.get(i).getY());
			point.set(2, 0, newPoints.get(i).getZ());
			point.set(3, 0, 1);
			Matrix projCurrent = CameraIntrinsics.getK4x4().times(currentPose.getHomogeneousMatrix()).times(point);
			projCurrent = projCurrent.times(1 / projCurrent.get(2, 0));
			Matrix projKeyframe = CameraIntrinsics.getK4x4()
					.times(this.map.getCurrentKeyframe().getPose().getHomogeneousMatrix()).times(point);
			projKeyframe = projKeyframe.times(1 / projKeyframe.get(2, 0));

			double errCurrent = Math
					.sqrt(Math.pow(projCurrent.get(0, 0) - untriangulatedCorrespondences.get(i).getX1(), 2)
							+ Math.pow(projCurrent.get(1, 0) - untriangulatedCorrespondences.get(i).getY1(), 2));
			double errKeyframe = Math
					.sqrt(Math.pow(projKeyframe.get(0, 0) - untriangulatedCorrespondences.get(i).getX0(), 2)
							+ Math.pow(projKeyframe.get(1, 0) - untriangulatedCorrespondences.get(i).getY0(), 2));

//			Utils.pl("errCurrent: " + errCurrent);
//			Utils.pl("errKeyframe: " + errKeyframe);

			avgErrCurrent += errCurrent;
			avgErrKeyframe += errKeyframe;

			currentErrors.add(errCurrent);
			keyframeErrors.add(errKeyframe);

		}

		avgErrCurrent /= newPoints.size();
		avgErrKeyframe /= newPoints.size();

		// get standard deviation of each error
		double stdDevCurrent = 0;
		double stdDevKeyframe = 0;

		for (int i = 0; i < currentErrors.size(); i++) {
			stdDevCurrent += Math.pow(currentErrors.get(i) - avgErrCurrent, 2);
			stdDevKeyframe += Math.pow(keyframeErrors.get(i) - avgErrKeyframe, 2);
		}

		stdDevCurrent /= currentErrors.size();
		stdDevKeyframe /= keyframeErrors.size();
		stdDevCurrent = Math.sqrt(stdDevCurrent);
		stdDevKeyframe = Math.sqrt(stdDevKeyframe);

		Utils.pl("Average current frame reprojection error: " + avgErrCurrent);
		Utils.pl("Average keyframe frame reprojection error: " + avgErrKeyframe);

		// if error is too high, reject the reconstruction
		if (avgErrCurrent + avgErrKeyframe > AVG_THRESHOLD) {
			return;
		}

		// set each point in the map
		for (int i = 0; i < newPoints.size(); i++) {

			// do not triangulate outliers
			if (currentErrors.get(i) > INDIVIDUAL_THRESHOLD || keyframeErrors.get(i) > INDIVIDUAL_THRESHOLD) {
				continue;
			}

			// set the point
			Point3D point3D = newPoints.get(i);
			synchronized (this.map) {
				untriangulatedMapPoints.get(i).setPoint(point3D);
				this.map.registerPoint(point3D);
			}
		}

	}

}
