package com.tramchester.cloud;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.codahale.metrics.Timer;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

public class SendMetricsToCloudWatch {
    private static final Logger logger = LoggerFactory.getLogger(SendMetricsToCloudWatch.class);

    private AmazonCloudWatch client;

    public SendMetricsToCloudWatch() {

        // see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html
        // For local dev best to set AWS_REGION env var
        try {
            client = AmazonCloudWatchClientBuilder.defaultClient();
        }
        catch (com.amazonaws.SdkClientException exception) {
            logger.warn("Unable to init cloud watch client, no metrics will be sent", exception);
        }
    }

    private List<MetricDatum> createDatum(String name, Timer timer) {
        List<MetricDatum> result = new ArrayList<>();

        Date timestamp = Date.from(Instant.now());
        Dimension resourcesDimension = new Dimension().withName("tramchester").withValue("api");

        MetricDatum datumCount = new MetricDatum()
                .withMetricName(name)
                .withTimestamp(timestamp)
                .withValue(Double.valueOf(timer.getCount()))
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

    public void putMetricData(SortedMap<String, Timer> toSubmit, String nameSpace) {
        if (client==null) {
            logger.warn("No cloud watch client available, will not send metrics");
            return;
        }

        List<MetricDatum> metricDatum = new LinkedList<>();
        toSubmit.forEach((name,timer) -> metricDatum.addAll(createDatum(name,timer)));

        int batchSize = 20;
        List<MetricDatum> batch = formBatch(metricDatum, batchSize);
        while (!batch.isEmpty()) {

            try {
                PutMetricDataRequest request = new PutMetricDataRequest()
                        .withNamespace(nameSpace)
                        .withMetricData(batch);
                client.putMetricData(request);
                logger.info("Sent metrics with namespace " + nameSpace);
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
