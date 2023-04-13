package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.LowestCostsForDestRoutes;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
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

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
public class RouteToRouteCostsTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private RouteToRouteCosts routesCostRepository;
    private TramRouteHelper routeHelper;
    private RouteRepository routeRepository;
    private StationRepository stationRepository;
    private final EnumSet<TransportMode> modes = TramsOnly;
    private TramDate date;
    private TimeRange timeRange;

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {
        config = tramchesterConfig;

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
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

        NumberOfChanges result = routesCostRepository.getNumberOfChanges(start, end, EnumSet.of(Train), date, timeRange);

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
