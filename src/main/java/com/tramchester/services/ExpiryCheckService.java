package com.tramchester.services;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.repository.ProvidesFeedInfo;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class ExpiryCheckService {
    private static final Logger logger = LoggerFactory.getLogger(ExpiryCheckService.class);

    private final LocalDate validUntil;
    private int days;

    public ExpiryCheckService(ProvidesFeedInfo providesFeedInfo, TramchesterConfig config) {
        this.days = config.getDataExpiryThreadhold();
        this.validUntil = providesFeedInfo.getFeedInfo().validUntil();

        logger.info(format("Will check for data expiring within %d days of %s", days, validUntil.toString()));
    }

    public void check(LocalDate currentDate, WhenCheckTriggered callback) {
        logger.info("Checking if data is expired or will expire soon");


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
