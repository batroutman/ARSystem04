package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

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

public class TestPipeline extends PoseEstimator {

	protected long frameNum = 0;

	protected ORBMatcher orbMatcher = new ORBMatcher();
	protected Map map = new Map();
	protected Tracker tracker = new Tracker(this.map);
	protected MapOptimizer mapOptimizer = new MapOptimizer(this.map);

	ImageData firstFrame;
	MatOfKeyPoint firstKeypoints;
	Mat firstDescriptors;

	Thread poseEstimationThread = new Thread() {
		@Override
		public void run() {
			mainloop();
		}
	};

	Thread mapOptimizationThread = new Thread() {
		@Override
		public void run() {
			mapOptimizer.mainloop();
		}
	};

	public TestPipeline() {
		super(new QueuedBuffer<FramePack>(), new SingletonBuffer<PipelineOutput>());
		this.init();
	}

	public TestPipeline(Buffer<FramePack> inputBuffer, Buffer<PipelineOutput> outputBuffer) {
		super(inputBuffer, outputBuffer);
		this.init();
	}

	@Override
	public void start() {
		this.poseEstimationThread.start();
		this.mapOptimizationThread.start();
	}

	@Override
	public void stop() {
		this.poseEstimationThread.interrupt();
	}

	protected void init() {
		Utils.pl("Initializing TestPipeline...");

	}

	protected void mainloop() {
		boolean keepGoing = true;
		while (keepGoing) {

			long start = System.currentTimeMillis();
			Utils.pl("frameNum: " + this.frameNum);

			// get the next frame
			FramePack currentFrame = this.inputBuffer.getNext();

			if (currentFrame == null) {
				keepGoing = false;
				continue;
			}

			// transform image and feature matching
			ImageData processedImage = new ImageData(currentFrame.getProcessedFrame());
			processedImage.autoContrast();
			processedImage.detectAndComputeORB();

			List<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();
			Pose pose = new Pose();
			List<MapPoint> mapPointPerDescriptor = new ArrayList<MapPoint>();
			List<MapPoint> correspondenceMapPoints = new ArrayList<MapPoint>();
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
						correspondences, correspondenceMapPoints, mapPointPerDescriptor, untriangulatedCorrespondences,
						untriangulatedMapPoints, pose);

				// pair-wise BA
				this.mapOptimizer.pairBundleAdjustment(pose, this.map.getCurrentKeyframe().getPose(),
						correspondenceMapPoints, correspondences, 10);

				// if poses are far enough away, triangulate untriangulated points
				if (pose.getDistanceFrom(this.map.getCurrentKeyframe().getPose()) >= 1
						&& untriangulatedCorrespondences.size() > 0) {

					this.triangulateUntrackedMapPoints(pose, untriangulatedCorrespondences, untriangulatedMapPoints);

				}

				// if starting to lose tracking, generate new keyframe
				if (numMatches < 100) {
					this.map.registerNewKeyframe(pose, processedImage.getKeypoints(), processedImage.getDescriptors(),
							mapPointPerDescriptor);
					this.mapOptimizer.fullBundleAdjustment(10);
				}

			}

			if (correspondences == null) {
				correspondences = new ArrayList<Correspondence2D2D>();
			}

			// set output
			PipelineOutput out = new PipelineOutput();
			long end = System.currentTimeMillis();
			double fps = 1000.0 / (end - start);
			out.pose = pose;
			out.rawFrame = currentFrame.getRawFrame();
			out.rawFrameBuffer = currentFrame.getRawFrameBuffer();
			out.frameNum = this.frameNum;
			out.fps = fps;
//			Utils.pl(fps + " fps");

			out.processedFrame = processedImage.getImage();
			byte[] processedBuffer = new byte[processedImage.getImage().rows() * processedImage.getImage().cols()];
			processedImage.getImage().get(0, 0, processedBuffer);
			out.processedFrameBuffer = processedBuffer;
			out.features = processedImage.getKeypoints().toList();
			out.numFeatures = out.features.size();
			out.correspondences = correspondences;
			out.cameras = this.map.getCameras();
			out.points = this.map.getAllPoints();

			this.outputBuffer.push(out);
			this.frameNum++;

			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	public void triangulateUntrackedMapPoints(Pose currentPose, List<Correspondence2D2D> untriangulatedCorrespondences,
			List<MapPoint> untriangulatedMapPoints) {

		// the sum of average reprojection errors over which the triangulation will be
		// discarded
		double AVG_THRESHOLD = 20;

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

			// do not triangulate outliers (points with error above 1 standard deviation of
			// the mean)
			if (currentErrors.get(i) > stdDevCurrent + avgErrCurrent
					|| keyframeErrors.get(i) > stdDevKeyframe + avgErrKeyframe) {
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
