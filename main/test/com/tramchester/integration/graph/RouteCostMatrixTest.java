package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.ImmutableBitSet;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
public class RouteCostMatrixTest {
    private static ComponentContainer componentContainer;

    private TramRouteHelper routeHelper;
    private TramDate date;
    private RouteCostMatrix routeMatrix;
    private RouteIndex routeIndex;
    private EnumSet<TransportMode> modes;
    private RouteRepository routeRepository;


    // NOTE: this test does not cause a full db rebuild, so might see VERSION node missing messages

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {

        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        TestEnv.clearDataCache(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);
        routeMatrix = componentContainer.get(RouteCostMatrix.class);
        routeIndex = componentContainer.get(RouteIndex.class);

        date = TestEnv.testDay();
        modes = EnumSet.of(Tram);
    }

    @Test
    void shouldHaveExpectedIndexWhereDirectInterchangePossible() {
        Route routeA = routeHelper.getOneRoute(BuryManchesterAltrincham, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        int depth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(1, depth);
    }

    @Test
    void shouldHaveExpectedIndexWhereNoDirectInterchangePossible() {
        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        int depth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(2, depth);
    }

    @Test
    void shouldHaveExpectedIndexForEcclesRouteOntoAltyRoute() {
        Route routeA = routeHelper.getOneRoute(EcclesManchesterAshtonUnderLyne, date);
        Route routeB = routeHelper.getOneRoute(AltrinchamManchesterBury, date);

        int depth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(1, depth);
    }

    @Test
    void shouldHaveExpectedIndexForEcclesRouteFromAltyRoute() {
        Route routeA = routeHelper.getOneRoute(AltrinchamManchesterBury, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        int depth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(1, depth);
    }

    @Test
    void shouldHaveExpectedChangeStationsForSimpleInterchange() {
        Route routeA = routeHelper.getOneRoute(AltrinchamManchesterBury, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(date, modes);

        assertNotEquals(0, dateOverlaps.numberOfBitsSet());

        List<List<RoutePair>> results = routeMatrix.getChangesFor(indexPair, dateOverlaps).collect(Collectors.toList());

        assertEquals(1, results.size());

        List<RoutePair> changes = results.get(0);

        assertEquals(1, changes.size());

        RoutePair change = changes.get(0);

        //RoutePair routePair = routeIndex.getPairFor(change);

        assertEquals(routeA, change.first());
        assertEquals(routeB, change.second());
    }

    @Test
    void SPIKEshouldHaveExpectedInterchangeForSimpleInterchange() {
        Route routeA = routeHelper.getOneRoute(AltrinchamManchesterBury, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(date, modes);

        assertNotEquals(0, dateOverlaps.numberOfBitsSet());

        RouteCostMatrix.AnyOfPaths results = routeMatrix.getInterchangesFor(indexPair, dateOverlaps);

        assertEquals(6, results.numberPossible(), results.toString());

        assertTrue(results.isValid(interchangeStation -> interchangeStation.getStationId().equals(Victoria.getId())));
    }

    @Test
    void SPIKEshouldHaveExpectedInterchangeForSimpleInterchangeNotOnDate() {

        // use date where we can get routes
        Route routeA = routeHelper.getOneRoute(AltrinchamManchesterBury, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        TramDate outOfRangeDate = TestEnv.testDay().plusWeeks(3 * 52);
        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(outOfRangeDate, modes);

        assertEquals(0, dateOverlaps.numberOfBitsSet());

        RouteCostMatrix.AnyOfPaths results = routeMatrix.getInterchangesFor(indexPair, dateOverlaps);

        assertEquals(0, results.numberPossible());

    }

    @Test
    void SPIKEshouldCheckFor2Changes() {

        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        assertEquals(2, routeMatrix.getConnectionDepthFor(routeA, routeB));

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        // ignore data and mode here
        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(date, modes);
        assertEquals(196, dateOverlaps.numberOfBitsSet());

        RouteCostMatrix.AnyOfPaths results = routeMatrix.getInterchangesFor(indexPair, dateOverlaps);

        assertEquals(7, results.numberPossible(), results.toString()); // two sets of changes needed

        assertTrue(results.isValid(interchangeStation -> interchangeStation.getStationId().equals(Cornbrook.getId()) ||
                interchangeStation.getStationId().equals(MarketStreet.getId())));

        StationAvailabilityRepository stationAvailabilityRepository = componentContainer.get(StationAvailabilityRepository.class);

        TimeRange timeRange = TimeRange.of(TramTime.of(23,10), TramTime.nextDay(1,10));
        assertTrue(results.isValid(interchangeStation ->
                stationAvailabilityRepository.isAvailable(interchangeStation.getStation(), date, timeRange, modes)));

    }

    @Test
    void shouldCheckFor2Changes() {

        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        assertEquals(2, routeMatrix.getConnectionDepthFor(routeA, routeB));

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        // ignore data and mode here
        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(date, modes);
        assertEquals(196, dateOverlaps.numberOfBitsSet());

        List<List<RoutePair>> results = routeMatrix.getChangesFor(indexPair, dateOverlaps).collect(Collectors.toList());

        assertFalse(results.isEmpty());

        results.forEach(result -> {
            assertEquals(2, result.size());
            RoutePair firstChange = result.get(0);
            RoutePair secondChange = result.get(1);

            assertEquals(routeA, firstChange.first(), result.toString());
            assertEquals(routeB, secondChange.second(), result.toString());
        });

    }

    @Test
    void shouldGetBitsSetIfAlreadySetForLowerDepth() {
        Route routeA = routeHelper.getOneRoute(TheTraffordCentreCornbrook, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        RouteIndexPair indexPair = routeIndex.getPairFor(RoutePair.of(routeA, routeB));

        int firstDepth = routeMatrix.getConnectionDepthFor(routeA, routeB);
        assertEquals(1, firstDepth);

        // set for depth 1, so ought to be set for all subsequent depths

        ImmutableBitSet rowAtDepthOne = routeMatrix.getExistingBitSetsForRoute(indexPair.first(), 1);
        assertTrue(rowAtDepthOne.isSet(indexPair.second()));

        ImmutableBitSet rowAtDepthTwo = routeMatrix.getExistingBitSetsForRoute(indexPair.first(), 2);
        assertTrue(rowAtDepthTwo.isSet(indexPair.second()));

        ImmutableBitSet rowAtDepthThree = routeMatrix.getExistingBitSetsForRoute(indexPair.first(), 3);
        assertTrue(rowAtDepthThree.isSet(indexPair.second()));

    }

    @Test
    void shouldHaveUniqueDegreeForEachRoutePair() {
        Set<Route> onDate = routeRepository.getRoutesRunningOn(date);

        assertFalse(onDate.isEmpty());

        onDate.forEach(first -> onDate.forEach(second -> {
            RoutePair routePair = RoutePair.of(first, second);

            if (!routePair.areSame()) {
                RouteIndexPair indexPair = routeIndex.getPairFor(routePair);
                List<Integer> results = routeMatrix.getAllDegrees(indexPair);
                assertTrue(results.size()<=1, "Too many degrees " + results + " for " +
                        indexPair + " " +routePair + " on " + date);
            }
        }));
    }

}
