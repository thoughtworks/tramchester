package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
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
import com.tramchester.testSupport.testTags.Summer2022;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.CentralZoneStation.StWerbergsRoad;
import static com.tramchester.testSupport.reference.TramStations.*;
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
    void shouldHaveAdditionalTramInterchanges() {
        for (IdFor<Station> interchangeId : AdditionalTramInterchanges.stations()) {
            Station interchange = stationRepository.getStationById(interchangeId);
            assertTrue(interchangeRepository.isInterchange(interchange), interchange.toString());
        }
    }

    @Summer2022
    @Test
    void shouldHaveExpectedInterchanges() {
        // todo shaw and crompton?

        // summer 2022 - removed Broadway
        List<TramStations> tramStations = Arrays.asList(StWerburghsRoad, TraffordBar, Cornbrook, HarbourCity, Pomona,
                Cornbrook, Deansgate, StPetersSquare, PiccadillyGardens, Piccadilly, Victoria, MarketStreet);

        Set<Station> expected = tramStations.stream().map(item -> item.from(stationRepository)).collect(Collectors.toSet());

        Set<Station> missing = expected.stream().
                filter(station -> !interchangeRepository.isInterchange(station)).
                collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), HasId.asIds(missing));

        Set<Station> unexpected = interchangeRepository.getAllInterchanges().stream().
                map(InterchangeStation::getStation).
                filter(station -> !expected.contains(station)).
                collect(Collectors.toSet());

        assertTrue(unexpected.isEmpty(), HasId.asIds(unexpected));
    }


    @Test
    void shouldHaveSomeNotInterchanges() {
        assertFalse(interchangeRepository.isInterchange(stationRepository.getStationById(TramStations.Altrincham.getId())));
        assertFalse(interchangeRepository.isInterchange(stationRepository.getStationById(TramStations.OldTrafford.getId())));
    }

    @Summer2022
    @Test
    void shouldHaveInterchangesForMediaCity() {
        assertTrue(interchangeRepository.isInterchange(stationRepository.getStationById(TramStations.HarbourCity.getId())));

        // not during summer 2020
        //assertTrue(interchangeRepository.isInterchange(stationRepository.getStationById(TramStations.Broadway.getId())));
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
     * Here to validate altrincham neighbours testing and interchanges
     * @see NeighboursAsInterchangesTest#altrinchamBecomesInterchangeWhenNeighboursCreated()
     */
    @Test
    public void altrinchamNotAnInterchange() {
        Station station = stationRepository.getStationById(TramStations.Altrincham.getId());
        assertFalse(interchangeRepository.isInterchange(station));
    }

}
