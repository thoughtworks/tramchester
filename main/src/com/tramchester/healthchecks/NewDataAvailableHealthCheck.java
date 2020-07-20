package com.tramchester.healthchecks;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.URLDownloadAndModTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class NewDataAvailableHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(NewDataAvailableHealthCheck.class);

    private final DataSourceConfig config;
    private final URLDownloadAndModTime urlDownloader;
    private final FetchFileModTime fetchFileModTime;

    public NewDataAvailableHealthCheck(DataSourceConfig config, URLDownloadAndModTime urlDownloader, FetchFileModTime fetchFileModTime) {
        this.config = config;
        this.urlDownloader = urlDownloader;
        this.fetchFileModTime = fetchFileModTime;
    }

    @Override
    protected Result check() {
        try {
            Path dataPath = config.getDataPath();
            Path latestZipFile = dataPath.resolve(config.getZipFilename());
            LocalDateTime serverModTime = urlDownloader.getModTime(config.getTramDataCheckUrl());
            LocalDateTime zipModTime = fetchFileModTime.getFor(latestZipFile);

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

    @Override
    public String getName() {
        return "new data for " + config.getName();
    }
}
