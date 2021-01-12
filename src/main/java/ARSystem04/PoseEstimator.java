package ARSystem04;

import buffers.Buffer;
import buffers.FramePack;
import buffers.PipelineOutput;

public abstract class PoseEstimator {

	protected Buffer<FramePack> inputBuffer = null;
	protected Buffer<PipelineOutput> outputBuffer = null;

	public PoseEstimator(Buffer<FramePack> inputBuffer, Buffer<PipelineOutput> outputBuffer) {
		this.inputBuffer = inputBuffer;
		this.outputBuffer = outputBuffer;
	}

	public abstract void start();

	public abstract void stop();

	public Buffer<FramePack> getInputBuffer() {
		return inputBuffer;
	}

	public void setInputBuffer(Buffer<FramePack> inputBuffer) {
		this.inputBuffer = inputBuffer;
	}

	public Buffer<PipelineOutput> getOutputBuffer() {
		return outputBuffer;
	}

	public void setOutputBuffer(Buffer<PipelineOutput> outputBuffer) {
		this.outputBuffer = outputBuffer;
	}

}
