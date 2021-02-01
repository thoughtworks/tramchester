package com.tramchester.integration.mappers;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.liveUpdates.LineAndDirection;
import com.tramchester.domain.liveUpdates.LineDirection;
import com.tramchester.domain.liveUpdates.Lines;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.CentralZoneStation;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.mappers.RouteToLineMapper;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.TestEnv.formId;
import static com.tramchester.testSupport.reference.TramStations.PeelHall;
import static com.tramchester.testSupport.reference.TramStations.SalfordQuay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RouteToLineMapperTest {
    private static ComponentContainer componentContainer;
    private StationRepository repository;
    private RouteToLineMapper mapper;

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
        repository = componentContainer.get(TransportData.class);
        mapper = componentContainer.get(RouteToLineMapper.class);
    }

    @Test
    void shouldMapEcclesLineStation() {
        StringIdFor<RouteStation> routeStationId = formId(SalfordQuay, AshtonunderLyneManchesterEccles);
        RouteStation routeStation = repository.getRouteStationById(routeStationId);
        LineAndDirection result = mapper.map(routeStation);

        assertEquals(Lines.Eccles, result.getLine());
        assertEquals(LineDirection.Outgoing, result.getDirection());
    }

    @Test
    void shouldMapAirportLineStation() {
        StringIdFor<RouteStation> routeStationIdA = formId(PeelHall, VictoriaManchesterAirport);
        RouteStation routeStationA = repository.getRouteStationById(routeStationIdA);
        LineAndDirection resultA = mapper.map(routeStationA);

        assertEquals(Lines.Airport, resultA.getLine());
        assertEquals(LineDirection.Outgoing, resultA.getDirection());

        StringIdFor<RouteStation> routeStationIdB = formId(PeelHall, ManchesterAirportVictoria);
        RouteStation routeStationB = repository.getRouteStationById(routeStationIdB);
        LineAndDirection resultB = mapper.map(routeStationB);

        assertEquals(Lines.Airport, resultB.getLine());
        assertEquals(LineDirection.Incoming, resultB.getDirection());
    }

    @Test
    void shouldMapCentralStationsAsExpected() {
        IdSet<Station> centralStationIds = Arrays.stream(CentralZoneStation.values()).
                map(CentralZoneStation::getId).collect(IdSet.idCollector());

        Set<RouteStation> centralRouteStations = repository.getRouteStations().stream().
                filter(routeStation -> centralStationIds.contains(routeStation.getStationId())).
                collect(Collectors.toSet());

        centralRouteStations.forEach(routeStation -> {
            LineAndDirection result = mapper.map(routeStation);
            assertEquals(CentralZoneStation.map.get(routeStation.getStationId()).getLine(), result.getLine());
        });
    }

    @Test
    void shouldMapEachRouteStationToALine() {
        Set<RouteStation> routeStations = repository.getRouteStations();

        for (RouteStation routeStation : routeStations) {
            LineAndDirection lineAndDirection = mapper.map(routeStation);
            assertNotEquals(LineAndDirection.Unknown, lineAndDirection, routeStation.toString());
        }
    }

}
