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

    private String PREFIX = "com.tramchester.";

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

        SortedMap<String, Timer> timersToSend = new TreeMap<>();
        timers.forEach((name, timer) -> {
            logger.debug("Add timer " + name + " to cloud watch metric");
            if (isScoped(name)) {
                timersToSend.put(createName(name),timer);
            }
        });
        SortedMap<String, Gauge<Integer>> gaugesToSend = new TreeMap<>();
        gauges.forEach((name, gauge) -> {
            if (isScoped(name)) {
                // assumes all gauges in scope are integer
                gaugesToSend.put(createName(name),gauge);
            }
        });
        logger.info(String.format("Send %s cloudwatch metrics", timersToSend.size()));
        client.putMetricData(formNamespace(PREFIX, providesConfig), timersToSend, gaugesToSend);
    }

    private String createName(String name) {
        return name.replace(PREFIX, "");
    }

    private boolean isScoped(String name) {
        return name.startsWith(PREFIX);
    }

    public static String formNamespace(String namespace, ConfigFromInstanceUserData providesConfig) {
        String currentEnvironment = providesConfig.get("ENV");
        if (currentEnvironment==null) {
            currentEnvironment = "Unknown";
        }
        if (namespace.endsWith(".")) {
            namespace = namespace.substring(0,namespace.length()-1);
        }
        return currentEnvironment + ":" + namespace.replaceAll("\\.",":");
    }

    public static CloudWatchReporter forRegistry(MetricRegistry registry, ConfigFromInstanceUserData providesConfig,
                                                 SendMetricsToCloudWatch client) {
        return new CloudWatchReporter(registry, "name", MetricFilter.ALL, TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS,
                providesConfig, client);
    }
}
