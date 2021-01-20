package ARSystem04;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import buffers.Buffer;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
import toolbox.Utils;
import types.FramePack;
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
			Mat processedImage = imgProc.logitTransform(currentFrame.getProcessedFrame());
			Mat processedImage2 = imgProc.downScale(processedImage);
			Mat processedImage4 = imgProc.downScale(processedImage2);
			Mat processedImage8 = imgProc.downScale(processedImage4);

			// get features from image
			MatOfKeyPoint keypoints = imgProc.getFastFeatures(processedImage);
			MatOfKeyPoint keypoints2 = imgProc.getFastFeatures(processedImage2);
			MatOfKeyPoint keypoints4 = imgProc.getFastFeatures(processedImage4);
			MatOfKeyPoint keypoints8 = imgProc.getFastFeatures(processedImage8);
//			Utils.pl("keypoint lengths: " + keypoints.toList().size() + ", " + keypoints2.toList().size() + ", "
//					+ keypoints4.toList().size() + ", " + keypoints8.toList().size());

			// modify keypoints to display on original image
			List<KeyPoint> listKeypoints = keypoints.toList();
			List<KeyPoint> listKeypoints2 = keypoints2.toList();
			List<KeyPoint> listKeypoints4 = keypoints4.toList();
			List<KeyPoint> listKeypoints8 = keypoints8.toList();

			for (int i = 0; i < listKeypoints2.size(); i++) {
				listKeypoints2.get(i).pt.x = listKeypoints2.get(i).pt.x * 2;
				listKeypoints2.get(i).pt.y = listKeypoints2.get(i).pt.y * 2;
				listKeypoints2.get(i).size = listKeypoints2.get(i).size * 2;
			}

			for (int i = 0; i < listKeypoints4.size(); i++) {
				listKeypoints4.get(i).pt.x = listKeypoints4.get(i).pt.x * 4;
				listKeypoints4.get(i).pt.y = listKeypoints4.get(i).pt.y * 4;
				listKeypoints4.get(i).size = listKeypoints4.get(i).size * 4;
			}

			for (int i = 0; i < listKeypoints8.size(); i++) {
				listKeypoints8.get(i).pt.x = listKeypoints8.get(i).pt.x * 8;
				listKeypoints8.get(i).pt.y = listKeypoints8.get(i).pt.y * 8;
				listKeypoints8.get(i).size = listKeypoints8.get(i).size * 8;
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
			out.features = new ArrayList<KeyPoint>();
			out.features.addAll(listKeypoints);
			out.features.addAll(listKeypoints2);
			out.features.addAll(listKeypoints4);
			out.features.addAll(listKeypoints8);
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
