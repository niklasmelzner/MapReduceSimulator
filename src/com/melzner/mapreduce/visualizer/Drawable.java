package com.melzner.mapreduce.visualizer;

import java.awt.*;

abstract class Drawable {

    abstract DoubleRect getDimensions();

    abstract void draw(Graphics2D gr2D, double dx, double dy, Rectangle rect);

}
