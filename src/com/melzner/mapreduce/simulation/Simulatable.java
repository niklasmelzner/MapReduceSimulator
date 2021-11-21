package com.melzner.mapreduce.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class Simulatable<EVENT_TYPE extends SimulationEventType> {

    private final ExecutionOrder executionOrder;
    private final List<Runnable> simulationStartListeners = new ArrayList<>();
    private Simulation.SimulationSession simulationSession;
    private boolean isBase;

    protected Simulatable() {
        this(ExecutionOrder.DEFAULT);
    }

    protected Simulatable(ExecutionOrder executionOrder) {
        this.executionOrder = executionOrder;
    }

    public ExecutionOrder getExecutionOrder() {
        return executionOrder;
    }

    public void invokeAction(long delay, Runnable runnable) {
        simulationSession.invokeAction(delay, runnable);
    }

    public Integer getEventTypeCount(SimulationEventType eventType) {
        return simulationSession.getEventCount(eventType);
    }

    public void invokeEvent(long delay, SimulationEvent<EVENT_TYPE> event) {
        simulationSession.invokeEvent(this, delay, event);
    }

    public <T> void updateRecordValue(Object key, Function<T, T> transformation, T def) {
        simulationSession.updateRecordValue(key, transformation, def);
    }

    private Object getRecordValue(Object key) {
        return simulationSession.getRecordValue(key);
    }

    protected void addSimulationStartListener(Runnable listener) {
        simulationStartListeners.add(listener);
    }

    protected abstract void startSimulation();

    protected abstract void onSimulationEvent(SimulationEvent<EVENT_TYPE> event);

    protected void onAdd(Simulation simulation) {

    }

    protected long getTimeStamp() {
        return simulationSession.getTimestamp();
    }

    public void setSimulationSession(Simulation.SimulationSession simulationSession) {
        this.simulationSession = simulationSession;
    }

    protected Simulatable<EVENT_TYPE> newInitialInstance() {
        throw new UnsupportedOperationException();
    }

    void setIsBase(boolean isBase) {
        this.isBase = isBase;
    }

    boolean isBase() {
        return isBase;
    }

    public void onSimulationStart() {
        for (Runnable listener : simulationStartListeners) {
            listener.run();
        }
    }
}
