package com.melzner.mapreduce.simulation;

public interface ExecutionOrder {
    ExecutionOrder DEFAULT = () -> 0;

    double getOrder();

}
