package com.tramchester.livedata.repository;

import com.tramchester.livedata.tfgm.TramStationDepartureInfo;

import java.util.Collection;

public interface LiveDataObserver {
    boolean seenUpdate(Collection<TramStationDepartureInfo> update);
}
