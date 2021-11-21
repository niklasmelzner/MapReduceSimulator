package com.melzner.mapreduce.examples.singletask;

import com.melzner.mapreduce.simulation.*;
import com.melzner.mapreduce.visualizer.Graph;

import java.util.concurrent.TimeUnit;

public class SingleTaskExample {

    public static void main(String[] args) {
        Simulation simulation = new Simulation();

        simulation.add(new Machine(
                TimeUnit.SECONDS.toNanos(1),
                TimeUnit.SECONDS.toNanos(6)
        ));

        runBusyMachineSimulation(simulation);

    }

    protected static void runBusyMachineSimulation(Simulation simulation) {
        SimulationResult result = simulation.run();

        Graph graph = Graph.create(result.getRecordHistory().get(RecordType.BUSY_MACHINES),
                entry -> new double[]{entry.getTimestamp(), (Integer) entry.getValue()});

        Simulation.newVisualizer("Single Task Example")
                .addGraph(graph)
                .setYLabel("busy")
                .show();
    }

    protected static class Machine extends Simulatable<Machine.MachineEvent> {
        private final long start;
        private final long duration;
        private boolean busy;

        public Machine(long start, long duration) {
            this.start = start;
            this.duration = duration;
        }

        @Override
        protected void startSimulation() {
            invokeEvent(start, new SimulationEvent<>(MachineEvent.START_TASK));
            updateRecordValue(RecordType.BUSY_MACHINES, i -> 0, 0);
        }

        @Override
        protected void onSimulationEvent(SimulationEvent<MachineEvent> event) {
            if (event.getType() == MachineEvent.START_TASK) {
                invokeEvent(duration, new SimulationEvent<>(MachineEvent.TASK_DONE));
                busy = true;
                updateRecordValue(RecordType.BUSY_MACHINES, i -> i + 1, 0);
            } else if (event.getType() == MachineEvent.TASK_DONE) {
                busy = false;
                updateRecordValue(RecordType.BUSY_MACHINES, i -> i - 1, 0);
            }
        }

        private enum MachineEvent implements SimulationEventType {
            START_TASK, TASK_DONE
        }
    }

    private enum RecordType {
        BUSY_MACHINES
    }

}
