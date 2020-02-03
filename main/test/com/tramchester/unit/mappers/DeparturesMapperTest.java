package com.tramchester.unit.mappers;

import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.integration.Stations;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.repository.LiveDataRepository;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeparturesMapperTest extends EasyMockSupport {

    private DeparturesMapper mapper;

    @Before
    public void beforeEachTestRuns() {
        LiveDataRepository liveDataRepository = createStrictMock(LiveDataRepository.class);
        mapper = new DeparturesMapper();
    }

    @Test
    public void shouldMapToDTOCorrectly() {
        Collection<DueTram> dueTrams = Arrays.asList(new DueTram(Stations.PiccadillyGardens, "DUE", 9,
                "single", LocalTime.of(10,32)));

        Set<DepartureDTO> results = mapper.mapToDTO(Stations.Bury, dueTrams);

        List<DepartureDTO> list = new LinkedList<>(results);

        assertEquals(1, list.size());
        DepartureDTO departureDTO = list.get(0);
        assertEquals(Stations.PiccadillyGardens.getName(), departureDTO.getDestination());
        assertEquals("DUE", departureDTO.getStatus());
        assertEquals(Stations.Bury.getName(), departureDTO.getFrom());
        assertEquals(TramTime.of(10,41), departureDTO.getWhen());
        assertEquals("single", departureDTO.getCarriages());

    }

}
