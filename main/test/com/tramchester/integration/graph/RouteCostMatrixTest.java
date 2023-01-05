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
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
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
    private int numberOfRoutes;
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
        numberOfRoutes = routeRepository.numberOfRoutes();
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
    void shouldReproduceIssueBetweenPiccAndTraffordLine() {

        Route routeA = routeHelper.getOneRoute(PiccadillyBury, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(date, modes);

        assertEquals(196, dateOverlaps.numberOfBitsSet());

        List<List<RoutePair>> results = routeMatrix.getChangesFor(indexPair, dateOverlaps).collect(Collectors.toList());

        assertFalse(results.isEmpty());

        results.forEach(result -> {
            assertEquals(2, result.size());
            RoutePair firstChange = result.get(0);
            RoutePair secondChange = result.get(1);

            assertEquals(routeA, firstChange.first());
            assertEquals(routeB, secondChange.second());
        });
    }

    @Test
    void shouldCheckFor2Changes() {

        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        assertEquals(2, routeMatrix.getConnectionDepthFor(routeA, routeB));

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        // ignore data and mode here
        IndexedBitSet dateOverlaps = IndexedBitSet.getIdentity(numberOfRoutes, numberOfRoutes);

        List<List<RoutePair>> results = routeMatrix.getChangesFor(indexPair, dateOverlaps).collect(Collectors.toList());

        assertFalse(results.isEmpty());

        results.forEach(result -> {
            assertEquals(2, result.size());
            RoutePair firstChange = result.get(0);
            RoutePair secondChange = result.get(1);

            assertEquals(routeA, firstChange.first(), result.toString());
            assertEquals(routeB, secondChange.second());
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
