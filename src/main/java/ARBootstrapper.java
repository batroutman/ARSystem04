import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;

import ARSystem04.PoseEstimator;
import ARSystem04.TestPipeline;
import buffers.Buffer;
import buffers.SingletonBuffer;
import buffers.TUMBuffer;
import types.PipelineOutput;

public class ARBootstrapper {

	String tumFile = "../datasets/rgbd_dataset_freiburg3_long_office_household/";

	public ARBootstrapper() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public void start() {

		// OfflineFrameBuffer ofb = new OfflineFrameBuffer(filename, false);
		TUMBuffer tumBuffer = new TUMBuffer(tumFile, true);
		Buffer<PipelineOutput> outputBuffer = new SingletonBuffer<PipelineOutput>();
		PoseEstimator pipeline = new TestPipeline(tumBuffer, outputBuffer);
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
		List<Double> list = new ArrayList<Double>(4);
		list.set(0, 7.0);

		pl(list.get(0));

	}

	public static void p(Object s) {
		System.out.print(s);
	}

	public static void pl(Object s) {
		System.out.println(s);
	}
}