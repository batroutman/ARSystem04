package buffers;

public class SingletonBuffer<T> implements Buffer<T> {

	protected T payload = null;

	public SingletonBuffer() {

	}

	public void push(T payload) {
		this.payload = payload;

	}

	public T getNext() {
		return this.payload;
	}

}
