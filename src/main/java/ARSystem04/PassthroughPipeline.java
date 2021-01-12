package ARSystem04;

import buffers.Buffer;
import buffers.FramePack;
import buffers.PipelineOutput;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
import toolbox.Utils;

public class PassthroughPipeline extends PoseEstimator {

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
		Utils.p("Hello world!");
	}

}
