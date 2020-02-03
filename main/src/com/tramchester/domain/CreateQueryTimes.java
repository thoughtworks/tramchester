package com.tramchester.domain;

import com.tramchester.config.TramchesterConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateQueryTimes {
    private TramchesterConfig config;

    public CreateQueryTimes(TramchesterConfig config) {
        this.config = config;
    }

    public List<TramTime> generate(TramTime initialQueryTime, boolean walkAtStart) {
        if (walkAtStart) {
            return Collections.singletonList(initialQueryTime);
        }

        List<TramTime> result = new ArrayList<>();

        int interval = config.getQueryInterval();
        int numberQueries = config.getNumberQueries();

        int minsToAdd = 0;
        for (int i = 0; i < numberQueries; i++) {
            result.add(initialQueryTime.plusMinutes(minsToAdd));
            minsToAdd = minsToAdd + interval;
        }

        return result;
    }
}
