package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StationDTOTest {

    @Test
    void shouldCreateCorrectly() {
        Station station = new Station("id", "area", "stopName", new LatLong(0.1D, -2D), true);
        StationDTO displayStation = new StationDTO(station);

        Assertions.assertEquals(station.getId(), displayStation.getId());
        Assertions.assertEquals(station.getName(), displayStation.getName());
        Assertions.assertEquals(station.getPlatforms().size(), displayStation.getPlatforms().size());
        Assertions.assertEquals(station.getLatLong(), displayStation.getLatLong());
        Assertions.assertTrue(displayStation.isTram());

    }
}
