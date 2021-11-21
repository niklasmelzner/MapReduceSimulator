package com.melzner.mapreduce.visualizer;

import java.awt.*;

class SimpleGraph extends InternalGraph {

    private final Graph graph;
    private final Color color;

    SimpleGraph(Graph graph, Color color) {
        this.graph = graph;
        this.color = color;
    }

    @Override
    DoubleRect getDimensions() {
        return DoubleRect.fromGraph(graph);
    }

    @Override
    public void draw(Graphics2D gr2D, double dx, double dy, Rectangle rect) {
        PointBuffer points = generatePointBuffer(graph, dx, dy, rect, stepsMax);

        gr2D.setColor(color);
        gr2D.draw(points.toPath());
    }

}
