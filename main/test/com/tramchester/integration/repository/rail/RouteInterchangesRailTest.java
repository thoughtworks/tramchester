package com.tramchester.integration.repository.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.InterchangeStation;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.RouteInterchanges;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class RouteInterchangesRailTest {
    private static ComponentContainer componentContainer;
    private RouteInterchanges routeInterchanges;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationRailTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        routeInterchanges = componentContainer.get(RouteInterchanges.class);
    }

    @Test
    void shouldGetInterchangesForARouteAllInterchanges() {

        Station piccadilly = ManchesterPiccadilly.getFrom(stationRepository);
        Station euston = LondonEuston.getFrom(stationRepository);

        String routeShortName = format("%s service from %s to %s", "VT", piccadilly.getName(), euston.getName());

        String longName = "VT service from Manchester Piccadilly Rail Station to London Euston Rail Station via Stockport " +
                "Rail Station, Macclesfield Rail Station, Stoke-on-Trent Rail Station, Milton Keynes Central Rail Station";

//        List<Station> callingPoints = Arrays.asList(piccadilly,
//                Stockport.getFrom(stationRepository),
//                Macclesfield.getFrom(stationRepository),
//                StokeOnTrent.getFrom(stationRepository),
//                MiltonKeynesCentral.getFrom(stationRepository),
//                euston);

        final List<Route> towardsLondon = piccadilly.getPickupRoutes().stream().
                filter(route -> route.getShortName().equals(routeShortName)).collect(Collectors.toList());
        List<Route> routes = towardsLondon.stream().
                //filter(route -> route.getTrips().stream().allMatch(trip -> trip.getStopCalls().getStationSequence().equals(callingPoints))).
                        filter(route -> longName.equals(route.getName())).
                collect(Collectors.toList());

        assertEquals(1, routes.size(), towardsLondon.toString());

        Route londonToManchester = routes.get(0);

        Set<InterchangeStation> interchanges = routeInterchanges.getFor(londonToManchester);

        assertEquals(5, interchanges.size(), interchanges.toString());

        IdSet<Station> stationIds = interchanges.stream().map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        assertTrue(stationIds.contains(Stockport.getId()));
        assertTrue(stationIds.contains(Macclesfield.getId()));
        assertTrue(stationIds.contains(StokeOnTrent.getId()));
        assertTrue(stationIds.contains(MiltonKeynesCentral.getId()));
        assertTrue(stationIds.contains(LondonEuston.getId()));

        RouteStation miltonKeynesRouteStation = stationRepository.getRouteStationById(RouteStation.createId(MiltonKeynesCentral.getId(),
                londonToManchester.getId()));

        int costToNextInterchange = routeInterchanges.costToInterchange(miltonKeynesRouteStation);
        assertEquals(0, costToNextInterchange);
    }

    @Test
    void shouldGetInterchangeForRouteWhereNotAllInterchanges() {
        Station piccadilly = stationRepository.getStationById(ManchesterPiccadilly.getId());

        Station chester = Chester.getFrom(stationRepository);
        Station hale = Hale.getFrom(stationRepository);
        Station delamere = Delamere.getFrom(stationRepository);

        String routeShortName = format("%s service from %s to %s", "NT", piccadilly.getName(), chester.getName());

        List<Route> manchesterToChesterRoutes = piccadilly.getPickupRoutes().stream().
                filter(route -> route.getShortName().equals(routeShortName)).
                filter(route -> route.getName().contains(delamere.getName()) && route.getName().contains(hale.getName())).
                collect(Collectors.toList());

        assertFalse(manchesterToChesterRoutes.isEmpty());

        IdFor<Route> manchesterToChester = manchesterToChesterRoutes.get(0).getId();

        RouteStation stockportRouteStation = stationRepository.getRouteStationById(RouteStation.createId(Stockport.getId(),
                manchesterToChester));
        assertEquals(0, routeInterchanges.costToInterchange(stockportRouteStation));

        RouteStation delamereRouteStation = stationRepository.getRouteStationById(RouteStation.createId(delamere.getId(),
                manchesterToChester));
        assertEquals(19, routeInterchanges.costToInterchange(delamereRouteStation));

    }
}
