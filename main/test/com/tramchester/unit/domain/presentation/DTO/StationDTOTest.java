package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StationDTOTest {

    @Test
    public void shouldGetCorrectProximityGroup() {
        Station station = new Station("id", "area", "stopName", new LatLong(0.1D, -2D), true);
        StationDTO displayStation = new StationDTO(station, new ProximityGroup(5,"ProxGroup"));
        assertEquals(new ProximityGroup(5,"ProxGroup"), displayStation.getProximityGroup());
    }
}
