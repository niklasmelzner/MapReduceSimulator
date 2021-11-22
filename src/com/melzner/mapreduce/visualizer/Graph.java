package com.melzner.mapreduce.visualizer;

import java.util.*;
import java.util.function.Function;

public class Graph {

    public final double xMin, xMax, yMin, yMax;
    final double[][] points;

    Graph(double[][] points) {
        this.points = points;
        xMin = points[0][0];
        xMax = points[points.length - 1][0];

        double yMin = points[0][1], yMax = points[0][1];

        for (double[] point : points) {
            double y = point[1];
            if (y < yMin) yMin = y;
            else if (y > yMax) yMax = y;
        }

        this.yMin = yMin;
        this.yMax = yMax;
    }

    public static <T> Graph create(Collection<T> elements, Function<T, double[]> pointMapping) {
        if (elements == null) {
            return new Graph(new double[][]{{0, 0}, {0, 0}});
        }
        double[][] points = new double[elements.size()][];
        Iterator<T> iterator = elements.iterator();
        for (int i = 0; i < points.length; i++) {
            points[i] = pointMapping.apply(iterator.next());
        }
        Arrays.sort(points, Comparator.comparingDouble(o -> o[0]));
        return new Graph(points);
    }

    public Graph add(Graph other) {
        List<double[]> points = new ArrayList<>();

        int indexThis = 0, indexOther = 0;
        double yLastThis = 0, yLastOther = 0;

        while (indexThis < this.points.length && indexOther < other.points.length) {
            double xThis = this.points[indexThis][0];
            double xOther = other.points[indexOther][0];

            if (xThis == xOther) {
                yLastThis = this.points[indexThis][1];
                yLastOther = other.points[indexOther][1];
                points.add(new double[]{xThis, yLastThis + yLastOther});
                indexThis++;
                indexOther++;
            } else if (xThis < xOther) {
                yLastThis = this.points[indexThis][1];
                points.add(new double[]{xThis, yLastThis + yLastOther});
                indexThis++;
            } else {
                yLastOther = other.points[indexOther][1];
                points.add(new double[]{xOther, yLastThis + yLastOther});
                indexOther++;
            }
        }
        for (; indexThis < this.points.length; indexThis++) {
            double[] point = this.points[indexThis];
            points.add(new double[]{point[0], point[1] + yLastOther});
        }
        for (; indexOther < other.points.length; indexOther++) {
            double[] point = other.points[indexOther];
            points.add(new double[]{point[0], yLastThis + point[1]});
        }

        return new Graph(points.toArray(new double[0][]));
    }

    public double calculateArea() {
        return calculateArea(xMin - 1, xMax + 1);
    }

    public double calculateArea(double min, double max) {

        double area = 0;
        for (int i = 0; i < points.length - 1; i++) {
            double[] p = points[i];
            double[] p1 = points[i + 1];
            if (p[0]>=min && p1[0]<max){
                area += p1[1] * (p1[0] - p[0]);
            }
        }
        return area;
    }
}
