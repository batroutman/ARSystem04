package ARSystem04;

import Jama.Matrix;
import buffers.Buffer;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
import types.FramePack;
import types.PipelineOutput;
import types.Pose;

public class MockPipeline extends PoseEstimator {

	int frameNum = 0;
	MockPointData mock = new MockPointData();

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

			Matrix poseQuat = this.mock.getQuaternion(frameNum);
			Matrix IC = this.mock.getIC(frameNum);
			Pose pose = new Pose();
			pose.setQw(poseQuat.get(0, 0));
			pose.setQx(poseQuat.get(1, 0));
			pose.setQy(poseQuat.get(2, 0));
			pose.setQz(poseQuat.get(3, 0));
			pose.setCx(IC.get(0, 3));
			pose.setCy(IC.get(1, 3));
			pose.setCz(IC.get(2, 3));
			po.pose = pose;
			pose.getHomogeneousMatrix().print(15, 5);

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
