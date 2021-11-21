package com.melzner.mapreduce.visualizer;

import java.awt.*;

class HorizontalLine extends Drawable {

    private final double y;
    private final Color color;

    HorizontalLine(double y, Color color) {
        this.y = y;
        this.color = color;
    }

    @Override
    DoubleRect getDimensions() {
        return null;
    }

    @Override
    void draw(Graphics2D gr2D, double dx, double dy, Rectangle rect) {
        gr2D.setColor(color);
        int yPix = (int) (dy * y);
        gr2D.drawLine(0, yPix, rect.width, yPix);
    }
}
