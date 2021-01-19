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
	public Pose pose = new Pose();

	// frame data
	public Mat rawFrame = null;
	public Mat processedFrame = null;
	public byte[] rawFrameBuffer = null;
	public byte[] processedFrameBuffer = null;

	// processed frame data
	public ArrayList<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();
	public ArrayList<Feature> features = new ArrayList<Feature>();

	// map data
	public ArrayList<Point3D> points = new ArrayList<Point3D>();
	public ArrayList<Pose> cameras = new ArrayList<Pose>();

}
