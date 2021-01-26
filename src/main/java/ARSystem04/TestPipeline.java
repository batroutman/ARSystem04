package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import Jama.Matrix;
import buffers.Buffer;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
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
			List<MapPoint> correspondingMapPoints = new ArrayList<MapPoint>();
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
						correspondences, correspondingMapPoints, untriangulatedCorrespondences, untriangulatedMapPoints,
						pose);

				// if poses are far enough away, triangulate untriangulated points
//				Utils.pl("pose.getDistanceFrom(this.map.getCurrentKeyframe().getPose()): "
//						+ pose.getDistanceFrom(this.map.getCurrentKeyframe().getPose()));
//				Utils.pl("number of untriangulated points: " + untriangulatedMapPoints.size());
				if (pose.getDistanceFrom(this.map.getCurrentKeyframe().getPose()) >= 1) {
					for (int i = 0; i < untriangulatedCorrespondences.size(); i++) {
						Correspondence2D2D c = untriangulatedCorrespondences.get(i);
						Matrix pointMatrix = Photogrammetry.triangulate(pose.getHomogeneousMatrix(),
								this.map.getCurrentKeyframe().getPose().getHomogeneousMatrix(), c);
						Point3D point3D = new Point3D(pointMatrix.get(0, 0), pointMatrix.get(1, 0),
								pointMatrix.get(2, 0));
						untriangulatedMapPoints.get(i).setPoint(point3D);
						synchronized (this.map) {
							this.map.registerPoint(point3D);
						}

					}
				}

				// if starting to lose tracking, generate new keyframe
				if (numMatches < 100) {
					this.map.registerNewKeyframe(pose, processedImage.getKeypoints(), processedImage.getDescriptors(),
							correspondingMapPoints);
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
}
