package types;

import java.util.ArrayList;

import org.opencv.core.Mat;

// PipelineOutput is a type that packages all information that the desktop container may display for any single frame
public class PipelineOutput {

	//
	public boolean finalFrame = false;
	public double frameNum = 0;

	// performance and diagnostic data
	public double fps = 0;
	public int numKeyframes = 0;
	public int numFeatures = 0;
	public int numCorrespondences = 0;
	public boolean tracking = false;

	// pose params
	public double qw = 1;
	public double qx = 0;
	public double qy = 0;
	public double qz = 0;

	public double r00 = 1;
	public double r01 = 0;
	public double r02 = 0;
	public double r10 = 0;
	public double r11 = 1;
	public double r12 = 0;
	public double r20 = 0;
	public double r21 = 0;
	public double r22 = 1;

	public double tx = 0;
	public double ty = 0;
	public double tz = 0;

	// frame data
	public Mat rawFrame = null;
	public Mat processedFrame = null;
	public byte[] rawFrameBuffer = null;
	public byte[] processedFrameBuffer = null;

	// processed frame data
	public ArrayList<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();

}
