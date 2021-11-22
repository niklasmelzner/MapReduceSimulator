package com.melzner.mapreduce.examples.stragglers;

import com.melzner.mapreduce.cluster.Machine;
import com.melzner.mapreduce.examples.TestComputationSimulation;
import com.melzner.mapreduce.scenario.ScenarioConfig;
import com.melzner.mapreduce.simulation.RecordHistoryEntry;
import com.melzner.mapreduce.simulation.SimulationResult;
import com.melzner.mapreduce.visualizer.Graph;
import com.melzner.mapreduce.visualizer.GraphVisualizer;
import org.xml.sax.SAXException;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class Stragglers extends TestComputationSimulation {

    public static void main(String[] args) throws IOException, SAXException {
        simulate("Stragglers", ScenarioConfig.load(Stragglers.class, SCENARIOS_PATH + "/stragglers.xml"), new StragglerExecutionModification());
        simulate("Straggler Compensation", ScenarioConfig.load(Stragglers.class, SCENARIOS_PATH + "/compensateStragglers.xml"), new StragglerExecutionModification());
    }

    public static class StragglerExecutionModification extends ExecutionModification {

        @Override
        protected void modifyRunningMachinesVisualizer(GraphVisualizer visualizer, SimulationResult result) {
            List<RecordHistoryEntry> runningStragglers = result.getRecordHistory().get(Machine.RecordType.RUNNING_STRAGGLERS);

            Graph graph = Graph.create(runningStragglers, INT_ENTRY_TO_POINT_TRANSFORMATION);
            visualizer.addGraph(graph, Color.BLUE);
        }

    }

}
