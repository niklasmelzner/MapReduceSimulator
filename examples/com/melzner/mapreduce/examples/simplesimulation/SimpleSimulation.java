package com.melzner.mapreduce.examples.simplesimulation;

import com.melzner.mapreduce.examples.TestComputationSimulation;
import com.melzner.mapreduce.scenario.ScenarioConfig;
import org.xml.sax.SAXException;

import java.io.IOException;

public class SimpleSimulation extends TestComputationSimulation {

    public static void main(String[] args) throws IOException, SAXException {
        simulate("Simulation 1", ScenarioConfig.load(SimpleSimulation.class, SCENARIOS_PATH + "/simpleSimulation.xml"));
    }
}
