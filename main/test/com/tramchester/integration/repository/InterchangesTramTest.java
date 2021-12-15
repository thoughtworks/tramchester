package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.InterchangeStation;
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
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.CentralZoneStation.Shudehill;
import static com.tramchester.domain.reference.CentralZoneStation.StWerbergsRoad;
import static org.junit.jupiter.api.Assertions.*;

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
            assertTrue(interchangeRepository.isInterchange(interchange), interchange.toString());
        }
    }

    @Test
    void shouldHaveSomeNotInterchanges() {
        assertFalse(interchangeRepository.isInterchange(stationRepository.getStationById(TramStations.Altrincham.getId())));
        assertFalse(interchangeRepository.isInterchange(stationRepository.getStationById(TramStations.OldTrafford.getId())));
    }

    @Test
    void shouldHaveInterchangesForMediaCity() {
        assertTrue(interchangeRepository.isInterchange(stationRepository.getStationById(TramStations.HarbourCity.getId())));
        assertTrue(interchangeRepository.isInterchange(stationRepository.getStationById(TramStations.Broadway.getId())));
    }

    @Test
    void shouldHaveExpectedPickupsAndDropoffs() {
        Optional<InterchangeStation> shouldHaveInterchange = interchangeRepository.getAllInterchanges().stream().
                filter(interchangeStation -> interchangeStation.getStationId().equals(StWerbergsRoad.getId())).
                findFirst();
        assertTrue(shouldHaveInterchange.isPresent());

        Station station = stationRepository.getStationById(StWerbergsRoad.getId());

        InterchangeStation interchangeStation = shouldHaveInterchange.get();

        assertEquals(interchangeStation.getPickupRoutes(), station.getPickupRoutes());
        assertEquals(interchangeStation.getDropoffRoutes(), station.getDropoffRoutes());
    }

    @Test
    void shouldAllBeSingleModeForTram() {
        Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        interchanges.forEach(interchangeStation -> assertFalse(interchangeStation.isMultiMode(), interchangeStation.toString()));
    }

    @Test
    void shouldHaveReachableInterchangeForEveryRoute() {
        Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        Set<Route> dropOffRoutes = interchanges.stream().flatMap(interchangeStation -> interchangeStation.getDropoffRoutes().stream()).collect(Collectors.toSet());
        Set<Route> pickupRoutes = interchanges.stream().flatMap(interchangeStation -> interchangeStation.getPickupRoutes().stream()).collect(Collectors.toSet());

        Set<Route> routesWithInterchanges = routeRepository.getRoutes().stream().
                filter(dropOffRoutes::contains).
                filter(pickupRoutes::contains).
                collect(Collectors.toSet());;

        Set<Route> all = routeRepository.getRoutes();

        assertEquals(all, routesWithInterchanges);
    }

    /***
     * Here to validate shudehill neighbours testing and interchanges
     * @see NeighboursAsInterchangesTest#shudehillBecomesInterchangeWhenNeighboursCreated()
     */
    @Test
    public void shudehillNotAnInterchange() {
        Station shudehill = stationRepository.getStationById(Shudehill.getId());
        assertFalse(interchangeRepository.isInterchange(shudehill));
    }

}
