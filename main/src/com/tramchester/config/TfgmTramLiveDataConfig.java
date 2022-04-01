package com.tramchester.config;

public interface TfgmTramLiveDataConfig {
    String getDataUrl();

    String getDataSubscriptionKey();

    String getS3Bucket();

    Long getRefreshPeriodSeconds();

    int getMaxNumberStationsWithoutMessages();

    int getMaxNumberStationsWithoutData();

    String getS3Prefix();
}
