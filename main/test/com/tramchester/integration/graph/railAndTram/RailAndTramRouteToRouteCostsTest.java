package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.NumberOfChanges;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.RailRouteHelper;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.*;

import java.util.EnumSet;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static com.tramchester.testSupport.reference.KnownTramRoute.BuryPiccadilly;
import static com.tramchester.testSupport.reference.KnownTramRoute.EastDidisburyManchesterShawandCromptonRochdale;
import static com.tramchester.testSupport.reference.TramStations.Altrincham;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@GMTest
public class RailAndTramRouteToRouteCostsTest {
    private StationRepository stationRepository;
    private static ComponentContainer componentContainer;

    private TramDate date;
    private RouteToRouteCosts routeToRouteCosts;
    private EnumSet<TransportMode> allTransportModes;
    private RailRouteHelper railRouteHelper;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterEach
    void afterAllEachTestsHasRun() {

    }

    @AfterAll
    static void afterAllTestsRun() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        date = TestEnv.testDay();
        allTransportModes = EnumSet.allOf(TransportMode.class);
        routeToRouteCosts = componentContainer.get(RouteToRouteCosts.class);
        stationRepository = componentContainer.get(StationRepository.class);
        railRouteHelper = new RailRouteHelper(componentContainer);
    }

    @Test
    void shouldValidHopsBetweenTramAndRailLongRange() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        EnumSet<TransportMode> all = allTransportModes;
        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(Bury), rail(Stockport),
                all, date, timeRange);

        assertEquals(1, result.getMin());
        //assertEquals(2, result.getMax());
    }

    @Test
    void shouldValidHopsBetweenTramAndRailNeighbours() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(Altrincham), rail(RailStationIds.Altrincham),
                allTransportModes, date, timeRange);

        assertEquals(1, result.getMin());
        assertEquals(1, result.getMax());
    }

    @Test
    void shouldValidHopsBetweenTramAndRailNeighbourThenTrain() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(Altrincham), rail(Stockport),
                allTransportModes, date, timeRange);

        assertEquals(1, result.getMin());
        assertEquals(2, result.getMax());
    }

    @Disabled("Is this realistic? Trains only but start at a tram station")
    @Test
    void shouldValidHopsBetweenTramAndRailNeighbourThenTrainWhenOnlyTrainModeEnabled() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(Altrincham), rail(Stockport),
                EnumSet.of(Train), date, timeRange);

        assertEquals(1, result.getMin());
        assertEquals(2, result.getMax());
    }

    @Test
    void shouldValidHopsBetweenTramAndRailShortRange() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(Cornbrook), rail(RailStationIds.Altrincham),
                allTransportModes, date, timeRange);

        assertEquals(1, result.getMin());
        assertEquals(2, result.getMax());
    }

    @Test
    void shouldNotHaveHopsBetweenTramAndRailWhenTramOnly() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        EnumSet<TransportMode> preferredModes = EnumSet.of(Tram);

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(TramStations.Bury), rail(Stockport),
                preferredModes, date, timeRange);

        assertEquals(Integer.MAX_VALUE, result.getMin());
        assertEquals(Integer.MAX_VALUE, result.getMax());
    }

    @Test
    void shouldHaveCorrectHopsBetweenRailStationsOnly() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        EnumSet<TransportMode> preferredModes = EnumSet.of(Train);
        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(rail(ManchesterPiccadilly), rail(Stockport),
                preferredModes, date, timeRange);

        assertEquals(0, result.getMin()); // non stop
        assertEquals(3, result.getMax()); // round the houses....
    }

    @Test
    void shouldHaveCorrectHopsBetweenTramStationsOnly() {
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 15), TramTime.of(22, 35));

        NumberOfChanges result = routeToRouteCosts.getNumberOfChanges(tram(Cornbrook), tram(StPetersSquare),
                allTransportModes, date, timeRange);

        assertEquals(0, result.getMin());
        assertEquals(1, result.getMax());
    }

    @Test
    void shouldHaveOneChangeRochdaleToEccles() {
        // Rochdale, Eccles
        TimeRange timeRange = TimeRange.of(TramTime.of(9,0), TramTime.of(10,0));
        Station rochdale = TramStations.Rochdale.from(stationRepository);
        Station eccles = TramStations.Eccles.from(stationRepository);
        NumberOfChanges changes = routeToRouteCosts.getNumberOfChanges(rochdale, eccles, TramsOnly, date, timeRange);

        assertFalse(changes.isNone());
        assertEquals(1, changes.getMin());
    }

    @Test
    void shouldRHaveChangesBetweenLiverpoolAndCreweRoutes() {
        // repro issue in routecostmatric
        TimeRange timeRange = TimeRange.of(TramTime.of(9,0), TramTime.of(10,0));

        Route routeA = railRouteHelper.getRoute(TrainOperatingCompanies.NT, RailStationIds.ManchesterVictoria, LiverpoolLimeStreet, 1);
        Route routeB = railRouteHelper.getRoute(TrainOperatingCompanies.NT, Crewe, ManchesterPiccadilly, 2);

        NumberOfChanges changes = routeToRouteCosts.getNumberOfChanges(routeA, routeB, date, timeRange, allTransportModes);

        assertFalse(changes.isNone());
        assertEquals(2, changes.getMin());

    }

    @Test
    void shouldReproduceErrorWithPinkAndYellowRoutesTramOnly() {
        // error was due to handling of linked interchange stations in StationAvailabilityRepository
        // specifically because Victoria is linked to rail routes and is only place to change between pink/yellow route
        TimeRange timeRange = TimeRange.of(TramTime.of(8, 45), TramTime.of(16, 45));

        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        TramRouteHelper tramRouteHelper = new TramRouteHelper(routeRepository);

        Route yellowInbound = tramRouteHelper.getOneRoute(BuryPiccadilly, date);
        Route pinkOutbound = tramRouteHelper.getOneRoute(EastDidisburyManchesterShawandCromptonRochdale, date);

        NumberOfChanges routeToRouteResult = routeToRouteCosts.getNumberOfChanges(yellowInbound, pinkOutbound, date, timeRange, TramsOnly);

        assertFalse(routeToRouteResult.isNone());

    }

    private Station tram(TramStations tramStation) {
        return tramStation.from(stationRepository);
    }

    private Station rail(RailStationIds railStation) {
        return railStation.from(stationRepository);
    }
}
