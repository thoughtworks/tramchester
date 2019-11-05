package com.tramchester.cloud;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

public class SendMetricsToCloudWatch {
    private static final Logger logger = LoggerFactory.getLogger(SendMetricsToCloudWatch.class);
    private final Dimension countersDimenion;
    private final Dimension resourcesDimension;

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

        MetricDatum datumCount = new MetricDatum()
                .withMetricName(name)
                .withTimestamp(timestamp)
                .withValue((double) timer.getCount())
                .withUnit(StandardUnit.Count)
                .withDimensions(resourcesDimension);
        result.add(datumCount);
        MetricDatum datumRate = new MetricDatum()
                .withMetricName(name+"_15minsRate")
                .withTimestamp(timestamp)
                .withValue(timer.getFifteenMinuteRate())
                .withDimensions(resourcesDimension)
                .withUnit(StandardUnit.Count);
        result.add(datumRate);
        return result;
    }

    private MetricDatum createGaugeDatum(Date timestamp, String name, Gauge<Integer> gauge) {

        return new MetricDatum()
                .withMetricName(name)
                .withTimestamp(timestamp)
                .withValue(Double.valueOf(gauge.getValue()))
                .withUnit(StandardUnit.Count)
                .withDimensions(countersDimenion);

    }

    public void putMetricData(String nameSpace, SortedMap<String, Timer> timers,
                              SortedMap<String, Gauge<Integer>> gauges) {
        if (client==null) {
            logger.warn("No cloud watch client available, will not send metrics");
            return;
        }
        Date timestamp = Date.from(Instant.now());

        List<MetricDatum> metricDatum = new LinkedList<>();
        timers.forEach((name,timer) -> metricDatum.addAll(createTimerDatum(timestamp, name, timer)));
        gauges.forEach((name,gauge)-> metricDatum.add(createGaugeDatum(timestamp, name, gauge)));

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
                logger.warn("Unable to log metrics to cloudwatch with namespace "+nameSpace,exception);
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
