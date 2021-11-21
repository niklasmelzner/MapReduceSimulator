package com.melzner.mapreduce.visualizer;

import java.awt.*;

class VerticalLine extends Drawable {

    private final double x;
    private final Color color;

    VerticalLine(double x, Color color) {
        this.x = x;
        this.color = color;
    }

    @Override
    DoubleRect getDimensions() {
        return null;
    }

    @Override
    void draw(Graphics2D gr2D, double dx, double dy, Rectangle rect) {
        gr2D.setColor(color);
        int xPix = (int) (dx * x);
        gr2D.drawLine(xPix, 0, xPix, rect.height);
    }
}
