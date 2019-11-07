package com.tramchester.cloud;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

import static java.lang.String.format;

public class SendMetricsToCloudWatch {
    private static final Logger logger = LoggerFactory.getLogger(SendMetricsToCloudWatch.class);
    private final Dimension countersDimenion;
    private final Dimension resourcesDimension;
    // lower limit set by AWS, request will thrown exception if between zero and this number
    public static final double LOWER_LIMIT = 8.515920e-109;

    private AmazonCloudWatch client;

    public SendMetricsToCloudWatch() {
        countersDimenion = new Dimension().withName("tramchester").withValue("counters");
        resourcesDimension = new Dimension().withName("tramchester").withValue("api");

        // see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html
        // For local dev best to set AWS_REGION env var
        try {
            client = AmazonCloudWatchClientBuilder.defaultClient();
        }
        catch (com.amazonaws.SdkClientException exception) {
            logger.warn("Unable to init cloud watch client, no metrics will be sent", exception);
        }
    }

    private List<MetricDatum> createTimerDatum(Date timestamp, String name, Timer timer) {
        List<MetricDatum> result = new ArrayList<>();

        result.add(new MetricDatum()
                .withMetricName(name)
                .withTimestamp(timestamp)
                .withValue(zeroFilter(timer.getCount()))
                .withUnit(StandardUnit.Count)
                .withDimensions(resourcesDimension));
        result.add(new MetricDatum()
                .withMetricName(name+"_15minsRate")
                .withTimestamp(timestamp)
                .withValue(zeroFilter(timer.getFifteenMinuteRate()))
                .withDimensions(resourcesDimension)
                .withUnit(StandardUnit.Count));
        return result;
    }

    private MetricDatum createGaugeDatum(Date timestamp, String name, Gauge<Integer> gauge) {
        return new MetricDatum()
                .withMetricName(name)
                .withTimestamp(timestamp)
                .withValue(zeroFilter(gauge.getValue()))
                .withUnit(StandardUnit.Count)
                .withDimensions(countersDimenion);

    }

    private List<MetricDatum> createMeterDatum(Date timestamp, String name, Meter meter) {
        List<MetricDatum> result = new ArrayList<>();

        result.add(new MetricDatum()
                .withMetricName(name)
                .withTimestamp(timestamp)
                .withValue(zeroFilter(meter.getCount()))
                .withUnit(StandardUnit.Count)
                .withDimensions(countersDimenion));

        result.add(new MetricDatum()
                .withMetricName(name+"_1minsRate")
                .withTimestamp(timestamp)
                .withValue(zeroFilter(meter.getOneMinuteRate()))
                .withUnit(StandardUnit.Count)
                .withDimensions(countersDimenion));

        return result;
    }

    public static Double zeroFilter(double value) {
        if (value<=LOWER_LIMIT) {
            return 0D;
        }
        return value;
    }

    public void putMetricData(String nameSpace, SortedMap<String, Timer> timers,
                              SortedMap<String, Gauge<Integer>> gauges, SortedMap<String, Meter> metersToSend) {
        if (client==null) {
            logger.warn("No cloud watch client available, will not send metrics");
            return;
        }
        Date timestamp = Date.from(Instant.now());

        List<MetricDatum> metricDatum = new LinkedList<>();
        timers.forEach((name,timer) -> metricDatum.addAll(createTimerDatum(timestamp, name, timer)));
        gauges.forEach((name,gauge)-> metricDatum.add(createGaugeDatum(timestamp, name, gauge)));
        metersToSend.forEach((name, meter) -> metricDatum.addAll(createMeterDatum(timestamp, name, meter)));

        int batchSize = 20;
        List<MetricDatum> batch = formBatch(metricDatum, batchSize);
        logger.info("Sent metrics with namespace " + nameSpace);

        while (!batch.isEmpty()) {
            try {
                PutMetricDataRequest request = new PutMetricDataRequest()
                        .withNamespace(nameSpace)
                        .withMetricData(batch);
                client.putMetricData(request);
            }
            catch (AmazonServiceException exception) {
                logger.error(format("Unable to log metrics to cloudwatch with namespace %s and batch %s",
                        nameSpace, batch), exception);
            }
            batch = formBatch(metricDatum, batchSize);
        }
    }

    private List<MetricDatum> formBatch(List<MetricDatum> source, int batchSize) {
        List<MetricDatum> result = new ArrayList<>();
        int top = (batchSize>source.size()) ? source.size() : batchSize;
        for (int i = 0; i < top; i++) {
            result.add(source.remove(0));
        }
        return result;
    }
}
