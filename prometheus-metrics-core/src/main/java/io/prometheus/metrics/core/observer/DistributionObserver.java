package io.prometheus.metrics.core.observer;

import io.prometheus.metrics.model.snapshots.Labels;

import java.util.concurrent.Callable;

public interface DistributionObserver extends Observer {
    void observe(double amount);
    default void observeWithExemplar(double amount) {
        observeWithExemplar(amount, Labels.EMPTY);
    }
    void observeWithExemplar(double amount, Labels labels);
    default void time(Callable<?> callable) {

    }
}
