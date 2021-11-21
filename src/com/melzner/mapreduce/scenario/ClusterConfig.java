package com.melzner.mapreduce.scenario;

import com.melzner.xmlutil.MapXML;
import com.melzner.xmlutil.XMLValue;

public class ClusterConfig {
    @MapXML("computationSpeed")
    public final XMLValue<Double> computationSpeed = new XMLValue<>(1.0, ScenarioConfig.CLUSTER);
    @MapXML("machines")
    public final XMLValue<Integer> machineCount = new XMLValue<>(100, ScenarioConfig.CLUSTER);
    @MapXML("maxSingleDiskSize")
    public final XMLValue<Long> maxSingleDiskSize = new XMLValue<>(1024L * 1024 * 1024 * 1024 * 1024 * 1024, ScenarioConfig.CLUSTER, ScenarioConfig::parseDataSize);
    @MapXML("blockSize")
    public final XMLValue<Long> dfsBlockSize = new XMLValue<>(64L * 1024 * 1024, ScenarioConfig.DFS, ScenarioConfig::parseDataSize);
    @MapXML("machineFailProbability")
    public final XMLValue<Double> machineFailProbability = new XMLValue<>(0.01, ScenarioConfig.CLUSTER);
    @MapXML("stragglingFactor")
    public final XMLValue<Double> stragglingFactor = new XMLValue<>(1.0, ScenarioConfig.CLUSTER);
    @MapXML("stragglerProbability")
    public final XMLValue<Double> stragglerProbability = new XMLValue<>(0d, ScenarioConfig.CLUSTER);
    @MapXML("compensateStragglerThreshold")
    public final XMLValue<Integer> compensateStragglerThreshold = new XMLValue<>(0, ScenarioConfig.CLUSTER);
}
