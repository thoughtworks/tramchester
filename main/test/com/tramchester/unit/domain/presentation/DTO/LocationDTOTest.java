package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.testSupport.Stations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;


class LocationDTOTest {

    @Test
    void shouldCreateDTOAsExpected() {

        Station testStation = Stations.createStation("9400ZZMAALT", "Altrincham area", "Altrincham");

        testStation.addPlatform(new Platform("9400ZZMAALT1", "platform one"));
        testStation.addPlatform(new Platform("9400ZZMAALT2", "platform two"));

        LocationDTO dto = new LocationDTO(testStation);

        Assertions.assertEquals(testStation.forDTO(), dto.getId());
        Assertions.assertEquals(testStation.getTransportMode(), dto.getTransportMode());
        Assertions.assertEquals(testStation.getName(), dto.getName());
        Assertions.assertEquals(testStation.getLatLong(), dto.getLatLong());

        Assertions.assertTrue(dto.hasPlatforms());
        Assertions.assertEquals(2, dto.getPlatforms().size());
        Assertions.assertEquals(Stations.Altrincham.forDTO()+"1", dto.getPlatforms().get(0).getId());

    }

}
