package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.SendMetricsToCloudWatch;
import com.tramchester.domain.ServiceTimeLimits;

import javax.inject.Inject;

@LazySingleton
public class SendMetricsToCloudWatchHealthcheck extends TramchesterHealthCheck {

    private final SendMetricsToCloudWatch sendMetricsToCloudWatch;

    @Inject
    public SendMetricsToCloudWatchHealthcheck(SendMetricsToCloudWatch sendMetricsToCloudWatch, ServiceTimeLimits serviceTimeLimits) {
        super(serviceTimeLimits);
        this.sendMetricsToCloudWatch = sendMetricsToCloudWatch;
    }


    @Override
    public String getName() {
        return "sendMetricsToCloudWatch";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected Result check() throws Exception {
        if (sendMetricsToCloudWatch.started()) {
            return Result.healthy();
        }

        return Result.unhealthy("Unable to upload cloud watch metrics");
    }
}
