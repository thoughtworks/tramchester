package com.tramchester.integration.graph;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DataLoader;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.dataimport.data.RouteMatrixData;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.search.BetweenRoutesCostRepository;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouteToRouteCostsTest {

    private static ComponentContainer componentContainer;

    private BetweenRoutesCostRepository routeCosts;
    private TramRouteHelper routeHelper;
    private RouteRepository routeRepository;
    private static Path indexFile;
    private static Path matrixFile;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        final Path cacheFolder = config.getCacheFolder();

        indexFile = cacheFolder.resolve(RouteToRouteCosts.INDEX_FILE);
        matrixFile = cacheFolder.resolve(RouteToRouteCosts.ROUTE_MATRIX_FILE);

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
        TestEnv.clearDataCache(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() throws IOException {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        routeCosts = componentContainer.get(BetweenRoutesCostRepository.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);
    }

    @Test
    void shouldHaveExpectedNumberOfInterconnections() {
        assertEquals((14*14), routeCosts.size());
    }

    @Test
    void shouldComputeCostsSameRoute() {
        Set<Route> routesA = routeHelper.get(AltrinchamPiccadilly);

        routesA.forEach(routeA -> assertEquals(0,routeCosts.getFor(routeA, routeA)));
    }

    @Test
    void shouldComputeCostsRouteOtherDirection() {
        Set<Route> routesA = routeHelper.get(AltrinchamPiccadilly);
        Set<Route> routesB = routeHelper.get(PiccadillyAltrincham);

        routesA.forEach(routeA -> routesB.forEach(routeB -> {
            assertEquals(1,routeCosts.getFor(routeA, routeB));
            assertEquals(1,routeCosts.getFor(routeB, routeA));
        }));
    }

    @Test
    void shouldComputeCostsDifferentRoutesOneChange() {
        Set<Route> routesA = routeHelper.get(CornbrookTheTraffordCentre);
        Set<Route> routesB = routeHelper.get(BuryPiccadilly);

        routesA.forEach(routeA -> routesB.forEach(routeB -> {
            assertEquals(2, routeCosts.getFor(routeA, routeB));
            assertEquals(2, routeCosts.getFor(routeB, routeA));
        }));

    }

    @Test
    void shouldComputeCostsDifferentRoutesTwoChanges() {
        Set<Route> routesA = routeHelper.get(AltrinchamPiccadilly);
        Set<Route> routesB = routeHelper.get(VictoriaWythenshaweManchesterAirport);

        routesA.forEach(routeA -> routesB.forEach(routeB -> {
            assertEquals(1, routeCosts.getFor(routeA, routeB));
            assertEquals(1, routeCosts.getFor(routeB, routeA));
        }));
    }

    @Test
    void shouldFindLowestHopCountForTwoStations() {
        Station start = stationRepository.getStationById(TramStations.Altrincham.getId());
        Station end = stationRepository.getStationById(TramStations.ManAirport.getId());
        NumberOfChanges result = routeCosts.getNumberOfChanges(start, end);

        assertEquals(1, result.getMin());
    }

    @Test
    void shouldFindHighestHopCountForTwoStations() {
        Station start = stationRepository.getStationById(TramStations.Ashton.getId());
        Station end = stationRepository.getStationById(TramStations.ManAirport.getId());
        NumberOfChanges result = routeCosts.getNumberOfChanges(start, end);

        assertEquals(1, result.getMax());
    }

    @Test
    void shouldFindLowestHopCountForTwoStationsSameRoute() {
        Station start = stationRepository.getStationById(TramStations.Victoria.getId());
        Station end = stationRepository.getStationById(TramStations.ManAirport.getId());
        NumberOfChanges result = routeCosts.getNumberOfChanges(start, end);

        assertEquals(0, result.getMin());
    }

    @Test
    void shouldFindMediaCityHops() {
        Station start = stationRepository.getStationById(TramStations.MediaCityUK.getId());
        Station end = stationRepository.getStationById(TramStations.Ashton.getId());
        NumberOfChanges result = routeCosts.getNumberOfChanges(start, end);

        assertEquals(0, result.getMin());
    }

    @Test
    void shouldFindHighestHopCountForTwoStationsSameRoute() {
        Station start = stationRepository.getStationById(TramStations.Victoria.getId());
        Station end = stationRepository.getStationById(TramStations.ManAirport.getId());
        NumberOfChanges result = routeCosts.getNumberOfChanges(start, end);

        assertEquals(1, result.getMax());
    }

    @Test
    void shouldSortAsExpected() {
        Set<Route> routesA = routeHelper.get(CornbrookTheTraffordCentre);
        Set<Route> routesB = routeHelper.get(VictoriaWythenshaweManchesterAirport);
        Set<Route> routesC = routeHelper.get(BuryPiccadilly);

        routesA.forEach(routeA -> routesB.forEach(routeB -> routesC.forEach(routeC -> {

            List<HasId<Route>> toSort = Arrays.asList(routeC, routeB, routeA);

            IdSet<Route> destinations = new IdSet<>(routeA.getId());
            Stream<HasId<Route>> results = routeCosts.sortByDestinations(toSort.stream(), destinations);

            List<HasId<Route>> list = results.collect(Collectors.toList());
            assertEquals(toSort.size(), list.size());
            assertEquals(routeA.getId(), list.get(0).getId());
            assertEquals(routeB.getId(), list.get(1).getId());
            assertEquals(routeC.getId(), list.get(2).getId());
        })));
    }

    @Test
    void shouldSaveIndexAsExpected() {
        CsvMapper mapper = new CsvMapper();

        assertTrue(indexFile.toFile().exists(), "Missing " + indexFile.toAbsolutePath());

        DataLoader<RouteIndexData> indexLoader = new DataLoader<>(indexFile, RouteIndexData.class, mapper);
        Stream<RouteIndexData> indexFromFile = indexLoader.load();
        List<RouteIndexData> resultsForIndex = indexFromFile.collect(Collectors.toList());

        IdSet<Route> expected = routeRepository.getRoutes().stream().collect(IdSet.collector());
        assertEquals(expected.size(), resultsForIndex.size());

        IdSet<Route> idsFromIndex = resultsForIndex.stream().map(RouteIndexData::getRouteId).collect(IdSet.idCollector());
        assertEquals(expected, idsFromIndex);
    }

    @Test
    void shouldSaveMatrixAsExpected() {
        CsvMapper mapper = new CsvMapper();

        assertTrue(matrixFile.toFile().exists(), "Missing " + matrixFile.toAbsolutePath());

        DataLoader<RouteMatrixData> indexLoader = new DataLoader<>(matrixFile, RouteMatrixData.class, mapper);
        Stream<RouteMatrixData> indexFromFile = indexLoader.load();
        List<RouteMatrixData> resultsForMatrix = indexFromFile.collect(Collectors.toList());

        final int numberOfRoutes = routeRepository.numberOfRoutes();
        assertEquals(numberOfRoutes, resultsForMatrix.size());

        resultsForMatrix.forEach(row -> {
            assertTrue(row.getSource()>=0);
            assertTrue(row.getSource()<numberOfRoutes);
            assertEquals(row.getDestinations().size(), numberOfRoutes);
        });
    }
}
