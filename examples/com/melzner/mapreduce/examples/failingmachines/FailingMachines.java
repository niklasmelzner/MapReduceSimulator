package com.melzner.mapreduce.examples.failingmachines;

import com.melzner.mapreduce.examples.TestComputationSimulation;
import com.melzner.mapreduce.scenario.ScenarioConfig;
import org.xml.sax.SAXException;

import java.io.IOException;

public class FailingMachines extends TestComputationSimulation {

    public static void main(String[] args) throws IOException, SAXException {
        simulate("Failing Machines", ScenarioConfig.load(FailingMachines.class, SCENARIOS_PATH + "/failingMachines.xml"));
    }

}
