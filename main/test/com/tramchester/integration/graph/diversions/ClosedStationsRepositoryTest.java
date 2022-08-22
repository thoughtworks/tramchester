package com.tramchester.integration.graph.diversions;

import com.google.common.collect.Sets;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationClosure;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.StationClosureForTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramClosedStationsTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.reference.TramStations.StPetersSquare;
import static com.tramchester.testSupport.reference.TramStations.TraffordCentre;
import static org.junit.jupiter.api.Assertions.*;

public class ClosedStationsRepositoryTest {
    // Note this needs to be > time for whole test fixture, see note below in @After

    private static ComponentContainer componentContainer;
    private static IntegrationTramClosedStationsTestConfig config;
    private static TramDate when;
    private static TramDate overlap;

    private StationRepository stationRepository;

    private ClosedStationsRepository closedStationsRepository;

    private TramDate afterClosures;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        when = TestEnv.testTramDay();
        overlap = when.plusDays(3);

        StationClosureForTest closureA = new StationClosureForTest(StPetersSquare, when, when.plusWeeks(1));
        StationClosureForTest closureB = new StationClosureForTest(TraffordCentre, overlap, when.plusWeeks(2));
        List<StationClosure> closedStations = Arrays.asList(closureA, closureB);

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
        stationRepository = componentContainer.get(StationRepository.class);
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
        IdSet<Station> closed = closedStationsRepository.getClosedStationsFor(when);
        assertEquals(1, closed.size());
        assertTrue(closed.contains(StPetersSquare.getId()));

        IdSet<Station> closedLater = closedStationsRepository.getClosedStationsFor(afterClosures);
        assertTrue(closedLater.isEmpty());
    }

    @Test
    void shouldHaveExpectedClosedStationsForSecondPeriod() {
        IdSet<Station> closed = closedStationsRepository.getClosedStationsFor(when.plusWeeks(2));
        assertEquals(1, closed.size());
        assertTrue(closed.contains(TraffordCentre.getId()));
    }

    @Test
    void shouldHaveExpectedClosedStationsForOverlap() {
        IdSet<Station> closed = closedStationsRepository.getClosedStationsFor(overlap);
        assertEquals(2, closed.size());
        assertTrue(closed.contains(TraffordCentre.getId()));
        assertTrue(closed.contains(StPetersSquare.getId()));
    }

    @Test void shouldFindRoutesImpactedByClosure() {
        Station station = StPetersSquare.from(stationRepository);

        Set<Route> stationRoutes = Sets.union(station.getDropoffRoutes(), station.getPickupRoutes());

        Set<Route> impactedRoutes = closedStationsRepository.getImpactedRoutes(when);
        assertEquals(stationRoutes.size(), impactedRoutes.size());
        assertTrue(impactedRoutes.containsAll(stationRoutes));

        Set<Route> impactedLater = closedStationsRepository.getImpactedRoutes(afterClosures);
        assertTrue(impactedLater.isEmpty());
    }

    @Test void shouldFindRoutesImpactedByClosureDuringOverlap() {
        Station stPeters = StPetersSquare.from(stationRepository);
        Station traffordCenter = TraffordCentre.from(stationRepository);

        Set<Route> stPetersRoutes = Sets.union(stPeters.getDropoffRoutes(), stPeters.getPickupRoutes());
        Set<Route> traffordCenterRoutes = Sets.union(traffordCenter.getDropoffRoutes(), traffordCenter.getPickupRoutes());
        Set<Route> bothStationRoutes = Sets.union(stPetersRoutes, traffordCenterRoutes);

        // assert that we don't have the same set of routes as need to check overlap happens as expected
        assertNotEquals(stPetersRoutes.size(), bothStationRoutes.size());
        assertNotEquals(traffordCenterRoutes.size(), bothStationRoutes.size());

        Set<Route> impactedRoutes = closedStationsRepository.getImpactedRoutes(overlap);
        assertEquals(bothStationRoutes.size(), impactedRoutes.size());
        assertTrue(impactedRoutes.containsAll(stPetersRoutes));
        assertTrue(impactedRoutes.containsAll(traffordCenterRoutes));

    }

}
