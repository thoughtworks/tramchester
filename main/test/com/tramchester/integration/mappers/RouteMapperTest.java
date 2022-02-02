package com.tramchester.integration.mappers;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.mappers.RoutesMapper;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.reference.KnownTramRoute.ManchesterAirportWythenshaweVictoria;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteMapperTest {
    private static ComponentContainer componentContainer;
    private TramRouteHelper tramRouteHelper;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        tramRouteHelper = new TramRouteHelper();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @Test
    void shouldGetRouteStationsInCorrectOrder() {
        RouteRepository routeRepsoitory = componentContainer.get(RouteRepository.class);
        RoutesMapper mapper = componentContainer.get(RoutesMapper.class);

        List<RouteDTO> dtos = mapper.getAllRoutes();
        Set<Route> routes = tramRouteHelper.get(ManchesterAirportWythenshaweVictoria, routeRepsoitory);

        routes.forEach(route -> {
            RouteDTO query = new RouteDTO(route, new LinkedList<>());

            int index = dtos.indexOf(query);

            List<LocationRefWithPosition> stations = dtos.get(index).getStations();
            LocationRefWithPosition stationRefWithPosition = stations.get(0);
            assertEquals(TramStations.ManAirport.getRawId(), stationRefWithPosition.getId());
            TestEnv.assertLatLongEquals(TramStations.ManAirport.getLatLong(), stationRefWithPosition.getLatLong(),
                    0.00001, "position");
            assertTrue(stationRefWithPosition.getTransportModes().contains(TransportMode.Tram));
            assertEquals(TramStations.Victoria.getRawId(), stations.get(stations.size()-1).getId());
        });


    }
}
