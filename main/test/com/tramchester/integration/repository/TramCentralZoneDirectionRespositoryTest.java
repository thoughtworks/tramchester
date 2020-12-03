package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.CentralZoneStations;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TramCentralZoneDirectionRespository;
import com.tramchester.testSupport.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.repository.TramCentralZoneDirectionRespository.Place.*;
import static com.tramchester.testSupport.RoutesForTesting.*;
import static com.tramchester.testSupport.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TramCentralZoneDirectionRespositoryTest {
    private static Dependencies dependencies;
    private TramCentralZoneDirectionRespository repository;
    private StationRepository stationRepository;
    private RouteCallingStations routeCallingStations;
    private RouteRepository routeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @AfterAll
    static void onceAfterAllTestsHaveRun() {
        dependencies.close();
    }

    @BeforeEach
    void beforeAnyTestsRun() {
        repository = dependencies.get(TramCentralZoneDirectionRespository.class);
        stationRepository = dependencies.get(StationRepository.class);
        routeCallingStations = dependencies.get(RouteCallingStations.class);
        routeRepository = dependencies.get(RouteRepository.class);
    }

    @Test
    void shouldHaveAwayEcclesRoute() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(SalfordQuay, ASH_TO_ECCLES));
        assertEquals(away, result);
    }

    @Test
    void shouldHaveTowardsEcclesRoute() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(SalfordQuay, ECCLES_TO_ASH));
        assertEquals(towards, result);
    }

    @Test
    void shouldHaveWithinEcclesRoute() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(Pomona, ECCLES_TO_ASH));
        assertEquals(within, result);
    }

    @Test
    void shouldHaveAwayAirport() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(ManAirport, VIC_TO_AIR));
        assertEquals(away, result);
    }

    @Test
    void shouldHaveTowardsAirport() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(ManAirport, AIR_TO_VIC));
        assertEquals(towards, result);
    }

    @Test
    void shouldHaveWithinAirportRoute() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(StWerburghsRoad, VIC_TO_AIR));
        assertEquals(within, result);
    }

    @Test
    void shouldHaveChorltonWithin() {
        assertEquals(within, repository.getStationPlacement(getRouteStation(Chorlton, ROCH_TO_DIDS)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(Chorlton, AIR_TO_VIC)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(Chorlton, DIDS_TO_ROCH)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(Chorlton, VIC_TO_AIR)));
    }

    @Test
    void shouldHaveTaffordBarWithinForExpectedRoutes() {
        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, ALTY_TO_PICC)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, DIDS_TO_ROCH)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, AIR_TO_VIC)));

        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, PICC_TO_ALTY)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, ROCH_TO_DIDS)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, VIC_TO_AIR)));
    }

    @Test
    void shouldHaveCorrectlyWithin() {
        Set<Station> found = new HashSet<>();

        routeRepository.getRoutes().forEach(route -> {
            List<Station> centralStationsForRoute = routeCallingStations.getStationsFor(route).stream().
                    filter(CentralZoneStations::contains).
                    collect(Collectors.toList());

            assertFalse(centralStationsForRoute.isEmpty());

            centralStationsForRoute.forEach(station -> {
                RouteStation routeStation = new RouteStation(station, route);
                assertEquals(within, repository.getStationPlacement(routeStation));
                found.add(station);
            });
        });

        assertEquals(CentralZoneStations.values().length, found.size());
    }

    @Test
    void shouldHaveInboundAndOutboundBalance() {
        Set<RouteStation> awayFromCentral = new HashSet<>();
        Set<RouteStation> towardsCentral = new HashSet<>();

        routeRepository.getRoutes().forEach(route -> {
            Set<RouteStation> routeStations = routeCallingStations.getStationsFor(route).stream().
                    map(station -> new RouteStation(station, route)).
                    collect(Collectors.toSet());

            for (RouteStation routeStation :routeStations) {
                TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(routeStation);
                if (result.equals(away)) {
                    awayFromCentral.add(routeStation);
                } else if (result.equals(towards)) {
                    towardsCentral.add(routeStation);
                }
            }
        });

        assertFalse(awayFromCentral.isEmpty());
        assertFalse(towardsCentral.isEmpty());

        assertEquals(awayFromCentral.size(), towardsCentral.size());

    }

    private RouteStation getRouteStation(TramStations station, Route route) {
        IdFor<RouteStation> routeStationId = RouteStation.formId(of(station), route);
        return stationRepository.getRouteStationById(routeStationId);
    }

}
