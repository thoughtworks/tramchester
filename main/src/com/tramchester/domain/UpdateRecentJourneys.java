package com.tramchester.domain;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.time.ProvidesNow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateRecentJourneys {
    private static final Logger logger = LoggerFactory.getLogger(UpdateRecentJourneys.class);

    private final int limit;

    public UpdateRecentJourneys(TramchesterConfig config) {
        this.limit = config.getRecentStopsToShow();
        if (limit<=0) {
            logger.warn("Limit on recent journeys set to " + limit);
        }
    }

    public RecentJourneys createNewJourneys(RecentJourneys recentJourneys, ProvidesNow providesNow, String stationId) {
        Timestamped timestamped = new Timestamped(stationId, providesNow.getDateTime());
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
