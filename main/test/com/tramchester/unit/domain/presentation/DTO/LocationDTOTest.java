package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.integration.Stations;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;


public class LocationDTOTest {

    @Test
    public void shouldCreateDTOAsExpected() {

        Station testStation = Stations.createStation("9400ZZMAALT", "Altrincham area", "Altrincham");

        testStation.addPlatform(new Platform("9400ZZMAALT1", "platform one"));
        testStation.addPlatform(new Platform("9400ZZMAALT2", "platform two"));

        LocationDTO dto = new LocationDTO(testStation);

        assertEquals(testStation.getId(), dto.getId());
        assertEquals(testStation.isTram(), dto.isTram());
        assertEquals(testStation.getName(), dto.getName());
        assertEquals(testStation.getLatLong(), dto.getLatLong());

        assertTrue(dto.hasPlatforms());
        assertEquals(2, dto.getPlatforms().size());
        assertEquals(Stations.Altrincham.getId()+"1", dto.getPlatforms().get(0).getId());

    }

}
