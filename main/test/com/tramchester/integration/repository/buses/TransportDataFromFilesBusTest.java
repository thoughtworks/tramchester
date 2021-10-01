package com.tramchester.integration.repository.buses;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.TransportDataLoaderFiles;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.ServiceCalendar;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.testTags.BusTest;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.TestEnv.StagecoachManchester;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static org.junit.jupiter.api.Assertions.*;

@DataUpdateTest
@BusTest
public
class TransportDataFromFilesBusTest {

    public static final int TGFM_BUS_AGENCIES = 35;
    public static final int TGFM_BUS_ROUTES = 1271;
    public static final int NUM_TFGM_BUS_STATIONS = 15513;
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private TransportData transportData;
    private Collection<Service> allServices;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationBusTestConfig();
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
        allServices = transportData.getServices();
    }

    @Test
    void shouldHaveExpectedAgenciesNumbersForBus() {
        assertEquals(TGFM_BUS_AGENCIES, transportData.getAgencies().size());
    }

    @Test
    void shouldHaveRouteNumbersForBus() {
        int numberRoutes = transportData.getRoutes().size();
        assertEquals(TGFM_BUS_ROUTES, numberRoutes);
    }

    @Test
    void shouldHaveExpectedStationAndPlatformNumbersForBus() {

        int numStations = transportData.getStations().size();
        assertEquals(NUM_TFGM_BUS_STATIONS, numStations);

        // no platforms represented in train data
        assertEquals(0, transportData.getPlatforms().size());
    }

    @Test
    void shouldGetSpecificBusRoutes() {
        Collection<Route> results = transportData.getRoutes();
        long gmsRoutes = results.stream().filter(route -> route.getAgency().equals(StagecoachManchester)).count();
        assertTrue(withinNPercent(292, gmsRoutes, 0.1F), Long.toString(gmsRoutes));
    }

    @Test
    void shouldGetOnlyBusRoutes() {
        Collection<Route> results = transportData.getRoutes();
        long notBus = results.stream().filter(route -> !route.getTransportMode().equals(TransportMode.Bus)).count();
        assertEquals(0, notBus);
    }

    @Test
    void shouldGetAgencies() {
        Set<Agency> agencies = transportData.getAgencies();
        assertTrue(agencies.contains(StagecoachManchester));
    }

    @Test
    void shouldHaveNotHaveRoutesWithZeroTrips() {
        Set<Route> routes = transportData.getRoutes();
        Set<Route> emptyRoutes = routes.stream().filter(route -> route.getTrips().isEmpty()).collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), emptyRoutes);
    }

    @Test
    void shouldHaveExpectedEndOfLinesAndRoutes() {
        IdFor<Agency> agencyId = StringIdFor.createId("ROST");

        Set<Route> inbounds = transportData.findRoutesByName(agencyId, "Rochdale - Bacup - Rawtenstall - Accrington");
        assertFalse(inbounds.isEmpty());
        inbounds.forEach(inbound -> assertEquals("464", inbound.getShortName()));

        Set<Route> outbounds = transportData.findRoutesByName(agencyId, "Accrington - Rawtenstall - Bacup - Rochdale");
        assertFalse(outbounds.isEmpty());
        outbounds.forEach(outbound -> assertEquals("464", outbound.getShortName()));

        Station firstStation = transportData.getStationById(StringIdFor.createId("2500ACC0009"));
        assertEquals("Bus Station", firstStation.getName());
        outbounds.forEach(outbound -> assertTrue(firstStation.servesRoute(outbound)));

        Station secondStation = transportData.getStationById(StringIdFor.createId("2500LAA15791"));
        assertEquals("Infant Street", secondStation.getName());
        outbounds.forEach(outbound -> assertTrue(secondStation.servesRoute(outbound)));
    }

    @Test
    void shouldGetServicesByDate() {
        LocalDate nextSaturday = TestEnv.nextSaturday();
        TramServiceDate date = new TramServiceDate(nextSaturday);
        Set<Service> results = transportData.getServicesOnDate(date);

        assertFalse(results.isEmpty());
        long onCorrectDate = results.stream().filter(svc -> svc.getCalendar().operatesOn(nextSaturday)).count();
        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        LocalDate noBusesDate = TestEnv.LocalNow().plusMonths(36).toLocalDate();
        Set<Service> futureServices = transportData.getServicesOnDate(new TramServiceDate(noBusesDate));
        assertTrue(results.size() > futureServices.size());
    }

    @Test
    void shouldGetStations() {

        for(BusStations testStation : BusStations.values()) {
            IdFor<Station> testStationId = testStation.getId();
            assertTrue(transportData.hasStationId(testStationId), "stop id is missing for " + testStation.name() + " id:"+ testStationId);
            Station found = transportData.getStationById(testStationId);
            assertEquals(testStation.getName(), found.getName());
        }
    }

    @Disabled("too slow currently for buses")
    @Test
    void shouldHaveConsistencyOfRouteAndTripAndServiceIds() {
        Collection<Route> allRoutes = transportData.getRoutes();
        List<Integer> svcSizes = new LinkedList<>();

        allRoutes.forEach(route -> svcSizes.add(route.getServices().size()));

        int allSvcs = svcSizes.stream().reduce(0, Integer::sum);

        assertEquals(allSvcs, allServices.size());

        Set<Station> allsStations = transportData.getStations();

        Set<Trip> allTrips = new HashSet<>();
        allsStations.forEach(station -> allTrips.addAll(getTripsFor(transportData.getTrips(), station)));

        int tripsSize = transportData.getTrips().size();
        assertEquals(tripsSize, allTrips.size());

        IdSet<Trip> tripIdsFromSvcs = transportData.getRoutes().stream().map(Route::getTrips).
                flatMap(Collection::stream).
                map(Trip::getId).collect(IdSet.idCollector());
        assertEquals(tripsSize, tripIdsFromSvcs.size());

    }

    @Test
    void shouldBeApplyingExceptionalDatesCorrectly() {

        TransportDataLoaderFiles dataReaderFactory = componentContainer.get(TransportDataLoaderFiles.class);
        List<TransportDataReader> transportDataReaders = dataReaderFactory.getReaders();
        TransportDataReader transportDataReader = transportDataReaders.get(0); // yuk
        Stream<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates();

        Set<CalendarDateData> applyToCurrentServices = calendarsDates.
                filter(calendarDateData -> transportData.hasServiceId(calendarDateData.getServiceId())).
                collect(Collectors.toSet());

        calendarsDates.close();

        assertFalse(applyToCurrentServices.isEmpty());

        assertEquals(1,  config.getGTFSDataSource().size(), "expected only one data source");
        GTFSSourceConfig sourceConfig = config.getGTFSDataSource().get(0);
        Set<LocalDate> excludedByConfig = sourceConfig.getNoServices();

        applyToCurrentServices.forEach(exception -> {
            Service service = transportData.getServiceById(exception.getServiceId());
            ServiceCalendar calendar = service.getCalendar();

            LocalDate exceptionDate = exception.getDate();
            int exceptionType = exception.getExceptionType();
            if (exceptionType == CalendarDateData.ADDED) {
                if (excludedByConfig.contains(exceptionDate)) {
                    assertFalse(calendar.operatesOn(exceptionDate));
                } else {
                    assertTrue(calendar.operatesOn(exceptionDate));
                }
            } else if (exceptionType == CalendarDateData.REMOVED) {
                assertFalse(calendar.operatesOn(exceptionDate));
            }
        });
    }

    // for things changing very frequently
    private boolean withinNPercent(long expected, long actual, float percentage) {
        int margin = Math.round(expected * percentage);
        long upper = expected + margin;
        long lower = expected - margin;

        return (actual>lower) && (actual<upper);
    }

}
