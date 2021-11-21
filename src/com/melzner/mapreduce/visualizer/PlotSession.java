package com.melzner.mapreduce.visualizer;

class PlotSession {
    private final Graph graph;
    private int getAtCurrent = 1;

    PlotSession(Graph graph) {
        this.graph = graph;
    }

    Double getValueAt(double x) {
        double xMin = graph.points[0][0];
        double xMax = graph.points[graph.points.length - 1][0];
        if (x < xMin) {
            return 0d;
        } else if (x > xMax) {
            return graph.points[graph.points.length - 1][1];
        }

        while (getAtCurrent < graph.points.length && graph.points[getAtCurrent][0] < x) {
            getAtCurrent++;
        }

        return graph.points[getAtCurrent - 1][1];
    }
}
