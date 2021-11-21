package com.melzner.mapreduce.visualizer;

import java.util.List;
import java.util.Objects;

class RenderData {

    final double xMin;
    final double xMax;
    final double yMin;
    final double yMax;

    RenderData(List<Drawable> drawables) {
        DoubleRect dimensions = null;
        for (Drawable drawable : drawables) {
            DoubleRect singleDimensions = drawable.getDimensions();
            if (singleDimensions != null) {
                if (dimensions == null) {
                    dimensions = singleDimensions;
                } else {
                    dimensions.expandBorders(singleDimensions);
                }
            }
        }
        if (dimensions == null) {
            dimensions = new DoubleRect(0, 0, 0, 0);
        }
        xMin = dimensions.x;
        xMax = dimensions.x + dimensions.width;
        yMin = dimensions.y;
        yMax = dimensions.y + dimensions.height;
    }

    RenderData(double xMin, double yMin, double xMax, double yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    @Override
    public int hashCode() {
        return Objects.hash(xMin, xMax, yMin, yMax);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RenderData that = (RenderData) o;
        return Double.compare(that.xMin, xMin) == 0 && Double.compare(that.xMax, xMax) == 0 && Double.compare(that.yMin, yMin) == 0 && Double.compare(that.yMax, yMax) == 0;
    }
}
