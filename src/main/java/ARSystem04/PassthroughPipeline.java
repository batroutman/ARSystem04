package ARSystem04;

import java.util.Random;

import Jama.Matrix;
import buffers.Buffer;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
import runtimevars.Parameters;
import toolbox.Utils;
import types.Correspondence2D2D;
import types.Feature;
import types.FramePack;
import types.PipelineOutput;
import types.Point3D;
import types.Pose;

public class PassthroughPipeline extends PoseEstimator {

	int frameNum = 0;
	double tz = 0;
	double qw = 0.707;
	double rotY = 0;
	Matrix rotation = new Matrix(4, 1);
	Matrix rotChange = new Matrix(4, 1);

	Thread poseEstimationThread = new Thread() {
		@Override
		public void run() {
			mainloop();
		}
	};

	public PassthroughPipeline() {
		super(new QueuedBuffer<FramePack>(), new SingletonBuffer<PipelineOutput>());
	}

	public PassthroughPipeline(Buffer<FramePack> inputBuffer, Buffer<PipelineOutput> outputBuffer) {
		super(inputBuffer, outputBuffer);
		rotation.set(0, 0, 1);
		rotChange.set(0, 0, 0.995);
		rotChange.set(2, 0, 0.096);
	}

	@Override
	public void start() {
		this.poseEstimationThread.start();
	}

	@Override
	public void stop() {
		this.poseEstimationThread.interrupt();
	}

	public void mainloop() {
		boolean keepGoing = true;
		while (keepGoing) {
			long start = System.currentTimeMillis();
			FramePack newFrame = inputBuffer.getNext();
			if (newFrame == null) {
				keepGoing = false;
				continue;
			}

			PipelineOutput po = new PipelineOutput();
			po.frameNum = this.frameNum++;
			po.rawFrame = newFrame.getRawFrame();
			po.processedFrame = newFrame.getProcessedFrame();
			po.rawFrameBuffer = newFrame.getRawFrameBuffer();
			po.processedFrame.get(0, 0, newFrame.getProcessedFrameBuffer());
			po.processedFrameBuffer = newFrame.getProcessedFrameBuffer();

			Random rand = new Random(100);

			for (int i = 0; i < 100; i++) {
				double x0 = rand.nextDouble() * Parameters.width;
				double y0 = rand.nextDouble() * Parameters.height;
				double x1 = rand.nextDouble() * Parameters.width;
				double y1 = rand.nextDouble() * Parameters.height;
				po.correspondences.add(new Correspondence2D2D(x0, y0, x1, y1));
				po.features.add(new Feature(x1, y1, (int) (15 * rand.nextDouble()) + 5));
			}

//			this.tz -= 0.003;
			po.tz = this.tz;

			// map points
			po.points.add(new Point3D(1, 1, 10));
			for (int i = 0; i < 1000; i++) {
				double x = rand.nextDouble() * 2 - 1;
				double y = rand.nextDouble() * 2 - 1;
				double z = rand.nextDouble() * 4 - 1;
				po.points.add(new Point3D(x, y, z));
			}

			// keyframe cameras
			Pose pose = new Pose();
			rotation.print(15, 5);

			pose.setQw(rotation.get(0, 0));
			pose.setQx(rotation.get(1, 0));
			pose.setQy(rotation.get(2, 0));
			pose.setQz(rotation.get(3, 0));

			Utils.pl("float: " + (float) 1.2868690386841046);
			Utils.pl("pose rotation in euler ==> rotX: " + pose.getRotX() + "  rotY: " + pose.getRotY() + "  rotZ: "
					+ pose.getRotZ());
			po.cameras.add(pose);
			rotation = Utils.quatMult(rotChange, rotation);

			try {
				Thread.sleep(32);
			} catch (Exception e) {
			}

			long end = System.currentTimeMillis();
			po.fps = 1000 / (end - start + 1);

			outputBuffer.push(po);

		}
	}

}
