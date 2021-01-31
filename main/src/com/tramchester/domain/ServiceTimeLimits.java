package com.tramchester.domain;

import com.netflix.governator.guice.lazy.LazySingleton;

import java.time.LocalTime;

/**
 * Core operating hours, just for tram currently, used to disable healthchecks during hours when ok for live data to be missing
 */
@LazySingleton
public class ServiceTimeLimits {

    // https://tfgm.com/public-transport/tram/tram-times

    private final LocalTime begin = LocalTime.of(6,0);
    private final LocalTime end = LocalTime.of(23,45);

    public boolean within(LocalTime query) {
        if (query.equals(begin) || query.equals(end)) {
            return true;
        }
        return (query.isAfter(begin) && query.isBefore(end));
    }


}
