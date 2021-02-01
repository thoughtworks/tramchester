package com.tramchester.integration.repository.trains;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.TransportDataFromFilesBuilder;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.TransportDataReaderFactory;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.TestEnv.ArrivaTrainsWales;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class TransportDataFromFilesTrainTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private TransportData transportData;
    private Collection<Service> allServices;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTrainTestConfig();
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
    void shouldHaveExpectedNumbersForTrain() {
        assertEquals(28, transportData.getAgencies().size());
        assertEquals(2564,transportData.getStations().size());
        assertEquals(3629, transportData.getRoutes().size());

        // no platforms represented in train data
        assertEquals(0, transportData.getPlatforms().size());
    }

    @Test
    void shouldGetAgencies() {
        List<Agency> agencies = new ArrayList<>(transportData.getAgencies());
        assertEquals(28, agencies.size());
        assertTrue(agencies.contains(ArrivaTrainsWales));
    }

    @Test
    void shouldGetRouteWithHeadsignsAndCorrectServices() {
        Route result = transportData.getRouteById(IdFor.createId("18085")); // ariva train man airport to chester
        assertEquals("AW train service from MIA to CTR", result.getName());
        assertEquals(ArrivaTrainsWales, result.getAgency());
        assertEquals("18085",result.getId().forDTO());
        assertTrue(TransportMode.isTrain(result));

        Set<Service> svcs = result.getServices();
        for (Service service : svcs) {
            assertTrue(service.getRoutes().contains(result), "Service did not contain route");
        }

        Set<String> headsigns = result.getHeadsigns();
        assertEquals(2, headsigns.size(), "expected headsigns");
        assertTrue(headsigns.contains("Eccles"));
        assertTrue(headsigns.contains("Trafford Bar"));
    }

    @Test
    void shouldGetTrainRoutes() {
        Collection<Route> results = transportData.getRoutes();
        long walesTrainRoutes = results.stream().filter(route -> route.getAgency().equals(ArrivaTrainsWales)).count();

        // todo lockdown 14->12
        assertEquals(12, walesTrainRoutes);
    }

    @Test
    void shouldGetServicesByDate() {
        LocalDate nextSaturday = TestEnv.nextSaturday();
        TramServiceDate date = new TramServiceDate(nextSaturday);
        Set<Service> results = transportData.getServicesOnDate(date);

        assertFalse(results.isEmpty());
        long onCorrectDate = results.stream().filter(svc -> svc.getCalendar().operatesOn(nextSaturday)).count();
        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        LocalDate noTramsDate = TestEnv.LocalNow().plusMonths(36).toLocalDate(); //transportData.getFeedInfo().validUntil().plusMonths(12);
        results = transportData.getServicesOnDate(new TramServiceDate(noTramsDate));
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldGetStation() {
        assertTrue(transportData.hasStationId(TrainStations.ManchesterPiccadilly.getId()));
        Station station = transportData.getStationById(TrainStations.ManchesterPiccadilly.getId());
        assertEquals("Manchester Piccadilly", station.getName());

        assertFalse(station.hasPlatforms());
    }

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

    @Disabled("Performance tests")
    @Test
    void shouldLoadData() {
        TransportDataFromFilesBuilder builder = componentContainer.get(TransportDataFromFilesBuilder.class);

        int count = 10;
        //int count = 1;
        long total = 0;
        for (int i = 0; i < count; i++) {
            long begin = System.currentTimeMillis();
            TransportDataFromFiles fromFiles = builder.create();
            fromFiles.getData();
            long finish = System.currentTimeMillis();

            total = total + (finish - begin);
        }

        System.out.println(String.format("Total: %s ms Average: %s ms", total, total/count));
    }

}
