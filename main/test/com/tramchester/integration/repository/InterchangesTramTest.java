package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.InterchangeType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.integration.graph.railAndTram.TramTrainNeighboursAsInterchangesTest;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.CentralZoneStation.StWerbergsRoad;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
public class InterchangesTramTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private InterchangeRepository interchangeRepository;
    private StationRepository stationRepository;
    private RouteRepository routeRepository;
    private TramRouteHelper tramRouteHelper;

    @BeforeAll
    static void onceBeforeAnyTestsRun(TramchesterConfig tramchesterConfig) {
        config = tramchesterConfig;
        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
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

        tramRouteHelper = new TramRouteHelper(routeRepository);
    }

    @Test
    void shouldHaveAdditionalTramInterchanges() {
        for (IdFor<Station> interchangeId : AdditionalTramInterchanges.stations()) {
            Station interchange = stationRepository.getStationById(interchangeId);
            assertTrue(interchangeRepository.isInterchange(interchange), interchange.toString());
        }
    }

    @Test
    void shouldHaveExpectedTramInterchanges() {

        List<TramStations> tramStations = Arrays.asList(StWerburghsRoad, TraffordBar, Cornbrook, HarbourCity,
                Pomona, Cornbrook, Deansgate, StPetersSquare,
                PiccadillyGardens, Piccadilly, Victoria, MarketStreet, Broadway);

        Set<Station> expected = tramStations.stream().map(item -> item.from(stationRepository)).collect(Collectors.toSet());

        Set<Station> additional = AdditionalTramInterchanges.stations().stream().map(id -> stationRepository.getStationById(id)).collect(Collectors.toSet());
        expected.addAll(additional);

        if (config.hasRailConfig()) {
            List<TramStations> adjacentToRail = Arrays.asList(RochdaleRail, NavigationRoad, Eccles, Ashton, Altrincham, ManAirport, EastDidsbury);
            Set<Station> forRail = adjacentToRail.stream().map(item -> item.from(stationRepository)).collect(Collectors.toSet());
            expected.addAll(forRail);
        }

        Set<Station> missing = expected.stream().
                filter(station -> !interchangeRepository.isInterchange(station)).
                collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), HasId.asIds(missing));

        Set<Station> unexpected = interchangeRepository.getAllInterchanges().stream().
                map(InterchangeStation::getStation).
                filter(station -> station.getTransportModes().contains(TransportMode.Tram)).
                filter(station -> !expected.contains(station)).
                collect(Collectors.toSet());

        assertTrue(unexpected.isEmpty(), HasId.asIds(unexpected));
    }

    @Test
    void shouldHaveExpectedDropoffAndPickupRoutesForInterchange() {
        TramDate date = TestEnv.testDay();

        Station stWerb = StWerburghsRoad.from(stationRepository);
        InterchangeStation interchange = interchangeRepository.getInterchange(stWerb);
        assertEquals(InterchangeType.NumberOfLinks, interchange.getType());

        Route toAirport = tramRouteHelper.getOneRoute(VictoriaWythenshaweManchesterAirport, date);
        assertTrue(interchange.getPickupRoutes().contains(toAirport));

        Route toEastDids = tramRouteHelper.getOneRoute(RochdaleShawandCromptonManchesterEastDidisbury, date);
        Set<Route> dropoffRoutes = interchange.getDropoffRoutes().stream().
                filter(route -> route.isAvailableOn(date)).
                collect(Collectors.toSet());
        assertTrue(dropoffRoutes.contains(toEastDids), HasId.asIds(dropoffRoutes));
    }

    @Test
    void shouldHaveSomeNotInterchanges() {
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

    @DisabledIf("isGMConfig")
    @Test
    void shouldAllBeSingleModeForTram() {
        Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        interchanges.forEach(interchangeStation -> assertFalse(interchangeStation.isMultiMode(), interchangeStation.toString()));
    }

    @Disabled("WIP, allow naptan change check in")
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

    @Test
    void shouldReproIssueWithMissingInterchangeForTraffordCentreToCornbrook() {
        Station cornbrook = Cornbrook.from(stationRepository);

        Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        List<Station> stations = interchanges.stream().map(InterchangeStation::getStation).collect(Collectors.toList());

        assertTrue(stations.contains(cornbrook));

        IdSet<Route> dropOffs = cornbrook.getDropoffRoutes().stream().collect(IdSet.collector());

        assertTrue(dropOffs.contains(Route.createId("METLRED:I:CURRENT")), dropOffs.toString());
    }

    @Test
    void shouldHaveReachableInterchangeForEveryRoute() {
        TramDate date = TestEnv.testDay();

        Set<InterchangeStation> interchanges = interchangeRepository.getAllInterchanges();
        Set<Route> dropOffRoutes = interchanges.stream().
                flatMap(interchangeStation -> interchangeStation.getDropoffRoutes().stream()).
                filter(route -> route.isAvailableOn(date)).
                collect(Collectors.toSet());
        Set<Route> pickupRoutes = interchanges.stream().
                flatMap(interchangeStation -> interchangeStation.getPickupRoutes().stream()).
                filter(route -> route.isAvailableOn(date)).
                collect(Collectors.toSet());

        IdSet<Route> routesWithoutInterchanges = routeRepository.getRoutes().stream().
                filter(route -> route.isAvailableOn(date)).
                filter(route -> !dropOffRoutes.contains(route)).
                filter(route -> !pickupRoutes.contains(route)).
                collect(IdSet.collector());

        assertTrue(routesWithoutInterchanges.isEmpty(), routesWithoutInterchanges.toString());

    }

    /***
     * Here to validate altrincham neighbours testing and interchanges
     * @see TramTrainNeighboursAsInterchangesTest#altrinchamBecomesInterchangeWhenNeighboursCreated()
     */
    @DisabledIf("isGMConfig")
    @Test
    public void altrinchamNotAnInterchange() {
        Station station = stationRepository.getStationById(Altrincham.getId());
        assertFalse(interchangeRepository.isInterchange(station));
    }

    static boolean isGMConfig() {
        return config instanceof RailAndTramGreaterManchesterConfig;
    }

}
