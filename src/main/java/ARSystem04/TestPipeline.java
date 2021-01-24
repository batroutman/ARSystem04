package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import buffers.Buffer;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
import toolbox.Utils;
import types.Correspondence2D2D;
import types.FramePack;
import types.ImageData;
import types.PipelineOutput;
import types.Pose;

public class TestPipeline extends PoseEstimator {

	protected long frameNum = 0;

	protected ORBMatcher orbMatcher = new ORBMatcher();
	protected Map map = new Map();

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
			ImageData processedImage = new ImageData(currentFrame.getProcessedFrame());
			processedImage.autoContrast();
			processedImage.detectAndComputeORB();

			List<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();
			Pose pose = new Pose();
			if (!this.map.isInitialized()) {
				correspondences = this.map.getInitializer().registerData(this.frameNum, processedImage);
				if (this.map.isInitialized()) {
					pose = new Pose(this.map.getCurrentKeyframe().getPose());
				}
			} else {
				correspondences = this.map.getInitializer().registerData(this.frameNum, processedImage);
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
			Utils.pl(fps + " fps");

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
