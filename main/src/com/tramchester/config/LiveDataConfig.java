package com.tramchester.config;

public interface LiveDataConfig {
    String getDataUrl();

    String getDataSubscriptionKey();

    String getS3Bucket();

    Long getRefreshPeriodSeconds();

    int getMaxNumberStationsWithoutMessages();

    int getMaxNumberStationsWithoutData();

    String getS3Prefix();
}
