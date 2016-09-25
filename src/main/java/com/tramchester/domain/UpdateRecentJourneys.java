package com.tramchester.domain;

import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateRecentJourneys {

    private int limit;

    public UpdateRecentJourneys(int limit) {
        this.limit = limit;
    }

    public RecentJourneys processFrom(RecentJourneys recentJourneys, String fromId) {
        Timestamped timestamped = new Timestamped(fromId, DateTime.now());
        Set<Timestamped> from = new HashSet<>();
        from.addAll(recentJourneys.getFrom());
        if (from.contains(timestamped)) {
            from.remove(timestamped);
        } else if (from.size()>=limit) {
            Timestamped last = findOldest(from);
            from.remove(last);
        }
        from.add(timestamped);
        return new RecentJourneys().setFrom(from);
    }

    private Timestamped findOldest(Set<Timestamped> from) {
        List<Timestamped> ordered = from.stream().sorted(Timestamped::compare).collect(Collectors.toList());
        return ordered.get(0);
    }

}
