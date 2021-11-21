package com.melzner.mapreduce.scenario;

import com.melzner.mapreduce.cluster.Cluster;
import com.melzner.mapreduce.computation.SimpleComputation;
import com.melzner.mapreduce.simulation.Simulation;
import com.melzner.mapreduce.simulation.SimulationResult;

public class Scenario {

    private final ScenarioConfig config;

    public Scenario(ScenarioConfig config) {
        this.config = config;
    }


    public SimulationResult simulate() {
        Simulation simulation = new Simulation();

        Cluster cluster = new Cluster(config.clusterConfig);
        simulation.add(cluster);

        for (SimpleComputationConfig config : config.simpleComputations) {
            simulation.add(new SimpleComputation(cluster, config));
        }

        return simulation.run();
    }
}
