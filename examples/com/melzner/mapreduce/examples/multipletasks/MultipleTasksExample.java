package com.melzner.mapreduce.examples.multipletasks;

import com.melzner.mapreduce.examples.singletask.SingleTaskExample;
import com.melzner.mapreduce.simulation.Simulation;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MultipleTasksExample extends SingleTaskExample {

    private static final Random random = new Random();

    public static void main(String[] args) {
        Simulation simulation = new Simulation();

        for (int i = 0; i < 1000; i++) {
            simulation.add(new Machine(
                    (long) (TimeUnit.SECONDS.toNanos(1) * gaussianBetween(1, 3, 0, 4)),
                    (long) (TimeUnit.SECONDS.toNanos(1) * gaussianBetween(6, 9, 5, 8))
            ));
        }

        runBusyMachineSimulation(simulation);
    }

    public static double gaussianBetween(double min, double max) {
        return gaussianBetween(min, max, min, max);
    }

    public static double gaussianBetween(double min, double max, double clipMin, double clipMax) {
        double d = (random.nextGaussian() / 4 + 0.5);
        return Math.max(clipMin, Math.min(min + d * (max - min), clipMax));
    }
}
