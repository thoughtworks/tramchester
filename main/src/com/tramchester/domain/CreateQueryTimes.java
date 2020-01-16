package com.tramchester.domain;

import com.tramchester.config.TramchesterConfig;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateQueryTimes {
    private TramchesterConfig config;

    public CreateQueryTimes(TramchesterConfig config) {
        this.config = config;
    }

    public List<TramTime> generate(TramTime initialQueryTime) {
        List<TramTime> result = new ArrayList<>();

        int interval = config.getQueryInterval();

        int minsToAdd = 0;
        while (minsToAdd<=config.getMaxWait()) {
            result.add(initialQueryTime.plusMinutes(minsToAdd));
            minsToAdd = minsToAdd + interval;
        }
        return result;
    }
}
