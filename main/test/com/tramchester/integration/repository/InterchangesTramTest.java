package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.graph.neighbours.NeighboursAsInterchangesTest;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.util.Set;

import static com.tramchester.domain.reference.CentralZoneStation.Shudehill;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.repository.common.InterchangeRepositoryTestSupport.RoutesWithInterchanges;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class InterchangesTramTest {
    private static ComponentContainer componentContainer;
    private InterchangeRepository interchangeRepository;
    private StationRepository stationRepository;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new IntegrationTramTestConfig(), TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        stationRepository = componentContainer.get(StationRepository.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
    }

    @Test
    void shouldHaveOfficialTramInterchanges() {
        for (IdFor<Station> interchangeId : AdditionalTramInterchanges.stations()) {
            Station interchange = stationRepository.getStationById(interchangeId);
            Assertions.assertTrue(interchangeRepository.isInterchange(interchange), interchange.toString());
        }
    }

    @Test
    void shouldHaveReachableInterchangeForEveryRoute() {
        Set<Route> routesWithInterchanges = RoutesWithInterchanges(interchangeRepository, stationRepository, Tram);
        Set<Route> all = routeRepository.getRoutes();

        assertEquals(all, routesWithInterchanges);
    }


    /***
     * Here to validate shude neighbours testing and interchanges
     * @see NeighboursAsInterchangesTest#shudehillBecomesInterchangeWhenNeighboursCreated()
     */
    @Test
    public void shudehillNotAnInterchange() {
        Station shudehill = stationRepository.getStationById(Shudehill.getId());
        assertFalse(interchangeRepository.isInterchange(shudehill));
    }

}
