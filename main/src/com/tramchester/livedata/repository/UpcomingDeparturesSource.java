package com.tramchester.livedata.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.PlatformDueTrams;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UpcomingDeparturesSource extends LiveDataCache {
    Optional<PlatformDueTrams> dueTramsForPlatform(IdFor<Platform> platform, LocalDate date, TramTime queryTime);
    List<UpcomingDeparture> dueTramsForStation(Station station, LocalDate date, TramTime queryTime);
}
