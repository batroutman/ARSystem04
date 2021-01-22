import org.opencv.core.Core;
import org.opencv.features2d.ORB;

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
		ORB orb = ORB.create();
		pl("orb.getDefaultName(): " + orb.getDefaultName());
		pl("orb.getEdgeThreshold(): " + orb.getEdgeThreshold());
		pl("orb.getFastThreshold(): " + orb.getFastThreshold());
		pl("orb.getFirstLevel(): " + orb.getFirstLevel());
		pl("orb.getMaxFeatures(): " + orb.getMaxFeatures());
		pl("orb.getNLevels(): " + orb.getNLevels());
		pl("orb.getPatchSize(): " + orb.getPatchSize());
		pl("orb.getScaleFactor(): " + orb.getScaleFactor());
		pl("orb.getScoreType(): " + orb.getScoreType());
		pl("orb.getWTA_K(): " + orb.getWTA_K());
		pl("ORB.FAST_SCORE: " + ORB.FAST_SCORE);
		pl("ORB.HARRIS_SCORE: " + ORB.HARRIS_SCORE);
	}

	public static void p(Object s) {
		System.out.print(s);
	}

	public static void pl(Object s) {
		System.out.println(s);
	}
}