package com.tramchester.integration.repository.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.InterchangeStation;
import com.tramchester.domain.Route;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        Station piccadilly = stationRepository.getStationById(ManchesterPiccadilly.getId());

        List<Route> londonRoutes = piccadilly.getPickupRoutes().stream().
                filter(route -> route.getShortName().equals("VT:MNCRPIC=>EUSTON via STKP, MACLSFD, STOKEOT, MKNSCEN")).
                collect(Collectors.toList());

        assertEquals(1, londonRoutes.size());

        Route londonToManchester = londonRoutes.get(0);

        Set<InterchangeStation> interchanges = routeInterchanges.getFor(londonToManchester);

        assertEquals(5, interchanges.size(), interchanges.toString());

        IdSet<Station> stationIds = interchanges.stream().map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        assertTrue(stationIds.contains(Stockport.getId()));
        assertTrue(stationIds.contains(Macclesfield.getId()));
        assertTrue(stationIds.contains(StokeOnTrent.getId()));
        assertTrue(stationIds.contains(MiltonKeynesCentral.getId()));
        assertTrue(stationIds.contains(LondonEuston.getId()));

        RouteStation miltonKeynes = stationRepository.getRouteStationById(RouteStation.createId(MiltonKeynesCentral.getId(),
                londonToManchester.getId()));

        int costToNextInterchange = routeInterchanges.costToInterchange(miltonKeynes);
        assertEquals(0, costToNextInterchange);
    }

    @Test
    void shouldGetInterchangeForRouteWhereNotAllInterchanges() {
        Station piccadilly = stationRepository.getStationById(ManchesterPiccadilly.getId());

        List<Route> manchesterToChesterRoutes = piccadilly.getPickupRoutes().stream().
                filter(route -> route.getShortName().startsWith("NT:MNCRPIC=>CHST via STKP")).
                filter(route -> route.getShortName().contains(Delamere.getId().forDTO()) && route.getShortName().contains("HALE")).
                collect(Collectors.toList());

        assertEquals(1, manchesterToChesterRoutes.size(), manchesterToChesterRoutes.toString());

        Route manchesterToChester = manchesterToChesterRoutes.get(0);

        RouteStation stockport = stationRepository.getRouteStationById(RouteStation.createId(Stockport.getId(), manchesterToChester.getId()));
        assertEquals(0, routeInterchanges.costToInterchange(stockport));

        RouteStation delamere = stationRepository.getRouteStationById(RouteStation.createId(Delamere.getId(), manchesterToChester.getId()));
        assertEquals(17, routeInterchanges.costToInterchange(delamere));

    }
}
