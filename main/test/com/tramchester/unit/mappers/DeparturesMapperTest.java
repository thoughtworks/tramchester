package com.tramchester.unit.mappers;

import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.mappers.DeparturesMapper;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.tramchester.testSupport.reference.TramStations.Bury;
import static com.tramchester.testSupport.reference.TramStations.PiccadillyGardens;
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
        Collection<DueTram> dueTrams = Collections.singletonList(new DueTram(PiccadillyGardens.fake(),
                "DUE", Duration.ofMinutes(9), "single", LocalTime.of(10, 32)));

        Set<DepartureDTO> results = mapper.mapToDTO(Bury.fake(), dueTrams, queryDate);

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
        Collection<DueTram> dueTrams = Collections.singletonList(new DueTram(PiccadillyGardens.fake(),
                "DUE", Duration.ofMinutes(9), "single", LocalTime.of(23, 58)));

        Set<DepartureDTO> results = mapper.mapToDTO(Bury.fake(), dueTrams, queryDate);

        List<DepartureDTO> list = new LinkedList<>(results);

        assertEquals(1, list.size());
        DepartureDTO departureDTO = list.get(0);
        LocalDateTime result = departureDTO.getDueTime();

        assertEquals(LocalTime.of(0,7), result.toLocalTime());
        assertEquals(queryDate.plusDays(1), result.toLocalDate());
    }

}
