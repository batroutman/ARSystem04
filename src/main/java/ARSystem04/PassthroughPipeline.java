package ARSystem04;

import buffers.Buffer;
import buffers.FramePack;
import buffers.PipelineOutput;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;

public class PassthroughPipeline extends PoseEstimator {

	int frameNum = 0;

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
			FramePack newFrame = inputBuffer.getNext();
			if (newFrame == null) {
				keepGoing = false;
				continue;
			}

			PipelineOutput po = new PipelineOutput();
			po.frameNum = this.frameNum++;
			po.rawFrame = newFrame.getRawFrame();
			outputBuffer.push(po);
			try {
				Thread.sleep(33);
			} catch (Exception e) {
			}

		}
	}

}
