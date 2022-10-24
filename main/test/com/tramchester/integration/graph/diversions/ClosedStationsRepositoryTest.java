package com.tramchester.integration.graph.diversions;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.ClosedStation;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationClosures;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.integration.testSupport.StationClosuresForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.CentralZoneStation.*;
import static com.tramchester.testSupport.reference.TramStations.StPetersSquare;
import static com.tramchester.testSupport.reference.TramStations.TraffordCentre;
import static org.junit.jupiter.api.Assertions.*;

public class ClosedStationsRepositoryTest {
    // Note this needs to be > time for whole test fixture, see note below in @After

    private static ComponentContainer componentContainer;
    private static IntegrationTramClosedStationsTestConfig config;
    private static TramDate when;
    private static TramDate overlap;

    private ClosedStationsRepository closedStationsRepository;

    private TramDate afterClosures;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        when = TestEnv.testTramDay();
        overlap = when.plusDays(3);

        StationClosuresForTest closureA = new StationClosuresForTest(StPetersSquare, when, when.plusWeeks(1), true);
        StationClosuresForTest closureB = new StationClosuresForTest(TraffordCentre, overlap, when.plusWeeks(2), false);
        List<StationClosures> closedStations = Arrays.asList(closureA, closureB);

        config = new IntegrationTramClosedStationsTestConfig(closedStations, true);
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        componentContainer.close();
        TestEnv.deleteDBIfPresent(config);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        closedStationsRepository = componentContainer.get(ClosedStationsRepository.class);
        afterClosures = when.plusWeeks(4);
    }

    @Test
    void shouldBeInEffectOnExpectedDays() {
        assertTrue(closedStationsRepository.hasClosuresOn(when));
        assertTrue(closedStationsRepository.hasClosuresOn(overlap));
        assertTrue(closedStationsRepository.hasClosuresOn(when.plusWeeks(2)));
        assertFalse(closedStationsRepository.hasClosuresOn(when.plusWeeks(6)));
    }

    @Test
    void shouldHaveExpectedClosedStationsForFirstPeriod() {
        Set<ClosedStation> closed = closedStationsRepository.getFullyClosedStationsFor(when);
        assertEquals(1, closed.size());
        IdSet<Station> ids = closed.stream().map(ClosedStation::getStation).collect(IdSet.collector());
        assertTrue(ids.contains(StPetersSquare.getId()));

        Set<ClosedStation> closedLater = closedStationsRepository.getFullyClosedStationsFor(afterClosures);
        assertTrue(closedLater.isEmpty());
    }

    @Test
    void shouldHaveExpectedClosedStationsForSecondPeriod() {
        Set<ClosedStation> fullyClosed = closedStationsRepository.getFullyClosedStationsFor(when.plusWeeks(2));
        assertTrue(fullyClosed.isEmpty());
    }

    @Test
    void shouldHaveExpectedClosedStationsForOverlap() {
        Set<ClosedStation> fullyClosed = closedStationsRepository.getFullyClosedStationsFor(overlap);
        assertEquals(1, fullyClosed.size());
        IdSet<Station> ids = fullyClosed.stream().map(ClosedStation::getStation).collect(IdSet.collector());
        assertTrue(ids.contains(StPetersSquare.getId()));
    }

    @Test
    void shouldHaveUpcomingClosures() {
        TramDate beforeClosures = when.minusWeeks(1);
        Set<ClosedStation> upcoming = closedStationsRepository.getUpcomingClosuresFor(beforeClosures);

        assertEquals(2, upcoming.size());

        TramDate afterClosures = when.plusWeeks(10);
        upcoming = closedStationsRepository.getUpcomingClosuresFor(afterClosures);
        assertTrue(upcoming.isEmpty());
    }

    @Test
    void shouldHaveClosedByDataSourceId() {
        Set<ClosedStation> closedStations = closedStationsRepository.getClosedStationsFor(DataSourceID.tfgm);
        assertEquals(2, closedStations.size());

    }

    @Test
    void shouldHaveNearbyStationsForClosed() {
        List<ClosedStation> closedStations = new ArrayList<>(closedStationsRepository.getFullyClosedStationsFor(when.plusDays(1)));
        assertEquals(1, closedStations.size());

        ClosedStation closedStation = closedStations.get(0);
        assertEquals(StPetersSquare.getId(), closedStation.getStation().getId());

        IdSet<Station> availableStations = closedStation.getNearbyOpenStations().stream().collect(IdSet.collector());
        assertFalse(availableStations.isEmpty());

        assertTrue(availableStations.contains(Deansgate.getId()));
        assertTrue(availableStations.contains(PiccadillyGardens.getId()));
        assertTrue(availableStations.contains(MarketStreet.getId()));


    }

}
