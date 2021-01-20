package ARSystem04;

import java.util.ArrayList;

import Jama.Matrix;
import buffers.Buffer;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
import types.FramePack;
import types.PipelineOutput;
import types.Point3D;
import types.Pose;

public class MockPipeline extends PoseEstimator {

	int frameNum = 0;
	MockPointData mock = new MockPointData();

	// quaternion orientation
	Matrix orientation = new Matrix(4, 1);

	ArrayList<Pose> keyframes = new ArrayList<Pose>();

	Thread poseEstimationThread = new Thread() {
		@Override
		public void run() {
			mainloop();
		}
	};

	public MockPipeline() {
		super(new QueuedBuffer<FramePack>(), new SingletonBuffer<PipelineOutput>());
	}

	public MockPipeline(Buffer<FramePack> inputBuffer, Buffer<PipelineOutput> outputBuffer) {
		super(inputBuffer, outputBuffer);
		this.orientation.set(0, 0, 1);
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
			if (this.mock.getMAX_FRAMES() <= this.frameNum) {
				keepGoing = false;
				continue;
			}

			PipelineOutput po = new PipelineOutput();
			po.frameNum = this.frameNum++;

			// image data
			po.rawFrameBuffer = this.mock.getImageBufferRGB(frameNum);
			po.processedFrameBuffer = this.mock.getImageBufferGrey(frameNum);

			// pose
			Matrix poseQuat = this.mock.getQuaternion(frameNum);
			Matrix IC = this.mock.getIC(frameNum);
			Pose pose = new Pose();
			pose.setQw(poseQuat.get(0, 0));
			pose.setQx(poseQuat.get(1, 0));
			pose.setQy(poseQuat.get(2, 0));
			pose.setQz(poseQuat.get(3, 0));
			pose.setCx(-IC.get(0, 3));
			pose.setCy(-IC.get(1, 3));
			pose.setCz(-IC.get(2, 3));
			po.pose = pose;
//			Utils.pl("pose ==> rotX: " + pose.getRotX() + "  rotY: " + pose.getRotY() + "  rotZ: " + pose.getRotZ());

			// map data
			for (Matrix point : this.mock.getWorldCoordinates()) {
				po.points.add(new Point3D(point.get(0, 0), point.get(1, 0), point.get(2, 0)));
			}

			if (frameNum % 10 == 0) {
				synchronized (this.keyframes) {
					this.keyframes.add(pose);
				}
			}
			po.cameras = this.keyframes;
//			po.cameras = new ArrayList<Pose>();
//			Pose dummyPose = new Pose();
//
//			dummyPose.setQw(this.orientation.get(0, 0));
//			dummyPose.setQx(this.orientation.get(1, 0));
//			dummyPose.setQy(this.orientation.get(2, 0));
//			dummyPose.setQz(this.orientation.get(3, 0));
//
//			dummyPose.setCz(-2);
//
//			Matrix rotChange = new Matrix(4, 1);
//			rotChange.set(0, 0, 1);
//			rotChange.set(2, 0, 0.1);
//			rotChange = rotChange.times(1 / rotChange.normF());
//			this.orientation = Utils.quatMult(rotChange, this.orientation);
//
//			po.cameras.add(dummyPose);

			try {
				Thread.sleep(10);
			} catch (Exception e) {
			}

			long end = System.currentTimeMillis();
			po.fps = 1000 / (end - start + 1);

			outputBuffer.push(po);

		}
	}

}
