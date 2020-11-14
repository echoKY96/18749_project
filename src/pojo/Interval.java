package pojo;

import java.io.Serializable;

public class Interval implements Serializable {
    private int left;
    private int right;

    public Interval() {
        this.left = 1;
        this.right = 10;
    }

    public Interval(int left, int right) {
        this.left = left;
        this.right = right;
    }

    public int getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public void setRight(int right) {
        this.right = right;
    }
}
