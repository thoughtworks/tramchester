package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.integration.testSupport.RouteCalculationCombinations;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;

class RouteCalculatorTestAllJourneys {

    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig testConfig;

    private final LocalDate when = TestEnv.testDay();
    private RouteCalculationCombinations combinations;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        combinations = new RouteCalculationCombinations(componentContainer);
    }

    @Test
    void shouldFindRouteEachStationToEveryOtherStream() {
        TransportData data = componentContainer.get(TransportData.class);

        final TramTime time = TramTime.of(8, 5);
        Set<Station> haveServices = data.getStationsForMode(Tram).stream().
                //filter(station -> hasServices(station, time)).
                collect(Collectors.toSet());


        JourneyRequest journeyRequest = new JourneyRequest(when, time, false, 2,
                testConfig.getMaxJourneyDuration(), 1);

        // pairs of stations to check
        Set<StationIdPair> stationIdPairs = haveServices.stream().flatMap(start -> haveServices.stream().
                filter(dest -> !combinations.betweenInterchanges(start, dest)).
                map(dest -> StationIdPair.of(start, dest))).
                filter(pair -> !pair.same()).
                filter(pair -> !combinations.betweenEndsOfRoute(pair)).
                collect(Collectors.toSet());

        combinations.validateAllHaveAtLeastOneJourney(stationIdPairs, journeyRequest);

    }

    private boolean hasServices(Station station, TramTime time) {
        Set<Route> routes = station.getRoutes();
        boolean anyServices = routes.stream().
                flatMap(route -> route.getServices().stream()).
                anyMatch(service -> service.getCalendar().operatesOn(when));
        if (!anyServices) {
            return false;
        }
        return routes.stream().flatMap(route -> route.getTrips().stream()).
                flatMap(trip -> trip.getStopCalls().stream()).
                filter(stopCall -> stopCall.getStationId().equals(station.getId())).
                anyMatch(stopCall -> time.between(stopCall.getArrivalTime(), stopCall.getDepartureTime()));
    }


}
