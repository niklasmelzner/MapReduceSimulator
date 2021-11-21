package com.melzner.mapreduce.visualizer;

import java.awt.geom.Path2D;

class PointBuffer {
    private static final double[] UNUSED_MIN_MAX_BUFFER = new double[2];
    private final double[] bufferX;
    private final double[] bufferY;
    private int current = 0;

    PointBuffer(int capacity) {
        bufferX = new double[capacity];
        bufferY = new double[capacity];
    }

    void add(double x, double y) {
        bufferX[current] = x;
        bufferY[current++] = y;
    }

    public Path2D toPath() {
        return toPath(UNUSED_MIN_MAX_BUFFER);
    }

    public Path2D toPath(double[] minMax) {
        Path2D path = new Path2D.Double();
        int c = 0;
        while (bufferY[c] == Double.POSITIVE_INFINITY) {
            c++;
        }
        minMax[0] = bufferX[c];
        path.moveTo(bufferX[c++], bufferY[0]);
        for (; c < current; c++) {
            if (bufferY[c] != Double.POSITIVE_INFINITY) {
                path.lineTo(minMax[1] = bufferX[c], bufferY[c - 1]);
                path.lineTo(minMax[1] = bufferX[c], bufferY[c]);
            }
        }
        return path;
    }

    public void addY(PointBuffer other) {
        if (other.current != current) throw new IllegalArgumentException();
        for (int i = 0; i < current; i++) {
            double v1 = bufferY[i];
            double v2 = other.bufferY[i];
            if (v1 == Double.POSITIVE_INFINITY) {
                if (v2 != Double.POSITIVE_INFINITY) {
                    bufferY[i] = v2;
                }
            } else if (v2 != Double.POSITIVE_INFINITY) {
                bufferY[i] = v1 + v2;
            }

        }
    }
}
