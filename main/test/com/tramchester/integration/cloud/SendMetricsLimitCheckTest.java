package com.tramchester.integration.cloud;

import com.tramchester.cloud.SendMetricsToCloudWatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

class SendMetricsLimitCheckTest {

    private CloudWatchClient client;
    private double belowLimit;
    private Instant now;

    // reproduce "issue" with cloudwatch metrcis, from the docs:
    // "Values must be in the range of 8.515920e-109 to 1.174271e+108"
    // i.e. between 0 and 8.515920e-109 an exception is thrown

    @BeforeEach
    void beforeEachTestRuns() {
        client =  CloudWatchClient.create();
        belowLimit = SendMetricsToCloudWatch.LOWER_LIMIT / 10000D;

        now = LocalDateTime.now().toInstant(ZoneOffset.UTC);
    }

    @Test
    void shouldReportZeroOk() {
        sendDatum(createDatum(0D, "testZero", now));
    }

    @Test
    void shouldReportAtLowerLimitOk() {
        sendDatum(createDatum(SendMetricsToCloudWatch.LOWER_LIMIT, "lowerLimit", now));
    }

    @Test
    void shouldConvertToZeroCorrectly() {
        sendDatum(createDatum(SendMetricsToCloudWatch.zeroFilter(belowLimit), "filteredBelowLimit", now));
    }

    private void sendDatum(MetricDatum datum) {
        PutMetricDataRequest request = PutMetricDataRequest.builder()
                .namespace("Unknown:com:tramchester")
                .metricData(datum).build();
        client.putMetricData(request);
    }

    private MetricDatum createDatum(double sampleValue, String metricName, Instant now) {
        return MetricDatum.builder()
                .metricName(metricName)
                .timestamp(now.truncatedTo(ChronoUnit.SECONDS))
                .value(sampleValue)
                .unit(StandardUnit.COUNT).build();
    }


}
