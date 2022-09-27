package com.tramchester.integration.graph;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.domain.*;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.SimpleList;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.graph.search.routes.RouteIndexPair;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.Summer2022;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

public class RouteToRouteCostsTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private RouteToRouteCosts routesCostRepository;
    private TramRouteHelper routeHelper;
    private RouteRepository routeRepository;
    private static Path indexFile;
    private StationRepository stationRepository;
    private final Set<TransportMode> modes = Collections.singleton(Tram);
    private TramDate date;
    private TimeRange timeRange;

    @BeforeAll
    static void onceBeforeAnyTestRuns() {
        config = new IntegrationTramTestConfig();
        final Path cacheFolder = config.getCacheFolder();

        indexFile = cacheFolder.resolve(RouteToRouteCosts.INDEX_FILE);

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        // Clear Cache - test creation here
        TestEnv.clearDataCache(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {

        // Clear Cache
        TestEnv.clearDataCache(componentContainer); // => this removes the index cache
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        routesCostRepository = componentContainer.get(RouteToRouteCosts.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);

        date = TestEnv.testDay();
        timeRange = TimeRange.of(TramTime.of(7,45), TramTime.of(22,45));
    }

    @Summer2022
    @Test
    void shouldHaveFullyConnectedForTramsWhereDatesOverlaps() {
        TramDate testDate = this.date;
        Set<Route> routes = routeRepository.getRoutes().stream().filter(route -> route.isAvailableOn(testDate)).collect(Collectors.toSet());

        TimeRange timeRangeForOverlaps = TimeRange.of(TramTime.of(8, 45), TramTime.of(16, 45));

        List<RoutePair> failed = new ArrayList<>();

        for (Route start : routes) {
            for (Route end : routes) {
                if (!start.equals(end) && start.isDateOverlap(end)) {
                    if (Integer.MAX_VALUE==routesCostRepository.getNumberChangesFor(start, end, testDate, timeRangeForOverlaps)) {
                        failed.add(new RoutePair(start, end));
                    }
                }
            }
        }

        assertTrue(failed.isEmpty(), "on date " + testDate + failed);

    }

    @Test
    void shouldReproIssueWithGreenLineRoute() {
        RouteCostMatrix routeCostMatrix = componentContainer.get(RouteCostMatrix.class);
        RouteIndex routeIndex = componentContainer.get(RouteIndex.class);

        Route greenInbound = routeHelper.getOneRoute(AltrinchamManchesterBury, date);

        int greenIndex = routeIndex.indexFor(greenInbound.getId());

        Set<Route> routes = routeRepository.getRoutes().stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        IndexedBitSet dateOverlaps = IndexedBitSet.getIdentity(routeRepository.numberOfRoutes());

        for(Route otherRoute : routes) {
            if (!otherRoute.getId().equals(greenInbound.getId())) {
                int otherIndex = routeIndex.indexFor(otherRoute.getId());

                RouteIndexPair routeIndexPair = RouteIndexPair.of(greenIndex, otherIndex);
                Stream<SimpleList<RouteIndexPair>> stream = routeCostMatrix.getChangesFor(routeIndexPair, dateOverlaps);

                Set<SimpleList<RouteIndexPair>> results = stream.collect(Collectors.toSet());

                assertFalse(results.isEmpty(), "no link for " + greenInbound + " and " + otherRoute);
            }
        }


    }

    @Disabled("Requires route with non-overlapping dates, which isn't the case for the current data")
    @Test
    void shouldFailToFindIfNoDateOverlop() {
        Set<Route> routesA = routeHelper.get(AltrinchamPiccadilly);
        Set<Route> routesB = routeHelper.get(PiccadillyAltrincham);

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

        assertEquals(Integer.MAX_VALUE, routesCostRepository.getNumberChangesFor(from, to, date, timeRange));
    }

    @Test
    void shouldComputeCostsSameRoute() {
        Set<Route> routesA = routeHelper.get(AltrinchamPiccadilly);

        routesA.forEach(routeA -> assertEquals(0, routesCostRepository.getNumberChangesFor(routeA, routeA, date, timeRange)));
    }

    @Test
    void shouldComputeCostsRouteOtherDirection() {
        Route routeA = routeHelper.getOneRoute(AltrinchamPiccadilly, date);
        Route routeB = routeHelper.getOneRoute(PiccadillyAltrincham, date);

        assertEquals(1, routesCostRepository.getNumberChangesFor(routeA, routeB, date, timeRange), "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(1, routesCostRepository.getNumberChangesFor(routeB, routeA, date, timeRange), "wrong for " + routeB.getId() + " " + routeA.getId());

    }

    @Test
    void shouldComputeCostsDifferentRoutesTwoChange() {
        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);
        Route routeB = routeHelper.getOneRoute(BuryPiccadilly, date);

        assertEquals(2, routesCostRepository.getNumberChangesFor(routeA, routeB, date, timeRange), "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(2, routesCostRepository.getNumberChangesFor(routeB, routeA, date, timeRange), "wrong for " + routeB.getId() + " " + routeA.getId());
    }

    @Test
    void shouldFailIfOurOfTimeRangeDifferentRoutesTwoChange() {
        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);
        Route routeB = routeHelper.getOneRoute(BuryPiccadilly, date);

        assertEquals(2, routesCostRepository.getNumberChangesFor(routeA, routeB, date, timeRange), "wrong for " + routeA.getId() + " " + routeB.getId());

        TimeRange outOfRange = TimeRange.of(TramTime.of(3,35), TramTime.of(3,45));
        assertEquals(Integer.MAX_VALUE, routesCostRepository.getNumberChangesFor(routeB, routeA, date, outOfRange), "wrong for " + routeB.getId() + " " + routeA.getId());
    }

    @Test
    void shouldBacktrackToChangesSingleChange() {
        Route routeA = routeHelper.getOneRoute(TheTraffordCentreCornbrook, date);
        Route routeB = routeHelper.getOneRoute(AltrinchamPiccadilly, date);

        List<List<RouteAndInterchanges>> results = routesCostRepository.getChangesFor(routeA, routeB);
        assertEquals(1, results.size());

        List<RouteAndInterchanges> firstSetOfChanges = results.get(0);
        assertEquals(1, firstSetOfChanges.size());

        RouteAndInterchanges actualChange = firstSetOfChanges.get(0);
        assertEquals(1, actualChange.getInterchangeStations().size());
        assertTrue(actualChange.getInterchangeStations().contains(Cornbrook.from(stationRepository)), results.toString());
    }

    @Test
    void shouldBacktrackToChangesMultipleChanges() {
        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        List<List<RouteAndInterchanges>> results = routesCostRepository.getChangesFor(routeA, routeB);
        assertEquals(2, results.size(), results.toString());

        List<RouteAndInterchanges> firstChangeSet = results.get(0);
        assertEquals(2, firstChangeSet.size());

        validateRoutingForBuryToTraffordCenterRoute(firstChangeSet);

        List<RouteAndInterchanges> secondChangeSet = results.get(0);
        assertEquals(2, secondChangeSet.size());

        validateRoutingForBuryToTraffordCenterRoute(secondChangeSet);
    }

    @Test
    void shouldGetCorrectNumberHopsForMultipleChanges() {
        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        int results = routesCostRepository.getNumberChangesFor(routeA, routeB, date, timeRange);

        assertEquals(2, results);
    }

    private void validateRoutingForBuryToTraffordCenterRoute(List<RouteAndInterchanges> firstChangeSet) {
        Set<Station> firstChanges = firstChangeSet.get(0).getInterchangeStations();
        assertTrue(firstChanges.containsAll(TramStations.allFrom(stationRepository, MarketStreet, Piccadilly, PiccadillyGardens)),
                firstChanges.toString());

        Set<Station> secondChanges = firstChangeSet.get(1).getInterchangeStations();
        assertEquals(2, secondChanges.size());
        assertTrue(secondChanges.containsAll(TramStations.allFrom(stationRepository, Cornbrook, Pomona)), secondChanges.toString());
    }

    @Test
    void shouldComputeCostsDifferentRoutesOneChanges() {
        Route routeA = routeHelper.getOneRoute(AltrinchamPiccadilly, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        assertEquals(1, routesCostRepository.getNumberChangesFor(routeA, routeB, date, timeRange), "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(1, routesCostRepository.getNumberChangesFor(routeB, routeA, date, timeRange), "wrong for " + routeB.getId() + " " + routeA.getId());

    }

    @Test
    void shouldFindLowestHopCountForTwoStations() {
        Station start = TramStations.Altrincham.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date, timeRange);

        assertEquals(1, result.getMin());
    }

    @Test
    void shouldFindHighestHopCountForTwoStations() {
        Station start = TramStations.Ashton.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date, timeRange);

        assertEquals(1, result.getMax());
    }

    @Test
    void shouldFindLowestHopCountForTwoStationsSameRoute() {
        Station start = TramStations.Victoria.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date, timeRange);

        assertEquals(0, result.getMin());
    }

    @Test
    void shouldFindNoHopeIfWrongTransportMode() {
        Station start = TramStations.Victoria.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);

        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, Collections.singleton(Train), date, timeRange);

        assertEquals(Integer.MAX_VALUE, result.getMin());
        assertEquals(Integer.MAX_VALUE, result.getMax());

    }

    @Test
    void shouldFindMediaCityHops() {
        Station start = TramStations.MediaCityUK.from(stationRepository);
        Station end = TramStations.Ashton.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date, timeRange);

        assertEquals(0, result.getMin());
    }

    @Test
    void shouldFindHighestHopCountForTwoStationsSameRoute() {
        Station start = TramStations.Victoria.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date, timeRange);

        assertEquals(0, result.getMin());

        assertEquals(1, result.getMax());
    }

    @Test
    void shouldSortAsExpected() {

        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);
        Route routeC = routeHelper.getOneRoute(BuryPiccadilly, date);

        Station destination = TramStations.TraffordCentre.from(stationRepository);
        LowestCostsForDestRoutes sorts = routesCostRepository.getLowestCostCalcutatorFor(LocationSet.singleton(destination), date, timeRange);

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

    @Test
    void shouldHandleServicesOverMidnight() {
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();
        TimeRange timeRange = TimeRange.of(TramTime.of(23,59), Duration.ZERO, Duration.ofMinutes(maxDuration));

        Station navigationRoad = NavigationRoad.from(stationRepository);

        NumberOfChanges changes = routesCostRepository.getNumberOfChanges(altrincham, navigationRoad, modes, date, timeRange);

        assertEquals(0, changes.getMin(), changes.toString());
    }

    @Test
    void shouldHandleServicesAtMidnight() {
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();
        TimeRange timeRange = TimeRange.of(TramTime.of(0,0), Duration.ZERO, Duration.ofMinutes(maxDuration));

        Station navigationRoad = NavigationRoad.from(stationRepository);

        NumberOfChanges changes = routesCostRepository.getNumberOfChanges(altrincham, navigationRoad, modes, date, timeRange);

        assertEquals(0, changes.getMin(), changes.toString());
    }

    @Test
    void shouldHandleServicesJustAfterMidnight() {
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();
        TimeRange timeRange = TimeRange.of(TramTime.of(0,1), Duration.ZERO, Duration.ofMinutes(maxDuration));

        Station navigationRoad = NavigationRoad.from(stationRepository);

        NumberOfChanges changes = routesCostRepository.getNumberOfChanges(altrincham, navigationRoad, modes, date, timeRange);

        assertEquals(0, changes.getMin(), changes.toString());
    }

    @Test
    void shouldReproIssueBetweenMonsalAndPiccadily() {
        // Compute number of changes between Id{'9400ZZMAMON'} and Id{'9400ZZMAPIC'} using modes '[]' on 2022-07-21 within
        // TimeRange{begin=TramTime{h=22, m=15}, end=TramTime{d=1 h=0, m=19}}

        TimeRange range = TimeRange.of(TramTime.of(22,15), TramTime.nextDay(0,19));
        NumberOfChanges results = routesCostRepository.getNumberOfChanges(Monsall.from(stationRepository), Piccadilly.from(stationRepository),
                modes, date, range);

        assertEquals(1, results.getMin());

    }


}
