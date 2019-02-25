package com.tramchester.repository;

import com.tramchester.domain.liveUpdates.StationDepartureInfo;

import java.util.Collection;

public interface LiveDataObserver {
    boolean seenUpdate(Collection<StationDepartureInfo> update);
}
