package com.tramchester.testSupport.reference;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.StationRepository;

public interface FakeStation extends HasId<Station> {

    @Override
    default IdFor<Station> getId() {
        return Station.createId(getRawId());
    }

    String getName();

    LatLong getLatLong();

    String getRawId();

    default Station from(StationRepository stationRepository) {
        return stationRepository.getStationById(getId());
    }

    Station fake();
}
