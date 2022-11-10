package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.SimpleList;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.graph.search.routes.RouteIndexPair;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static org.junit.jupiter.api.Assertions.*;

public class RouteCostMatrixTest {
    private static ComponentContainer componentContainer;

    private TramRouteHelper routeHelper;
    private TramDate date;
    private RouteCostMatrix routeMatrix;
    private RouteIndex routeIndex;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTramTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
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
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);
        routeMatrix = componentContainer.get(RouteCostMatrix.class);
        routeIndex = componentContainer.get(RouteIndex.class);

        date = TestEnv.testDay();
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

        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(date);

        assertNotEquals(0, dateOverlaps.numberOfBitsSet());

        List<SimpleList<RouteIndexPair>> results = routeMatrix.getChangesFor(indexPair, dateOverlaps).collect(Collectors.toList());

        assertEquals(1, results.size());

        List<RouteIndexPair> changes = results.get(0).stream().collect(Collectors.toList());

        assertEquals(1, changes.size());

        RouteIndexPair change = changes.get(0);

        RoutePair routePair = routeIndex.getPairFor(change);

        assertEquals(routeA, routePair.getFirst());
        assertEquals(routeB, routePair.getSecond());

    }

    @Test
    void shouldReproduceIssueBetweenPiccAndTraffordLine() {
        TramDate testDate = date; //TramDate.of(2022,11,14);

        Route routeA = routeHelper.getOneRoute(PiccadillyBury, testDate);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, testDate);

        RouteIndexPair indexPair = routeIndex.getPairFor(new RoutePair(routeA, routeB));

        IndexedBitSet dateOverlaps = routeMatrix.createOverlapMatrixFor(testDate);

        assertEquals(196, dateOverlaps.numberOfBitsSet());

        List<SimpleList<RouteIndexPair>> results = routeMatrix.getChangesFor(indexPair, dateOverlaps).collect(Collectors.toList());

        assertEquals(7, results.size(), results.toString());

        results.forEach(result -> {
            List<RouteIndexPair> changes = result.stream().collect(Collectors.toList());
            assertEquals(2, changes.size());
            RouteIndexPair firstChange = changes.get(0);
            RouteIndexPair secondChange = changes.get(1);

            assertEquals(indexPair.first(), firstChange.first());
            assertEquals(indexPair.second(), secondChange.second());
        });
    }

}
