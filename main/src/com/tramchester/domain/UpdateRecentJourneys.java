package com.tramchester.domain;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.presentation.Timestamped;
import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class UpdateRecentJourneys {
    private static final Logger logger = LoggerFactory.getLogger(UpdateRecentJourneys.class);

    private final int limit;

    @Inject
    public UpdateRecentJourneys(TramchesterConfig config) {
        this.limit = config.getRecentStopsToShow();
        if (limit<=0) {
            logger.warn("Limit on recent journeys set to " + limit);
        }
    }

    public RecentJourneys createNewJourneys(RecentJourneys recentJourneys, ProvidesNow providesNow, Location<?> location) {
        Timestamped timestamped = new Timestamped(location, providesNow.getDateTime());
        Set<Timestamped> from = new HashSet<>(recentJourneys.getRecentIds());
        if (from.contains(timestamped)) {
            from.remove(timestamped);
        } else if (from.size()>=limit) {
            Timestamped last = findOldest(from);
            from.remove(last);
        }
        from.add(timestamped);
        return new RecentJourneys().setTimestamps(from);
    }

    private Timestamped findOldest(Set<Timestamped> from) {
        List<Timestamped> ordered = from.stream().sorted(Timestamped::compare).collect(Collectors.toList());
        return ordered.get(0);
    }

}
