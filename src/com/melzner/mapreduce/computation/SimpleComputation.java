package com.melzner.mapreduce.computation;

import com.melzner.mapreduce.cluster.Cluster;
import com.melzner.mapreduce.cluster.Cluster.ClusterEventType;
import com.melzner.mapreduce.cluster.DFS;
import com.melzner.mapreduce.cluster.Machine;
import com.melzner.mapreduce.scenario.SimpleComputationConfig;
import com.melzner.mapreduce.simulation.Simulatable;
import com.melzner.mapreduce.simulation.SimulationEvent;
import com.melzner.mapreduce.simulation.SimulationEventType;
import com.melzner.mapreduce.simulation.ValueSimulationEvent;
import com.melzner.xmlutil.XMLValue;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SimpleComputation extends Simulatable<SimpleComputation.EventType> {

    private final Cluster cluster;
    private final SimpleComputationConfig config;

    private final DFS.DFSFile file;
    private TaskExecutor<?> taskExecutor;
    private MapTaskExecutor mapTasksExecutor;
    private ReduceTaskExecutor reduceTaskExecutor;
    private boolean shuffleActive;

    public SimpleComputation(Cluster cluster, SimpleComputationConfig config) {
        super(MapReduceExecutionOrder.COMPUTATION);
        file = cluster.getDFS().writeFile(
                (long) config.clusterConfig.dfsBlockSize.get() * config.inputSplitSize.get()
        );
        this.cluster = cluster;
        this.config = config;

        cluster.addListener(ClusterEventType.BUSY_STATE_CHANGED, e -> {
            if (! e.machine.isBusy()) {
                invokeEvent(config.delayAssignTaskToMachineNetwork.get(),
                        new ValueSimulationEvent<>(EventType.INVOKE_TASKS_FOR_MACHINE, e.machine));
            }
        });
    }

    @Override
    protected void startSimulation() {
        invokeEvent(0, new SimulationEvent<>(EventType.INVOKE_TASKS));
        taskExecutor = mapTasksExecutor = new MapTaskExecutor();
        reduceTaskExecutor = new ReduceTaskExecutor();
        cluster.addListener(ClusterEventType.MACHINE_FAILED, e -> taskExecutor.onMachineFail(e.machine));
    }

    @Override
    protected void onSimulationEvent(SimulationEvent<EventType> event) {
        if (shuffleActive) {
            if (event.getType() == EventType.SHUFFLE_DONE) {
                taskExecutor = reduceTaskExecutor;
                shuffleActive = false;
                updateRecordValue(RecordType.SHUFFLE_DONE,i->getTimeStamp(),0L);
                invokeEvent(0, new SimulationEvent<>(EventType.INVOKE_TASKS));
            }
        }
        switch (event.getType()) {
            case INVOKE_TASKS:
                taskExecutor.invokeTasks();
                break;
            case TASK_SUCCESS:
                if (mapTasksExecutor.tasks.getTasks(MapTaskState.DONE).size() ==
                        mapTasksExecutor.tasks.size() && taskExecutor == mapTasksExecutor) {
                    shuffleActive = true;
                    updateRecordValue(RecordType.MAPPING_DONE, i->getTimeStamp(),0L);
                    invokeEvent(config.shuffleDuration.get(), new SimulationEvent<>(EventType.SHUFFLE_DONE));
                    break;
                }
            case TASK_FAIL:
            case NOT_INVOKED:
            case INVOKE_TASKS_FOR_MACHINE:
                Machine machine = ((ValueSimulationEvent<?, Machine>) event).getValue();
                taskExecutor.invokeTasks(machine);
                break;
        }

    }

    private abstract class TaskExecutor<STATE> {
        final TaskGroup<STATE> tasks;
        final STATE STATE_IDLE;
        final STATE STATE_PREPARED;
        final STATE STATE_RUNNING;
        final STATE STATE_DONE;
        final Map<Machine, Set<Integer>> runningTasks = new HashMap<>();
        final Map<Integer, Integer> pendingInstancesPerTask = new HashMap<>();
        final Map<Integer, Set<Machine.Task>> runningInstancesPerTask = new HashMap<>();
        final Set<Machine> pendingMachines = new HashSet<>();
        final Set<Machine> assignedFunction = new HashSet<>();
        final int compensateStragglersThreshold = config.clusterConfig.compensateStragglerThreshold.get();
        private final XMLValue<Long> computationDuration;
        long invokedLastTask = 0;

        private TaskExecutor(TaskGroup<STATE> tasks,
                             XMLValue<Long> computationDuration,
                             STATE stateIdle, STATE statePrepared,
                             STATE stateRunning, STATE stateDone) {
            this.tasks = tasks;
            this.computationDuration = computationDuration;
            STATE_IDLE = stateIdle;
            STATE_PREPARED = statePrepared;
            STATE_RUNNING = stateRunning;
            STATE_DONE = stateDone;
        }

        abstract Machine getNewMachineForTask(Integer index);

        boolean startTask(Integer taskIndex) {
            Machine machine = getNewMachineForTask(taskIndex);
            if (machine == null) {
                return false;
            }

            tasks.changeTaskState(taskIndex, STATE_PREPARED);
            runningTasks.computeIfAbsent(machine, m -> new HashSet<>()).add(taskIndex);
            long internalDelay = Math.max(invokedLastTask + config.delayAssignTaskToMachineInternal.get() - getTimeStamp(), 0);
            long taskDuration = computationDuration.get();
            boolean added = assignedFunction.add(machine);
            if (added) {
                internalDelay += config.assignFunctionDelay.get();
                taskDuration += config.setupMachineDelay.get();
            }
            pendingMachines.add(machine);
            pendingInstancesPerTask.merge(taskIndex, 1, Integer::sum);
            machine.startTask(config.delayAssignTaskToMachineNetwork.get() + internalDelay, taskDuration,
                    task -> {
                        runningInstancesPerTask.computeIfAbsent(taskIndex, i -> new HashSet<>()).add(task);
                        tasks.changeTaskState(taskIndex, STATE_RUNNING, STATE_PREPARED);
                        task.action(config.hardDriveDelay.get(), t -> t.readData(config.clusterConfig.dfsBlockSize.get(), t.remainingTime()));
                        task.onNotInvoked(t -> onNotInvoked(taskIndex, machine, added));
                        task.onEnd(t -> {
                            pendingMachines.remove(machine);
                            pendingInstancesPerTask.merge(taskIndex, - 1, Integer::sum);
                            runningInstancesPerTask.get(taskIndex).remove(task);
                        });
                    },
                    () -> onTaskSuccess(taskIndex, machine)
            );
            invokedLastTask = getTimeStamp() + internalDelay;
            return true;
        }

        void onNotInvoked(Integer idleTask, Machine machine, boolean added) {
            if (added) {
                assignedFunction.remove(machine);
            }
            tasks.changeTaskState(idleTask, STATE_IDLE, STATE_PREPARED, STATE_RUNNING);
            invokeEvent(config.delayAssignTaskToMachineNetwork.get(), new ValueSimulationEvent<>(EventType.NOT_INVOKED, machine));
        }

        void onMachineFail(Machine machine) {
            Set<Integer> runningTasksForMachine = runningTasks.get(machine);
            if (runningTasksForMachine != null) {
                for (Integer failedTask : runningTasksForMachine) {
                    tasks.changeTaskState(failedTask, STATE_IDLE);
                }
            }
            runningTasks.remove(machine);
            invokeEvent(config.delayAssignTaskToMachineNetwork.get(), new ValueSimulationEvent<>(EventType.TASK_FAIL, machine));
        }

        abstract void invokeTasks();

        abstract void invokeTasks(Machine machine);

        void onTaskSuccess(Integer idleTask, Machine machine) {
            tasks.changeTaskState(idleTask, STATE_DONE);
            for (Machine.Task task : new ArrayList<>(runningInstancesPerTask.get(idleTask))) {
                if (task.getMachine() != machine) {
                    task.terminate();
                }
            }
            runningTasks.get(machine).remove(idleTask);
            invokeEvent(config.delayAssignTaskToMachineNetwork.get(), new ValueSimulationEvent<>(EventType.TASK_SUCCESS, machine));
        }

    }

    private class ReduceTaskExecutor extends TaskExecutor<ReduceTaskState> {

        ReduceTaskExecutor() {
            super(new TaskGroup<>(config.inputSplitSize.get(), config.outputSplitSize.get(),
                            ReduceTaskState.values(), ReduceTaskState.IDLE),
                    config.reduceComputationDuration,
                    ReduceTaskState.IDLE, ReduceTaskState.PREPARED,
                    ReduceTaskState.RUNNING, ReduceTaskState.DONE);

        }

        @Override
        Machine getNewMachineForTask(Integer index) {
            return cluster.getFreeMachine(m -> ! pendingMachines.contains(m));
        }

        @Override
        public void invokeTasks() {
            Set<Integer> idleTasks = tasks.getTasks(ReduceTaskState.IDLE);
            for (Integer idleTask : new ArrayList<>(idleTasks)) {
                if (! startTask(idleTask)) {
                    break;
                } else if (idleTasks.size() < compensateStragglersThreshold) {
                    for (Integer pendingTask : new ArrayList<>(tasks.getTasks(ReduceTaskState.PREPARED))) {
                        for (int i = pendingInstancesPerTask.get(pendingTask); i < DFS.COPY_COUNT; i++) {
                            if (! startTask(pendingTask)) break;
                        }
                    }
                    for (Integer runningTask : new ArrayList<>(tasks.getTasks(ReduceTaskState.RUNNING))) {
                        for (int i = pendingInstancesPerTask.get(runningTask); i < DFS.COPY_COUNT; i++) {
                            if (! startTask(runningTask)) break;
                        }
                    }
                }
            }

        }

        @Override
        public void invokeTasks(Machine machine) {
            invokeTasks();
        }

    }

    private class MapTaskExecutor extends TaskExecutor<MapTaskState> {

        MapTaskExecutor() {
            super(new TaskGroup<>(0, config.inputSplitSize.get(), MapTaskState.values(), MapTaskState.IDLE),
                    config.mapComputationDuration,
                    MapTaskState.IDLE, MapTaskState.PREPARED,
                    MapTaskState.RUNNING, MapTaskState.DONE);
        }

        @Override
        Machine getNewMachineForTask(Integer index) {
            return file.getFreeMachineForBlock(index, m -> ! pendingMachines.contains(m));
        }

        @Override
        public void invokeTasks() {
            Integer[] idleTasks = tasks.getTasks(MapTaskState.IDLE).toArray(new Integer[0]);
            int i = 0;
            for (Integer idleTask : idleTasks) {
                if (startTask(idleTask) && ++ i == config.inputSplitSize.get() / 3000) return;
            }
        }

        @Override
        public void invokeTasks(Machine machine) {
            Set<Integer> idleTasks = tasks.getTasks(MapTaskState.IDLE);

            for (Integer block : machine.getHardDrive().getBlocksForFile(file)) {
                if (tasks.getState(block) == MapTaskState.IDLE && startTask(block)) {
                    break;
                }
            }
            if (idleTasks.size() < compensateStragglersThreshold) {
                for (Integer pendingTask : new ArrayList<>(tasks.getTasks(MapTaskState.PREPARED))) {
                    for (int i = pendingInstancesPerTask.get(pendingTask); i < DFS.COPY_COUNT; i++) {
                        if (! startTask(pendingTask)) break;
                    }
                }
                for (Integer runningTask : new ArrayList<>(tasks.getTasks(MapTaskState.RUNNING))) {
                    for (int i = pendingInstancesPerTask.get(runningTask); i < DFS.COPY_COUNT; i++) {
                        if (! startTask(runningTask)) break;
                    }
                }
            }
        }
    }

    private class TaskGroup<TASK_STATE> {

        private final Map<TASK_STATE, Set<Integer>> stateToTask = new HashMap<>();
        private final Map<Integer, TASK_STATE> taskToState = new HashMap<>();

        private TaskGroup(Integer start, Integer taskCount, TASK_STATE[] states, TASK_STATE idleState) {
            for (TASK_STATE value : states) stateToTask.put(value, new HashSet<>());

            Set<Integer> tasks = new HashSet<>(taskCount);
            for (Integer i = 0; i < taskCount; i++) {
                tasks.add(start + i);
                taskToState.put(start + i, idleState);
            }
            stateToTask.put(idleState, tasks);
            updateRecordValue(idleState, i -> i + taskCount, 0);
        }

        public Set<Integer> getTasks(TASK_STATE type) {
            return stateToTask.get(type);
        }

        @SafeVarargs
        public final void changeTaskState(Integer task, TASK_STATE state, TASK_STATE... expectedStates) {
            TASK_STATE previousState = taskToState.get(task);
            if (expectedStates.length == 0 || isExpectedState(previousState, expectedStates)) {
                updateRecordValue(previousState, i -> i - 1, 0);
                stateToTask.get(previousState).remove(task);
                stateToTask.get(state).add(task);
                taskToState.put(task, state);
                updateRecordValue(state, i -> i + 1, 0);
            }
        }

        private boolean isExpectedState(TASK_STATE state, TASK_STATE[] expectedStates) {
            for (TASK_STATE expectedState : expectedStates) if (expectedState == state) return true;
            return false;
        }

        TASK_STATE getState(Integer task) {
            return taskToState.get(task);
        }

        public int size() {
            return taskToState.size();
        }
    }

    public enum EventType implements SimulationEventType {
        INVOKE_TASKS, TASK_SUCCESS, TASK_FAIL, NOT_INVOKED, INVOKE_TASKS_FOR_MACHINE, SHUFFLE_DONE
    }

    public enum MapTaskState {
        IDLE, PREPARED, RUNNING, DONE
    }

    public enum ReduceTaskState {
        IDLE, PREPARED, RUNNING, DONE
    }

    public enum RecordType{
        MAPPING_DONE, SHUFFLE_DONE
    }

}
