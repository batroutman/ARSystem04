import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.highgui.HighGui;

import ARSystem04.PoseEstimator;
import ARSystem04.TestPipeline;
import buffers.Buffer;
import buffers.SingletonBuffer;
import buffers.TUMBuffer;
import types.FramePack;
import types.ImageData;
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

		homogeneousExample();

	}

	public void homogeneousExample() {

		// Loading the core library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		TUMBuffer tumBuffer = new TUMBuffer(tumFile, true);

		for (int i = 0; i < tumBuffer.frameLimit; i++) {
			FramePack fp = tumBuffer.getNext();
			Mat ogImage = fp.getProcessedFrame();

			ImageData imgData = new ImageData(ogImage);
			long start = System.currentTimeMillis();
			imgData.detectHomogeneousFeatures();
			long end = System.currentTimeMillis();

			pl("extraction time: " + (end - start) + "ms");

			// Visualize results
			Mat resultImg = new Mat();
			Scalar color = new Scalar(0, 255, 0);
			Features2d.drawKeypoints(ogImage, imgData.getKeypoints(), resultImg, color, 0);
			HighGui.imshow("SSC KeyPoints", resultImg);
			HighGui.waitKey(33);
		}
	}

	/*
	 * Suppression via Square Convering (SSC) algorithm. Check Algorithm 2 in the
	 * paper:
	 * https://www.sciencedirect.com/science/article/abs/pii/S016786551830062X
	 */
	// https://github.com/BAILOOL/ANMS-Codes
	private static List<KeyPoint> ssc(final List<KeyPoint> keyPoints, final int numRetPoints, final float tolerance,
			final int cols, final int rows) {

		// Several temp expression variables to simplify equation solution
		int expression1 = rows + cols + 2 * numRetPoints;
		long expression2 = ((long) 4 * cols + (long) 4 * numRetPoints + (long) 4 * rows * numRetPoints
				+ (long) rows * rows + (long) cols * cols - (long) 2 * rows * cols
				+ (long) 4 * rows * cols * numRetPoints);
		double expression3 = Math.sqrt(expression2);
		double expression4 = (double) numRetPoints - 1;

		// first solution
		double solution1 = -Math.round((expression1 + expression3) / expression4);
		// second solution
		double solution2 = -Math.round((expression1 - expression3) / expression4);

		// binary search range initialization with positive solution
		int high = (int) ((solution1 > solution2) ? solution1 : solution2);
		int low = (int) Math.floor(Math.sqrt((double) keyPoints.size() / numRetPoints));
		int width;
		int prevWidth = -1;

		ArrayList<Integer> resultVec = new ArrayList<>();
		boolean complete = false;
		int kMin = Math.round(numRetPoints - (numRetPoints * tolerance));
		int kMax = Math.round(numRetPoints + (numRetPoints * tolerance));

		ArrayList<Integer> result = new ArrayList<>(keyPoints.size());
		while (!complete) {
			width = low + (high - low) / 2;

			// needed to reassure the same radius is not repeated again
			if (width == prevWidth || low > high) {
				// return the keypoints from the previous iteration
				resultVec = result;
				break;
			}
			result.clear();
			double c = (double) width / 2; // initializing Grid
			int numCellCols = (int) Math.floor(cols / c);
			int numCellRows = (int) Math.floor(rows / c);

			// Fill temporary boolean array
			boolean[][] coveredVec = new boolean[numCellRows + 1][numCellCols + 1];

			// Perform square suppression
			for (int i = 0; i < keyPoints.size(); i++) {
				// get position of the cell current point is located at
				int row = (int) Math.floor(keyPoints.get(i).pt.y / c);
				int col = (int) Math.floor(keyPoints.get(i).pt.x / c);
				if (!coveredVec[row][col]) { // if the cell is not covered
					result.add(i);

					// get range which current radius is covering
					int rowMin = (int) (((row - (int) Math.floor(width / c)) >= 0) ? (row - Math.floor(width / c)) : 0);
					int rowMax = (int) (((row + Math.floor(width / c)) <= numCellRows) ? (row + Math.floor(width / c))
							: numCellRows);
					int colMin = (int) (((col - Math.floor(width / c)) >= 0) ? (col - Math.floor(width / c)) : 0);
					int colMax = (int) (((col + Math.floor(width / c)) <= numCellCols) ? (col + Math.floor(width / c))
							: numCellCols);

					// cover cells within the square bounding box with width w
					for (int rowToCov = rowMin; rowToCov <= rowMax; rowToCov++) {
						for (int colToCov = colMin; colToCov <= colMax; colToCov++) {
							if (!coveredVec[rowToCov][colToCov]) {
								coveredVec[rowToCov][colToCov] = true;
							}
						}
					}
				}
			}

			// solution found
			if (result.size() >= kMin && result.size() <= kMax) {
				resultVec = result;
				complete = true;
			} else if (result.size() < kMin) {
				high = width - 1; // update binary search range
			} else {
				low = width + 1; // update binary search range
			}
			prevWidth = width;
		}

		// Retrieve final keypoints
		List<KeyPoint> kp = new ArrayList<>();
		for (int i : resultVec) {
			kp.add(keyPoints.get(i));
		}

		return kp;
	}

	public static void p(Object s) {
		System.out.print(s);
	}

	public static void pl(Object s) {
		System.out.println(s);
	}
}