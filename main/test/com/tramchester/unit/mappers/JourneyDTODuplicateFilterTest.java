package com.tramchester.unit.mappers;

import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.VehicleStageDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.SimpleStageDTO;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.JourneyDTODuplicateFilter;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JourneyDTODuplicateFilterTest {

    private TramDate queryDate;
    private TramTime queryTime;
    private JourneyDTODuplicateFilter filter;

    @BeforeEach
    void beforeEachTestRuns() {
        queryDate = TestEnv.testDay();
        queryTime = TramTime.of(9,5);
        filter = new JourneyDTODuplicateFilter();
    }

    @Test
    void shouldFilterOutIfSameTimesStopsAndChanges() {

        List<LocationRefWithPosition> changeStations = Collections.singletonList(getStationRef(Deansgate));
        List<LocationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(TramTime.of(9,14), 10, changeStations, path);
        JourneyDTO journeyB = createJourneyFor(TramTime.of(9,14), 10, changeStations, path);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(1, results.size());
    }

    @Test
    void shouldFilterOutIfSameTimesStopsAndDiffChanges() {

        List<LocationRefWithPosition> changeStationsA = Collections.singletonList(getStationRef(Deansgate));
        List<LocationRefWithPosition> changeStationsB = Collections.singletonList(getStationRef(TraffordBar));

        List<LocationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(TramTime.of(9,14), 10, changeStationsA, path);
        JourneyDTO journeyB = createJourneyFor(TramTime.of(9,14), 10, changeStationsB, path);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(1, results.size());
    }

    @Test
    void shouldNotFilterOutIfDiffDepartTimes() {

        List<LocationRefWithPosition> changeStations = Collections.singletonList(getStationRef(Deansgate));

        List<LocationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(TramTime.of(9,14), 10, changeStations, path);
        JourneyDTO journeyB = createJourneyFor(TramTime.of(9,17), 10, changeStations, path);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB, journeyA));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(2, results.size());
    }

    @Test
    void shouldNotFilterOutIfDiffDuration() {

        List<LocationRefWithPosition> changeStations = Collections.singletonList(getStationRef(Deansgate));

        List<LocationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(TramTime.of(9,14), 15, changeStations, path);
        JourneyDTO journeyB = createJourneyFor(TramTime.of(9,14), 10, changeStations, path);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB, journeyA));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(2, results.size());
    }

    @Test
    void shouldNotFilterOutIfSDiffPath() {

        List<LocationRefWithPosition> changeStations = Collections.singletonList(getStationRef(Deansgate));

        List<LocationRefWithPosition> pathA = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));
        List<LocationRefWithPosition> pathB = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(Altrincham));

        JourneyDTO journeyA = createJourneyFor(TramTime.of(9,14), 10, changeStations, pathA);
        JourneyDTO journeyB = createJourneyFor(TramTime.of(9,14), 10, changeStations, pathB);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB, journeyB));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(2, results.size());
    }

    private JourneyDTO createJourneyFor(TramTime departTime, int duration, List<LocationRefWithPosition> changeStations,
                                        List<LocationRefWithPosition> path) {
        List<Note> notes = Collections.emptyList();
        LocationRefWithPosition begin = getStationRef(Ashton);
        VehicleStageDTO stageA = new VehicleStageDTO();
        VehicleStageDTO stageB = new VehicleStageDTO();
        List<SimpleStageDTO> stages = Arrays.asList(stageA, stageB);

        LocalDateTime firstDepartureTime = departTime.toDate(queryDate);
        LocalDateTime expectedArrivalTime = departTime.plusMinutes(duration).toDate(queryDate);

        return new JourneyDTO(begin, stages,
                expectedArrivalTime, firstDepartureTime,
                changeStations, queryTime, notes,
                path, queryDate);
    }

    private LocationRefWithPosition getStationRef(TramStations tramStations) {
        return new LocationRefWithPosition(tramStations.fake());
    }
}
