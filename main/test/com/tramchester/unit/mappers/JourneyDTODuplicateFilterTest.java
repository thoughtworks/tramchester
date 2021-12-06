package com.tramchester.unit.mappers;

import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.JourneyDTODuplicateFilter;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JourneyDTODuplicateFilterTest {

    private LocalDate queryDate;
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

        List<StationRefWithPosition> changeStations = Collections.singletonList(getStationRef(Deansgate));
        List<StationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(LocalTime.of(9,14), 10, changeStations, path);
        JourneyDTO journeyB = createJourneyFor(LocalTime.of(9,14), 10, changeStations, path);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(1, results.size());
    }

    @Test
    void shouldFilterOutIfSameTimesStopsAndDiffChanges() {

        List<StationRefWithPosition> changeStationsA = Collections.singletonList(getStationRef(Deansgate));
        List<StationRefWithPosition> changeStationsB = Collections.singletonList(getStationRef(TraffordBar));

        List<StationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(LocalTime.of(9,14), 10, changeStationsA, path);
        JourneyDTO journeyB = createJourneyFor(LocalTime.of(9,14), 10, changeStationsB, path);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(1, results.size());
    }

    @Test
    void shouldNotFilterOutIfDiffDepartTimes() {

        List<StationRefWithPosition> changeStations = Collections.singletonList(getStationRef(Deansgate));

        List<StationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(LocalTime.of(9,14), 10, changeStations, path);
        JourneyDTO journeyB = createJourneyFor(LocalTime.of(9,17), 10, changeStations, path);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB, journeyA));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(2, results.size());
    }

    @Test
    void shouldNotFilterOutIfDiffDuration() {

        List<StationRefWithPosition> changeStations = Collections.singletonList(getStationRef(Deansgate));

        List<StationRefWithPosition> path = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));

        JourneyDTO journeyA = createJourneyFor(LocalTime.of(9,14), 15, changeStations, path);
        JourneyDTO journeyB = createJourneyFor(LocalTime.of(9,14), 10, changeStations, path);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB, journeyA));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(2, results.size());
    }

    @Test
    void shouldNotFilterOutIfSDiffPath() {

        List<StationRefWithPosition> changeStations = Collections.singletonList(getStationRef(Deansgate));

        List<StationRefWithPosition> pathA = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(NavigationRoad));
        List<StationRefWithPosition> pathB = Arrays.asList(getStationRef(Ashton), getStationRef(Deansgate),
                getStationRef(Altrincham));

        JourneyDTO journeyA = createJourneyFor(LocalTime.of(9,14), 10, changeStations, pathA);
        JourneyDTO journeyB = createJourneyFor(LocalTime.of(9,14), 10, changeStations, pathB);

        Set<JourneyDTO> journeys = new HashSet<>(Arrays.asList(journeyA, journeyB, journeyB));

        Set<JourneyDTO> results = filter.apply(journeys);
        assertEquals(2, results.size());
    }

    private JourneyDTO createJourneyFor(LocalTime departTime, int duration, List<StationRefWithPosition> changeStations, List<StationRefWithPosition> path) {
        List<Note> notes = Collections.emptyList();
        StationRefWithPosition begin = getStationRef(Ashton);
        StageDTO stageA = new StageDTO();
        StageDTO stageB = new StageDTO();
        List<StageDTO> stages = Arrays.asList(stageA, stageB);

        LocalDateTime firstDepartureTime = LocalDateTime.of(queryDate, departTime);
        LocalDateTime expectedArrivalTime = LocalDateTime.of(queryDate, departTime.plusMinutes(duration));

        return new JourneyDTO(begin, stages,
                expectedArrivalTime, firstDepartureTime,
                changeStations, queryTime, notes,
                path, queryDate);
    }

    private StationRefWithPosition getStationRef(TramStations tramStations) {
        return new StationRefWithPosition(of(tramStations));
    }
}
