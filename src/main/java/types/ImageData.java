package types;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;

public class ImageData {

	protected static ORB orb = ORB.create(500, 2, 4, 31, 0, 2, ORB.FAST_SCORE, 31, 20);

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
		orb.compute(this.image, this.keypoints, this.descriptors);
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
