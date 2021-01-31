package com.tramchester.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

public class RegistersMetricsWithDropwizard implements RegistersMetrics {
    private final MetricRegistry metricRegistry;

    public RegistersMetricsWithDropwizard(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    private void registerMetricForClass(Class<?> klass, String category, String name, Gauge<Integer> method) {
        metricRegistry.register(MetricRegistry.name(klass, category, name), method);
    }

    @Override
    public void add(HasMetrics hasMetrics, String category, String name, Gauge<Integer> method) {
        registerMetricForClass(hasMetrics.getClass(), category, name, method);
    }

    public void registerMetricsFor(HasMetrics hasMetrics) {
        hasMetrics.registerMetrics(this);
    }
}
