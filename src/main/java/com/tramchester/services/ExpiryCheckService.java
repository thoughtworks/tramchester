package com.tramchester.services;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.repository.ProvidesFeedInfo;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class ExpiryCheckService {
    private static final Logger logger = LoggerFactory.getLogger(ExpiryCheckService.class);

    private ProvidesFeedInfo providesFeedInfo;
    private int days;

    public ExpiryCheckService(ProvidesFeedInfo providesFeedInfo, TramchesterConfig config) {
        this.providesFeedInfo = providesFeedInfo;
        this.days = config.getDataExpiryThreadhold();

        logger.info(format("Will check for data expiring within %d days ", days));
    }

    public void check(LocalDate currentDate, WhenCheckTriggered callback) {
        LocalDate validUntil = providesFeedInfo.getFeedInfo().validUntil();

        logger.info(format("Checking if data is expired or will expire with %d days of %s", days, validUntil));

        if (currentDate.isAfter(validUntil) || currentDate.isEqual(validUntil)) {
           callback.triggered(true, validUntil);
           return;
        }

        LocalDate boundary = validUntil.minusDays(days);
        if (currentDate.isAfter(boundary) || currentDate.isEqual(boundary)) {
            callback.triggered(false, validUntil);
            return;
        }

        logger.info("Data is not due to expire until " +validUntil.toString());
    }

    public interface WhenCheckTriggered {
        void triggered(boolean hasAlreadyExpired, LocalDate validUntil);
    }

}
