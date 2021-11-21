package com.melzner.mapreduce.simulation;

public class SimulationEvent<T extends SimulationEventType> {

    private final T type;
    private boolean destroyed;

    public SimulationEvent(T type) {
        this.type = type;
    }

    public T getType() {
        return type;
    }

    @Override
    public String toString() {
        return "SimulationEvent{" + getType() + "}";
    }

    public void destroy() {
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
