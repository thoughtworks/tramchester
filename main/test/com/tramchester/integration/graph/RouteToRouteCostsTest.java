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
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.graph.search.routes.RouteIndexPair;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DualTest;
import com.tramchester.testSupport.testTags.GMTest;
import com.tramchester.testSupport.testTags.PiccGardens2022;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
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
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {
        config = tramchesterConfig; //new IntegrationTramTestConfig();
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

    @Test
    void shouldHaveFullyConnectedForTramsWhereDatesOverlaps() {
        Set<Route> routes = routeRepository.getRoutes(TramsOnly).stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        TimeRange timeRangeForOverlaps = TimeRange.of(TramTime.of(8, 45), TramTime.of(16, 45));

        List<RoutePair> failed = new ArrayList<>();

        for (Route start : routes) {
            for (Route end : routes) {
                if (!start.equals(end) && start.isDateOverlap(end)) {
                    if (routesCostRepository.getNumberOfChanges(start, end, date, timeRangeForOverlaps).isNone()) {
                        failed.add(new RoutePair(start, end));
                    }
                }
            }
        }

        assertTrue(failed.isEmpty(), "on date " + date + failed);

    }

    @Test
    void shouldReproIssueWithGreenLineRoute() {
        RouteCostMatrix routeCostMatrix = componentContainer.get(RouteCostMatrix.class);
        RouteIndex routeIndex = componentContainer.get(RouteIndex.class);

        Route greenInbound = routeHelper.getOneRoute(AltrinchamManchesterBury, date);

        int greenIndex = routeIndex.indexFor(greenInbound.getId());

        Set<Route> routes = routeRepository.getRoutes(TramsOnly).stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

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

    @Test
    void shouldComputeCostsSameRoute() {
        Route routeA = routeHelper.getOneRoute(ManchesterAirportWythenshaweVictoria, date);

        assertEquals(0, getMinCost(routesCostRepository.getNumberOfChanges(routeA, routeA, date, timeRange)));
    }

    @Test
    void shouldComputeCostsRouteOtherDirection() {
        Route routeA = routeHelper.getOneRoute(ManchesterAirportWythenshaweVictoria, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        assertEquals(1, getMinCost(routesCostRepository.getNumberOfChanges(routeA, routeB, date, timeRange)), "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(1, getMinCost(routesCostRepository.getNumberOfChanges(routeB, routeA, date, timeRange)), "wrong for " + routeB.getId() + " " + routeA.getId());

    }

    @Test
    void shouldComputeCostsDifferentRoutesTwoChange() {
        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);
        Route routeB = routeHelper.getOneRoute(ReplacementRouteDeansgatePiccadilly, date); // was BuryPiccadilly

        assertEquals(2, getMinCost(routesCostRepository.getNumberOfChanges(routeA, routeB, date, timeRange)),
                "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(2, getMinCost(routesCostRepository.getNumberOfChanges(routeB, routeA, date, timeRange)),
                "wrong for " + routeB.getId() + " " + routeA.getId());
    }

    @Test
    void shouldFailIfOurOfTimeRangeDifferentRoutesTwoChange() {
        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);
        Route routeB = routeHelper.getOneRoute(ReplacementRoutePiccadillyDeansgate, date); // BuryPiccadilly

        assertEquals(2, getMinCost(routesCostRepository.getNumberOfChanges(routeA, routeB, date, timeRange)),
                "wrong for " + routeA.getId() + " " + routeB.getId());

        TimeRange outOfRange = TimeRange.of(TramTime.of(3,35), TramTime.of(3,45));
        assertEquals(Integer.MAX_VALUE, getMinCost(routesCostRepository.getNumberOfChanges(routeB, routeA, date, outOfRange)),
                "wrong for " + routeB.getId() + " " + routeA.getId());
    }

    @Test
    void shouldBacktrackToChangesSingleChange() {
        Route routeA = routeHelper.getOneRoute(TheTraffordCentreCornbrook, date);
        Route routeB = routeHelper.getOneRoute(AltrinchamManchesterBury, date);

        List<List<RouteAndChanges>> results = routesCostRepository.getChangesFor(routeA, routeB);
        assertEquals(1, results.size());

        List<RouteAndChanges> firstSetOfChanges = results.get(0);
        assertEquals(1, firstSetOfChanges.size());

        RouteAndChanges actualChange = firstSetOfChanges.get(0);

        assertEquals(1, actualChange.getStations().size());
        assertTrue(getStationsFor(actualChange).contains(Cornbrook.from(stationRepository)), results.toString());
    }

    @Disabled("Not while picc gardens is closed")
    @PiccGardens2022
    @Test
    void shouldBacktrackToChangesMultipleChanges() {
        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, date); // was BuryPiccadilly
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        // 2 -> 7
        List<List<RouteAndChanges>> results = routesCostRepository.getChangesFor(routeA, routeB);
        assertEquals(7, results.size(), results.toString());

        List<RouteAndChanges> firstChangeSet = results.get(0);
        assertEquals(2, firstChangeSet.size());

        // todo walking from market street
        //validateRoutingForBuryToTraffordCenterRoute(firstChangeSet);
        assertTrue(getStationsFor(firstChangeSet.get(0)).contains(MarketStreet.from(stationRepository)));

        List<RouteAndChanges> secondChangeSet = results.get(0);
        assertEquals(2, secondChangeSet.size());

        // todo walking from market street
        //validateRoutingForBuryToTraffordCenterRoute(secondChangeSet);
        assertTrue(getStationsFor(secondChangeSet.get(0)).contains(MarketStreet.from(stationRepository)));
    }

    @PiccGardens2022
    @Test
    void shouldHaveExpectedRouteToRouteCostsForClosedStations() {
        RouteToRouteCosts routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);

        Location<?> stPetersSquare = StPetersSquare.from(stationRepository);
        Location<?> piccGardens = PiccadillyGardens.from(stationRepository);

        TimeRange timeRange = TimeRange.of(TramTime.of(6,0), TramTime.of(23,55));
        Set<TransportMode> mode = EnumSet.of(TransportMode.Tram);

        NumberOfChanges costs = routeToRouteCosts.getNumberOfChanges(stPetersSquare, piccGardens, mode, date, timeRange);

        assertEquals(1, costs.getMin());
    }

    private Set<Station> getStationsFor(RouteAndChanges routeAndChanges) {
        return routeAndChanges.getStations().stream().map(InterchangeStation::getStation).collect(Collectors.toSet());
    }

    @Test
    void shouldComputeCostsDifferentRoutesOneChanges() {
        Route routeA = routeHelper.getOneRoute(AltrinchamManchesterBury, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        assertEquals(1, getMinCost(routesCostRepository.getNumberOfChanges(routeA, routeB, date, timeRange)),
                "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(1, getMinCost(routesCostRepository.getNumberOfChanges(routeB, routeA, date, timeRange)),
                "wrong for " + routeB.getId() + " " + routeA.getId());

    }

    @Test
    void shouldFindLowestHopCountForTwoStations() {
        Station start = TramStations.Altrincham.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date, timeRange);

        assertEquals(1, getMinCost(result));
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

        assertEquals(0, getMinCost(result));
    }

    @Test
    void shouldFindNoHopeIfWrongTransportMode() {
        Station start = TramStations.Victoria.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);

        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, Collections.singleton(Train), date, timeRange);

        assertEquals(Integer.MAX_VALUE, getMinCost(result));
        assertEquals(Integer.MAX_VALUE, result.getMax());

    }

    @PiccGardens2022
    @Test
    void shouldFindMediaCityHops() {
        Station mediaCity = MediaCityUK.from(stationRepository);
        Station ashton = Ashton.from(stationRepository);

        NumberOfChanges result = routesCostRepository.getNumberOfChanges(mediaCity, ashton, modes, date, timeRange);

        assertEquals(0, getMinCost(result));
    }

    @Test
    void shouldFindHighestHopCountForTwoStationsSameRoute() {
        Station start = TramStations.Victoria.from(stationRepository);
        Station end = TramStations.ManAirport.from(stationRepository);
        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, modes, date, timeRange);

        assertEquals(0, getMinCost(result));
    }

    @PiccGardens2022
    @Test
    void shouldSortAsExpected() {

        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);
        Route routeC = routeHelper.getOneRoute(ReplacementRoutePiccadillyDeansgate, date); // BuryPiccadilly

        Station destination = TramStations.TraffordCentre.from(stationRepository);
        LowestCostsForDestRoutes sorts = routesCostRepository.getLowestCostCalcutatorFor(LocationSet.singleton(destination), date, timeRange, modes);

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

        assertEquals(0, getMinCost(changes), changes.toString());
    }

    @Test
    void shouldHandleServicesAtMidnight() {
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();
        TimeRange timeRange = TimeRange.of(TramTime.of(0,0), Duration.ZERO, Duration.ofMinutes(maxDuration));

        Station navigationRoad = NavigationRoad.from(stationRepository);

        NumberOfChanges changes = routesCostRepository.getNumberOfChanges(altrincham, navigationRoad, modes, date, timeRange);

        assertEquals(0, getMinCost(changes), changes.toString());
    }

    @Test
    void shouldHandleServicesJustAfterMidnight() {
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();
        TimeRange timeRange = TimeRange.of(TramTime.of(0,1), Duration.ZERO, Duration.ofMinutes(maxDuration));

        Station navigationRoad = NavigationRoad.from(stationRepository);

        NumberOfChanges changes = routesCostRepository.getNumberOfChanges(altrincham, navigationRoad, modes, date, timeRange);

        assertEquals(0, getMinCost(changes), changes.toString());
    }

    private int getMinCost(NumberOfChanges routesCostRepository) {
        return routesCostRepository.getMin();
    }

}
