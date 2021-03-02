package com.tramchester.integration.repository.buses;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.TransportDataReaderFactory;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.ServiceCalendar;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.RoutesForTesting;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.RouteDirection.Inbound;
import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.TestEnv.*;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class TransportDataFromFilesBusTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private TransportData transportData;
    private Collection<Service> allServices;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder<>().create(config, TestEnv.NoopRegisterMetrics());
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
    void shouldHaveExpectedNumbersForBus() {
        assertEquals(27, transportData.getAgencies().size());
        assertEquals(13578, transportData.getStations().size());
        assertEquals(647, transportData.getRoutes().size());

        // no platforms represented in train data
        assertEquals(0, transportData.getPlatforms().size());
    }

    @Test
    void shouldGetAgencies() {
        List<Agency> agencies = new ArrayList<>(transportData.getAgencies());
        assertEquals(27, agencies.size());
        assertTrue(agencies.contains(StagecoachManchester));
    }

    @Test
    void shouldGetRouteWithHeadsignsAndCorrectServices() {
        Route result = transportData.getRouteById(RoutesForTesting.ALTY_TO_STOCKPORT_WBT.getId());
        assertEquals("Altrincham - Lymm - Stockton Heath - Warrington", result.getName());
        assertEquals(WarringtonsOwnBuses, result.getAgency());
        assertEquals("WBT:5A:I:",result.getId().forDTO());
        assertTrue(TransportMode.isBus(result));

        Set<Service> svcs = result.getServices();
        for (Service service : svcs) {
            assertEquals(result, service.getRoute(), "Service did not contain route");
        }

        List<String> headsigns = new ArrayList<>(result.getHeadsigns());
        assertEquals(1, headsigns.size(), "expected headsigns");
        assertEquals("Warrington, Bus Interchange", headsigns.get(0));
    }

    @Test
    void shouldHaveExpectedEndOfLinesAndRoutes() {
        Route inbound = transportData.getRouteById(StringIdFor.createId("RDT:464:I:"));
        assertEquals("Rochdale - Whitworth - Rawtenstall - Accrington", inbound.getName());

        Route outbound = transportData.getRouteById(StringIdFor.createId("RDT:464:O:"));
        assertEquals("Accrington - Rawtenstall - Whitworth - Rochdale", outbound.getName());

        Station firstStation = transportData.getStationById(StringIdFor.createId("2500ACC0009"));
        assertEquals("Accrington, Bus Station (Stand 9)", firstStation.getName());
        //assertTrue(firstStation.servesRoute(inbound));
        assertTrue(firstStation.servesRoute(outbound));

        Station secondStation = transportData.getStationById(StringIdFor.createId("2500LAA15791"));
        assertEquals("Accrington, opp Infant Street", secondStation.getName());
        //assertTrue(secondStation.servesRoute(inbound));
        assertTrue(secondStation.servesRoute(outbound));
    }

    @Test
    void shouldGetBusRoutes() {
        Collection<Route> results = transportData.getRoutes();
        long gmsRoutes = results.stream().filter(route -> route.getAgency().equals(StagecoachManchester)).count();

        // todo lockdown 14->12
        assertEquals(196, gmsRoutes);
    }

    @Test
    void shouldGetServicesByDate() {
        LocalDate nextSaturday = TestEnv.nextSaturday();
        TramServiceDate date = new TramServiceDate(nextSaturday);
        Set<Service> results = transportData.getServicesOnDate(date);

        assertFalse(results.isEmpty());
        long onCorrectDate = results.stream().filter(svc -> svc.getCalendar().operatesOn(nextSaturday)).count();
        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        LocalDate noBusesDate = TestEnv.LocalNow().plusMonths(36).toLocalDate(); //transportData.getFeedInfo().validUntil().plusMonths(12);
        results = transportData.getServicesOnDate(new TramServiceDate(noBusesDate));
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldGetStation() {
        assertTrue(transportData.hasStationId(BusStations.PiccadilyStationStopA.getId()));
        Station station = transportData.getStationById(BusStations.PiccadilyStationStopA.getId());
        assertEquals("Manchester City Centre, Piccadilly Station (Stop A)", station.getName());

        assertFalse(station.hasPlatforms());
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

        IdSet<Trip> tripIdsFromSvcs = transportData.getServices().stream().map(Service::getAllTrips).
                flatMap(Collection::stream).
                map(Trip::getId).collect(IdSet.idCollector());
        assertEquals(tripsSize, tripIdsFromSvcs.size());

        IdSet<Service> tripServicesId = new IdSet<>();
        allTrips.forEach(trip -> tripServicesId.add(trip.getService().getId()));
        assertEquals(allSvcs, tripServicesId.size());
    }

    @Test
    void shouldBeApplyingExceptionalDatesCorrectly() {

        TransportDataReaderFactory dataReaderFactory = componentContainer.get(TransportDataReaderFactory.class);
        List<TransportDataReader> transportDataReaders = dataReaderFactory.getReaders();
        TransportDataReader transportDataReader = transportDataReaders.get(0); // yuk
        Stream<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates();

        Set<CalendarDateData> applyToCurrentServices = calendarsDates.
                filter(calendarDateData -> transportData.hasServiceId(calendarDateData.getServiceId())).
                collect(Collectors.toSet());

        calendarsDates.close();

        assertFalse(applyToCurrentServices.isEmpty());

        assertEquals(1,  config.getDataSourceConfig().size(), "expected only one data source");
        DataSourceConfig sourceConfig = config.getDataSourceConfig().get(0);
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
}
