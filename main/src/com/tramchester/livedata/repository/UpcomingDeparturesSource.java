package com.tramchester.livedata.repository;

import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;

import java.util.List;

public interface UpcomingDeparturesSource  {

    @Deprecated
    List<UpcomingDeparture> dueTramsForPlatform(IdFor<Platform> platform);

    List<UpcomingDeparture> dueTramsForStation(Station station);
}
