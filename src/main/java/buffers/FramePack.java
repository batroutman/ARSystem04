package buffers;

import org.opencv.core.Mat;

// bundles a raw frame and its processed frame (the raw frame that has undergone some degree of preprocessing for tracking)
public class FramePack {

	private long timestamp = 0;
	private Mat rawFrame = null;
	private Mat processedFrame = null;

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public Mat getRawFrame() {
		return rawFrame;
	}

	public void setRawFrame(Mat rawFrame) {
		this.rawFrame = rawFrame;
	}

	public Mat getProcessedFrame() {
		return processedFrame;
	}

	public void setProcessedFrame(Mat processedFrame) {
		this.processedFrame = processedFrame;
	}

}
