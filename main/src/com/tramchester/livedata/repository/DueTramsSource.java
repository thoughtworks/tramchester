package com.tramchester.livedata.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.livedata.domain.liveUpdates.PlatformDueTrams;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ImplementedBy(DueTramsRepository.class)
public interface DueTramsSource extends LiveDataCache {
    Optional<PlatformDueTrams> dueTramsForPlatform(IdFor<Platform> platform, LocalDate date, TramTime queryTime);
    List<DueTram> dueTramsForStation(Station station, LocalDate date, TramTime queryTime);
}
