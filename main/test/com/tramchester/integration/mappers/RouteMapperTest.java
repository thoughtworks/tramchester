package com.tramchester.integration.mappers;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.mappers.RoutesMapper;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteMapperTest {
    private static ComponentContainer componentContainer;
    private TramRouteHelper tramRouteHelper;
    private RouteRepository routeRepsoitory;
    private LocalDate date;
    private RoutesMapper mapper;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        tramRouteHelper = new TramRouteHelper();
        routeRepsoitory = componentContainer.get(RouteRepository.class);
        date = TestEnv.testDay();
        mapper = componentContainer.get(RoutesMapper.class);
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        componentContainer.close();
    }

    @Test
    void shouldGetRouteStationsInCorrectOrder() {

        List<RouteDTO> dtos = mapper.getRouteDTOs(TestEnv.testDay());


        Route fromAirportRoute = tramRouteHelper.getOneRoute(AltrinchamPiccadilly, routeRepsoitory, date);

        RouteDTO query = new RouteDTO(fromAirportRoute, new LinkedList<>());

        int index = dtos.indexOf(query);

        List<LocationRefWithPosition> stations = dtos.get(index).getStations();
        LocationRefWithPosition stationRefWithPosition = stations.get(0);
        assertEquals(Altrincham.getRawId(), stationRefWithPosition.getId(), "for route " + fromAirportRoute);
        TestEnv.assertLatLongEquals(Altrincham.getLatLong(), stationRefWithPosition.getLatLong(),
                0.00001, "position");
        assertTrue(stationRefWithPosition.getTransportModes().contains(TransportMode.Tram));

        assertEquals(Piccadilly.getRawId(), stations.get(stations.size()-1).getId());

    }

    @Test
    void shouldHaveWorkaroundForAirportRouteIdsTransposedInData() {
        Route fromAirportRoute = tramRouteHelper.getOneRoute(ManchesterAirportWythenshaweVictoria, routeRepsoitory, date);

        List<Station> results = mapper.getStationsOn(fromAirportRoute, false);

        assertEquals(ManAirport.getId(), results.get(0).getId());
        assertEquals(Victoria.getId(), results.get(results.size()-1).getId());

    }

    @Test
    void shouldHaveWorkaroundForTraffordCentreRouteIdsTransposedInData() {
        Route fromTraffordCenter = tramRouteHelper.getOneRoute(TheTraffordCentreCornbrook, routeRepsoitory, date);

        List<Station> results = mapper.getStationsOn(fromTraffordCenter, false);

        assertEquals(TraffordCentre.getId(), results.get(0).getId());
        assertEquals(Cornbrook.getId(), results.get(results.size()-1).getId());

    }
}
