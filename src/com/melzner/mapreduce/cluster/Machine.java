package com.melzner.mapreduce.cluster;

import com.melzner.mapreduce.computation.MapReduceExecutionOrder;
import com.melzner.mapreduce.scenario.ClusterConfig;
import com.melzner.mapreduce.simulation.Simulatable;
import com.melzner.mapreduce.simulation.SimulationEvent;
import com.melzner.mapreduce.simulation.SimulationEventType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class Machine extends Simulatable<Machine.EventType> {

    private static final Random random = new Random();
    final HardDrive hardDrive;
    private final Cluster cluster;
    private final int id;
    private final ClusterConfig configuration;
    private final double computationSpeed;
    private final boolean isStraggler;
    private boolean busy, alive = true;

    Machine(Cluster cluster, int id, ClusterConfig configuration) {
        super(MapReduceExecutionOrder.MACHINE);
        this.cluster = cluster;
        this.id = id;
        this.configuration = configuration;
        hardDrive = new HardDrive(configuration);
        double computationSpeed = configuration.computationSpeed.get();

        Double stragglerProbability = configuration.stragglerProbability.get();
        isStraggler = (stragglerProbability != null && random.nextDouble() < stragglerProbability);
        if (isStraggler) {
            computationSpeed *= configuration.stragglingFactor.get();
        }
        this.computationSpeed = computationSpeed;
    }

    public int getId() {
        return id;
    }

    public HardDrive getHardDrive() {
        return hardDrive;
    }

    public void startTask(long networkDelay, double taskDuration, Consumer<Task> taskConsumer,
                          Runnable onSuccess) {
        if (! isAlive()) {
            throw new UnsupportedOperationException("machine is not alive anymore");
        } else if (isBusy()) {
            throw new UnsupportedOperationException("machine is busy");
        }

        taskDuration *= computationSpeed;

        invokeEvent(networkDelay, new StartTaskEvent((long) taskDuration, taskConsumer, onSuccess));
    }

    public void startTask(long networkDelay, double taskDuration, Runnable onSuccess) {
        startTask(networkDelay, taskDuration, null, onSuccess);
    }

    @Override
    protected void startSimulation() {

    }

    @Override
    protected void onSimulationEvent(SimulationEvent<EventType> event) {
        switch (event.getType()) {
            case START:
                StartTaskEvent startEvent = (StartTaskEvent) event;
                boolean fail = random.nextDouble() <= configuration.machineFailProbability.get();
                Task task = new Task(getTimeStamp() + startEvent.taskDuration);
                if (startEvent.taskConsumer != null) {
                    startEvent.taskConsumer.accept(task);
                }
                if (busy) {
                    task.onBusy();
                    return;
                }
                setBusy(true);
                updateRecordValue(RecordType.RUNNING_MACHINES, i -> i + 1, 0);
                if (isStraggler) updateRecordValue(RecordType.RUNNING_STRAGGLERS, i -> i + 1, 0);
                if (fail) {
                    invokeEvent((long) (random.nextDouble() * startEvent.taskDuration), new MachineFailEvent(task));
                } else {
                    invokeEvent(startEvent.taskDuration, new TaskSuccessEvent(startEvent.onSuccess, task));
                }
                break;
            case MACHINE_FAIL:
                alive = false;
                cluster.onMachineFailed(this);
                updateRecordValue(RecordType.FAILED_MACHINES, i -> i + 1, 0);
                updateRecordValue(RecordType.RUNNING_MACHINES, i -> i - 1, 0);
                if (isStraggler) updateRecordValue(RecordType.RUNNING_STRAGGLERS, i -> i - 1, 0);
                updateRecordValue(RecordType.ACTIVE_MACHINES, i -> i - 1, 0);
                ((MachineFailEvent) event).task.onMachineFail();
                break;
            case TASK_SUCCESS:
                updateRecordValue(RecordType.RUNNING_MACHINES, i -> i - 1, 0);
                if (isStraggler) updateRecordValue(RecordType.RUNNING_STRAGGLERS, i -> i - 1, 0);
                setBusy(false);
                ((TaskSuccessEvent) event).onSuccess.run();
                ((TaskSuccessEvent) event).task.onSuccess();
                break;
            case TASK_TERMINATED:
                updateRecordValue(RecordType.RUNNING_MACHINES, i -> i - 1, 0);
                if (isStraggler) updateRecordValue(RecordType.RUNNING_STRAGGLERS, i -> i - 1, 0);
                setBusy(false);
                break;
        }
    }

    @Override
    protected Simulatable<EventType> newInitialInstance() {
        return new Machine(cluster, id, configuration);
    }

    @Override
    public void onSimulationStart() {
        updateRecordValue(RecordType.ACTIVE_MACHINES, i -> i + 1, 0);
    }

    public boolean isBusy() {
        return busy;
    }

    private void setBusy(boolean b) {
        busy = b;
        cluster.onBusyStateChanged(this);
    }

    public boolean isAlive() {
        return alive;
    }

    private static class TaskSuccessEvent extends SimulationEvent<EventType> {

        private final Runnable onSuccess;
        private final Task task;

        private TaskSuccessEvent(Runnable onSuccess, Task task) {
            super(EventType.TASK_SUCCESS);
            this.onSuccess = onSuccess;
            this.task = task;
            task.bindEvent(this);
        }
    }

    private static class MachineFailEvent extends SimulationEvent<EventType> {
        private final Task task;

        private MachineFailEvent(Task task) {
            super(EventType.MACHINE_FAIL);
            this.task = task;
            task.bindEvent(this);
        }
    }

    private static class StartTaskEvent extends SimulationEvent<EventType> {
        private final long taskDuration;
        private final Consumer<Task> taskConsumer;
        private final Runnable onSuccess;

        private StartTaskEvent(long taskDuration, Consumer<Task> taskConsumer, Runnable onSuccess) {
            super(EventType.START);
            this.taskDuration = taskDuration;
            this.taskConsumer = taskConsumer;
            this.onSuccess = onSuccess;
        }

    }

    protected enum EventType implements SimulationEventType {
        MACHINE_FAIL, TASK_SUCCESS, TASK_TERMINATED, START
    }

    public enum RecordType {
        RUNNING_MACHINES, FAILED_MACHINES, DATA_TRANSFER, RUNNING_STRAGGLERS, ACTIVE_MACHINES
    }

    public class Task {

        private final long timestampEnd;
        private final List<Consumer<Task>> onNotInvokedListeners = new ArrayList<>();
        private final List<Consumer<Task>> onEndListeners = new ArrayList<>();
        private final List<SimulationEvent<EventType>> boundEvents = new ArrayList<>();
        double dataRateDelta = 0;
        private boolean running = true;

        private Task(long timestampEnd) {
            this.timestampEnd = timestampEnd;
        }

        public Machine getMachine() {
            return Machine.this;
        }

        public Task action(long delay, Consumer<Task> action) {
            invokeAction(delay, () -> action.accept(this));
            return this;
        }

        public void readData(long size, long duration) {
            if (running) {
                double delta = (1.0 * size / duration);
                dataRateDelta += delta;
                updateRecordValue(RecordType.DATA_TRANSFER, i -> i + delta, 0.0);
            }
        }

        public long remainingTime() {
            return timestampEnd - getTimeStamp();
        }

        private void endTask() {
            running = false;
            updateRecordValue(RecordType.DATA_TRANSFER, i -> i - dataRateDelta, 0.0);
            for (Consumer<Task> listener : onEndListeners) {
                listener.accept(this);
            }
        }

        private void onMachineFail() {
            endTask();
        }

        private void onSuccess() {
            endTask();
        }

        private void onBusy() {
            for (Consumer<Task> listener : onNotInvokedListeners) {
                listener.accept(this);
            }
            endTask();
        }

        public Task onNotInvoked(Consumer<Task> listener) {
            onNotInvokedListeners.add(listener);
            return this;
        }

        public Task onEnd(Consumer<Task> listener) {
            onEndListeners.add(listener);
            return this;
        }

        void bindEvent(SimulationEvent<EventType> event) {
            boundEvents.add(event);
        }

        public void terminate() {
            endTask();
            for (SimulationEvent<EventType> boundEvent : boundEvents) {
                boundEvent.destroy();
            }
            invokeEvent(0, new SimulationEvent<>(EventType.TASK_TERMINATED));
        }
    }
}
