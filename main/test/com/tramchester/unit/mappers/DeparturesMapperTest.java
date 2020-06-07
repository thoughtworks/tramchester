package com.tramchester.unit.mappers;

import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.testSupport.Stations;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.*;

class DeparturesMapperTest extends EasyMockSupport {

    private DeparturesMapper mapper;

    @BeforeEach
    void beforeEachTestRuns() {
        mapper = new DeparturesMapper();
    }

    @Test
    void shouldMapToDTOCorrectly() {
        Collection<DueTram> dueTrams = Collections.singletonList(new DueTram(Stations.PiccadillyGardens, "DUE", 9,
                "single", LocalTime.of(10, 32)));

        Set<DepartureDTO> results = mapper.mapToDTO(Stations.Bury, dueTrams);

        List<DepartureDTO> list = new LinkedList<>(results);

        Assertions.assertEquals(1, list.size());
        DepartureDTO departureDTO = list.get(0);
        Assertions.assertEquals(Stations.PiccadillyGardens.getName(), departureDTO.getDestination());
        Assertions.assertEquals("DUE", departureDTO.getStatus());
        Assertions.assertEquals(Stations.Bury.getName(), departureDTO.getFrom());
        Assertions.assertEquals(TramTime.of(10,41), departureDTO.getWhen());
        Assertions.assertEquals("single", departureDTO.getCarriages());

    }

}
