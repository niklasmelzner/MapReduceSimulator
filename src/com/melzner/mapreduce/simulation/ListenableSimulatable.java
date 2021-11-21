package com.melzner.mapreduce.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class ListenableSimulatable<EVENT_TYPE extends SimulationEventType,
        LISTENER_EVENT_TYPE extends ListenerEventType,
        LISTENER_EVENT extends ListenerEvent<LISTENER_EVENT_TYPE>> extends Simulatable<EVENT_TYPE> {

    private final Map<LISTENER_EVENT_TYPE, List<Consumer<LISTENER_EVENT>>> listeners = new HashMap<>();

    protected ListenableSimulatable(ExecutionOrder executionOrder) {
        super(executionOrder);
    }

    public void addListener(LISTENER_EVENT_TYPE eventType, Consumer<LISTENER_EVENT> listener) {
        listeners.computeIfAbsent(eventType, e -> new ArrayList<>()).add(listener);
    }

    public void fireEvent(LISTENER_EVENT_TYPE eventType, LISTENER_EVENT event) {
        event.type = eventType;
        List<Consumer<LISTENER_EVENT>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            for (Consumer<LISTENER_EVENT> eventListener : eventListeners) {
                eventListener.accept(event);
            }
        }
    }
}
