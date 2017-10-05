package com.tramchester.services;

import com.tramchester.repository.ProvidesFeedInfo;
import org.joda.time.LocalDate;

public class ExpiryCheckService {

    private ProvidesFeedInfo providesFeedInfo;
    private int days;

    public ExpiryCheckService(ProvidesFeedInfo providesFeedInfo, int days) {

        this.providesFeedInfo = providesFeedInfo;
        this.days = days;
    }

    public void check(LocalDate currentDate, WhenCheckTriggered callback) {
        LocalDate validUntil = providesFeedInfo.getFeedInfo().validUntil();

        if (currentDate.isAfter(validUntil) || currentDate.isEqual(validUntil)) {
           callback.triggered(true, validUntil);
           return;
        }

        LocalDate boundary = validUntil.minusDays(days);
        if (currentDate.isAfter(boundary) || currentDate.isEqual(boundary)) {
            callback.triggered(false, validUntil);
            return;
        }
    }

    public interface WhenCheckTriggered {
        void triggered(boolean hasAlreadyExpired, LocalDate validUntil);
    }

}
