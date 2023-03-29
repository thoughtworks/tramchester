package com.tramchester.integration.repository.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TrainTest
public class RailAndTramRouteRepositoryTest {
    public static final int ALL_GM_ROUTES = 682;
    private static ComponentContainer componentContainer;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        RailAndTramGreaterManchesterConfig config = new RailAndTramGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        routeRepository = componentContainer.get(RouteRepository.class);
    }

    @Test
    void shouldHaveExpectedNumberOfRailRoutes() {
        Set<Route> allRoutes = routeRepository.getRoutes();

        assertEquals(ALL_GM_ROUTES, allRoutes.size());
    }

    @Test
    void shouldHaveExpectedNumberOfTramRoutes() {
        int numberTramRoutes = 42;

        Set<Route> tramRoutes = routeRepository.getRoutes(TransportMode.TramsOnly);
        assertEquals(numberTramRoutes, tramRoutes.size());

        Set<Route> railRoutes = routeRepository.getRoutes(EnumSet.of(Train));
        assertEquals(ALL_GM_ROUTES-numberTramRoutes, railRoutes.size());
    }

    @Test
    void shouldHaveExpectedNumberManchesterToStockport() {

        List<Route> result = routeRepository.getRoutes().stream().
                filter(route -> beginsAtAndCallsAt(route, ManchesterPiccadilly.getId(), Stockport.getId())).
                collect(Collectors.toList());

        assertEquals(61, result.size(), HasId.asIds(result));
    }

    @Test
    void shouldHaveRoutesPassingThroughGMWithCorrectStopCallsAndRouteId() {

        IdFor<Agency> agencyId = TrainOperatingCompanies.VT.getAgencyId();

        Set<Route> matchingRoutes = routeRepository.getRoutes().stream().
                filter(route -> route.getAgency().getId().equals(agencyId)).
                filter(route -> callsAtEndsAt(route, Stockport.getId(), ManchesterPiccadilly.getId())).
                collect(Collectors.toSet());

        assertEquals(10, matchingRoutes.size(), HasId.asIds(matchingRoutes));

        Set<Route> routesFromEustonViaStockport = matchingRoutes.stream().
                filter(route -> railRouteStartsAt(route, LondonEuston.getId())).
                filter(route -> railRouteEndsAt(route, ManchesterPiccadilly.getId())).
                collect(Collectors.toSet());

        assertEquals(7, routesFromEustonViaStockport.size());

        Set<Integer> indexes = routesFromEustonViaStockport.stream().
                map(route -> (RailRouteId) route.getId()).
                map(RailRouteId::getIndex).
                collect(Collectors.toSet());

        assertEquals(7, indexes.size());
        assertTrue(indexes.contains(4), indexes + " routes: " + HasId.asIds(routesFromEustonViaStockport));

    }

    @Test
    void shouldReproIssueWithMissingRouteId() {
        RailRouteId routeId = new RailRouteId(LondonEuston.getId(), ManchesterPiccadilly.getId(), TrainOperatingCompanies.VT.getAgencyId(), 4);

        assertTrue(routeRepository.hasRouteId(routeId));
    }

//    @Test
//    void shouldReproIssueWithRailRouteIdSerialized() throws JsonProcessingException {
//
//        RailRouteId sourceId = new RailRouteId(LondonEuston.getId(), ManchesterPiccadilly.getId(), TrainOperatingCompanies.VT.getAgencyId(), 4);
//        assertTrue(routeRepository.hasRouteId(sourceId));
//
//        ObjectMapper mapper = new ObjectMapper();
//
//        RouteIndexData routeIndexData = new RouteIndexData(56, sourceId);
//
//        for (int i = 0; i < 100000; i++) {
//
//            String asString = mapper.writeValueAsString(routeIndexData);
//
//            RouteIndexData result = mapper.readValue(asString, RouteIndexData.class);
//
//            IdFor<Route> finalId = result.getRouteId();
//            assertTrue(routeRepository.hasRouteId(finalId));
//        }
//
//    }

    private boolean railRouteStartsAt(Route route, IdFor<Station> stationId) {
        final RailRouteId railRouteId = (RailRouteId) route.getId();
        return railRouteId.getBegin().equals(stationId);
    }

    private boolean railRouteEndsAt(Route route, IdFor<Station> stationId) {
        final RailRouteId railRouteId = (RailRouteId) route.getId();
        return railRouteId.getEnd().equals(stationId);
    }

    private boolean beginsAtAndCallsAt(Route route, IdFor<Station> first, IdFor<Station> callsAt) {
        return route.getTrips().stream().
                map(Trip::getStopCalls).
                filter(stopCalls -> stopCalls.getFirstStop().getStationId().equals(first)).
                anyMatch(stopCalls -> stopCalls.callsAt(callsAt));
    }

    private boolean callsAtEndsAt(Route route, IdFor<Station> callsAt, IdFor<Station> endsAt) {
        return route.getTrips().stream().
                map(Trip::getStopCalls).
                filter(stopCalls -> stopCalls.getLastStop().getStationId().equals(endsAt)).
                anyMatch(stopCalls -> stopCalls.callsAt(callsAt));
    }
}
