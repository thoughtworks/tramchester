package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.InterchangeStation;
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
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.*;
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
        Set<TramStations> tramStations = new HashSet<>(Arrays.asList(StWerburghsRoad, TraffordBar, Cornbrook, HarbourCity, Pomona,
                Cornbrook, Deansgate, StPetersSquare, PiccadillyGardens, Piccadilly, Victoria, MarketStreet));

        Set<Station> expected = tramStations.stream().map(item -> item.from(stationRepository)).collect(Collectors.toSet());

        Set<Station> additional = AdditionalTramInterchanges.stations().stream().map(id -> stationRepository.getStationById(id)).collect(Collectors.toSet());

        expected.addAll(additional);

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


    @Summer2022
    @Test
    void shouldHaveSomeNotInterchanges() {
        // TODO Alty needs to be interchange during summer 2022
        //assertFalse(interchangeRepository.isInterchange(stationRepository.getStationById(TramStations.Altrincham.getId())));
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

    @Summer2022
    @Test
    void shouldNotHaveAdditionalInterchangesAfter19August2020() {
        LocalDate currentDay = LocalDate.now();

        // changes to 30th, still in the data....
        LocalDate cutoffDate = LocalDate.of(2022, 8, 30);

        if (currentDay.isAfter(cutoffDate)) {
            Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
            IdSet<Station> stationIds = interchanges.stream().map(InterchangeStation::getStation).collect(IdSet.collector());

            assertFalse(stationIds.contains(Whitefield.getId()));
            assertFalse(stationIds.contains(Altrincham.getId()));

            // should be   [9400ZZMAGMX, 9400ZZMAPIC] in config
        }

    }

    @Summer2022
    @Test
    void shouldReproIssueWithMissingInterchangeForTraffordCentreToCornbrook() {
        Station cornbrook = Cornbrook.from(stationRepository);

        Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        List<Station> stations = interchanges.stream().map(InterchangeStation::getStation).collect(Collectors.toList());

        assertTrue(stations.contains(cornbrook));

        IdSet<Route> dropOffs = cornbrook.getDropoffRoutes().stream().collect(IdSet.collector());

        assertTrue(dropOffs.contains(StringIdFor.createId("METLRED:I:CURRENT")), dropOffs.toString());
    }

    @Summer2022
    @Test
    void shouldHaveAdditionalInterchangesDuringReplacementPeriod() {
        Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        IdSet<Station> stations = interchanges.stream().map(InterchangeStation::getStation).collect(IdSet.collector());

        // METLML4
        assertTrue(stations.contains(Altrincham.getId()));
        assertTrue(stations.contains(StringIdFor.createId("9400ZZMATIM"))); // timperley

        // METLML2 & METLM3
        assertTrue(stations.contains(TramStations.Whitefield.getId())); // Whitefield
        assertTrue(stations.contains(StringIdFor.createId("9400ZZMACRU"))); // Crumpsall

    }

    @Test
    void shouldHaveReachableInterchangeForEveryRoute() {
        Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        Set<Route> dropOffRoutes = interchanges.stream().flatMap(interchangeStation -> interchangeStation.getDropoffRoutes().stream()).collect(Collectors.toSet());
        Set<Route> pickupRoutes = interchanges.stream().flatMap(interchangeStation -> interchangeStation.getPickupRoutes().stream()).collect(Collectors.toSet());

        IdSet<Route> routesWithoutInterchanges = routeRepository.getRoutes().stream().
                filter(route -> !dropOffRoutes.contains(route)).
                filter(route -> !pickupRoutes.contains(route)).
                collect(IdSet.collector());;

        //IdSet<Route> all = routeRepository.getRoutes().stream().collect(IdSet.collector());

        assertTrue(routesWithoutInterchanges.isEmpty(), routesWithoutInterchanges.toString());
    }

    /***
     * Here to validate altrincham neighbours testing and interchanges
     * @see NeighboursAsInterchangesTest#altrinchamBecomesInterchangeWhenNeighboursCreated()
     */
    @Summer2022
    @Disabled("is an interchange for a period")
    @Test
    public void altrinchamNotAnInterchange() {
        Station station = stationRepository.getStationById(TramStations.Altrincham.getId());
        assertFalse(interchangeRepository.isInterchange(station));
    }

}
