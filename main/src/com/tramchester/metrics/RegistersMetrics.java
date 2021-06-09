package com.tramchester.metrics;

import com.codahale.metrics.Gauge;

public interface RegistersMetrics {
    void add(HasMetrics hasMetrics, String category, String name, Gauge<Integer> method);
}
