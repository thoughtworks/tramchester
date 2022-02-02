package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.StationLink;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.domain.reference.TransportMode;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.StPetersSquare;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationLinkDTOTest extends EasyMockSupport {

    private DTOFactory stationDTOFactory;

    @BeforeEach
    void beforeEachTestRuns() {
        stationDTOFactory = new DTOFactory();
    }

    @Test
    void shouldCreateTramLink() {
        final Station altrincham = Altrincham.fake();
        final Station stPeters = StPetersSquare.fake();

        Set<TransportMode> modes = new HashSet<>(Arrays.asList(TransportMode.Bus, TransportMode.Tram));

        StationLink stationLink = new StationLink(altrincham, stPeters, modes);

        replayAll();
        StationLinkDTO dto = stationDTOFactory.createStationLinkDTO(stationLink);
        verifyAll();

        assertEquals(altrincham.getId().forDTO(), dto.getBegin().getId());
        assertEquals(stPeters.getId().forDTO(), dto.getEnd().getId());

        assertEquals(2, dto.getTransportModes().size());

        assertTrue( dto.getTransportModes().contains(TransportMode.Bus));
        assertTrue( dto.getTransportModes().contains(TransportMode.Tram));

    }
}
