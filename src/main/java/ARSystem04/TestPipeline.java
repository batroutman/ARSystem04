package ARSystem04;

import org.opencv.core.Mat;

import buffers.Buffer;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
import toolbox.Utils;
import types.FramePack;
import types.ImageData;
import types.PipelineOutput;

public class TestPipeline extends PoseEstimator {

	protected long frameNum = 0;

	protected ImageProcessor imgProc = new ImageProcessor();

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

			// transform image
			Mat processedImage = imgProc.autoContrast(currentFrame.getProcessedFrame());
			ImageData pyramid = imgProc.generatePyramid(processedImage);
			imgProc.getFastFeatures(pyramid);

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
			out.features = pyramid.generateNormalizedKeypoints();
			out.numFeatures = out.features.size();

			this.outputBuffer.push(out);
			this.frameNum++;

			try {
				Thread.sleep(10);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
}
