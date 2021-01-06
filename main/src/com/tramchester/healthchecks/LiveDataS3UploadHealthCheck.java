package com.tramchester.healthchecks;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.cloud.data.DownloadsLiveData;
import com.tramchester.config.LiveDataConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.time.ProvidesLocalNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@LazySingleton
public class LiveDataS3UploadHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(LiveDataS3UploadHealthCheck.class);

    private final ProvidesLocalNow providesLocalNow;
    private final DownloadsLiveData downloadsLiveData;
    private final LiveDataConfig config;

    private Duration checkDuration;

    @Inject
    public LiveDataS3UploadHealthCheck(ProvidesLocalNow providesLocalNow, DownloadsLiveData downloadsLiveData, TramchesterConfig config) {
        this.providesLocalNow = providesLocalNow;
        this.downloadsLiveData = downloadsLiveData;
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
    public Result check() throws Exception {
        logger.info("Check for live data in S3");
        LocalDateTime checkTime = providesLocalNow.getDateTime().minus(checkDuration);

        Stream<StationDepartureInfoDTO> results = downloadsLiveData.downloadFor(checkTime, checkDuration);
        long number = results.count();
        results.close();

        if (number==0) {
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
