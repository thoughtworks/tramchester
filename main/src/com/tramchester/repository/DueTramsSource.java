package com.tramchester.repository;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.PlatformDueTrams;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;

import java.util.List;
import java.util.Optional;

public interface DueTramsSource extends LiveDataCache {
    Optional<PlatformDueTrams> dueTramsFor(IdFor<Platform> platform, TramServiceDate tramServiceDate, TramTime queryTime);
    List<DueTram> dueTramsFor(Station station, TramServiceDate tramServiceDate, TramTime queryTime);
}
