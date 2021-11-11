package com.tramchester.integration.repository.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.AgencyRepository;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.repository.TransportDataFromFilesTramTest.NUM_TFGM_TRAM_ROUTES;
import static com.tramchester.integration.repository.TransportDataFromFilesTramTest.NUM_TFGM_TRAM_STATIONS;
import static com.tramchester.integration.repository.buses.TransportDataFromFilesBusTest.TGFM_BUS_AGENCIES;
import static com.tramchester.integration.repository.buses.TransportDataFromFilesBusTest.TGFM_BUS_ROUTES;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class TransportDataFromFilesNeighboursTest {
    private static ComponentContainer componentContainer;
    private CompositeStationRepository compositeStationRepository;
    private StationRepository stationRepository;
    private RouteRepository routeRepository;
    private AgencyRepository agencyRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new NeighboursTestConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        stationRepository = componentContainer.get(StationRepository.class);
        routeRepository = componentContainer.get(RouteRepository.class);
        agencyRepository = componentContainer.get(AgencyRepository.class);

        compositeStationRepository = componentContainer.get(CompositeStationRepository.class);
    }

    @Test
    void shouldHaveTramStation() {
        Station shudehillTram = compositeStationRepository.getStationById(Shudehill.getId());
        assertNotNull(shudehillTram);
    }

    @Test
    void shouldHaveTGFMBusPlusTramAgencies() {
        assertEquals(TGFM_BUS_AGENCIES + 1, agencyRepository.getAgencies().size());
    }

    @Test
    void shouldHaveRouteNumbersForBus() {
        long numberBusRoutes = routeRepository.getRoutes().stream().
                filter(route -> route.getTransportMode().equals(Bus)).count();
        assertEquals(TGFM_BUS_ROUTES, numberBusRoutes);
    }

    @Test
    void shouldHaveExpectedStationNumberAndPlatformsForBus() {

        final Set<Station> busStations = stationRepository.getStationsForMode(Bus);
        int numStations = busStations.size();
        assertTrue(numStations > 15400, "big change");
        assertTrue(numStations < 16400, "big change");

        // no platforms represented in train data
        assertFalse(busStations.stream().anyMatch(Station::hasPlatforms));

        // no bus stations are also tram for tfgm
        assertFalse(busStations.stream().anyMatch(station -> station.serves(Tram)));
    }

    @Test
    void shouldFindTheCompositeBusStation() {
        CompositeStation shudehillCompositeBus = compositeStationRepository.findByName("Shudehill Interchange");

        assertNotNull(shudehillCompositeBus);

        shudehillCompositeBus.getContained().forEach(busStop -> {
            Station found = stationRepository.getStationById(busStop.getId());
            assertNotNull(found, "No stop found for " + found);
        });
    }

    @Test
    void shouldNotHaveBoth() {
        long both = stationRepository.getStationStream().
                filter(station -> (station.serves(Tram) && station.serves(Bus))).count();
        assertEquals(0, both);
    }

    @Test
    void shouldHaveExpectedNumbersForTram() {

        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        long tramRoutes = getTramRoutes(routeRepository).count();
        assertEquals(NUM_TFGM_TRAM_ROUTES, tramRoutes);

        final Set<Station> stationsForMode = stationRepository.getStationsForMode(Tram);
        long tram = stationsForMode.size();
        assertEquals(NUM_TFGM_TRAM_STATIONS, tram);
    }

    @Test
    void shouldHaveCorrectStationsForTramRoutes() {
        Set<RouteReadOnly> tramRoutes = getTramRoutes(routeRepository).collect(Collectors.toSet());

        Set<Station> stationsOnTramRoutes = stationRepository.getStationsForMode(Tram).stream().
                filter(station -> station.getRoutes().stream().anyMatch(tramRoutes::contains)).
                collect(Collectors.toSet());

        assertEquals(NUM_TFGM_TRAM_STATIONS, stationsOnTramRoutes.size());
    }

    @NotNull
    private Stream<RouteReadOnly> getTramRoutes(RouteRepository routeRepository) {
        return routeRepository.getRoutes().stream().filter(route -> route.getTransportMode().equals(Tram));
    }

}
