package com.melzner.mapreduce.visualizer;

import java.awt.*;
import java.awt.geom.Path2D;

class StackedGraph extends InternalGraph {

    private final DoubleRect dimensions;
    private final Color[] colors;
    private final Graph[] graphs;
    private final Graph[] stackedGraphs;

    StackedGraph(Graph[] graphs, Color[] colors) {
        dimensions = DoubleRect.fromGraph(graphs[0]);
        dimensions.expandBorders(new DoubleRect(dimensions.x, 0, dimensions.width, 0));
        this.colors = colors;
        this.graphs = graphs;

        stackedGraphs = new Graph[graphs.length];
        stackedGraphs[0] = this.graphs[0];
        for (int i = 1; i < graphs.length; i++) {
            stackedGraphs[i] = stackedGraphs[i - 1].add(graphs[i]);
            dimensions.expandBorders(DoubleRect.fromGraph(stackedGraphs[i]));
        }
    }

    @Override
    DoubleRect getDimensions() {
        return dimensions;
    }

    @Override
    public void draw(Graphics2D gr2D, double dx, double dy, Rectangle rect) {
        PointBuffer[] graphPoints = new PointBuffer[graphs.length];
        for (int i = 0; i < graphs.length; i++) {
            graphPoints[i] = generatePointBuffer(graphs[i], dx, dy, rect, stepsMax);
        }
        PointBuffer points = graphPoints[0];
        Path2D[] paths = new Path2D[graphs.length];
        for (int i = 0; i < graphs.length; i++) {
            if (i != 0) {
                points.addY(graphPoints[i]);
            }
            double[] minMax = new double[2];
            Path2D path = points.toPath(minMax);

            path.lineTo(minMax[1], 0);
            path.lineTo(minMax[0], 0);
            path.closePath();

            paths[i] = path;
        }

        for (int i = paths.length - 1; i >= 0; i--) {
            gr2D.setColor(colors[i]);
            gr2D.fill(paths[i]);
        }
    }
}
