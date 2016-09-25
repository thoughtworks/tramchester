package com.tramchester.domain;

import java.util.Set;

public class RecentJourneys {
    private Set<Timestamped> timestamps;

    public RecentJourneys setFrom(Set<Timestamped> fromStationsIds) {
        this.timestamps = fromStationsIds;
        return this;
    }

    public Set<Timestamped> getFrom() {
        return timestamps;
    }
}
