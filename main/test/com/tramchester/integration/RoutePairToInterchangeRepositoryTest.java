package com.tramchester.integration;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.RoutePair;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.routes.RoutePairToInterchangeRepository;
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
import java.util.Set;

import static com.tramchester.domain.reference.CentralZoneStation.*;
import static com.tramchester.testSupport.TestEnv.Modes.TramsOnly;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
public class RoutePairToInterchangeRepositoryTest {

    private static ComponentContainer componentContainer;

    private TramRouteHelper routeHelper;
    private final EnumSet<TransportMode> modes = TramsOnly;
    private TramDate date;
    private RoutePairToInterchangeRepository repository;
    //private RouteIndex routeIndex;

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
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
        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        routeHelper = new TramRouteHelper(routeRepository);
        //routeIndex = componentContainer.get(RouteIndex.class);

        date = TestEnv.testDay().plusWeeks(1);

        repository = componentContainer.get(RoutePairToInterchangeRepository.class);
    }

    @Test
    void shouldGetExpectedSingleInterchangesBetweenRoutes() {
        Route cornbrookToTraffordCenter = routeHelper.getOneRoute(CornbrookTheTraffordCentre, date);
        Route victoriaToAirport = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        RoutePair routeIndexPair = RoutePair.of(cornbrookToTraffordCenter, victoriaToAirport);

        assertTrue(repository.hasAnyInterchangesFor(routeIndexPair));

        Set<InterchangeStation> interchanges = repository.getInterchanges(routeIndexPair, modes);

        IdSet<Station> stationIds = interchanges.stream().map(InterchangeStation::getStation).collect(IdSet.collector());

        assertEquals(1, stationIds.size());
        assertTrue(stationIds.contains(Cornbrook.getId()), stationIds.toString());
    }

    @Test
    void shouldGetExpectedMultipleInterchangesBetweenRoutes() {
        Route ashtonToEccles = routeHelper.getOneRoute(AshtonUnderLyneManchesterEccles, date);
        Route victoriaToAirport = routeHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);

        RoutePair routeIndexPair = RoutePair.of(ashtonToEccles, victoriaToAirport);

        assertTrue(repository.hasAnyInterchangesFor(routeIndexPair));

        Set<InterchangeStation> interchanges = repository.getInterchanges(routeIndexPair, modes);

        IdSet<Station> stationIds = interchanges.stream().map(InterchangeStation::getStation).collect(IdSet.collector());

        assertEquals(4+1, stationIds.size(), stationIds.toString());
        assertTrue(stationIds.contains(StPetersSquare.getId()), stationIds.toString());
        assertTrue(stationIds.contains(Deansgate.getId()), stationIds.toString());
        assertTrue(stationIds.contains(Cornbrook.getId()), stationIds.toString());
        assertTrue(stationIds.contains(TraffordBar.getId()), stationIds.toString());
        // some ashton to eccles trams seem to call at market street, maybe to/from depot??
        assertTrue(stationIds.contains(MarketStreet.getId()), stationIds.toString());

    }


}
