package com.melzner.xmlutil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class XMLValue<T> {

    private static final Random random = new Random();
    final Function<String, T> customTransformation;
    private final XMLElement parent;
    private final List<Function<T, T>> getTransformations = new ArrayList<>();
    private T value;
    private String name;

    public XMLValue(T def, XMLElement parent) {
        this(def, parent, null);
    }

    public XMLValue(T def, Function<String, T> customTransformation) {
        this(def, null, customTransformation);
    }

    public XMLValue(T def, XMLElement parent, Function<String, T> customTransformation) {
        value = def;
        this.parent = parent;
        this.customTransformation = customTransformation;
    }

    public XMLValue<T> set(T value) {
        this.value = value;
        return this;
    }

    public T get() {
        T value = this.value;
        for (Function<T, T> getTransformation : getTransformations) {
            value = getTransformation.apply(value);
        }
        return value;
    }

    XMLElement getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return "XMLValue{" + (name != null ? name + " = " : "") + value + '}';
    }

    void setName(String name) {
        this.name = name;
    }

    public XMLValue<T> applyCustomTransformation(String v) {
        set(customTransformation.apply(v));
        return this;
    }

    @SuppressWarnings("unchecked")
    public void applyRandom(double rMin, double rMax) {
        gaussianBetween(rMin, rMax);
        if (value instanceof Integer) {
            getTransformations.add(t -> (T) (Integer) ((Double) ((Integer) t * gaussianBetween(rMin, rMax))).intValue());
        } else if (value instanceof Double) {
            getTransformations.add(t -> (T) (Double) ((Double) t * gaussianBetween(rMin, rMax)));
        } else if (value instanceof Long) {
            getTransformations.add(t -> (T) (Long) ((Double) ((Long) t * gaussianBetween(rMin, rMax))).longValue());
        } else {
            throw new UnsupportedOperationException("cannot apply random to value of " + value.getClass());
        }
    }

    public static double gaussianBetween(double min, double max) {
        double d = Math.max(Math.min(1.0, (random.nextGaussian() / 4 + 0.5)), 0.0);
        return min + d * (max - min);
    }
}
