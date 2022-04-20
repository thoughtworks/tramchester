package com.tramchester.livedata.repository;

import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;

import java.util.List;

public interface UpcomingDeparturesSource  {

    List<UpcomingDeparture> forStation(Station station);
}
