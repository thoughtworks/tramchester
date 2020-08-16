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
    private final ConfigFromInstanceUserData providesConfig;

    private static final String PREFIX_LOG_NAMESPACE = "ch.qos.logback.core.Appender.";

    private static final String PREFIX = "com.tramchester.";

    private CloudWatchReporter(MetricRegistry registry, String name, MetricFilter filter, TimeUnit rateUnit,
                               TimeUnit durationUnit, ConfigFromInstanceUserData providesConfig, SendMetricsToCloudWatch client) {
        super(registry, name, filter, rateUnit, durationUnit);
        this.providesConfig = providesConfig;
        this.client = client;
    }

    @Override
    public void report(SortedMap<String, Gauge> unTypedGauges, SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {

        SortedMap<String, Gauge<Number>> gauges = new TreeMap<>();

        unTypedGauges.entrySet().stream().
                filter(entry -> entry.getValue().getValue() instanceof Number).
                forEach(entry -> gauges.put(entry.getKey(), (Gauge<Number>)entry.getValue()));

        SortedMap<String, Timer> timersToSend = getTimersToSend(timers);
        SortedMap<String, Meter> metersToSend = getMetersToSend(meters);
        SortedMap<String, Gauge<Number>> gaugesToSend = getGuagesToSend(gauges);

        String msg = String.format("Send %s timers %s gauges %s meters to cloudwatch metrics",
                timersToSend.size(), gaugesToSend.size(), metersToSend.size());
        if (timersToSend.isEmpty() || meters.isEmpty() || gauges.isEmpty()) {
            logger.warn(msg);
        } else {
            logger.info(msg);
        }
        client.putMetricData(formNamespace(PREFIX, providesConfig), timersToSend, gaugesToSend, metersToSend);
    }

    private <T extends Number> SortedMap<String, Gauge<T>> getGuagesToSend(SortedMap<String, Gauge<T>> gauges) {
        SortedMap<String, Gauge<T>> gaugeTreeMap = new TreeMap<>();
        gauges.forEach((name, gauge) -> {
            if (isScoped(name)) {
                gaugeTreeMap.put(createName(name), gauge);
            }
        });
        return gaugeTreeMap;
    }

    private SortedMap<String, Meter> getMetersToSend(SortedMap<String, Meter> meters) {
        SortedMap<String, Meter> metersToSend = new TreeMap<>();
        meters.forEach((name,meter) -> {
            if (name.startsWith(PREFIX_LOG_NAMESPACE)) {
                    metersToSend.put(createLogMetricName(name), meter);
            }
        });
        return metersToSend;
    }

    private SortedMap<String, Timer> getTimersToSend(SortedMap<String, Timer> timers) {
        SortedMap<String, Timer> timersToSend = new TreeMap<>();
        timers.forEach((name, timer) -> {
            logger.debug("Add timer " + name + " to cloud watch metric");
            if (isScoped(name)) {
                timersToSend.put(createName(name),timer);
            }
        });
        return timersToSend;
    }

    private String createLogMetricName(String name) {
        return name.replace(PREFIX_LOG_NAMESPACE, "");
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
