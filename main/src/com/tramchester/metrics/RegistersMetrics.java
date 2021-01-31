package com.tramchester.metrics;

import com.codahale.metrics.Gauge;
import com.tramchester.repository.DueTramsRepository;

public interface RegistersMetrics {
    void add(HasMetrics hasMetrics, String category, String name, Gauge<Integer> method);
}
