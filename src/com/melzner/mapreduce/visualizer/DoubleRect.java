package com.melzner.mapreduce.visualizer;

public class DoubleRect {
    public double x;
    public double y;
    public double width;
    public double height;

    public DoubleRect(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void expandBorders(DoubleRect dimensions) {
        if (dimensions.x < x) x = dimensions.x;
        if (dimensions.y < y) y = dimensions.y;
        if (dimensions.x + dimensions.width > x + width) width = dimensions.x + dimensions.width - x;
        if (dimensions.y + dimensions.height > y + height) height = dimensions.y + dimensions.height - y;
    }

    public static DoubleRect fromGraph(Graph graph) {
        return fromMinMax(graph.xMin, graph.yMin, graph.xMax, graph.yMax);
    }

    public static DoubleRect fromGraphs(Graph... graphs) {
        DoubleRect rect = fromGraph(graphs[0]);
        for (int i = 1; i < graphs.length; i++) rect.expandBorders(fromGraph(graphs[i]));
        return rect;
    }

    public static DoubleRect fromMinMax(double xMin, double yMin, double xMax, double yMax) {
        return new DoubleRect(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    @Override
    public String toString() {
        return "DoubleRect{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
