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
import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.graph.search.routes.RouteCostMatrix;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DualTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.TramsOnly;
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
    private final Set<TransportMode> modes = TramsOnly;
    private TramDate date;
    private TimeRange timeRange;

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {
        config = tramchesterConfig;
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
        Set<Route> routes = routeRepository.getRoutes(modes).stream().
                filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        TimeRange timeRangeForOverlaps = TimeRange.of(TramTime.of(8, 45), TramTime.of(16, 45));

        List<RoutePair> failed = new ArrayList<>();

        for (Route start : routes) {
            for (Route end : routes) {
                if (!start.equals(end) && start.isDateOverlap(end)) {
                    if (routesCostRepository.getNumberOfChanges(start, end, date, timeRangeForOverlaps, modes).isNone()) {
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
        RouteIndexPairFactory pairFactory = componentContainer.get(RouteIndexPairFactory.class);

        Route greenInbound = routeHelper.getOneRoute(AltrinchamManchesterBury, date);

        int greenIndex = routeIndex.indexFor(greenInbound.getId());

        Set<Route> routes = routeRepository.getRoutes(TramsOnly).stream().filter(route -> route.isAvailableOn(date)).collect(Collectors.toSet());

        int numberOfRoutes = routeRepository.numberOfRoutes();
        IndexedBitSet dateOverlaps = IndexedBitSet.getIdentity(numberOfRoutes, numberOfRoutes);

        for(Route otherRoute : routes) {
            if (!otherRoute.getId().equals(greenInbound.getId())) {
                int otherIndex = routeIndex.indexFor(otherRoute.getId());

                RouteIndexPair routeIndexPair = pairFactory.get(greenIndex, otherIndex);
                Stream<List<RoutePair>> stream = routeCostMatrix.getChangesFor(routeIndexPair, dateOverlaps);

                Set<List<RoutePair>> results = stream.collect(Collectors.toSet());

                assertFalse(results.isEmpty(), "no link for " + greenInbound + " and " + otherRoute);
            }
        }

    }

    @Test
    void shouldComputeCostsSameRoute() {
        Route routeA = routeHelper.getOneRoute(ManchesterAirportWythenshaweVictoria, date);

        assertEquals(0, getMinCost(routesCostRepository.getNumberOfChanges(routeA, routeA, date, timeRange, modes)));
    }

    @Test
    void shouldComputeCostsRouteOtherDirection() {
        Route routeA = routeHelper.getOneRoute(ManchesterAirportWythenshaweVictoria, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        assertEquals(1, getMinCost(routesCostRepository.getNumberOfChanges(routeA, routeB, date, timeRange, modes)), "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(1, getMinCost(routesCostRepository.getNumberOfChanges(routeB, routeA, date, timeRange, modes)), "wrong for " + routeB.getId() + " " + routeA.getId());

    }

    @Test
    void shouldComputeCostsDifferentRoutesTwoChange() {
        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);
        Route routeB = routeHelper.getOneRoute(BuryPiccadilly, date);

        assertEquals(2, getMinCost(routesCostRepository.getNumberOfChanges(routeA, routeB, date, timeRange, modes)),
                "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(2, getMinCost(routesCostRepository.getNumberOfChanges(routeB, routeA, date, timeRange, modes)),
                "wrong for " + routeB.getId() + " " + routeA.getId());
    }

    @Test
    void shouldFailIfOurOfTimeRangeDifferentRoutesTwoChange() {
        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);
        Route routeB = routeHelper.getOneRoute(BuryPiccadilly, date);

        assertEquals(2, getMinCost(routesCostRepository.getNumberOfChanges(routeA, routeB, date, timeRange, modes)),
                "wrong for " + routeA.getId() + " " + routeB.getId());

        TimeRange outOfRange = TimeRange.of(TramTime.of(3,35), TramTime.of(3,45));
        assertEquals(Integer.MAX_VALUE, getMinCost(routesCostRepository.getNumberOfChanges(routeB, routeA, date, outOfRange, modes)),
                "wrong for " + routeB.getId() + " " + routeA.getId());
    }

    @Test
    void shouldBacktrackToChangesSingleChange() {
        Route routeA = routeHelper.getOneRoute(TheTraffordCentreCornbrook, date);
        Route routeB = routeHelper.getOneRoute(AltrinchamManchesterBury, date);

        List<List<RouteAndChanges>> results = routesCostRepository.getChangesFor(routeA, routeB, modes);
        assertEquals(1, results.size());

        List<RouteAndChanges> firstSetOfChanges = results.get(0);
        assertEquals(1, firstSetOfChanges.size());

        RouteAndChanges actualChange = firstSetOfChanges.get(0);

        assertEquals(2, actualChange.getInterchangeStations().size());
        assertTrue(getStationsFor(actualChange).contains(Cornbrook.from(stationRepository)), results.toString());
    }

    @Test
    void shouldBacktrackToChangesOneChanges() {

        // expect one change, at victoria
        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, date);
        Route routeB = routeHelper.getOneRoute(RochdaleShawandCromptonManchesterEastDidisbury, date);

        List<List<RouteAndChanges>> results = routesCostRepository.getChangesFor(routeA, routeB, modes);
        assertEquals(1, results.size(), results.toString());

        // expected each list of changes to contain 2 elements in this case
        List<RouteAndChanges> oneChangeNeeded = results.stream().
                filter(routeAndChanges -> routeAndChanges.size()==1).
                map(routeAndChanges -> routeAndChanges.get(0)).
                collect(Collectors.toList());

        assertEquals(1, oneChangeNeeded.size(), results.toString());

        RouteAndChanges change = oneChangeNeeded.get(0);

        RoutePair changeBetween = change.getRoutePair();
        assertEquals(routeA, changeBetween.first());
        assertEquals(routeB, changeBetween.second());

        IdSet<Station> interchanges = change.getInterchangeStations().stream().map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        assertEquals(1, interchanges.size());
        assertTrue(interchanges.contains(Victoria.getId()));
    }

    @Test
    void shouldBacktrackToChangesTwoChanges() {
        int expectedNumberOfDifferentRoutings = 10;

        // no direct changes possible, expect all to feature 2 changes
        // also many different combinations are possible
        Route routeA = routeHelper.getOneRoute(BuryPiccadilly, date);
        Route routeB = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);

        List<List<RouteAndChanges>> results = routesCostRepository.getChangesFor(routeA, routeB, modes);
        assertEquals(expectedNumberOfDifferentRoutings, results.size(), results.toString());

        // The pairs of changes needed
        List<Pair<RouteAndChanges, RouteAndChanges>> twoChangesNeeded = results.stream().
                filter(routeAndChanges -> routeAndChanges.size()==2).
                map(routeAndChanges -> Pair.of(routeAndChanges.get(0), routeAndChanges.get(1))).
                collect(Collectors.toList());

        assertEquals(expectedNumberOfDifferentRoutings, twoChangesNeeded.size());

        twoChangesNeeded.forEach(pair -> {
            RouteAndChanges firstChanges = pair.getLeft();
            RouteAndChanges secondChanges = pair.getRight();

            assertEquals(routeA, firstChanges.getRoutePair().first());
            Route intermediate = firstChanges.getRoutePair().second();
            assertEquals(intermediate, secondChanges.getRoutePair().first());
            assertEquals(routeB, secondChanges.getRoutePair().second());
        });

        // check changes that involve piccadilly to altrincham route
        Route piccadillyToAlty = routeHelper.getOneRoute(PiccadillyAltrincham, date);
        List<Pair<RouteAndChanges, RouteAndChanges>> viaPiccToAltyRoute = twoChangesNeeded.stream().
                filter(pair -> pair.getLeft().getRoutePair().second().equals(piccadillyToAlty)).
                collect(Collectors.toList());

        assertEquals(1, viaPiccToAltyRoute.size(), viaPiccToAltyRoute.toString());

        Set<InterchangeStation> viaPiccToAltyInterchange = viaPiccToAltyRoute.get(0).getLeft().getInterchangeStations();

        assertEquals(2, viaPiccToAltyInterchange.size(), HasId.asIds(viaPiccToAltyInterchange));

        IdSet<Station> interchangesForPiccAltyRouting = viaPiccToAltyInterchange.stream().map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        assertTrue(interchangesForPiccAltyRouting.contains(PiccadillyGardens.getId()));
        assertTrue(interchangesForPiccAltyRouting.contains(Piccadilly.getId()));

        // check changes that involve bury to altrincham route
        Route buryToAlty = routeHelper.getOneRoute(BuryManchesterAltrincham, date);
        List<Pair<RouteAndChanges, RouteAndChanges>> viaBuryToAlty = twoChangesNeeded.stream().
                filter(pair -> pair.getLeft().getRoutePair().second().equals(buryToAlty)).
                collect(Collectors.toList());

        assertEquals(1, viaBuryToAlty.size(), viaBuryToAlty.toString());

        Set<InterchangeStation> viaBuryToAltyInterchange = viaBuryToAlty.get(0).getLeft().getInterchangeStations();

        assertEquals(2, viaBuryToAltyInterchange.size(), viaBuryToAlty.toString());

        IdSet<Station> interchangesForBuryAltyRouting = viaBuryToAltyInterchange.stream().map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        assertTrue(interchangesForBuryAltyRouting.contains(Victoria.getId()), interchangesForBuryAltyRouting.toString());
        assertTrue(interchangesForBuryAltyRouting.contains(MarketStreet.getId()), interchangesForBuryAltyRouting.toString());
    }

    private Set<Station> getStationsFor(RouteAndChanges routeAndChanges) {
        return routeAndChanges.getInterchangeStations().stream().map(InterchangeStation::getStation).collect(Collectors.toSet());
    }

    @Test
    void shouldComputeCostsDifferentRoutesOneChanges() {
        Route routeA = routeHelper.getOneRoute(AltrinchamManchesterBury, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        assertEquals(1, getMinCost(routesCostRepository.getNumberOfChanges(routeA, routeB, date, timeRange, modes)),
                "wrong for " + routeA.getId() + " " + routeB.getId());
        assertEquals(1, getMinCost(routesCostRepository.getNumberOfChanges(routeB, routeA, date, timeRange, modes)),
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

    @Test
    void shouldSortAsExpected() {

        Route routeA = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);
        Route routeB = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);
        Route routeC = routeHelper.getOneRoute(BuryPiccadilly, date);

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

        // disruption week of 28/11
        TramDate nextWeek = this.date.plusWeeks(1);
        NumberOfChanges changes = routesCostRepository.getNumberOfChanges(altrincham, navigationRoad, modes, nextWeek, timeRange);

        assertEquals(0, getMinCost(changes), "On " + nextWeek + " " + changes);
    }

    @Test
    void shouldHandleServicesJustAfterMidnight() {
        Station altrincham = Altrincham.from(stationRepository);

        long maxDuration = config.getMaxJourneyDuration();
        TimeRange timeRange = TimeRange.of(TramTime.of(0,1), Duration.ZERO, Duration.ofMinutes(maxDuration));

        Station navigationRoad = NavigationRoad.from(stationRepository);

        // disruption week of 28/11
        TramDate nextWeek = this.date.plusWeeks(1);
        NumberOfChanges changes = routesCostRepository.getNumberOfChanges(altrincham, navigationRoad, modes, nextWeek, timeRange);

        assertEquals(0, getMinCost(changes), "On " + nextWeek+ " " + changes);
    }

    private int getMinCost(NumberOfChanges routesCostRepository) {
        return routesCostRepository.getMin();
    }

}
