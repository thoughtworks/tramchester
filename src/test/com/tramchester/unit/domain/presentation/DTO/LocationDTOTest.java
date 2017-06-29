package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.integration.Stations;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;


public class LocationDTOTest {

    @Test
    public void shouldCreateDTOAsExpected() {

        LocationDTO dto = new LocationDTO(Stations.Altrincham);

        assertEquals(Stations.Altrincham.getId(), dto.getId());
        assertEquals(Stations.Altrincham.isTram(), dto.isTram());
        assertEquals(Stations.Altrincham.getName(), dto.getName());
        assertEquals(Stations.Altrincham.getLatLong(), dto.getLatLong());

    }

}
