package com.tramchester.unit.cloud;


import com.codahale.metrics.*;
import com.tramchester.cloud.CloudWatchReporter;
import com.tramchester.cloud.ConfigFromInstanceUserData;
import com.tramchester.cloud.SendMetricsToCloudWatch;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.SortedMap;
import java.util.TreeMap;

class CloudWatchReporterTest extends EasyMockSupport {

    private ConfigFromInstanceUserData providesConfig;
    private SendMetricsToCloudWatch sender;
    private CloudWatchReporter reporter;

    @BeforeEach
    void beforeEachTestRuns() {
        providesConfig = createMock(ConfigFromInstanceUserData.class);
        sender = createMock(SendMetricsToCloudWatch.class);

        MetricRegistry metricRegistry = new MetricRegistry();

        reporter = new CloudWatchReporter(metricRegistry, "name", providesConfig, sender);

    }

    @Test
    void shouldFormNamespaceCorrectly() {

        // the interface itself uses raw Gauge
        SortedMap<String, Gauge> unTypedGauges = new TreeMap<>();
        SortedMap<String, Counter > unused_counters = new TreeMap<>();
        SortedMap<String, Histogram > unused_histograms = new TreeMap<>();
        SortedMap<String, Meter > meters = new TreeMap<>();
        SortedMap<String, Timer> timers = new TreeMap<>();

        // meters
        Meter expectedMeter = new Meter();
        meters.put("ignoreMe", new Meter());
        meters.put("ch.qos.logback.core.Appender.error", expectedMeter);
        meters.put("ch.qos.logback.core.Appender.ignore", new Meter());

        // gauge
        TestGauge expectedGauge = new TestGauge(42);
        unTypedGauges.put("ignored", new TestGauge(1));
        unTypedGauges.put("com.tramchester.something.hitNumber", expectedGauge);
        unTypedGauges.put("com.tramchester.ignoreme.hitRate", new TestGauge(2));
        unTypedGauges.put("com.tramchester.ignoreme.missRate", new TestGauge(3));

        // timers
        Timer expectedTimer = new Timer();
        timers.put("ignoreMe", new Timer());
        timers.put("com.tramchester.ignored.filtering", new Timer());
        timers.put("com.tramchester.ignored.total", new Timer());
        timers.put("com.tramchester.include.me", expectedTimer);

        EasyMock.expect(providesConfig.get("ENV")).andReturn("environment");

        SortedMap<String, Timer> timersToSend = new TreeMap<>();
        SortedMap<String, Gauge<Number>> gaugesToSend = new TreeMap<>();
        SortedMap<String, Meter> metersToSend = new TreeMap<>();

        metersToSend.put("error", expectedMeter);
        gaugesToSend.put("something.hitNumber", expectedGauge);
        timersToSend.put("include.me", expectedTimer);

        sender.putMetricData("environment:com:tramchester", timersToSend, gaugesToSend, metersToSend);
        EasyMock.expectLastCall();

        replayAll();
        reporter.report(unTypedGauges, unused_counters, unused_histograms, meters, timers);
        verifyAll();
    }

    public static class TestGauge implements Gauge<Number> {

        private final Number number;

        public TestGauge(Number number) {
            this.number = number;
        }

        @Override
        public Number getValue() {
            return number;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestGauge testGauge = (TestGauge) o;
            return number.equals(testGauge.number);
        }

        @Override
        public String toString() {
            return "TestGauge{" +
                    "number=" + number +
                    '}';
        }
    }
}
