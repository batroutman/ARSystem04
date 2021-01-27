package types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

import toolbox.Utils;

public class ImageData {

	protected static ORB orb = ORB.create(1000, 2, 3, 31, 0, 2, ORB.FAST_SCORE, 31, 20); // optimized for speed
//	protected static ORB orb = ORB.create(500, 1.2f, 8, 31, 0, 2, ORB.FAST_SCORE, 31, 20); // default

	protected List<Mat> masks = new ArrayList<Mat>();

	protected Mat image = new Mat();
	protected MatOfKeyPoint keypoints = new MatOfKeyPoint();
	protected Mat descriptors = new Mat();

	public ImageData() {
	}

	public ImageData(Mat image) {
		this.image = image;
	}

	public void autoContrast() {
		Imgproc.equalizeHist(this.image, this.image);
	}

	public void detectAndComputeORB() {
		orb.detect(this.image, this.keypoints);
		List<KeyPoint> listKeypoints = this.keypoints.toList();
		for (int i = 0; i < listKeypoints.size(); i++) {
			KeyPoint keypoint = listKeypoints.get(i);
			Utils.pl("keypoint ==> angle: " + keypoint.angle + ",  class_id: " + keypoint.class_id + ",  octave: "
					+ keypoint.octave + ",  pt.x: " + keypoint.pt.x + ",  pt.y: " + keypoint.pt.y + ",  response: "
					+ keypoint.response + ",  size: " + keypoint.size);
		}
		orb.compute(this.image, this.keypoints, this.descriptors);
	}

	public void detectAndComputeHomogeneousORB() {
		this.detectHomogeneousFeatures();
		orb.compute(this.image, this.keypoints, this.descriptors);
	}

	public void detectHomogeneousFeatures() {
		List<MatOfKeyPoint> cellKeypoints = new ArrayList<MatOfKeyPoint>();

		// downscale the image to extract FAST features with patch size 28, 56, and 112
		Mat down28 = this.downScale(this.image);
		down28 = this.downScale(down28);
		Mat down56 = this.downScale(down28);
		Mat down112 = this.downScale(down56);

		// init FAST
		int fastThresh = 20;
		FastFeatureDetector fastDetector = FastFeatureDetector.create(fastThresh);

		// get FAST features
		MatOfKeyPoint keypoints28 = new MatOfKeyPoint();
		MatOfKeyPoint keypoints56 = new MatOfKeyPoint();
		MatOfKeyPoint keypoints112 = new MatOfKeyPoint();
		fastDetector.detect(down28, keypoints28);
		fastDetector.detect(down56, keypoints56);
		fastDetector.detect(down112, keypoints112);

		// sort lists
		List<KeyPoint> sorted28 = keypoints28.toList();
		List<KeyPoint> sorted56 = keypoints56.toList();
		List<KeyPoint> sorted112 = keypoints112.toList();
		sorted28.sort((kp1, kp2) -> (int) (kp2.response - kp1.response));
		sorted56.sort((kp1, kp2) -> (int) (kp2.response - kp1.response));
		sorted112.sort((kp1, kp2) -> (int) (kp2.response - kp1.response));

		// perform ssc filtering (https://github.com/BAILOOL/ANMS-Codes)
		int numRetPoints28 = 200;
		int numRetPoints56 = 100;
		int numRetPoints112 = 50;
		float tolerance = (float) 0.1;

		List<KeyPoint> sscKeyPoints28 = ssc(sorted28, numRetPoints28, tolerance, down28.cols(), down28.rows());
		List<KeyPoint> sscKeyPoints56 = ssc(sorted56, numRetPoints56, tolerance, down56.cols(), down56.rows());
		List<KeyPoint> sscKeyPoints112 = ssc(sorted112, numRetPoints112, tolerance, down112.cols(), down112.rows());

		// scale keypoints up to match the full sized image
		for (int i = 0; i < sscKeyPoints28.size(); i++) {
			KeyPoint keypoint = sscKeyPoints28.get(i);
			keypoint.size = 31;
			keypoint.octave = 0;
			keypoint.pt.x *= 4;
			keypoint.pt.y *= 4;
		}

		for (int i = 0; i < sscKeyPoints56.size(); i++) {
			KeyPoint keypoint = sscKeyPoints56.get(i);
			keypoint.size = 62;
			keypoint.octave = 1;
			keypoint.pt.x *= 8;
			keypoint.pt.y *= 8;
		}

		for (int i = 0; i < sscKeyPoints112.size(); i++) {
			KeyPoint keypoint = sscKeyPoints112.get(i);
			keypoint.size = 124;
			keypoint.octave = 2;
			keypoint.pt.x *= 16;
			keypoint.pt.y *= 16;
		}

		// merge keypoints
		sscKeyPoints28.addAll(sscKeyPoints56);
		sscKeyPoints28.addAll(sscKeyPoints112);

		this.keypoints.fromList(sscKeyPoints28);

	}

