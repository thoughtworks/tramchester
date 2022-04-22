package com.tramchester.unit.mappers;

import com.tramchester.domain.Agency;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.mappers.DeparturesMapper;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeparturesMapperTest extends EasyMockSupport {

    private DeparturesMapper mapper;
    private LocalDateTime lastUpdated;
    private Station displayLocation;
    private final Agency agency = TestEnv.MetAgency();
    private final TransportMode mode = TransportMode.Tram;

    @BeforeEach
    void beforeEachTestRuns() {
        lastUpdated = TestEnv.LocalNow();
        mapper = new DeparturesMapper();
        displayLocation = Bury.fake();
    }

    @Test
    void shouldMapToDTOCorrectly() {
        TramTime when = TramTime.of(10, 41);

        Collection<UpcomingDeparture> dueTrams = Collections.singletonList(
                new UpcomingDeparture(lastUpdated.toLocalDate(), displayLocation, PiccadillyGardens.fake(),
                "DUE", when, "single", agency, mode));

        Set<DepartureDTO> results = mapper.mapToDTO(dueTrams, lastUpdated);

        List<DepartureDTO> list = new LinkedList<>(results);

        assertEquals(1, list.size());
        DepartureDTO departureDTO = list.get(0);
        assertEquals(PiccadillyGardens.getName(), departureDTO.getDestination());
        assertEquals("DUE", departureDTO.getStatus());
        assertEquals(Bury.getName(), departureDTO.getFrom());

        assertEquals(when.asLocalTime(), departureDTO.getDueTime().toLocalTime());
        assertEquals(lastUpdated.toLocalDate(), departureDTO.getDueTime().toLocalDate());

        assertEquals("single", departureDTO.getCarriages());
    }

    @Test
    void shouldHandleCrossingMidnight() {
        TramTime when = TramTime.of(23,58).plusMinutes(9);

        Collection<UpcomingDeparture> dueTrams = Collections.singletonList(
                new UpcomingDeparture(lastUpdated.toLocalDate(), displayLocation, PiccadillyGardens.fake(),
                "DUE", when, "single", agency, mode));

        Set<DepartureDTO> results = mapper.mapToDTO(dueTrams, lastUpdated);

        List<DepartureDTO> list = new LinkedList<>(results);

        assertEquals(1, list.size());
        DepartureDTO departureDTO = list.get(0);
        LocalDateTime result = departureDTO.getDueTime();

        assertEquals(LocalTime.of(0,7), result.toLocalTime());
        assertEquals(lastUpdated.plusDays(1).toLocalDate(), result.toLocalDate());
    }

}
