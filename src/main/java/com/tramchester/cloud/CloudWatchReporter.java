package com.tramchester.cloud;

import com.codahale.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class CloudWatchReporter extends ScheduledReporter {
    private static final Logger logger = LoggerFactory.getLogger(CloudWatchReporter.class);
    private final SendMetricsToCloudWatch client;
    private ConfigFromInstanceUserData providesConfig;

    private String PREFIX = "com.tramchester";

    protected CloudWatchReporter(MetricRegistry registry, String name, MetricFilter filter, TimeUnit rateUnit,
                                 TimeUnit durationUnit, ConfigFromInstanceUserData providesConfig, SendMetricsToCloudWatch client) {
        super(registry, name, filter, rateUnit, durationUnit);
        this.providesConfig = providesConfig;
        this.client = client;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {

        SortedMap<String, Timer> toSubmit = new TreeMap<>();

        timers.forEach((name, timer) -> {
            logger.debug("Add timer " + name + " to cloud watch metric");
            if (name.startsWith(PREFIX)) {
                toSubmit.put(name,timer);
            }
        });
        logger.info(String.format("Send %s cloudwatch metrics", toSubmit.size()));
        client.putMetricData(toSubmit, formNamespace(PREFIX, providesConfig));
    }

    public static String formNamespace(String namespace, ConfigFromInstanceUserData providesConfig) {
        String currentEnvironment = providesConfig.get("ENV");
        if (currentEnvironment==null) {
            currentEnvironment = "Unknown";
        }
        return currentEnvironment + ":" + namespace.replaceAll("\\.",":");
    }

    public static CloudWatchReporter forRegistry(MetricRegistry registry, ConfigFromInstanceUserData providesConfig,
                                                 SendMetricsToCloudWatch client) {
        return new CloudWatchReporter(registry, "name", MetricFilter.ALL, TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS,
                providesConfig, client);
    }
}
