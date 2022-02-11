package com.tramchester.integration.graph;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.BetweenRoutesCostRepository;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static org.junit.jupiter.api.Assertions.*;

public class RouteToRouteCostsTest {

    private static ComponentContainer componentContainer;

    private BetweenRoutesCostRepository routesCostRepository;
    private TramRouteHelper routeHelper;
    private RouteRepository routeRepository;
    private static Path indexFile;
    private StationRepository stationRepository;
    private final Set<TransportMode> modes = Collections.singleton(Tram);

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        final Path cacheFolder = config.getCacheFolder();

        indexFile = cacheFolder.resolve(RouteToRouteCosts.INDEX_FILE);

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
        stationRepository = componentContainer.get(StationRepository.class);
        routesCostRepository = componentContainer.get(BetweenRoutesCostRepository.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper();
    }

    @Test
    void shouldHaveFullyConnectedForTramsWhereDatesOverlaps() {
        Set<Route> routes = routeRepository.getRoutes();
        for (Route start : routes) {
            for (Route end : routes) {
                if (!start.equals(end) && start.isDateOverlap(end)) {
                    assertNotEquals(Integer.MAX_VALUE, routesCostRepository.getFor(start, end),
                            "no path between routes " + start.getId() + " " + end.getId());
                }
            }
        }
    }

    @Disabled("Requires route with non-overlapping dates, which isn't the case for the current data")
    @Test
    void shouldFailToFindIfNoDateOverlop() {
        Set<Route> routesA = routeHelper.get(AltrinchamPiccadilly, routeRepository);
        Set<Route> routesB = routeHelper.get(PiccadillyAltrincham, routeRepository);

        // First:
        // try to find routes with no overlap in order that can check the cost between those routes is MAX_VALUE
        //Optional<Route> mayBeNoDateOverlap = routesB.stream().filter(route -> !firstRouteA.isDateOverlap(route)).findFirst();

        List<Pair<Route, Route>> nonDateOverlapPairs = routesA.stream().
                map(routeA -> Pair.of(routeA ,
                        routesB.stream().filter(routeB -> !routeA.isDateOverlap(routeB)).collect(Collectors.toSet()))).
                filter(pair -> !pair.getRight().isEmpty()).
                flatMap(pair ->
                        pair.getRight().stream().map(other -> Pair.of(pair.getLeft(), other))).collect(Collectors.toList());

        assertFalse(nonDateOverlapPairs.isEmpty(), routesA + " no overlap " + routesB);

        Pair<Route, Route> firstNonOverlap = nonDateOverlapPairs.get(0);

        Route from = firstNonOverlap.getLeft();
        Route to = firstNonOverlap.getRight();

        assertEquals(Integer.MAX_VALUE, routesCostRepository.getFor(from, to));
    }

    @Test
    void shouldComputeCostsSameRoute() {
        Set<Route> routesA = routeHelper.get(AltrinchamPiccadilly, routeRepository);

        routesA.forEach(routeA -> assertEquals(0, routesCostRepository.getFor(routeA, routeA)));
    }

    @Test
    void shouldComputeCostsRouteOtherDirection() {
        Set<Route> routesA = routeHelper.get(AltrinchamPiccadilly, routeRepository);
        Set<Route> routesB = routeHelper.get(PiccadillyAltrincham, routeRepository);

        routesA.forEach(routeA -> routesB.forEach(routeB -> {
            if (routeA.isDateOverlap(routeB)) {
                assertEquals(1, routesCostRepository.getFor(routeA, routeB), "wrong for " + routeA.getId() + " " + routeB.getId());
                assertEquals(1, routesCostRepository.getFor(routeB, routeA), "wrong for " + routeB.getId() + " " + routeA.getId());
            }
        }));
    }

    @Test
    void shouldComputeCostsDifferentRoutesOneChange() {
        Set<Route> routesA = routeHelper.get(CornbrookTheTraffordCentre, routeRepository);
        Set<Route> routesB = routeHelper.get(BuryPiccadilly, routeRepository);

        routesA.forEach(routeA -> routesB.forEach(routeB -> {
            if (routeA.isDateOverlap(routeB)) {
                assertEquals(2, routesCostRepository.getFor(routeA, routeB), "wrong for " + routeA.getId() + " " + routeB.getId());
                assertEquals(2, routesCostRepository.getFor(routeB, routeA), "wrong for " + routeB.getId() + " " + routeA.getId());
            }
        }));

    }

