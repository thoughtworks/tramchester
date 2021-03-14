package com.tramchester.integration.mappers;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.mappers.RoutesMapper;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.RoutesForTesting;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static com.tramchester.domain.reference.KnownTramRoute.ManchesterAirportWythenshaweVictoria;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteMapperTest {
    private static ComponentContainer componentContainer;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder<>().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @Test
    void shouldGetRouteStationsInCorrectOrder() {
        RoutesMapper mapper = componentContainer.get(RoutesMapper.class);

        List<RouteDTO> dtos = mapper.getAllRoutes();
        Route route = RoutesForTesting.createTramRoute(ManchesterAirportWythenshaweVictoria);
        RouteDTO query = new RouteDTO(route, new LinkedList<>());

        int index = dtos.indexOf(query);

        List<StationRefWithPosition> stations = dtos.get(index).getStations();
        StationRefWithPosition stationRefWithPosition = stations.get(0);
        assertEquals(TramStations.ManAirport.forDTO(), stationRefWithPosition.getId());
        TestEnv.assertLatLongEquals(TramStations.ManAirport.getLatLong(), stationRefWithPosition.getLatLong(), 0.00001, "position");
        assertTrue(stationRefWithPosition.getTransportModes().contains(TransportMode.Tram));
        assertEquals(TramStations.Victoria.forDTO(), stations.get(stations.size()-1).getId());
    }
}
