package ARSystem04;

import buffers.Buffer;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
import runtimevars.Parameters;
import types.Correspondence2D2D;
import types.FramePack;
import types.PipelineOutput;

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
			po.correspondences.add(new Correspondence2D2D(50, 50, 200, 256));
			po.correspondences.add(new Correspondence2D2D(0, 0, 45, 30));
			po.correspondences.add(new Correspondence2D2D(0, 0, Parameters.width - 10, Parameters.height - 10));

			try {
				Thread.sleep(33);
			} catch (Exception e) {
			}

			long end = System.currentTimeMillis();
			po.fps = 1000 / (end - start + 1);

			outputBuffer.push(po);

		}
	}

}
