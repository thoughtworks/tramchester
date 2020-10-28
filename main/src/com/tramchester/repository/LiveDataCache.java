package com.tramchester.repository;

import com.tramchester.domain.liveUpdates.StationDepartureInfo;

import java.util.List;

public interface LiveDataCache {
    int updateCache(List<StationDepartureInfo> departureInfos);
}
