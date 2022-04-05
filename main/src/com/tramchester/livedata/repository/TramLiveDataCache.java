package com.tramchester.livedata.repository;

import com.tramchester.livedata.tfgm.TramStationDepartureInfo;

import java.util.List;

public interface TramLiveDataCache {
    int updateCache(List<TramStationDepartureInfo> departureInfos);
}
