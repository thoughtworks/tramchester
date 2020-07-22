package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StationDTOTest {

    @Test
    void shouldCreateCorrectly() {
        Station station = Station.forTest("id", "area", "stopName",
                new LatLong(0.1D, -2D), TransportMode.Tram);
        StationDTO dto = new StationDTO(station);

        Assertions.assertEquals(station.forDTO(), dto.getId());
        Assertions.assertEquals(station.getName(), dto.getName());
        Assertions.assertEquals(station.getPlatforms().size(), dto.getPlatforms().size());
        Assertions.assertEquals(station.getLatLong(), dto.getLatLong());
        Assertions.assertEquals(station.getTransportMode(), dto.getTransportMode());
        Assertions.assertTrue(dto.isTram());

    }
}
