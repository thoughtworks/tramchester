package com.tramchester.cloud;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.codahale.metrics.Timer;
import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;

public class SendMetricsToCloudWatch {
    private static final Logger logger = LoggerFactory.getLogger(SendMetricsToCloudWatch.class);

    private AmazonCloudWatch client;

    public SendMetricsToCloudWatch() {

        // see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html
        // For local dev best to set AWS_REGION env var
        client = AmazonCloudWatchClientBuilder.defaultClient();
    }

    private MetricDatum createDatum(String name, Timer timer) {
        Date timestamp = Date.from(Instant.now());

        MetricDatum datum = new MetricDatum()
                .withMetricName(name + "_count")
                .withTimestamp(timestamp)
                .withValue(Double.valueOf(timer.getCount()))
                .withUnit(StandardUnit.Count);
        return datum;
    }

    public void putMetricData(SortedMap<String, Timer> toSubmit, String nameSpace) {
        List<MetricDatum> metricDatum = new LinkedList<>();
        toSubmit.forEach((name,timer) -> metricDatum.add(createDatum(name,timer)));

        try {
            PutMetricDataRequest request = new PutMetricDataRequest()
                    .withNamespace(nameSpace)
                    .withMetricData(metricDatum);
            client.putMetricData(request);
            logger.info("Sent metric with namespace " + nameSpace);
        }
        catch (AmazonServiceException exception) {
            logger.warn("Unable to log metrics to cloudwatch with namespace "+nameSpace,exception);
        }
    }
}
