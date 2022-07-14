package com.tramchester.integration.graph;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.graph.search.RouteToRouteCosts;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.Summer2022;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class RouteToRouteCostsTest {

    private static ComponentContainer componentContainer;

    private RouteToRouteCosts routesCostRepository;
    private TramRouteHelper routeHelper;
    private RouteRepository routeRepository;
    private static Path indexFile;
    private StationRepository stationRepository;
    private final Set<TransportMode> modes = Collections.singleton(Tram);
    private LocalDate date;

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
        TestEnv.clearDataCache(componentContainer); // => this removes the index cache
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        routesCostRepository = componentContainer.get(RouteToRouteCosts.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper();

        date = TestEnv.testDay();
    }

    @Test
    void shouldHaveFullyConnectedForTramsWhereDatesOverlaps() {
        Set<Route> routes = routeRepository.getRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        List<RoutePair> failed = new ArrayList<>();
        for (Route start : routes) {
            for (Route end : routes) {
                if (!start.equals(end) && start.isDateOverlap(end)) {
                    if (Integer.MAX_VALUE==routesCostRepository.getFor(start, end, date)) {
                        failed.add(new RoutePair(start, end));
                    }
                }
            }
            assertTrue(failed.isEmpty(), failed.toString());
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

        assertEquals(Integer.MAX_VALUE, routesCostRepository.getFor(from, to, date));
    }

    @Test
    void shouldComputeCostsSameRoute() {
        Set<Route> routesA = routeHelper.get(AltrinchamPiccadilly, routeRepository);

        routesA.forEach(routeA -> assertEquals(0, routesCostRepository.getFor(routeA, routeA, date)));
    }

    @Test
    void shouldComputeCostsRouteOtherDirection() {
        Route routeA = routeHelper.getOneRoute(AltrinchamPiccadilly, routeRepository, date);
        Route routeB = routeHelper.getOneRoute(PiccadillyAltrincham, routeRepository, date);

        assertEquals(1, routesCostRepository.getFor(routeA, routeB, date), "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(1, routesCostRepository.getFor(routeB, routeA, date), "wrong for " + routeB.getId() + " " + routeA.getId());

    }

    @Test
    void shouldComputeCostsDifferentRoutesTwoChange() {
        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, routeRepository, date);
        Route routeB = routeHelper.getOneRoute(BuryPiccadilly, routeRepository, date);

        assertEquals(2, routesCostRepository.getFor(routeA, routeB, date), "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(2, routesCostRepository.getFor(routeB, routeA, date), "wrong for " + routeB.getId() + " " + routeA.getId());

    }

    @Test
    void shouldBacktrackToChangesSingleChange() {
        Route routeA = routeHelper.getOneRoute(TheTraffordCentreCornbrook, routeRepository, date);
        Route routeB = routeHelper.getOneRoute(AltrinchamPiccadilly, routeRepository, date);

        List<List<RouteInterchanges>> results = routesCostRepository.getChangesFor(routeA, routeB);
        assertEquals(1, results.size());

        List<RouteInterchanges> firstSetOfChanges = results.get(0);
        assertEquals(1, firstSetOfChanges.size());

        RouteInterchanges actualChange = firstSetOfChanges.get(0);
        assertEquals(1, actualChange.getInterchangeStations().size());
        assertTrue(actualChange.getInterchangeStations().contains(Cornbrook.from(stationRepository)), results.toString());
    }

    @Test
    void shouldBacktrackToChangesMultipleChanges() {
        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, routeRepository, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, routeRepository, date);

        List<List<RouteInterchanges>> results = routesCostRepository.getChangesFor(routeA, routeB);
        assertEquals(2, results.size(), results.toString());

        List<RouteInterchanges> firstChangeSet = results.get(0);
        assertEquals(2, firstChangeSet.size());

        validateRoutingForBuryToTraffordCenterRoute(firstChangeSet);

        List<RouteInterchanges> secondChangeSet = results.get(0);
        assertEquals(2, secondChangeSet.size());

        validateRoutingForBuryToTraffordCenterRoute(secondChangeSet);
    }

    @Test
    void shouldGetCorrectNumberHopsForMultipleChanges() {
        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, routeRepository, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, routeRepository, date);

        int results = routesCostRepository.getFor(routeA, routeB, date);

        assertEquals(2, results);
    }

    private void validateRoutingForBuryToTraffordCenterRoute(List<RouteInterchanges> firstChangeSet) {
        Set<Station> firstChanges = firstChangeSet.get(0).getInterchangeStations();
        assertTrue(firstChanges.containsAll(TramStations.allFrom(stationRepository, MarketStreet, Piccadilly, PiccadillyGardens)),
                firstChanges.toString());

        Set<Station> secondChanges = firstChangeSet.get(1).getInterchangeStations();
        assertEquals(2, secondChanges.size());
        assertTrue(secondChanges.containsAll(TramStations.allFrom(stationRepository, Cornbrook, Pomona)), secondChanges.toString());
    }

    @Test
    void shouldComputeCostsDifferentRoutesOneChanges() {
        Route routeA = routeHelper.getOneRoute(AltrinchamPiccadilly, routeRepository, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, routeRepository, date);

        assertEquals(1, routesCostRepository.getFor(routeA, routeB, date), "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(1, routesCostRepository.getFor(routeB, routeA, date), "wrong for " + routeB.getId() + " " + routeA.getId());

    }

    @Test
    void shouldFindLowestHopCountForTwoStations() {
        Station start = TramStations.Altrincham.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date);

        assertEquals(1, result.getMin());
    }

    @Test
    void shouldFindHighestHopCountForTwoStations() {
        Station start = TramStations.Ashton.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date);

        assertEquals(1, result.getMax());
    }

    @Test
    void shouldFindLowestHopCountForTwoStationsSameRoute() {
        Station start = TramStations.Victoria.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date);

        assertEquals(0, result.getMin());
    }

    @Test
    void shouldFindNoHopeIfWrongTransportMode() {
        Station start = TramStations.Victoria.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);

        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, Collections.singleton(Train), date);

        assertEquals(Integer.MAX_VALUE, result.getMin());
        assertEquals(Integer.MAX_VALUE, result.getMax());

    }

    @Test
    void shouldFindMediaCityHops() {
        Station start = TramStations.MediaCityUK.from(stationRepository);
        Station end = TramStations.Ashton.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date);

        assertEquals(0, result.getMin());
    }

    @Summer2022
    @Test
    void shouldFindHighestHopCountForTwoStationsSameRoute() {
        Station start = TramStations.Victoria.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date);

        assertEquals(0, result.getMin());

        // summer 2022?
        assertEquals(1, result.getMax());
    }

    @Test
    void shouldSortAsExpected() {

        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, routeRepository, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, routeRepository, date);
        Route routeC = routeHelper.getOneRoute(BuryPiccadilly, routeRepository, date);

        Station destination = TramStations.TraffordCentre.from(stationRepository);
        LowestCostsForDestRoutes sorts = routesCostRepository.getLowestCostCalcutatorFor(LocationSet.singleton(destination), date);

        Stream<Route> toSort = Stream.of(routeC, routeB, routeA);

        Stream<Route> results = sorts.sortByDestinations(toSort);
        List<HasId<Route>> list = results.collect(Collectors.toList());

        assertEquals(3, list.size());
        assertEquals(routeA.getId(), list.get(0).getId());
        assertEquals(routeB.getId(), list.get(1).getId());
        assertEquals(routeC.getId(), list.get(2).getId());

    }

    @Test
    void shouldSaveIndexAsExpected() {
        CsvMapper mapper = CsvMapper.builder().addModule(new AfterburnerModule()).build();

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
