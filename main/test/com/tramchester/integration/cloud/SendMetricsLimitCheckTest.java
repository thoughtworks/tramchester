package com.tramchester.integration.cloud;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.InvalidParameterValueException;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.tramchester.cloud.SendMetricsToCloudWatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

class SendMetricsLimitCheckTest {

    private AmazonCloudWatch client;
    private double belowLimit;

    // reproduce "issue" with cloudwatch metrcis, from the docs:
    // "Values must be in the range of 8.515920e-109 to 1.174271e+108"
    // i.e. between 0 and 8.515920e-109 an exception is thrown

    @BeforeEach
    void beforeEachTestRuns() {
        client = AmazonCloudWatchClientBuilder.defaultClient();
        belowLimit = SendMetricsToCloudWatch.LOWER_LIMIT / 10D;

    }

    @Test
    void shouldReportZeroOk() {
        sendDatum(createDatum(0D, "testZero"));
    }

    @Test
    void shouldReportAtLowerLimitOk() {
        sendDatum(createDatum(SendMetricsToCloudWatch.LOWER_LIMIT, "lowerLimit"));
    }

    @Test
    void shouldThrowBelowLowerLimitOk() {
        try {
            sendDatum(createDatum(belowLimit, "belowLimitShouldThrow"));
            Assertions.fail("expected to throw");
        }
        catch (InvalidParameterValueException expectedException) {
            // expected
        }
    }

    @Test
    void shouldConvertToZeroCorrectly() {
        sendDatum(createDatum(SendMetricsToCloudWatch.zeroFilter(belowLimit), "filteredBelowLimit"));
    }

    private void sendDatum(MetricDatum datum) {
        PutMetricDataRequest request = new PutMetricDataRequest()
                .withNamespace("Unknown:com:tramchester")
                .withMetricData(datum);
        client.putMetricData(request);
    }

    private MetricDatum createDatum(double sampleValue, String metricName) {
        Date timestamp = Date.from(Instant.now());
        return new MetricDatum()
                .withMetricName(metricName)
                .withTimestamp(timestamp)
                .withValue(sampleValue)
                .withUnit(StandardUnit.Count);
    }


}
