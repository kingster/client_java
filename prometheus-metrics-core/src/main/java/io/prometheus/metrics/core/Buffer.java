package io.prometheus.metrics.core;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class Buffer<T extends io.prometheus.metrics.model.MetricData> {

    private static final long signBit = 1L << 63;
    private final AtomicLong observationCount = new AtomicLong(0);
    private double[] observationBuffer = new double[0];
    private int bufferPos = 0;
    private final Object writeLock = new Object();

    boolean append(double amount) {
        long count = observationCount.incrementAndGet();
        if ((count & signBit) == 0) {
            return false; // sign bit not set -> buffer not active.
        } else {
            doAppend(amount);
            return true;
        }
    }

    private void doAppend(double amount) {
        synchronized (writeLock) {
            if (bufferPos >= observationBuffer.length) {
                observationBuffer = Arrays.copyOf(observationBuffer, observationBuffer.length + 128);
            }
            observationBuffer[bufferPos] = amount;
            bufferPos++;
        }
    }

    T run(Function<Long, Boolean> complete, Supplier<T> runnable, Consumer<Double> observeFunction) {
        double[] buffer;
        int bufferSize;
        T result;
        synchronized (this) {
            Long count = observationCount.getAndAdd(signBit);
            while (!complete.apply(count)) {
                Thread.yield();
            }
            result = runnable.get();
            int expectedBufferSize = (int) (observationCount.addAndGet(signBit) - count);
            while (bufferPos != expectedBufferSize) {
                Thread.yield();
            }
            buffer = observationBuffer;
            bufferSize = bufferPos;
            observationBuffer = new double[0];
            bufferPos = 0;
        }
        for (int i = 0; i < bufferSize; i++) {
            observeFunction.accept(buffer[i]);
        }
        return result;
    }
}