package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchDataFromUrl;
import com.tramchester.dataimport.FileModTime;
import com.tramchester.dataimport.URLDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class NewDataAvailableHealthCheck extends HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(NewDataAvailableHealthCheck.class);

    private final TramchesterConfig config;
    private final URLDownloader urlDownloader;
    private final FileModTime fileModTime;

    public NewDataAvailableHealthCheck(TramchesterConfig config, URLDownloader urlDownloader, FileModTime fileModTime) {
        this.config = config;
        this.urlDownloader = urlDownloader;
        this.fileModTime = fileModTime;
    }

    @Override
    protected Result check() {
        try {
            Path dataPath = config.getDataPath();
            Path latestZipFile = dataPath.resolve(FetchDataFromUrl.ZIP_FILENAME);
            LocalDateTime serverModTime = urlDownloader.getModTime(config.getTramDataCheckUrl());
            LocalDateTime zipModTime = fileModTime.getFor(latestZipFile);

            String diag = String.format("Local zip mod time: %s Server mod time: %s", zipModTime, serverModTime);
            if (serverModTime.isAfter(zipModTime)) {
                String msg = "Newer timetable is available " + diag;
                logger.warn(msg);
                return Result.unhealthy(msg);
            } else {
                String msg = "No newer timetable is available " + diag;
                logger.info(msg);
                return Result.healthy(msg);
            }
        } catch (IOException ioException) {
            logger.warn("Unable to check for newer timetable data", ioException);
            return Result.unhealthy("Unable to check for newer data " + ioException.getMessage());
        }
    }

}
