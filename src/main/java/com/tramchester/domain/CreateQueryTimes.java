package com.tramchester.domain;

import com.tramchester.config.TramchesterConfig;

import java.util.ArrayList;
import java.util.List;

public class CreateQueryTimes {
    private TramchesterConfig config;

    public CreateQueryTimes(TramchesterConfig config) {
        this.config = config;
    }

    public List<Integer> generate(int initialQueryTime) {
        List<Integer> result = new ArrayList<>();
        int queryTime = initialQueryTime;
        int interval = config.getQueryInterval();
        int limit = initialQueryTime + config.getMaxWait();
        while (queryTime< limit) {
            result.add(queryTime);
            queryTime = queryTime + interval;
        }
        return result;
    }
}
