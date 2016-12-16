package i5.las2peer.services.modelPersistenceService.model.node;

/**
 * 
 * (Data-)Class for Node positions. Stores SyncMeta node position values.
 * 
 */
public class NodePosition {
	private int left;
	private int top;
	private int width;
	private int height;
	private int zIndex;

	/**
	 * 
	 * Creates a new NodePosition. Please see the SyncMeta source code for
	 * additional explanation of the arguments' semantics.
	 * 
	 * @param left
	 *            left argument
	 * @param top
	 *            top argument
	 * @param width
	 *            width argument
	 * @param height
	 *            height argument
	 * @param zIndex
	 *            zIndex argument
	 * 
	 */
	public NodePosition(int left, int top, int width, int height, int zIndex) {
		this.left = left;
		this.top = top;
		this.width = width;
		this.height = height;
		this.zIndex = zIndex;
	}

	public int getLeft() {
		return left;
	}

	public int getTop() {
		return top;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getzIndex() {
		return zIndex;
	}

}
