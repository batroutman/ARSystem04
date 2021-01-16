package types;

public class Feature {

	protected double x = 0;
	protected double y = 0;
	protected double patchSize = 7;

	public Feature() {

	}

	public Feature(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Feature(double x, double y, double patchSize) {
		this.x = x;
		this.y = y;
		this.patchSize = patchSize;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getPatchSize() {
		return patchSize;
	}

	public void setPatchSize(double patchSize) {
		this.patchSize = patchSize;
	}

}
