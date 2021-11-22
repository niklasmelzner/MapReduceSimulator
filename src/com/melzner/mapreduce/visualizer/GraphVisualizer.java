package com.melzner.mapreduce.visualizer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GraphVisualizer {

    private static final int BORDER = 30, BORDER_TOP_RIGHT = 10;
    private static final float GRAPH_STROKE_WIDTH = 1.5f;
    private static final float COORDINATE_SYSTEM_STROKE_WIDTH = 1.5f;
    private static final float COORDINATE_SYSTEM_GRID_STROKE_WIDTH = 0.5f;
    private static final Color COORDINATE_SYSTEM_GRID_COLOR = Color.DARK_GRAY;
    private static final Color COORDINATE_SYSTEM_GRID_COLOR_2 = Color.GRAY;
    private static final int COORDINATE_SYSTEM_ADDITIONAL_LENGTH = 15;
    private static final int COORDINATE_SYSTEM_MARK_SIZE = 8;
    private static final int COORDINATE_SYSTEM_CROSS_SIZE = 7;
    private static final int SCALING_STEPS = 100;
    private static final Color COORDINATE_SYSTEM_COLOR = Color.BLACK;
    private final JFrame frame = new JFrame();
    private final List<Drawable> drawables = new ArrayList<>();
    private boolean showSecondaryGrid;
    private Function<Double, Double> xAxisUnitTransformation = i -> i;
    private Function<Double, Double> yAxisUnitTransformation = i -> i;
    private String xLabel = "", yLabel = "";

    public GraphVisualizer() {
        frame.setSize(900, 600);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.getContentPane().add(new Component() {
            @Override
            public void paint(Graphics g) {
                GraphVisualizer.this.paint(g, getWidth(), getHeight());
            }
        });
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
    }

    public void setSecondaryGridVisibility(boolean showSecondaryGrid) {
        this.showSecondaryGrid = showSecondaryGrid;
    }

    public void paint(Graphics g, int width, int height) {
        Graphics2D gr2D = (Graphics2D) g;
        Font font = gr2D.getFont();
        gr2D.setFont(new Font(font.getFamily(), Font.PLAIN, 13));
        drawGraphs(gr2D, new Rectangle(
                BORDER,
                BORDER_TOP_RIGHT + COORDINATE_SYSTEM_ADDITIONAL_LENGTH,
                width - BORDER - BORDER_TOP_RIGHT - COORDINATE_SYSTEM_ADDITIONAL_LENGTH,
                height - BORDER - BORDER_TOP_RIGHT - COORDINATE_SYSTEM_ADDITIONAL_LENGTH));
    }

    public void setYAxisUnitTransformation(Function<Double, Double> yAxisUnitTransformation) {
        this.yAxisUnitTransformation = yAxisUnitTransformation;
    }

    public GraphVisualizer setXAxisUnitTransformation(Function<Double, Double> xAxisUnitTransformation) {
        this.xAxisUnitTransformation = xAxisUnitTransformation;
        return this;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void drawGraphs(Graphics2D gr2D, Rectangle rect) {
        int xLabelWidth = gr2D.getFontMetrics().stringWidth(xLabel);
        rect.width -= xLabelWidth;
        int yLabelWidth = gr2D.getFontMetrics().stringWidth(yLabel);
        rect.y += yLabelWidth;
        rect.height -= yLabelWidth;


        RenderData renderData = new RenderData(drawables);

        double borderInPix = 10;

        double borderX = borderInPix * (renderData.xMax - renderData.xMin) / rect.width;
        double borderY = borderInPix * (renderData.yMax - renderData.yMin) / rect.height;

        double borderStart = 0, borderEnd = borderX, borderTop = borderY, borderBottom = 0;

        RenderData drawData = new RenderData(
                renderData.xMin - borderStart,
                renderData.yMin - borderBottom,
                renderData.xMax + borderEnd,
                renderData.yMax + borderTop
        );

        AffineTransform transform = AffineTransform.getTranslateInstance(+ rect.x, + rect.y);
        try (NonIOCloseable ignored = transform(gr2D, transform)) {

            gr2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawCoordinateSystem(gr2D, rect, drawData);

            gr2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            gr2D.setStroke(new BasicStroke(GRAPH_STROKE_WIDTH));

            double dx = 1.0 * rect.width / (drawData.xMax - drawData.xMin);
            double dy = 1.0 * rect.height / (drawData.yMax - drawData.yMin);


            AffineTransform graphTransform = AffineTransform.getScaleInstance(1, - 1);
            graphTransform.concatenate(AffineTransform.getTranslateInstance(
                    dx * borderStart,
                    dy * borderBottom - rect.height));

            try (NonIOCloseable ignored1 = transform(gr2D, graphTransform)) {
                Rectangle renderRect = new Rectangle(
                        0,
                        0,
                        (int) (rect.width - dx * (borderStart + borderEnd)) + 2,
                        (int) (rect.height - dy * (borderTop + borderBottom)) + 2
                );

                for (Drawable drawable : drawables) {
                    drawable.draw(gr2D, dx, dy, renderRect);
                }
            }
        }

    }

    private NonIOCloseable setStroke(Graphics2D gr2D, Stroke s) {
        Stroke originalStroke = gr2D.getStroke();
        gr2D.setStroke(s);

        return new NonIOCloseable() {
            @Override
            public void close() {
                gr2D.setStroke(originalStroke);
                super.close();
            }
        };
    }

    private NonIOCloseable setColor(Graphics2D gr2D, Color color) {
        Color originalColor = gr2D.getColor();
        gr2D.setColor(color);

        return new NonIOCloseable() {
            @Override
            public void close() {
                gr2D.setColor(originalColor);
                super.close();
            }
        };
    }

    private NonIOCloseable transform(Graphics2D gr2D, AffineTransform transform) {
        AffineTransform originalTransform = gr2D.getTransform();

        AffineTransform resultingTransform = new AffineTransform();
        resultingTransform.concatenate(originalTransform);
        resultingTransform.concatenate(transform);
        gr2D.setTransform(resultingTransform);
        return new NonIOCloseable() {
            @Override
            public void close() {
                gr2D.setTransform(originalTransform);
            }
        };
    }

    private void drawCoordinateSystem(Graphics2D gr2D, Rectangle rect, RenderData renderData) {
        gr2D.setColor(COORDINATE_SYSTEM_COLOR);
        gr2D.setStroke(new BasicStroke(COORDINATE_SYSTEM_STROKE_WIDTH));
        // x
        gr2D.drawLine(0, rect.height, rect.width + COORDINATE_SYSTEM_ADDITIONAL_LENGTH, rect.height);
        gr2D.drawLine(rect.width + COORDINATE_SYSTEM_ADDITIONAL_LENGTH, rect.height,
                rect.width + COORDINATE_SYSTEM_ADDITIONAL_LENGTH - COORDINATE_SYSTEM_CROSS_SIZE,
                rect.height - COORDINATE_SYSTEM_CROSS_SIZE);
        gr2D.drawLine(rect.width + COORDINATE_SYSTEM_ADDITIONAL_LENGTH, rect.height,
                rect.width + COORDINATE_SYSTEM_ADDITIONAL_LENGTH - COORDINATE_SYSTEM_CROSS_SIZE,
                rect.height + COORDINATE_SYSTEM_CROSS_SIZE);

        try (NonIOCloseable ignored = transform(gr2D, AffineTransform.getTranslateInstance(0, rect.height))) {
            drawAxisScaling(gr2D, rect.width, xLabel,
                    xAxisUnitTransformation.apply(renderData.xMin),
                    xAxisUnitTransformation.apply(renderData.xMax),
                    rect.height, false);
        }

        // y
        gr2D.drawLine(0, rect.height, 0, - COORDINATE_SYSTEM_ADDITIONAL_LENGTH);
        gr2D.drawLine(0, - COORDINATE_SYSTEM_ADDITIONAL_LENGTH,
                - COORDINATE_SYSTEM_CROSS_SIZE,
                - COORDINATE_SYSTEM_ADDITIONAL_LENGTH + COORDINATE_SYSTEM_CROSS_SIZE);
        gr2D.drawLine(0, - COORDINATE_SYSTEM_ADDITIONAL_LENGTH,
                + COORDINATE_SYSTEM_CROSS_SIZE,
                - COORDINATE_SYSTEM_ADDITIONAL_LENGTH + COORDINATE_SYSTEM_CROSS_SIZE);

        AffineTransform transform = new AffineTransform();
        //transform.concatenate(AffineTransform.getTranslateInstance(0, rect.height));
        transform.concatenate(AffineTransform.getRotateInstance(Math.PI / 2, 0, 0));
        try (NonIOCloseable ignored = transform(gr2D, transform)) {
            drawAxisScaling(gr2D, rect.height, yLabel,
                    yAxisUnitTransformation.apply(renderData.yMin),
                    yAxisUnitTransformation.apply(renderData.yMax),
                    rect.width, true);
        }
    }

    private void drawAxisScaling(Graphics2D gr2D, int length, String axisLabel, double valueMin, double valueMax,
                                 int gridLength, boolean invertX) {
        double valueRange = valueMax - valueMin;

        int stringHeight = gr2D.getFontMetrics().getHeight();

        double unit = getScaleUnit(valueRange * SCALING_STEPS / length);

        double valueStart = (int) (valueMin / unit) * unit;
        if (valueStart < valueMin) {
            valueStart += unit;
        }

        int maxDecimalPlaces = getToStringDecimalPlaces(unit);

        for (double value = valueStart; value < valueMax; value += unit) {
            int x = (int) ((value - valueMin) * length / valueRange);

            String label = toStringWithMaximalDecimalPlaces(value, maxDecimalPlaces);

            int stringWidth = gr2D.getFontMetrics().stringWidth(label);

            if (invertX) {
                x = length - x;
            }
            gr2D.drawLine(x, - COORDINATE_SYSTEM_MARK_SIZE / 2, x, COORDINATE_SYSTEM_MARK_SIZE / 2);
            gr2D.drawString(label, x - stringWidth / 2, stringHeight + 3);
            try (NonIOCloseable ignored = setStroke(gr2D, new BasicStroke(COORDINATE_SYSTEM_GRID_STROKE_WIDTH))) {
                try (NonIOCloseable ignored1 = setColor(gr2D, COORDINATE_SYSTEM_GRID_COLOR)) {
                    gr2D.drawLine(x, 0, x, - gridLength);
                }
                if (showSecondaryGrid) {
                    try (NonIOCloseable ignored1 = setColor(gr2D, COORDINATE_SYSTEM_GRID_COLOR_2)) {
                        int x1 = (int) ((value + unit / 2 - valueMin) * length / valueRange);
                        if (invertX) x1 = length - x1;
                        gr2D.drawLine(x1, 0, x1, - gridLength);
                    }
                }
            }
        }

        int stringWidth = gr2D.getFontMetrics().stringWidth(axisLabel);

        gr2D.drawString(axisLabel, invertX ? - stringWidth - COORDINATE_SYSTEM_ADDITIONAL_LENGTH : length
                + COORDINATE_SYSTEM_ADDITIONAL_LENGTH, stringHeight + 3);

    }

    private String toStringWithMaximalDecimalPlaces(double value, int maxDecimalPlaces) {
        if (maxDecimalPlaces == 0) {
            return "" + (int) value;
        }
        String valueString = "" + value;
        int i = valueString.indexOf(".");
        if (i != - 1 && valueString.length() - 1 - i > maxDecimalPlaces) {
            return ("" + value).substring(0, i + 1 + maxDecimalPlaces);
        }
        return "" + value;
    }

    private int getToStringDecimalPlaces(double value) {
        String valueString = "" + value;
        int i = valueString.indexOf(".");
        if (i == 0) return - 1;
        else {
            int places = valueString.length() - 1 - i;
            if (places == 1 && (int) value == value) {
                return 0;
            }
            return places;
        }
    }

    private double getScaleUnit(double v) {
        double unit = 0;
        double minDistance = - 1;
        for (int i : new int[]{1, 2, 5}) {
            int lowerPower = (int) Math.floor(Math.log(v / i) / Math.log(10));
            double lowerUnit = i * Math.pow(10, lowerPower);
            double upperUnit = i * Math.pow(10, lowerPower + 1);
            double lowerDistance = Math.abs(v - lowerUnit);
            double upperDistance = Math.abs(v - upperUnit);
            if (minDistance == - 1 || minDistance > lowerDistance) {
                minDistance = lowerDistance;
                unit = lowerUnit;
            }
            if (minDistance > upperDistance) {
                minDistance = upperDistance;
                unit = upperUnit;
            }
        }

        return unit;
    }

    public void show() {
        frame.setVisible(false);
        frame.setVisible(true);
    }

    public GraphVisualizer addGraph(Graph graph) {
        return addGraph(graph, Color.BLACK);
    }

    public GraphVisualizer addGraph(Graph graph, Color color) {
        drawables.add(new SimpleGraph(graph, color));
        return this;
    }

    public GraphVisualizer addStackedGraphs(Graph[] graphs, Color... colors) {
        drawables.add(new StackedGraph(graphs, colors));
        return this;
    }

    public GraphVisualizer addHorizontalLine(double y, Color color) {
        drawables.add(new HorizontalLine(y, color));
        return this;
    }

    public GraphVisualizer addVerticalLine(double x, Color color) {
        drawables.add(new VerticalLine(x, color));
        return this;
    }

    public GraphVisualizer setXLabel(String xLabel) {
        this.xLabel = xLabel;
        return this;
    }

    public GraphVisualizer setYLabel(String yLabel) {
        this.yLabel = yLabel;
        return this;
    }

    public GraphVisualizer setTitle(String title) {
        frame.setTitle(title);
        return this;
    }

}
