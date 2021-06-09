package com.tramchester.healthchecks;

import com.google.inject.Inject;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.ClientForS3;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ServiceTimeLimits;

import static java.lang.String.format;

@LazySingleton
public class ClientForS3Healthcheck extends TramchesterHealthCheck {

    private final ClientForS3 clientForS3;
    private final TramchesterConfig config;

    @Inject
    public ClientForS3Healthcheck(ClientForS3 clientForS3, TramchesterConfig config, ServiceTimeLimits serviceTimeLimits) {
        super(serviceTimeLimits);
        this.clientForS3 = clientForS3;
        this.config = config;
    }

    @Override
    public String getName() {
        return "clientForS3";
    }

    @Override
    public boolean isEnabled() {
        return config.getLiveDataConfig()!=null;
    }

    @Override
    protected Result check() {
        LiveDataConfig liveDataConfig = config.getLiveDataConfig();

        if (clientForS3.isStarted()) {
            return Result.healthy(format("Running for %s %s", liveDataConfig.getS3Bucket(),
                    liveDataConfig.getS3Prefix()));
        }

        return Result.unhealthy(format("Not running for %s %s", liveDataConfig.getS3Bucket(),
                liveDataConfig.getS3Prefix()));
    }
}
