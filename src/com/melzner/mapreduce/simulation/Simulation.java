package com.melzner.mapreduce.simulation;

import com.melzner.mapreduce.visualizer.GraphVisualizer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Simulation {

    private final List<Simulatable<?>> simulatables = new ArrayList<>();
    private int addingCount = 0;

    public synchronized void add(Simulatable<?> simulatable) {
        try {
            if (addingCount++ == 0) {
                simulatable.setIsBase(true);
            }
            simulatables.add(simulatable);
            simulatable.onAdd(this);
        } finally {
            addingCount--;
        }
    }

    public synchronized SimulationResult run() {
        /*List<Simulatable<?>> newSimulatables = new ArrayList<>();
        List<Simulatable<?>> originalSimulatables = simulatables;
        try {
            simulatables = newSimulatables;
            for (Simulatable<?> simulatable : originalSimulatables) {
                if (simulatable.isBase()) {
                    add(simulatable.newInitialInstance());
                }
            }
        } finally {
            simulatables = originalSimulatables;
        }*/
        return new SimulationSession(simulatables).run();

    }

    public static GraphVisualizer newVisualizer(String title) {
        return new GraphVisualizer()
                .setTitle(title)
                .setXAxisUnitTransformation(i -> (double) TimeUnit.NANOSECONDS.toMillis((long) (double) i) / 1000.0)
                .setXLabel("t[s]");
    }

    static class SimulationSession {
        private final List<Simulatable<?>> simulatables;
        private final Map<SimulationEventType, Integer> eventTypeCount = new HashMap<>();
        private final PriorityQueue<SchedulerEntry> eventQueue =
                new PriorityQueue<>(Comparator.<SchedulerEntry>comparingLong(o -> o.timestamp)
                        .thenComparingDouble(SchedulerEntry::getOrder));
        private final Map<Object, Object> recordData = new HashMap<>();
        private final Map<Object, List<RecordHistoryEntry>> recordHistory = new HashMap<>();
        private long currentTimestamp;
        private SimulationState currentState;

        private SimulationSession(List<Simulatable<?>> simulatables) {
            this.simulatables = new ArrayList<>(simulatables);
            this.simulatables.sort(Comparator.comparingDouble(s -> s.getExecutionOrder().getOrder()));
            for (Simulatable<?> simulatable : this.simulatables) {
                simulatable.setSimulationSession(this);
            }
        }

        long getTimestamp() {
            return currentTimestamp;
        }

        SimulationResult run() {
            long tNow = System.nanoTime();
            currentTimestamp = 0;
            currentState = new SimulationState(0);
            startSimulation();
            while (eventQueue.size() > 0) {
                SchedulerEntry eventEntry = eventQueue.poll();
                List<SchedulerEntry> entries = new ArrayList<>();
                entries.add(eventEntry);
                while (eventQueue.size() > 0 && eventQueue.peek().timestamp == eventEntry.timestamp) {
                    entries.add(eventQueue.poll());
                }

                long lastTimestamp = currentTimestamp;
                currentTimestamp = eventEntry.timestamp;

                boolean executed = false;
                for (SchedulerEntry entry : entries) {
                    if (entry instanceof EventEntry) {
                        eventTypeCount.merge(((EventEntry<?>) entry).event.getType(), - 1, Integer::sum);
                    }
                    executed = entry.execute() || executed;
                }
                if (! executed) {
                    currentTimestamp = lastTimestamp;
                }
            }
            System.out.println("finished simulation in " + TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - tNow) / 1000.0 + " ms");

            return new SimulationResult(currentTimestamp, recordHistory);
        }

        public Integer getEventCount(SimulationEventType eventType) {
            Integer count = eventTypeCount.get(eventType);
            return count == null ? 0 : count;
        }

        private void startSimulation() {
            for (Simulatable<?> simulatable : simulatables) {
                simulatable.onSimulationStart();
                simulatable.startSimulation();
            }
        }

        public SimulationState getState() {
            return currentState;
        }

        void invokeAction(long delay, Runnable action) {
            if (delay < 0) {
                throw new IllegalArgumentException("delay can't be negative");
            }
            eventQueue.add(new ActionEntry(currentTimestamp + delay, action));
        }

        <E extends SimulationEventType> void invokeEvent(Simulatable<E> simulatable, long delay, SimulationEvent<E> event) {
            if (delay < 0) {
                throw new IllegalArgumentException("delay can't be negative");
            }
            eventTypeCount.merge(event.getType(), 1, Integer::sum);
            eventQueue.add(new EventEntry<>(currentTimestamp + delay, simulatable, event));
        }

        @SuppressWarnings("unchecked")
        public <T> void updateRecordValue(Object key, Function<T, T> transformation, T def) {
            recordData.compute(key, (k, v) -> {
                T newValue = transformation.apply(v == null ? def : (T) v);
                if (! Objects.equals(newValue, v)) {
                    List<RecordHistoryEntry> history = recordHistory.computeIfAbsent(key, k1 -> new ArrayList<>());
                    RecordHistoryEntry lastEntry = history.isEmpty() ? null : history.get(history.size() - 1);
                    if (lastEntry == null || lastEntry.timestamp != currentTimestamp) {
                        history.add(new RecordHistoryEntry(currentTimestamp, newValue));
                    } else {
                        lastEntry.value = newValue;
                    }
                }
                return newValue;
            });
        }

        public Object getRecordValue(Object key) {
            return recordData.get(key);
        }
    }

    private static abstract class SchedulerEntry {

        final long timestamp;

        SchedulerEntry(long timestamp) {
            this.timestamp = timestamp;
        }

        abstract boolean execute();

        double getOrder() {
            return 0;
        }
    }

    private static class ActionEntry extends SchedulerEntry {

        private final Runnable action;

        ActionEntry(long timestamp, Runnable action) {
            super(timestamp);
            this.action = action;
        }

        @Override
        boolean execute() {
            action.run();
            return true;
        }

    }

    private static class EventEntry<EVENT_TYPE extends SimulationEventType> extends SchedulerEntry {

        private final Simulatable<EVENT_TYPE> simulatable;
        private final SimulationEvent<EVENT_TYPE> event;

        private EventEntry(long timestamp, Simulatable<EVENT_TYPE> simulatable, SimulationEvent<EVENT_TYPE> event) {
            super(timestamp);
            this.simulatable = simulatable;
            this.event = event;
        }

        @Override
        public String toString() {
            return "EventEntry<" + timestamp + ">[" + simulatable + "]{" + event + "}";
        }

        @Override
        boolean execute() {
            if (! event.isDestroyed()) {
                simulatable.onSimulationEvent(event);
                return true;
            }
            return false;
        }

        @Override
        double getOrder() {
            return simulatable.getExecutionOrder().getOrder();
        }
    }

}
