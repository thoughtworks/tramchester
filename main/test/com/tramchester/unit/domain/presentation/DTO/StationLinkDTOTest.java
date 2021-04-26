package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.StationLink;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.StPetersSquare;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationLinkDTOTest {

    @Test
    void shouldCreateTramLink() {
        Set<TransportMode> modes = new HashSet<>(Arrays.asList(TransportMode.Bus, TransportMode.Tram));
        StationLink stationLink = new StationLink(TramStations.of(Altrincham), TramStations.of(StPetersSquare), modes);

        StationLinkDTO dto = StationLinkDTO.create(stationLink);

        assertEquals(Altrincham.getId().forDTO(), dto.getBegin().getId());
        assertEquals(StPetersSquare.getId().forDTO(), dto.getEnd().getId());
        assertEquals(2, dto.getTransportModes().size());
        assertTrue( dto.getTransportModes().contains(TransportMode.Bus));
        assertTrue( dto.getTransportModes().contains(TransportMode.Tram));

    }
}
