package com.melzner.mapreduce.simulation;

public class SimulationState {

    private final long timeStamp;

    SimulationState(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

}
