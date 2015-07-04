package i5.las2peer.services.modelPersistenceService.models.node;

public class NodePosition {
  private int left;
  private int top;
  private int width;
  private int height;
  private int zIndex;

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
