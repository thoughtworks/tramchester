package com.tramchester.unit.mappers;

import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.tramchester.testSupport.TramStations.Bury;
import static com.tramchester.testSupport.TramStations.PiccadillyGardens;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeparturesMapperTest extends EasyMockSupport {

    private DeparturesMapper mapper;
    private LocalDate queryDate;

    @BeforeEach
    void beforeEachTestRuns() {
        queryDate = TestEnv.LocalNow().toLocalDate();
        mapper = new DeparturesMapper();
    }

    @Test
    void shouldMapToDTOCorrectly() {
        Collection<DueTram> dueTrams = Collections.singletonList(new DueTram(TramStations.of(PiccadillyGardens),
                "DUE", 9, "single", LocalTime.of(10, 32)));

        Set<DepartureDTO> results = mapper.mapToDTO(TramStations.of(Bury), dueTrams, queryDate);

        List<DepartureDTO> list = new LinkedList<>(results);

        assertEquals(1, list.size());
        DepartureDTO departureDTO = list.get(0);
        assertEquals(PiccadillyGardens.getName(), departureDTO.getDestination());
        assertEquals("DUE", departureDTO.getStatus());
        assertEquals(Bury.getName(), departureDTO.getFrom());
        LocalDateTime when = departureDTO.getDueTime();
        assertEquals(LocalTime.of(10,41), when.toLocalTime());
        assertEquals(queryDate, when.toLocalDate());

        assertEquals("single", departureDTO.getCarriages());
    }

    @Test
    void shouldHandleCrossingMidnight() {
        Collection<DueTram> dueTrams = Collections.singletonList(new DueTram(TramStations.of(PiccadillyGardens),
                "DUE", 9, "single", LocalTime.of(23, 58)));

        Set<DepartureDTO> results = mapper.mapToDTO(TramStations.of(Bury), dueTrams, queryDate);

        List<DepartureDTO> list = new LinkedList<>(results);

        assertEquals(1, list.size());
        DepartureDTO departureDTO = list.get(0);
        LocalDateTime result = departureDTO.getDueTime();

        assertEquals(LocalTime.of(0,7), result.toLocalTime());
        assertEquals(queryDate.plusDays(1), result.toLocalDate());
    }

}
