package com.tramchester.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.repository.ProvidesFeedInfo;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class DataExpiryHealthCheck extends HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(DataExpiryHealthCheck.class);

    private ProvidesFeedInfo providesFeedInfo;
    private TramchesterConfig config;

    public DataExpiryHealthCheck(ProvidesFeedInfo providesFeedInfo, TramchesterConfig config) {
        this.providesFeedInfo = providesFeedInfo;
        this.config = config;
    }

    @Override
    public Result check() {
        LocalDate currentDate = LocalDate.now();
        return checkForDate(currentDate);
    }

    public Result checkForDate(LocalDate currentDate) {
        int days = config.getDataExpiryThreadhold();

        LocalDate validUntil = providesFeedInfo.getFeedInfo().validUntil();

        logger.info(format("Checking if data is expired or will expire with %d days of %s", days, validUntil));

        if (currentDate.isAfter(validUntil) || currentDate.isEqual(validUntil)) {
            return Result.unhealthy("Tram data expired on " + validUntil.toString());
        }

        LocalDate boundary = validUntil.minusDays(days);
        if (currentDate.isAfter(boundary) || currentDate.isEqual(boundary)) {
            return Result.unhealthy("Tram data will expire on " + validUntil.toString());
        }

        return Result.healthy("Data is not due to expire until " +validUntil.toString());
    }
}
