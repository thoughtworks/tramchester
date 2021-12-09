package com.tramchester.domain.time;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.StationWalk;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CreateQueryTimes {
    private final TramchesterConfig config;

    @Inject
    public CreateQueryTimes(TramchesterConfig config) {
        this.config = config;
    }

    public List<TramTime> generate(TramTime initialQueryTime, Set<StationWalk> walksAtStart) {

        return generate(initialQueryTime);

        //result.add(initialQueryTime);

//        List<TramTime> result = walksAtStart.stream().
//                map(walk -> initialQueryTime.minusMinutes(walk.getCost())).
//                sorted().
//                collect(Collectors.toList());
//
//        int interval = config.getQueryInterval();
//        int numberQueries = config.getNumberQueries();
//
//        int minsToAdd = 0;
//        for (int i = 0; i < numberQueries; i++) {
//            result.add(initialQueryTime.plusMinutes(minsToAdd));
//            minsToAdd = minsToAdd + interval;
//        }
//
//        return result;
    }

    public List<TramTime> generate(TramTime initialQueryTime) {
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
