//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.google.zxing.common;

import com.google.zxing.ResultPoint;

public class DetectorResult {
    private final BitMatrix bits;
    private final ResultPoint[] points;
    private int state = -1;

    public DetectorResult(BitMatrix bits, ResultPoint[] points) {
        this.bits = bits;
        this.points = points;
    }

    public DetectorResult(BitMatrix bits, ResultPoint[] points,int state) {
        this.bits = bits;
        this.points = points;
        this.state = state;
    }

    //modified
    //
    public final int getState(){return this.bits.getValue();}

    public final BitMatrix getBits() {
        return this.bits;
    }

    public final ResultPoint[] getPoints() {
        return this.points;
    }
}
