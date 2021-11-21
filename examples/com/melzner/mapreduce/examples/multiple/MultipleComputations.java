package com.melzner.mapreduce.examples.multiple;

import com.melzner.mapreduce.examples.TestComputationSimulation;
import com.melzner.mapreduce.examples.stragglers.Stragglers;
import com.melzner.mapreduce.scenario.ScenarioConfig;
import org.xml.sax.SAXException;

import java.io.IOException;

public class MultipleComputations extends TestComputationSimulation {

    public static void main(String[] args) throws IOException, SAXException {
        simulate("1 Computation", ScenarioConfig.load(MultipleComputations.class, SCENARIOS_PATH + "/multipleComputations1Computation.xml"),
                new Stragglers.StragglerExecutionModification());
        simulate("3 Computations", ScenarioConfig.load(MultipleComputations.class, SCENARIOS_PATH + "/multipleComputations3Computations.xml"),
                new Stragglers.StragglerExecutionModification());
        simulate("5 Computations", ScenarioConfig.load(MultipleComputations.class, SCENARIOS_PATH + "/multipleComputations5Computations.xml"),
                new Stragglers.StragglerExecutionModification());
    }
}
