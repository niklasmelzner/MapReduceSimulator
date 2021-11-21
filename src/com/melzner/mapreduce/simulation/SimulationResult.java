package com.melzner.mapreduce.simulation;

import java.util.*;
import java.util.function.BiFunction;

public class SimulationResult {

    private final long duration;
    private final Map<Object, List<RecordHistoryEntry>> recordHistory;

    SimulationResult(long duration, Map<Object, List<RecordHistoryEntry>> recordHistory) {
        this.duration = duration;
        this.recordHistory = recordHistory;
    }

    public long getDuration() {
        return duration;
    }

    public Map<Object, List<RecordHistoryEntry>> getRecordHistory() {
        return recordHistory;
    }

    public static <T> SimulationResult average(Simulation simulation, int times, BiFunction<T, T, T> transformation) {
        SimulationResult result = simulation.run();
        for (int i = 1; i < times; i++) {
            result = result.average(simulation.run(), transformation);
        }
        return result;
    }

    private <T> SimulationResult average(SimulationResult other, BiFunction<T, T, T> transformation) {
        Map<Object, List<RecordHistoryEntry>> newHistory = new HashMap<>();
        Set<Object> thisKeys = recordHistory.keySet();
        Set<Object> otherKeys = other.recordHistory.keySet();
        Set<Object> equalKeys = new HashSet<>(thisKeys);
        equalKeys.retainAll(otherKeys);

        for (Object key : equalKeys) {
            newHistory.put(key, average(recordHistory.get(key), other.recordHistory.get(key), transformation));
        }

        thisKeys.removeAll(equalKeys);
        otherKeys.removeAll(equalKeys);

        for (Object key : thisKeys) {
            newHistory.put(key, recordHistory.get(key));
        }
        for (Object key : otherKeys) {
            newHistory.put(key, other.recordHistory.get(key));
        }

        return new SimulationResult((duration + other.duration) / 2, newHistory);
    }

    @SuppressWarnings("unchecked")
    private <T> List<RecordHistoryEntry> average(List<RecordHistoryEntry> entries1, List<RecordHistoryEntry> entries2,
                                                 BiFunction<T, T, T> transformation) {
        if (entries1.isEmpty()) return entries2;
        else if (entries2.isEmpty()) return entries1;

        List<RecordHistoryEntry> newEntries = new ArrayList<>();

        Iterator<RecordHistoryEntry> itEntries1 = entries1.iterator();
        Iterator<RecordHistoryEntry> itEntries2 = entries2.iterator();

        RecordHistoryEntry currentEntry1 = itEntries1.next();
        RecordHistoryEntry currentEntry2 = itEntries2.next();

        while (itEntries1 != null && itEntries2 != null) {
            T value = transformation.apply((T) currentEntry1.value, (T) currentEntry2.value);
            boolean forward1 = false, forward2 = false;
            if (currentEntry1.timestamp == currentEntry2.timestamp) {
                newEntries.add(new RecordHistoryEntry(currentEntry1.timestamp, value));
                forward1 = forward2 = true;
            } else if (currentEntry1.timestamp < currentEntry2.timestamp) {
                newEntries.add(new RecordHistoryEntry(currentEntry1.timestamp, value));
                forward1 = true;
            } else {
                newEntries.add(new RecordHistoryEntry(currentEntry2.timestamp, value));
                forward2 = true;
            }
            if (forward1) {
                if (itEntries1.hasNext()) currentEntry1 = itEntries1.next();
                else itEntries1 = null;
            }
            if (forward2) {
                if (itEntries2.hasNext()) currentEntry2 = itEntries2.next();
                else itEntries2 = null;
            }
        }
        while (itEntries1 != null && itEntries1.hasNext()) newEntries.add(itEntries1.next());
        while (itEntries2 != null && itEntries2.hasNext()) newEntries.add(itEntries2.next());
        return newEntries;
    }
}
