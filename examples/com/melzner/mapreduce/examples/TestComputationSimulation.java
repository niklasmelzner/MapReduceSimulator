package com.melzner.mapreduce.examples;

import com.melzner.mapreduce.cluster.Machine;
import com.melzner.mapreduce.computation.SimpleComputation.MapTaskState;
import com.melzner.mapreduce.scenario.Scenario;
import com.melzner.mapreduce.scenario.ScenarioConfig;
import com.melzner.mapreduce.scenario.SimpleComputationConfig;
import com.melzner.mapreduce.simulation.RecordHistoryEntry;
import com.melzner.mapreduce.simulation.Simulation;
import com.melzner.mapreduce.simulation.SimulationResult;
import com.melzner.mapreduce.visualizer.Graph;
import com.melzner.mapreduce.visualizer.GraphVisualizer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.melzner.mapreduce.computation.SimpleComputation.ReduceTaskState;

public abstract class TestComputationSimulation {
    protected static final String SCENARIOS_PATH = "/com/melzner/mapreduce/examples/scenarios";

    protected static final Function<RecordHistoryEntry, double[]> INT_ENTRY_TO_POINT_TRANSFORMATION = entry -> new double[]{entry.getTimestamp(), (Integer) entry.getValue()};

    protected static void simulate(String name, ScenarioConfig config) {
        simulate(name, config, new ExecutionModification() {
        });
    }

    protected static void simulate(String name, ScenarioConfig config, ExecutionModification modification) {
        Function<RecordHistoryEntry, double[]> transformation = INT_ENTRY_TO_POINT_TRANSFORMATION;

        System.out.println("starting simulation '" + name + "'");
        SimulationResult result = new Scenario(config).simulate();

        long timestampFinished = result.getDuration();

        Map<Object, List<RecordHistoryEntry>> record = result.getRecordHistory();

        Color colorIdle = new Color(250, 30, 30);
        Color colorInProgress = new Color(255, 255, 30);
        Color colorDone = new Color(30, 255, 30);

        Color[] colors = {
                colorIdle,
                colorInProgress,
                colorInProgress,
                colorDone,
                colorIdle,
                colorInProgress,
                colorInProgress,
                colorDone,
        };
        List<Object> states = new ArrayList<>();
        Collections.addAll(states, ReduceTaskState.values());
        Collections.addAll(states, MapTaskState.values());
        Graph[] graphs = new Graph[states.size()];

        int totalOutput = 0;
        for (SimpleComputationConfig sC : config.simpleComputations) {
            totalOutput += sC.outputSplitSize.get();
        }
        for (int i = 0; i < states.size(); i++) {
            graphs[i] = Graph.create(record.get(states.get(i)), transformation);
        }

        Simulation.newVisualizer(name + ": Task Status")
                .addVerticalLine(timestampFinished, Color.GRAY)
                .addStackedGraphs(graphs, colors)
                .addHorizontalLine(totalOutput, Color.BLACK)
                .show();
        Graph runningMachines = Graph.create(record.get(Machine.RecordType.RUNNING_MACHINES), transformation);

        System.out.println("Computation duration: " + TimeUnit.NANOSECONDS.toMillis(result.getDuration()) / 1000.0 + " s");
        System.out.println("Integrated RunningMachines graph: " + TimeUnit.NANOSECONDS.toSeconds((long) runningMachines.calculateArea()) + " machines*s");

        GraphVisualizer runningMachinesVisualizer = Simulation.newVisualizer(name + ": Running Machines")
                .addVerticalLine(timestampFinished, Color.GRAY)
                .addGraph(Graph.create(record.get(Machine.RecordType.ACTIVE_MACHINES), transformation), Color.RED)
                .addGraph(runningMachines, Color.BLACK);

        modification.modifyRunningMachinesVisualizer(runningMachinesVisualizer, result);
        runningMachinesVisualizer.show();
    }

    protected static class ExecutionModification {

        protected void modifyRunningMachinesVisualizer(GraphVisualizer visualizer, SimulationResult result) {

        }

    }

}
