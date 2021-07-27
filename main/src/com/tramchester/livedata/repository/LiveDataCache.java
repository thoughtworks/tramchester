package com.tramchester.livedata.repository;

import com.tramchester.livedata.domain.liveUpdates.StationDepartureInfo;

import java.util.List;

public interface LiveDataCache {
    int updateCache(List<StationDepartureInfo> departureInfos);
}
