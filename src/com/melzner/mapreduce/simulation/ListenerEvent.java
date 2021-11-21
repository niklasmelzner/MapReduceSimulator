package com.melzner.mapreduce.simulation;

public abstract class ListenerEvent<LISTENER_EVENT_TYPE extends ListenerEventType> {
    LISTENER_EVENT_TYPE type;

    public LISTENER_EVENT_TYPE getType() {
        return type;
    }
}
