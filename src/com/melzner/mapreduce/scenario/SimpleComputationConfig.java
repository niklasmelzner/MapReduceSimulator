package com.melzner.mapreduce.scenario;

import com.melzner.xmlutil.MapXML;
import com.melzner.xmlutil.XMLElement;
import com.melzner.xmlutil.XMLValue;

import java.util.concurrent.TimeUnit;

public class SimpleComputationConfig {
    public static final XMLElement MASTER = new XMLElement("master");

    @MapXML("shuffleDuration")
    public final XMLValue<Long> shuffleDuration = new XMLValue<>(TimeUnit.MILLISECONDS.toNanos(30), ScenarioConfig::parseTime);
    @MapXML("mapComputationDuration")
    public final XMLValue<Long> mapComputationDuration = new XMLValue<>(TimeUnit.MILLISECONDS.toNanos(30), ScenarioConfig::parseTime);
    @MapXML("reduceComputationDuration")
    public final XMLValue<Long> reduceComputationDuration = new XMLValue<>(TimeUnit.MILLISECONDS.toNanos(30), ScenarioConfig::parseTime);
    @MapXML("assignTaskToMachineInternalDelay")
    public final XMLValue<Long> delayAssignTaskToMachineInternal = new XMLValue<>(TimeUnit.MILLISECONDS.toNanos(1), MASTER, ScenarioConfig::parseTime);
    @MapXML("assignTaskToMachineNetworkDelay")
    public final XMLValue<Long> delayAssignTaskToMachineNetwork = new XMLValue<>(TimeUnit.MILLISECONDS.toNanos(3), MASTER, ScenarioConfig::parseTime);
    @MapXML("assignFunctionDelay")
    public final XMLValue<Long> assignFunctionDelay = new XMLValue<>(TimeUnit.MILLISECONDS.toNanos(10), MASTER, ScenarioConfig::parseTime);
    @MapXML("setupMachineDelay")
    public final XMLValue<Long> setupMachineDelay = new XMLValue<>(TimeUnit.MILLISECONDS.toNanos(10), MASTER, ScenarioConfig::parseTime);
    @MapXML("hardDriveDelay")
    public final XMLValue<Long> hardDriveDelay = new XMLValue<>(TimeUnit.MILLISECONDS.toNanos(1), MASTER, ScenarioConfig::parseTime);
    @MapXML("inputSplitSize")
    public final XMLValue<Integer> inputSplitSize = new XMLValue<>(10000, MASTER);
    @MapXML("outputSplitSize")
    public final XMLValue<Integer> outputSplitSize = new XMLValue<>(2000, MASTER);
    public final ClusterConfig clusterConfig;

    public SimpleComputationConfig(ClusterConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
    }
}
