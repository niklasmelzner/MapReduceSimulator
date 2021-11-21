package com.melzner.mapreduce.simulation;

public class RecordHistoryEntry {

    final long timestamp;
    Object value;

    RecordHistoryEntry(long timestamp, Object value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public double getTimestamp() {
        return timestamp;
    }
}
