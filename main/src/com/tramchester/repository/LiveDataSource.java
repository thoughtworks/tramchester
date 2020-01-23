package com.tramchester.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;

import java.util.List;
import java.util.Optional;

public interface LiveDataSource {
    List<StationDepartureInfo> departuresFor(Station station);
    Optional<StationDepartureInfo> departuresFor(Platform platform);
    List<DueTram> dueTramsFor(Station station);
}
