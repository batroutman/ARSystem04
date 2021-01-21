package ARSystem04;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.imgproc.Imgproc;

import types.ImageData;

public class ImageProcessor {

	public static int[] FASTThresholds = { 50, 50, 50, 50 };
	public Mat logitLookUp = new Mat(1, 256, CvType.CV_8U);
	public FastFeatureDetector FAST = FastFeatureDetector.create(35, true, FastFeatureDetector.TYPE_9_16);

	public int featureNumTarget = 100;

	public ImageProcessor() {
		this.init();
	}

	public void init() {
		this.loadLogitLookUp();
	}

	public void loadLogitLookUp() {
		byte[] lookUpTableData = new byte[(int) (this.logitLookUp.total() * this.logitLookUp.channels())];
		for (int i = 0; i < this.logitLookUp.cols(); i++) {
			lookUpTableData[i] = (byte) this.logit(i);
		}
		this.logitLookUp.put(0, 0, lookUpTableData);
	}

	public double logit(double p) {
		// normal range of log((p+1) / (256 - p)) is -5.545177444479562 to
		// +5.545177444479562, which is a range of 11.090354888959125 (for p values
		// [0,255]).
		// (log((p + 1) / (256 - p)) + 5.545177444479562) / 11.090354888959125 * 255
		// simplifies to the following output
		return (Math.log((p + 1) / (256 - p)) + 5.545177444479562) * 22.992952214167854;
	}

	public Mat logitTransform(Mat src) {
		Mat dest = new Mat();
		Core.LUT(src, this.logitLookUp, dest);
		return dest;
	}

	public Mat autoContrast(Mat src) {
		Mat dest = new Mat();
		Imgproc.equalizeHist(src, dest);
		return dest;
	}

	public MatOfKeyPoint getFastFeatures(Mat img) {
		MatOfKeyPoint keypoints = new MatOfKeyPoint();
		this.FAST.detect(img, keypoints);
		return keypoints;
	}

	public void getFastFeatures(ImageData pyramid) {
		for (int i = 0; i < pyramid.getNumOctaves(); i++) {
			this.FAST.setThreshold(FASTThresholds[i]);
			MatOfKeyPoint keypoints = this.getFastFeatures(pyramid.getOctaves().get(i));
			pyramid.getKeypoints().set(i, keypoints);
		}
	}

	public Mat downScale(Mat src) {
		Mat dest = new Mat();
		Imgproc.pyrDown(src, dest, new Size((int) (src.cols() / 2), (int) (src.rows() / 2)));
		return dest;
	}

	public Mat upScale(Mat src) {
		Mat dest = new Mat();
		Imgproc.pyrUp(src, dest, new Size((int) (src.cols() * 2), (int) (src.rows() * 2)));
		return dest;
	}

	public ImageData generatePyramid(Mat image) {
		return this.generatePyramid(image, 3);
	}

	public ImageData generatePyramid(Mat image, int numOctaves) {
		ImageData pyramid = new ImageData(numOctaves);

		// load octaves with downscaled images
		pyramid.getOctaves().set(0, image);
		for (int i = 1; i < pyramid.getNumOctaves(); i++) {
			pyramid.getOctaves().set(i, this.downScale(pyramid.getOctaves().get(i - 1)));
		}

		return pyramid;

	}

}
