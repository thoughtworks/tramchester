package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class RunningRoutesAndServicesTest {

    private static ComponentContainer componentContainer;
    private TransportData transportData;
    private RunningRoutesAndServices runningRoutesAndServices;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
        runningRoutesAndServices = componentContainer.get(RunningRoutesAndServices.class);
    }


    @Test
    void shouldHaveTripsServicesAndRoutesThatCrossIntoNextDay() {

        Set<Trip> tripsIntoNextDay = transportData.getTrips().stream().filter(Trip::intoNextDay).collect(Collectors.toSet());

        assertFalse(tripsIntoNextDay.isEmpty());

        Set<Service> servicesFromTrips = tripsIntoNextDay.stream().map(Trip::getService).collect(Collectors.toSet());

        servicesFromTrips.forEach(service -> assertTrue(runningRoutesAndServices.intoNextDay(service.getId()),
                service.getId() + " should be into next day"));

        Set<Service> servicesIntoNextDay = transportData.getServices().stream().
                filter(service -> runningRoutesAndServices.intoNextDay(service.getId())).collect(Collectors.toSet());

        assertEquals(servicesIntoNextDay, servicesFromTrips);

        Set<Route> routesFromTrips = tripsIntoNextDay.stream().map(Trip::getRoute).collect(Collectors.toSet());
        assertFalse(routesFromTrips.isEmpty());

        Set<Route> routesIntoNextDay = transportData.getRoutes().stream().filter(Route::intoNextDay).collect(Collectors.toSet());
        assertEquals(routesFromTrips, routesIntoNextDay);
    }
}
