package com.tramchester.healthchecks;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.URLDownloadAndModTime;
import com.tramchester.domain.ServiceTimeLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;

public class NewDataAvailableHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(NewDataAvailableHealthCheck.class);

    private final RemoteDataSourceConfig config;
    private final URLDownloadAndModTime urlDownloader;
    private final FetchFileModTime fetchFileModTime;

    public NewDataAvailableHealthCheck(RemoteDataSourceConfig config, URLDownloadAndModTime urlDownloader,
                                       FetchFileModTime fetchFileModTime, ServiceTimeLimits serviceTimeLimits) {
        super(serviceTimeLimits);
        this.config = config;
        this.urlDownloader = urlDownloader;
        this.fetchFileModTime = fetchFileModTime;
    }

    @Override
    protected Result check() {
        String dataCheckUrl = config.getDataCheckUrl();

        try {

            LocalDateTime serverModTime = urlDownloader.getModTime(dataCheckUrl);
            LocalDateTime zipModTime = fetchFileModTime.getFor(config);

            String diag = String.format("Local zip mod time: %s Server mod time: %s Url: %s",
                    zipModTime, serverModTime, dataCheckUrl);
            if (serverModTime.isAfter(zipModTime)) {
                String msg = "Newer data is available " + diag;
                logger.warn(msg);
                return Result.unhealthy(msg);
            } else if (serverModTime.equals(LocalDateTime.MIN)) {
                String msg = "Source is missing, cannot check for new data at " + dataCheckUrl;
                logger.error(msg);
                return Result.unhealthy(msg);
            }
            else {
                String msg = "No newer data is available " + diag;
                logger.info(msg);
                return Result.healthy(msg);
            }
        } catch (IOException ioException) {
            String msg = "Unable to check for newer data at " + dataCheckUrl;
            logger.error(msg, ioException);
            return Result.unhealthy(msg + ioException.getMessage());
        }
    }

    @Override
    public String getName() {
        return "new data for " + config.getName();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
