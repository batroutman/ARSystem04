package ARSystem04;

import java.util.Random;

import buffers.Buffer;
import buffers.QueuedBuffer;
import buffers.SingletonBuffer;
import runtimevars.Parameters;
import types.Correspondence2D2D;
import types.Feature;
import types.FramePack;
import types.PipelineOutput;

public class PassthroughPipeline extends PoseEstimator {

	int frameNum = 0;
	double tz = 0;

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

			Random rand = new Random(100);

			for (int i = 0; i < 100; i++) {
				double x0 = rand.nextDouble() * Parameters.width;
				double y0 = rand.nextDouble() * Parameters.height;
				double x1 = rand.nextDouble() * Parameters.width;
				double y1 = rand.nextDouble() * Parameters.height;
				po.correspondences.add(new Correspondence2D2D(x0, y0, x1, y1));
				po.features.add(new Feature(x1, y1, (int) (15 * rand.nextDouble()) + 5));
			}

			this.tz -= 0.003;
			po.tz = this.tz;

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
