package com.tramchester.testSupport;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.StationRepository;

public interface TestStations extends HasId<Station> {
    default Station from(StationRepository stationRepository) {
        return stationRepository.getStationById(getId());
    }
}
