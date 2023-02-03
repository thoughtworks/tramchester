package com.tramchester.cloud;

import com.codahale.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class CloudWatchReporter extends ScheduledReporter {
    private static final Logger logger = LoggerFactory.getLogger(CloudWatchReporter.class);

    private static final String PREFIX_LOG_NAMESPACE = "ch.qos.logback.core.Appender.";
    private static final String PREFIX = "com.tramchester.";
    private static final String GAUGE_PREFIX = PREFIX+"livedata.";

    private final ConfigFromInstanceUserData providesConfig;
    private final SendMetricsToCloudWatch client;

    public static CloudWatchReporter forRegistry(MetricRegistry registry, ConfigFromInstanceUserData providesConfig,
                                                 SendMetricsToCloudWatch client) {
        return new CloudWatchReporter(registry, "name",
                providesConfig, client);
    }

    public CloudWatchReporter(MetricRegistry registry, String name, ConfigFromInstanceUserData providesConfig, SendMetricsToCloudWatch client) {
        super(registry, name,  MetricFilter.ALL, TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS);
        this.providesConfig = providesConfig;
        this.client = client;
    }

    @Override
    public void report(SortedMap<String, Gauge> unTypedGauges, SortedMap<String, Counter> unused_counters,
                       SortedMap<String, Histogram> unused_histograms, SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {

        SortedMap<String, Gauge<Number>> gauges = new TreeMap<>();

        unTypedGauges.entrySet().stream().
                filter(entry -> entry.getValue().getValue() instanceof Number).
                forEach(entry -> gauges.put(entry.getKey(), (Gauge<Number>)entry.getValue()));

        SortedMap<String, Timer> timersToSend = getTimersToSend(timers);
        SortedMap<String, Meter> metersToSend = getMetersToSend(meters);
        SortedMap<String, Gauge<Number>> gaugesToSend = getGuagesToSend(gauges);

        String namespace = formNamespace(PREFIX);
        String msg = String.format("Send %s timers %s gauges %s meters to cloudwatch metrics with namespace %s",
                timersToSend.size(), gaugesToSend.size(), metersToSend.size(), namespace);
        if (timersToSend.isEmpty() || meters.isEmpty() || gauges.isEmpty()) {
            logger.warn(msg);
        } else {
            logger.info(msg);
        }
        client.putMetricData(namespace, timersToSend, gaugesToSend, metersToSend);
    }

    private <T extends Number> SortedMap<String, Gauge<T>> getGuagesToSend(SortedMap<String, Gauge<T>> gauges) {
        SortedMap<String, Gauge<T>> gaugesToSend = new TreeMap<>();
        gauges.forEach((name, gauge) -> {
            if (gaugeInScope(name)) {
                gaugesToSend.put(createName(name), gauge);
            }
        });
        logger.debug("Sending gauges " + gaugesToSend.keySet());
        return gaugesToSend;
    }

    private SortedMap<String, Meter> getMetersToSend(SortedMap<String, Meter> meters) {
        SortedMap<String, Meter> metersToSend = new TreeMap<>();
        meters.forEach((name,meter) -> {
            if (meterInScope(name)) {
                // near environment here
                metersToSend.put(createLogMetricName(name), meter);
            }
        });
        logger.debug("Sending meters" + metersToSend.keySet());

        return metersToSend;
    }

    private SortedMap<String, Timer> getTimersToSend(SortedMap<String, Timer> timers) {
        SortedMap<String, Timer> timersToSend = new TreeMap<>();
        timers.forEach((name, timer) -> {
            logger.debug("Add timer " + name + " to cloud watch metric");
            if (timerInScope(name)) {
                timersToSend.put(createName(name),timer);
            }
        });
        logger.debug("Sending timers" + timersToSend.keySet());
        return timersToSend;
    }

    private String createLogMetricName(String name) {
        return name.replace(PREFIX_LOG_NAMESPACE, "");
    }

    private String createName(String name) {
        return name.replace(PREFIX, "");
    }

    private boolean timerInScope(String name) {
        if (!name.startsWith(PREFIX)) {
            return false;
        }

        return !name.endsWith(".filtering") && !name.endsWith(".total");
    }

    private boolean gaugeInScope(String name) {
        if (!name.startsWith(GAUGE_PREFIX)) {
            return false;
        }

        return !name.endsWith(".hitRate") && !name.endsWith(".missRate");
    }

    private boolean meterInScope(String name) {
        if (!name.startsWith(PREFIX_LOG_NAMESPACE)) {
            return false;
        }
        return (name.endsWith("error") || name.endsWith("warn"));
    }

    public String formNamespace(final String originalPrefix) {
        final String currentEnvironment = providesConfig.get("ENV");

        String namespace = Objects.requireNonNullElse(currentEnvironment, "Unknown");

        String prefix = originalPrefix;
        if (prefix.endsWith(".")) {
            prefix = prefix.substring(0,prefix.length()-1);
        }
        return namespace + ":" + prefix.replaceAll("\\.",":");
    }

}
