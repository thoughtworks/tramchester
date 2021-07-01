package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ServiceTimeLimits;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.livedata.CountsUploadedLiveData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@LazySingleton
public class LiveDataS3UploadHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataS3UploadHealthCheck.class);

    private final ProvidesNow providesNow;
    private final CountsUploadedLiveData countsUploadedLiveData;
    private final LiveDataConfig config;

    private Duration checkDuration;

    @Inject
    public LiveDataS3UploadHealthCheck(ProvidesNow providesNow, CountsUploadedLiveData countsUploadedLiveData,
                                       TramchesterConfig config, ServiceTimeLimits serviceTimeLimits) {
        super(serviceTimeLimits);
        this.providesNow = providesNow;
        this.countsUploadedLiveData = countsUploadedLiveData;
        this.config = config.getLiveDataConfig();
    }

    @Override
    public boolean isEnabled() {
        return config!=null;
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (isEnabled()) {
            checkDuration = Duration.of(2 * config.getRefreshPeriodSeconds(), ChronoUnit.SECONDS);
            logger.info("started");
        } else {
            logger.warn("disabled");
        }
    }

    @Override
    public String getName() {
        return "liveDataS3Upload";
    }

    @Override
    public Result check() {
        logger.info("Check for live data in S3");
        LocalDateTime checkTime = providesNow.getDateTime().minus(checkDuration);

        long number = countsUploadedLiveData.count(checkTime, checkDuration);

        if (number==0 && (!isLateNight(checkTime))) {
                String msg = "No live data found in S3 at " + checkTime + " for " + checkDuration;
                logger.error(msg);
                return Result.unhealthy(msg);
        } else {
            String msg = "Found " + number + " records in S3 at " + checkTime + " and duration " + checkDuration;
            logger.info(msg);
            return Result.healthy(msg);
        }

    }
}
