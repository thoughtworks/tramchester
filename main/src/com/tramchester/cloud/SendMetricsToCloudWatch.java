package com.tramchester.cloud;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import javax.inject.Inject;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;

@LazySingleton
public class SendMetricsToCloudWatch {
    private static final Logger logger = LoggerFactory.getLogger(SendMetricsToCloudWatch.class);
    private final List<Dimension> countersDimenion;
    private final List<Dimension> resourcesDimension;

    // lower limit set by AWS, request will thrown exception if between zero and this number
    public static final double LOWER_LIMIT = 8.515920e-109;
    private final ProvidesNow providesNow;

    private CloudWatchClient client;

    // TODO Pass in provides local now

    @Inject
    public SendMetricsToCloudWatch(ProvidesNow providesNow) {
        this.providesNow = providesNow;
        countersDimenion = Collections.singletonList(Dimension.builder().name("tramchester").value("counters").build());
        resourcesDimension = Collections.singletonList(Dimension.builder().name("tramchester").value("api").build());

        // see http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/java-dg-region-selection.html
        // For local dev best to set AWS_REGION env var
        try {
            client = CloudWatchClient.create();
        }
        catch (AwsServiceException | SdkClientException exception) {
            logger.warn("Unable to init cloud watch client, no metrics will be sent", exception);
        }
    }

    private List<MetricDatum> createTimerDatums(Instant timestamp, String name, Timer timer) {
        List<MetricDatum> result = new ArrayList<>();

        result.add(createMetricDatum(name, timestamp, timer.getCount(), resourcesDimension));
        result.add(createMetricDatum(name+"_15minsRate", timestamp, timer.getFifteenMinuteRate(), resourcesDimension));
        result.add(createMetricDatum(name+"_1minsRate", timestamp, timer.getOneMinuteRate(), resourcesDimension));
        result.add(createMetricDatum(name+"_mean", timestamp, timer.getMeanRate(), resourcesDimension));

        return result;
    }

    private <T extends Number> MetricDatum createMetricDatum(String name, Instant timestamp, T metric, List<Dimension> dimension) {
        return MetricDatum.builder()
                .metricName(name)
                .timestamp(timestamp)
                .value(zeroFilter(metric))
                .unit(StandardUnit.COUNT)
                .dimensions(dimension)
                .build();
    }

    private <T extends Number> MetricDatum createGaugeDatum(Instant timestamp, String name, Gauge<T> gauge) {
        return createMetricDatum(name, timestamp, gauge.getValue(), countersDimenion);
    }

    private List<MetricDatum> createMeterDatum(Instant timestamp, String name, Meter meter) {
        List<MetricDatum> result = new ArrayList<>();

        result.add(createMetricDatum(name, timestamp, meter.getCount(), countersDimenion));
        result.add(createMetricDatum(name+"_1minsRate", timestamp, meter.getOneMinuteRate(), countersDimenion));

        return result;
    }

    public static <T extends Number> Double zeroFilter(T value) {
        double doubleValue = value.doubleValue();
        if (doubleValue <=LOWER_LIMIT) {
            return 0D;
        }
        return doubleValue;
    }

    public <T extends Number> void putMetricData(String nameSpace, SortedMap<String, Timer> timers,
                              SortedMap<String, Gauge<T>> intGauges, SortedMap<String, Meter> metersToSend) {
        if (client==null) {
            logger.warn("No cloud watch client available, will not send metrics");
            return;
        }

        // must be resolution of seconds
        // https://github.com/aws/aws-sdk-java-v2/issues/1820
        Instant timestamp = providesNow.getDateTime().toInstant(ZoneOffset.UTC).truncatedTo(SECONDS);

        List<MetricDatum> metricDatum = new LinkedList<>();
        timers.forEach((name,timer) -> metricDatum.addAll(createTimerDatums(timestamp, name, timer)));
        intGauges.forEach((name,gauge)-> metricDatum.add(createGaugeDatum(timestamp, name, gauge)));
        metersToSend.forEach((name, meter) -> metricDatum.addAll(createMeterDatum(timestamp, name, meter)));

        int batchSize = 20;
        List<MetricDatum> batch = formBatch(metricDatum, batchSize);
        logger.info("Sent metrics with namespace " + nameSpace);

        while (!batch.isEmpty()) {
            try {
                PutMetricDataRequest request = PutMetricDataRequest.builder()
                        .namespace(nameSpace)
                        .metricData(batch).build();
                client.putMetricData(request);
            }
            catch (AwsServiceException exception) {
                logger.error(format("Unable to log metrics to cloudwatch with namespace %s and batch %s",
                        nameSpace, batch), exception);
            }
            batch = formBatch(metricDatum, batchSize);
        }
    }

    private List<MetricDatum> formBatch(List<MetricDatum> source, int batchSize) {
        List<MetricDatum> result = new ArrayList<>();
        int top = Math.min(batchSize, source.size());
        for (int i = 0; i < top; i++) {
            result.add(source.remove(0));
        }
        return result;
    }
}
