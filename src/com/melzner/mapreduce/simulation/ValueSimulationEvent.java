package com.melzner.mapreduce.simulation;

public class ValueSimulationEvent<T extends SimulationEventType, V> extends SimulationEvent<T> {

    private final V value;

    public ValueSimulationEvent(T type, V value) {
        super(type);
        this.value = value;
    }

    public V getValue() {
        return value;
    }
}
