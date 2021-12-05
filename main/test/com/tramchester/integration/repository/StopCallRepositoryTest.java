package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.ServiceRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StopCallRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class StopCallRepositoryTest {
    private static ComponentContainer componentContainer;

    private StopCallRepository stopCallRepository;
    private StationRepository stationRepository;
    private ServiceRepository serviceRepository;

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
    void beforeEachTest() {
        stopCallRepository = componentContainer.get(StopCallRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);
        serviceRepository = componentContainer.get(ServiceRepository.class);
    }

    @Test
    void shouldGetStopCallsForAStation() {
        final LocalDate date = TestEnv.testDay();
        IdSet<Service> servicesForDate = serviceRepository.getServicesOnDate(date);

        final IdFor<Station> stationId = TramStations.ManAirport.getId();

        Station station = stationRepository.getStationById(stationId);

        final TramTime begin = TramTime.of(9, 0);
        final TramTime end = TramTime.of(10, 0);

        Set<StopCall> results = stopCallRepository.getStopCallsFor(station, date, begin, end);
        assertFalse(results.isEmpty());

        results.forEach(stopCall -> assertEquals(stationId, stopCall.getStationId()));

        boolean wrongService = results.stream().
                filter(stopCall -> !servicesForDate.contains(stopCall.getServiceId())).
                findAny().isEmpty();
        assertTrue(wrongService);

        long correctTimes = results.stream().
                filter(stopCall -> stopCall.getArrivalTime().between(begin, end)).
                count();
        assertEquals(results.size(), correctTimes);

        assertEquals(5, results.size(), results.toString());
    }
}
