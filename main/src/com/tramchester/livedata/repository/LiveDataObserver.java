package com.tramchester.livedata.repository;

import com.tramchester.livedata.tfgm.StationDepartureInfo;

import java.util.Collection;

public interface LiveDataObserver {
    boolean seenUpdate(Collection<StationDepartureInfo> update);
}
