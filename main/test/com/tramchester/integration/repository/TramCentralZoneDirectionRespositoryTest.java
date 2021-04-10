package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.CentralZoneStation;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RouteCallingStations;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TramCentralZoneDirectionRespository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.repository.TramCentralZoneDirectionRespository.Place.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TramCentralZoneDirectionRespositoryTest {
    private static ComponentContainer componentContainer;
    private TramCentralZoneDirectionRespository repository;
    private StationRepository stationRepository;
    private RouteCallingStations routeCallingStations;
    private RouteRepository routeRepository;

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

    @BeforeEach
    void beforeAnyTestsRun() {
        repository = componentContainer.get(TramCentralZoneDirectionRespository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        routeCallingStations = componentContainer.get(RouteCallingStations.class);
        routeRepository = componentContainer.get(RouteRepository.class);
    }

    @Test
    void shouldHaveAwayEcclesRoute() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(SalfordQuay, AshtonUnderLyneManchesterEccles));
        assertEquals(away, result);
    }

    @Test
    void shouldHaveTowardsEcclesRoute() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(SalfordQuay, EcclesManchesterAshtonUnderLyne));
        assertEquals(towards, result);
    }

    @Test
    void shouldHaveWithinEcclesRoute() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(Pomona, EcclesManchesterAshtonUnderLyne));
        assertEquals(within, result);
    }

    @Test
    void shouldHaveAwayAirport() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(ManAirport, VictoriaWythenshaweManchesterAirport));
        assertEquals(away, result);
    }

    @Test
    void shouldHaveTowardsAirport() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(ManAirport, ManchesterAirportWythenshaweVictoria));
        assertEquals(towards, result);
    }

    @Test
    void shouldHaveWithinAirportRoute() {
        TramCentralZoneDirectionRespository.Place result = repository.getStationPlacement(getRouteStation(StWerburghsRoad, VictoriaWythenshaweManchesterAirport));
        assertEquals(within, result);
    }

    @Test
    void shouldHaveChorltonWithin() {
        assertEquals(within, repository.getStationPlacement(getRouteStation(Chorlton, RochdaleShawandCromptonManchesterEastDidisbury)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(Chorlton, ManchesterAirportWythenshaweVictoria)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(Chorlton, EastDidisburyManchesterShawandCromptonRochdale)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(Chorlton, VictoriaWythenshaweManchesterAirport)));
    }

    @Test
    void shouldHaveTaffordBarWithinForExpectedRoutes() {
        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, AltrinchamPiccadilly)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, EastDidisburyManchesterShawandCromptonRochdale)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, ManchesterAirportWythenshaweVictoria)));

        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, PiccadillyAltrincham)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, RochdaleShawandCromptonManchesterEastDidisbury)));
        assertEquals(within, repository.getStationPlacement(getRouteStation(TraffordBar, VictoriaWythenshaweManchesterAirport)));
    }

    @Test
    void shouldHaveCorrectlyWithin() {
        Set<Station> found = new HashSet<>();

        routeRepository.getRoutes().forEach(route -> {
            List<Station> centralStationsForRoute = routeCallingStations.getStationsFor(route).stream().
                    filter(CentralZoneStation::contains).
                    collect(Collectors.toList());

            assertFalse(centralStationsForRoute.isEmpty());

            centralStationsForRoute.forEach(station -> {
                RouteStation routeStation = new RouteStation(station, route);
                assertEquals(within, repository.getStationPlacement(routeStation));
                found.add(station);
            });
        });

        assertEquals(CentralZoneStation.values().length, found.size());
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

    private RouteStation getRouteStation(TramStations station, KnownTramRoute knownTramRoute) {
        Route route = TestEnv.findTramRoute(routeRepository, knownTramRoute);
        IdFor<RouteStation> routeStationId = RouteStation.createId(station.getId(), route.getId());
        return stationRepository.getRouteStationById(routeStationId);
    }

}
