package com.melzner.mapreduce.cluster;

import com.melzner.mapreduce.computation.MapReduceExecutionOrder;
import com.melzner.mapreduce.scenario.ClusterConfig;
import com.melzner.mapreduce.simulation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class Cluster extends ListenableSimulatable<SimulationEventType, Cluster.ClusterEventType, Cluster.ClusterEvent> {

    final ClusterConfig configuration;
    private final DFS dfs;
    private final Machine[] machines;
    private final List<Machine> unmodifiableMachines;

    public Cluster(ClusterConfig configuration) {
        super(MapReduceExecutionOrder.CLUSTER);
        this.configuration = configuration;
        machines = new Machine[configuration.machineCount.get()];
        for (int i = 0; i < configuration.machineCount.get(); i++) {
            Machine machine = new Machine(this, i, configuration);
            machines[i] = machine;
        }
        unmodifiableMachines = List.of(machines);
        dfs = new DFS(this);
    }

    public DFS getDFS() {
        return dfs;
    }

    public List<Machine> getMachines() {
        return unmodifiableMachines;
    }

    public void printStatus(PrintConfiguration printConfig) {
        int length = (int) Math.sqrt(machines.length);
        if (machines.length - length * length != 0) length++;
        printStatus(printConfig, length);
    }

    public void printStatus(PrintConfiguration printConfig, int unitsPerLine) {
        int rowCount = machines.length / unitsPerLine;
        int lastRowLength = unitsPerLine;
        if (machines.length % unitsPerLine > 0) {
            rowCount++;
            lastRowLength = machines.length % unitsPerLine;
        }
        int[] columnWidths = new int[unitsPerLine];
        PrintBlock[][] printBlocks = new PrintBlock[rowCount][unitsPerLine];
        for (int i = 0; i < machines.length; i++) {
            int column = i % unitsPerLine;
            int row = i / unitsPerLine;
            Machine machine = machines[i];
            PrintBlock block = printConfig.generatePrintBlock(machine);
            if (block.maxLineLength > columnWidths[column]) {
                columnWidths[column] = block.maxLineLength;
            }
            printBlocks[row][column] = block;
        }

        String rowSeparator = buildRowSeparator(printConfig, columnWidths);

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            PrintBlock[] row = printBlocks[rowIndex];
            int rowLength = rowIndex == printBlocks.length - 1 ? lastRowLength : unitsPerLine;
            String[] lines = new String[printConfig.fields.size()];
            Arrays.fill(lines, "");
            for (int columnIndex = 0; columnIndex < rowLength; columnIndex++) {
                row[columnIndex].addTo(lines, columnWidths[columnIndex],
                        columnIndex == rowLength - 1 ? "" : printConfig.columnSeparator);

            }

            for (String line : lines) {
                System.out.println(line);
            }

            if (rowIndex != rowCount - 1 && rowSeparator != null) {
                System.out.println(rowSeparator);
            }
        }

    }

    private String buildRowSeparator(PrintConfiguration printConfig, int[] columnWidths) {
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < columnWidths.length; i++) {
            for (int j = 0; j < columnWidths[i]; j++) {
                separator.append(printConfig.rowSeparator);
            }
            if (i != columnWidths.length - 1) {
                separator.append(printConfig.crossSeparator);
            }
        }
        return separator.toString();
    }

    @Override
    protected void startSimulation() {

    }

    @Override
    protected void onSimulationEvent(SimulationEvent<SimulationEventType> event) {

    }

    @Override
    protected void onAdd(Simulation simulation) {
        for (Machine machine : machines) {
            simulation.add(machine);
        }
    }

    @Override
    public Simulatable<SimulationEventType> newInitialInstance() {
        return new Cluster(configuration);
    }

    public void onMachineFailed(Machine machine) {
        fireEvent(ClusterEventType.MACHINE_FAILED, new ClusterEvent(machine));
    }

    public void onBusyStateChanged(Machine machine) {
        fireEvent(ClusterEventType.BUSY_STATE_CHANGED, new ClusterEvent(machine));
    }

    public Machine getMachine(int id) {
        return machines[id];
    }

    public Machine getFreeMachine() {
        return getFreeMachine(m -> true);
    }

    public Machine getFreeMachine(Predicate<Machine> condition) {
        for (Machine machine : machines) {
            if (! machine.isBusy() && machine.isAlive() && condition.test(machine)) {
                return machine;
            }
        }
        return null;
    }

    private static class PrintBlock {

        private final String[] lines;
        private int currentLine = 0;
        private int maxLineLength = 0;

        private PrintBlock(int lineCount) {
            lines = new String[lineCount];
        }

        public void addLine(String line) {
            lines[currentLine++] = line;
            if (line.length() > maxLineLength) {
                maxLineLength = line.length();
            }
        }

        public void addTo(String[] lines, int length, String additionalChars) {
            for (int i = 0; i < lines.length; i++) {
                lines[i] += this.lines[i];
                int thisLength = this.lines[i].length();
                for (int j = 0; j < length - thisLength; j++) {
                    lines[i] += " ";
                }
                lines[i] += additionalChars;
            }
        }
    }

    public static class PrintConfiguration {

        private final List<Function<Machine, Object>> fields = new ArrayList<>();
        private final String columnSeparator;
        private final String rowSeparator;
        private final String crossSeparator;

        public PrintConfiguration() {
            this(null, null, null);
        }

        public PrintConfiguration(String columnSeparator, String rowSeparator, String crossSeparator) {
            this.columnSeparator = columnSeparator;
            this.rowSeparator = rowSeparator;
            this.crossSeparator = crossSeparator;
        }

        public PrintConfiguration addField(Function<Machine, Object> field) {
            fields.add(field);
            return this;
        }

        PrintBlock generatePrintBlock(Machine machine) {
            PrintBlock printBlock = new PrintBlock(fields.size());
            for (Function<Machine, Object> field : fields) {
                printBlock.addLine(field.apply(machine).toString());
            }
            return printBlock;
        }

    }

    public enum ClusterEventType implements ListenerEventType {
        BUSY_STATE_CHANGED, MACHINE_FAILED
    }

    public static class ClusterEvent extends ListenerEvent<ClusterEventType> {
        public final Machine machine;

        public ClusterEvent(Machine machine) {
            this.machine = machine;
        }
    }
}

