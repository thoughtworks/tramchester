package com.tramchester.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LiveDataSource {
    void enrich(PlatformDTO platform, TramServiceDate tramServiceDate, TramTime queryTime);
    void enrich(LocationDTO locationDTO, LocalDateTime current);
    List<StationDepartureInfo> departuresFor(Station station);
    Optional<StationDepartureInfo> departuresFor(Platform platform);
}
