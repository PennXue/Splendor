package project.view.splendor.communication;

public class Position {

  // indexing starting from zero
  private int x;
  private int y;

  public Position(int paramX, int paramY) {
    x = paramX;
    y = paramY;
  }

  public int getX() {
    return x;
  }

  public void setX(int x) {
    this.x = x;
  }

  public int getY() {
    return y;
  }

  public void setY(int y) {
    this.y = y;
  }
}