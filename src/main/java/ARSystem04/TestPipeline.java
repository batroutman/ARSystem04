package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.ORB;

import buffers.Buffer;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
import toolbox.Utils;
import types.Correspondence2D2D;
import types.FramePack;
import types.ImageData;
import types.PipelineOutput;

public class TestPipeline extends PoseEstimator {

	protected long frameNum = 0;

	protected ImageProcessor imgProc = new ImageProcessor();
	protected ORBMatcher orbMatcher = new ORBMatcher();
	ORB orb = ORB.create();

	ImageData firstFrame;
	MatOfKeyPoint firstKeypoints;
	Mat firstDescriptors;

	Thread poseEstimationThread = new Thread() {
		@Override
		public void run() {
			mainloop();
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
	}

	@Override
	public void stop() {
		this.poseEstimationThread.interrupt();
	}

	protected void init() {
		Utils.pl("Initializing TestPipeline...");
		this.orb.setScoreType(ORB.FAST_SCORE);
		this.orb.setScaleFactor(2);
		Utils.pl("ORB num features: " + this.orb.getMaxFeatures());
		Utils.pl("ORB num levels: " + this.orb.getNLevels());
		Utils.pl("ORB FAST threshold: " + this.orb.getFastThreshold());
		Utils.pl("ORB patch size: " + this.orb.getPatchSize());
		Utils.pl("ORB scale factor: " + this.orb.getScaleFactor());
		Utils.pl("ORB score type: " + this.orb.getScoreType());
		Utils.pl("ORB WTA_K: " + this.orb.getWTA_K());
		Utils.pl("FAST: " + ORB.FAST_SCORE);
	}

	protected void mainloop() {
		boolean keepGoing = true;
		while (keepGoing) {

			long start = System.currentTimeMillis();

			// get the next frame
			FramePack currentFrame = this.inputBuffer.getNext();

			if (currentFrame == null) {
				keepGoing = false;
				continue;
			}

			// transform image and feature matching
			Mat processedImage = imgProc.autoContrast(currentFrame.getProcessedFrame());
//			ImageData pyramid = imgProc.generatePyramid(processedImage);
//			imgProc.getFastFeatures(pyramid);
//			pyramid.generateNormalizedKeypoints();
//			imgProc.getORBDescriptors(pyramid);
//			pyramid.mergeDescriptors();
			MatOfKeyPoint keypoints = new MatOfKeyPoint();
			Mat descriptors = new Mat();

			this.orb.detect(processedImage, keypoints);
			this.orb.compute(processedImage, keypoints, descriptors);
			List<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();
			if (frameNum == 0) {
//				this.firstFrame = pyramid;
				this.firstKeypoints = keypoints;
				this.firstDescriptors = descriptors;
			} else {
//				correspondences = orbMatcher.matchDescriptors2(firstFrame.getNormalizedKeypoints(),
//						firstFrame.getMergedDescriptors(), pyramid.getNormalizedKeypoints(),
//						pyramid.getMergedDescriptors());
				correspondences = orbMatcher.matchDescriptors2(this.firstKeypoints.toList(), this.firstDescriptors,
						keypoints.toList(), descriptors);
			}

			// set output
			PipelineOutput out = new PipelineOutput();
			long end = System.currentTimeMillis();
			double fps = 1000.0 / (end - start);
			out.rawFrame = currentFrame.getRawFrame();
			out.rawFrameBuffer = currentFrame.getRawFrameBuffer();
			out.frameNum = this.frameNum;
			out.fps = fps;
//			Utils.pl(fps + " fps");

			out.processedFrame = processedImage;
			byte[] processedBuffer = new byte[processedImage.rows() * processedImage.cols()];
			processedImage.get(0, 0, processedBuffer);
			out.processedFrameBuffer = processedBuffer;
			out.features = keypoints.toList();
			out.numFeatures = out.features.size();
			out.correspondences = correspondences;

			this.outputBuffer.push(out);
			this.frameNum++;

			try {
				Thread.sleep(0);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
}
