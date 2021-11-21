package com.melzner.mapreduce.visualizer;

import java.awt.*;

abstract class InternalGraph extends Drawable {
    final int stepsMax = 1000;

    PointBuffer generatePointBuffer(Graph graph, double dx, double dy, Rectangle rect, int stepsMax) {
        int steps = Math.min(rect.width, stepsMax);
        double dx1 = 1.0 * rect.width / steps;

        PointBuffer points = new PointBuffer(5 + (int) (rect.width / dx1));

        PlotSession plotSession = new PlotSession(graph);
        for (double x = 0; x < rect.width; x += dx1) {
            double vX = x / dx;
            Double vY = plotSession.getValueAt(vX);
            if (vY != Double.POSITIVE_INFINITY) {
                points.add(vX * dx, vY * dy);
            } else {
                points.add(vX * dx, Double.POSITIVE_INFINITY);
            }
        }

        return points;
    }
}