    @Test
    void shouldComputeCostsDifferentRoutesTwoChanges() {
        Set<Route> routesA = routeHelper.get(AltrinchamPiccadilly, routeRepository);
        Set<Route> routesB = routeHelper.get(VictoriaWythenshaweManchesterAirport, routeRepository);

        routesA.forEach(routeA -> routesB.forEach(routeB -> {
            if (routeA.isDateOverlap(routeB)) {
                assertEquals(1, routesCostRepository.getFor(routeA, routeB), "wrong for " + routeA.getId() + " " + routeB.getId());
                assertEquals(1, routesCostRepository.getFor(routeB, routeA), "wrong for " + routeB.getId() + " " + routeA.getId());
            }
        }));
    }

    @Test
    void shouldFindLowestHopCountForTwoStations() {
        Station start = TramStations.Altrincham.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes);

        assertEquals(1, result.getMin());
    }

    @Test
    void shouldFindHighestHopCountForTwoStations() {
        Station start = TramStations.Ashton.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes);

        assertEquals(1, result.getMax());
    }

    @Test
    void shouldFindLowestHopCountForTwoStationsSameRoute() {
        Station start = TramStations.Victoria.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes);

        assertEquals(0, result.getMin());
    }

    @Test
    void shouldFindNoHopeIfWrongTransportMode() {
        Station start = TramStations.Victoria.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);

        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, Collections.singleton(Train));

        assertEquals(Integer.MAX_VALUE, result.getMin());
        assertEquals(Integer.MAX_VALUE, result.getMax());

    }

    @Test
    void shouldFindMediaCityHops() {
        Station start = TramStations.MediaCityUK.from(stationRepository);
        Station end = TramStations.Ashton.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes);

        assertEquals(0, result.getMin());
    }

    @Test
    void shouldFindHighestHopCountForTwoStationsSameRoute() {
        Station start = TramStations.Victoria.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes);

        assertEquals(1, result.getMax());
    }

    @Test
    void shouldSortAsExpected() {

        Set<Route> routesA = routeHelper.get(CornbrookTheTraffordCentre, routeRepository);
        Set<Route> routesB = routeHelper.get(VictoriaWythenshaweManchesterAirport, routeRepository);
        Set<Route> routesC = routeHelper.get(BuryPiccadilly, routeRepository);

        Station destination = TramStations.TraffordCentre.from(stationRepository);
        LowestCostsForDestRoutes sorts = routesCostRepository.getLowestCostCalcutatorFor(LocationSet.singleton(destination));

        routesA.forEach(routeA -> routesB.forEach(routeB -> routesC.forEach(routeC -> {

            Stream<Route> toSort = Stream.of(routeC, routeB, routeA);

            Stream<Route> results = sorts.sortByDestinations(toSort);

            List<HasId<Route>> list = results.collect(Collectors.toList());
            assertEquals(3, list.size());
            assertEquals(routeA.getId(), list.get(0).getId());
            assertEquals(routeB.getId(), list.get(1).getId());
            assertEquals(routeC.getId(), list.get(2).getId());
        })));
    }

    @Test
    void shouldSaveIndexAsExpected() {
        CsvMapper mapper = new CsvMapper();

        assertTrue(indexFile.toFile().exists(), "Missing " + indexFile.toAbsolutePath());

        TransportDataFromCSVFile<RouteIndexData, RouteIndexData> indexLoader = new TransportDataFromCSVFile<>(indexFile, RouteIndexData.class, mapper);
        Stream<RouteIndexData> indexFromFile = indexLoader.load();
        List<RouteIndexData> resultsForIndex = indexFromFile.collect(Collectors.toList());

        IdSet<Route> expected = routeRepository.getRoutes().stream().collect(IdSet.collector());
        assertEquals(expected.size(), resultsForIndex.size());

        IdSet<Route> idsFromIndex = resultsForIndex.stream().map(RouteIndexData::getRouteId).collect(IdSet.idCollector());
        assertEquals(expected, idsFromIndex);
    }

}
