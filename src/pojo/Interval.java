package pojo;

import java.io.Serializable;

public class Interval implements Serializable {
    private static final int DEFAULT_LO = 1;
    private static final int DEFAULT_HI = 10;

    private int lo;
    private int hi;

    public static Interval getDeFaultInterval() {
        return new Interval(DEFAULT_LO, DEFAULT_HI);
    }

    private Interval(int lo, int hi) {
        this.lo = lo;
        this.hi = hi;
    }

    public int getLo() {
        return lo;
    }

    public void setLo(int lo) {
        this.lo = lo;
    }

    public int getHi() {
        return hi;
    }

    public void setHi(int hi) {
        this.hi = hi;
    }

    public int getMid() {
        return (this.lo + this.hi) / 2;
    }
}
