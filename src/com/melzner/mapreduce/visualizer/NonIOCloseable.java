package com.melzner.mapreduce.visualizer;

import java.io.Closeable;

class NonIOCloseable implements Closeable {

    @Override
    public void close() {

    }
}
