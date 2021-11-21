package com.melzner.mapreduce;

import com.melzner.mapreduce.examples.TestComputationSimulation;
import com.melzner.mapreduce.scenario.ScenarioConfig;
import org.xml.sax.SAXException;

import java.io.IOException;

public class TestConfig extends TestComputationSimulation {

    public static void main(String[] args) throws IOException, SAXException {

        simulate("simple", ScenarioConfig.load(TestConfig.class, "scenarios/simpleTest.xml"));
        simulate("failingMachines", ScenarioConfig.load(TestConfig.class, "scenarios/withFailingMachines.xml"));

    }

}
