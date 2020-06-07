package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class StationDTOTest {

    @Test
    void shouldGetCorrectProximityGroup() {
        Station station = new Station("id", "area", "stopName", new LatLong(0.1D, -2D), true);
        StationDTO displayStation = new StationDTO(station, new ProximityGroup(5,"ProxGroup"));
        Assertions.assertEquals(new ProximityGroup(5,"ProxGroup"), displayStation.getProximityGroup());
    }
}
