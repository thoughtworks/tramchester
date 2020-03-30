package com.tramchester.healthchecks;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.repository.ProvidesFeedInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

import static java.lang.String.format;

public class DataExpiryHealthCheck extends TramchesterHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(DataExpiryHealthCheck.class);

    private final ProvidesFeedInfo providesFeedInfo;
    private final TramchesterConfig config;
    private final ProvidesLocalNow providesLocalNow;

    public DataExpiryHealthCheck(ProvidesFeedInfo providesFeedInfo, ProvidesLocalNow providesLocalNow, TramchesterConfig config) {
        this.providesFeedInfo = providesFeedInfo;
        this.providesLocalNow = providesLocalNow;
        this.config = config;
    }

    @Override
    public Result check() {
        return checkForDate(providesLocalNow.getDate());
    }

    public Result checkForDate(LocalDate currentDate) {
        int days = config.getDataExpiryThreadhold();

        LocalDate validUntil = providesFeedInfo.getFeedInfo().validUntil();

        logger.info(format("Checking if data is expired or will expire with %d days of %s", days, validUntil));

        if (currentDate.isAfter(validUntil) || currentDate.isEqual(validUntil)) {
            String message = "Tram data expired on " + validUntil.toString();
            logger.error(message);
            return Result.unhealthy(message);
        }

        LocalDate boundary = validUntil.minusDays(days);
        if (currentDate.isAfter(boundary) || currentDate.isEqual(boundary)) {
            String message = "Tram data will expire on " + validUntil.toString();
            return Result.unhealthy(message);
        }

        String message = "Data is not due to expire until " + validUntil.toString();
        logger.info(message);
        return Result.healthy(message);
    }

    @Override
    public String getName() {
        return "dataExpiry";
    }
}
