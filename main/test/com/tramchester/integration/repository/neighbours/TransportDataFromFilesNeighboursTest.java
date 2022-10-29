package com.tramchester.integration.repository.neighbours;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.NeighboursTestConfig;
import com.tramchester.repository.AgencyRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.BusTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.integration.repository.TransportDataFromFilesTramTest.NUM_TFGM_TRAM_STATIONS;
import static com.tramchester.integration.repository.buses.TransportDataFromFilesBusTest.TGFM_BUS_AGENCIES;
import static com.tramchester.integration.repository.buses.TransportDataFromFilesBusTest.TGFM_BUS_ROUTES;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
public class TransportDataFromFilesNeighboursTest {
    private static ComponentContainer componentContainer;
    private StationGroupsRepository stationGroupsRepository;
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

        stationGroupsRepository = componentContainer.get(StationGroupsRepository.class);
    }

    @Test
    void shouldHaveTramStation() {
        Station shudehillTram = stationRepository.getStationById(Shudehill.getId());
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

        final Set<Station> busStations = stationRepository.getStationsServing(Bus);
        int numStations = busStations.size();
        assertTrue(numStations > 14000, "big change " + numStations);
        assertTrue(numStations < 16400, "big change " + numStations);

        // no platforms represented in train data
        assertFalse(busStations.stream().anyMatch(Station::hasPlatforms));

        // no bus stations are also tram for tfgm
        assertFalse(busStations.stream().anyMatch(station -> station.servesMode(Tram)));
    }

    @Test
    void shouldFindTheCompositeBusStation() {
        StationGroup shudehillCompositeBus = stationGroupsRepository.findByName("Shudehill Interchange");

        assertNotNull(shudehillCompositeBus);

        shudehillCompositeBus.getContained().forEach(busStop -> {
            Station found = stationRepository.getStationById(busStop.getId());
            assertNotNull(found, "No stop found for " + found);
        });
    }

    @Test
    void shouldNotHaveBoth() {
        long both = stationRepository.getActiveStationStream().
                filter(station -> (station.servesMode(Tram) && station.servesMode(Bus))).count();
        assertEquals(0, both);
    }

    @Test
    void shouldHaveExpectedNumbersForTram() {

        TramDate when = TestEnv.testDay();

        RouteRepository routeRepository = componentContainer.get(RouteRepository.class);
        long tramRoutes = getTramRoutes(routeRepository).count();
        assertEquals(KnownTramRoute.numberOn(when), tramRoutes);

        final Set<Station> stationsForMode = stationRepository.getStationsServing(Tram);
        long tram = stationsForMode.size();
        assertEquals(NUM_TFGM_TRAM_STATIONS, tram);
    }

    @Test
    void shouldHaveCorrectStationsForTramRoutes() {
        Set<Route> tramRoutes = getTramRoutes(routeRepository).collect(Collectors.toSet());

        Set<Station> stationsOnTramRoutes = stationRepository.getStationsServing(Tram).stream().
                filter(station -> station.getPickupRoutes().stream().anyMatch(tramRoutes::contains) ||
                        station.getDropoffRoutes().stream().anyMatch(tramRoutes::contains)).
                collect(Collectors.toSet());

        assertEquals(NUM_TFGM_TRAM_STATIONS, stationsOnTramRoutes.size());
    }

    @NotNull
    private Stream<Route> getTramRoutes(RouteRepository routeRepository) {
        return routeRepository.getRoutes().stream().filter(route -> route.getTransportMode().equals(Tram));
    }

}
