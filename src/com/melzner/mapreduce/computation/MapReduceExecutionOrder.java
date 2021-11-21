package com.melzner.mapreduce.computation;

import com.melzner.mapreduce.simulation.ExecutionOrder;

public enum MapReduceExecutionOrder implements ExecutionOrder {
    CLUSTER(1), COMPUTATION(2), MACHINE(0);

    private final double order;

    MapReduceExecutionOrder(double order) {
        this.order = order;
    }

    @Override
    public double getOrder() {
        return order;
    }
}
