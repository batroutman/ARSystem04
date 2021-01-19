import org.opencv.core.Core;

import ARSystem04.MockPipeline;
import ARSystem04.PoseEstimator;
import buffers.Buffer;
import buffers.SingletonBuffer;
import buffers.TUMBuffer;
import types.FramePack;
import types.PipelineOutput;

public class ARBootstrapper {

	String tumFile = "../datasets/rgbd_dataset_freiburg3_long_office_household/";

	public ARBootstrapper() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public void start() {

		// OfflineFrameBuffer ofb = new OfflineFrameBuffer(filename, false);
//		TUMBuffer tumBuffer = new TUMBuffer(tumFile, true);
		Buffer<PipelineOutput> outputBuffer = new SingletonBuffer<PipelineOutput>();
		PoseEstimator pipeline = new MockPipeline(null, outputBuffer);
		// ARPipeline pipeline = new MockPipeline(sfb, spb, sfb);
		OpenGLARDisplay ARDisplay = new OpenGLARDisplay(outputBuffer);

		pipeline.start();
		ARDisplay.displayLoop();

		println("Done.");
	}

	public static void println(Object obj) {
		System.out.println(obj);
	}

	public static void main(String[] args) {
		ARBootstrapper arBootstrapper = new ARBootstrapper();
		arBootstrapper.start();
//		arBootstrapper.tests();

	}

	public void tests() {
		TUMBuffer tumBuffer = new TUMBuffer(tumFile, true);
		FramePack fp = tumBuffer.getNext();
		pl(fp.getRawFrame().rows()); // 480
		pl(fp.getRawFrame().cols()); // 640
		pl(fp.getRawFrame().get(0, 0).length); // 3
		pl(fp.getRawFrame().get(0, 0)[0]); // 157.0 (B)
		pl(fp.getRawFrame().get(0, 0)[1]); // 161.0 (G)
		pl(fp.getRawFrame().get(0, 0)[2]); // 158.0 (R)
		pl("");
		pl(fp.getProcessedFrame().rows()); // 480
		pl(fp.getProcessedFrame().cols()); // 640
		pl(fp.getProcessedFrame().get(0, 0).length); // 1
		pl(fp.getProcessedFrame().get(0, 0)[0]); // 160

	}

	public static void p(Object s) {
		System.out.print(s);
	}

	public static void pl(Object s) {
		System.out.println(s);
	}
}