	// pass in the full sized image buffer, width of the image, keypoints (with
	// corrected xy values and sizes)
	public void ICAngles(byte[] imgBuffer, int imgWidth, int imgHeight, List<KeyPoint> pts,
			HashMap<Integer, List<Integer>> u_max_map) {
		int ptidx, ptsize = pts.size();
		int width = imgWidth;
		int height = imgHeight;

		for (ptidx = 0; ptidx < ptsize; ptidx++) {

			List<Integer> u_max = u_max_map.get((int) pts.get(ptidx).size);

			int half_k = (int) (pts.get(ptidx).size / 2);

			int centerX = (int) pts.get(ptidx).pt.x;
			int centerY = (int) pts.get(ptidx).pt.y;

			int m_01 = 0, m_10 = 0;

			// Treat the center line differently, v=0
			for (int u = -half_k; u <= half_k; ++u) {
				int x = u + centerX;
				m_10 += x < 0 || x >= width ? 0 : u * imgBuffer[rowMajor(x, centerY, width)];
			}

			// Go line by line in the circular patch
			for (int v = 1; v <= half_k; ++v) {
				// Proceed over the two lines
				int v_sum = 0;
				int d = u_max.get(v);
				for (int u = -d; u <= d; ++u) {
					int x = centerX + u;
					int y = centerY + v;

					int val_plus = x < 0 || y < 0 || x >= width || y >= height ? 0 : imgBuffer[rowMajor(x, y, width)];
					y = centerY - v;
					int val_minus = x < 0 || y < 0 || x >= width || y >= height ? 0 : imgBuffer[rowMajor(x, y, width)];

					v_sum += (val_plus - val_minus);
					m_10 += u * (val_plus + val_minus);
				}
				m_01 += v * v_sum;
			}

			pts.get(ptidx).angle = (float) Math.atan2((float) m_01, (float) m_10);
		}
	}

	public HashMap<Integer, List<Integer>> getUMaxMap(int[] patchSizes) {

		HashMap<Integer, List<Integer>> u_max_map = new HashMap<Integer, List<Integer>>();

		for (int i = 0; i < patchSizes.length; i++) {

			int halfPatchSize = patchSizes[i] / 2;
			List<Integer> umax = new ArrayList<Integer>(halfPatchSize + 2);

			for (int j = 0; j < halfPatchSize + 2; j++) {
				umax.add(0);
			}

			int v, v0, vmax = (int) Math.floor(halfPatchSize * Math.sqrt(2.f) / 2 + 1);
			int vmin = (int) Math.ceil(halfPatchSize * Math.sqrt(2.f) / 2);
			for (v = 0; v <= vmax; ++v)
				umax.set(v, (int) Math.round(Math.sqrt((double) halfPatchSize * halfPatchSize - v * v)));

			// Make sure we are symmetric
			for (v = halfPatchSize, v0 = 0; v >= vmin; --v) {
				while (umax.get(v0) == umax.get(v0 + 1))
					++v0;
				umax.set(v, v0);
				++v0;
			}

			u_max_map.put(patchSizes[i], umax);

		}

		return u_max_map;

	}

	public static int rowMajor(int x, int y, int width) {
		return width * y + x;
	}

	public Mat downScale(Mat src) {
		Mat dest = new Mat();
		Imgproc.pyrDown(src, dest, new Size((int) (src.cols() / 2), (int) (src.rows() / 2)));
		return dest;
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
			width = width == 0 ? 1 : width;

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

	public Mat getImage() {
		return image;
	}

	public void setImage(Mat image) {
		this.image = image;
	}

	public MatOfKeyPoint getKeypoints() {
		return keypoints;
	}

	public void setKeypoints(MatOfKeyPoint keypoints) {
		this.keypoints = keypoints;
	}

	public Mat getDescriptors() {
		return descriptors;
	}

	public void setDescriptors(Mat descriptors) {
		this.descriptors = descriptors;
	}

}
