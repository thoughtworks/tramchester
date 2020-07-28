package com.tramchester.integration.repository;


import com.tramchester.Dependencies;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.TransportDataReaderFactory;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.dataimport.parsers.CalendarDatesDataMapper;
import com.tramchester.domain.*;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.DataExpiryCategory;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static org.junit.jupiter.api.Assertions.*;

class TransportDataFromFilesTest {

    private static Dependencies dependencies;

    private TransportData transportData;
    private Collection<Service> allServices;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = dependencies.get(TransportData.class);
        allServices = transportData.getServices();
    }

    @Test
    void shouldHaveExpectedNumbersForTram() {
        assertEquals(1, transportData.getAgencies().size());
        assertEquals(99,transportData.getStations().size());
        assertEquals(12, transportData.getRoutes().size());
        assertEquals(195, transportData.getPlatforms().size());
    }

    @Test
    void shouldGetFeedInfo() {
        FeedInfo result = transportData.getFeedInfos().get("tfgm");
        assertEquals("http://www.tfgm.com", result.getPublisherUrl());
    }

    @Test
    void shouldHaveCorrectLocationForAirportInTestEnvironment() {
        Station actualStation = transportData.getStationById(Stations.ManAirport.getId());
        assertEquals(TestEnv.manAirportLocation, actualStation.getLatLong());
    }

    @Test
    void shouldGetAgenciesWithNames() {
        List<Agency> agencies = new ArrayList<>(transportData.getAgencies());
        assertEquals(1, agencies.size()); // just MET for trams
        assertEquals("MET", agencies.get(0).getId().forDTO());
        assertEquals("Metrolink", agencies.get(0).getName());
    }

    @Test
    void shouldGetRouteWithHeadsigns() {
        Route result = transportData.getRouteById(RoutesForTesting.ASH_TO_ECCLES.getId());
        assertEquals("Ashton-under-Lyne - Manchester - Eccles", result.getName());
        assertEquals(TestEnv.MetAgency(),result.getAgency());
        assertEquals("MET:   3:I:",result.getId().forDTO());
        assertTrue(TransportMode.isTram(result));

        Set<String> headsigns = result.getHeadsigns();
        assertEquals(2, headsigns.size(), "expected headsigns");
        assertTrue(headsigns.contains("Eccles"));
        assertTrue(headsigns.contains("Trafford Bar"));
    }

    @Test
    void shouldGetTramRoutes() {
        Collection<Route> results = transportData.getRoutes();
        long tramRoutes = results.stream().filter(route -> route.getAgency().equals(TestEnv.MetAgency())).count();

        // todo lockdown 14->12
        assertEquals(12, tramRoutes);
    }

    @Test
    void shouldGetServicesByDate() {
        LocalDate nextSaturday = TestEnv.nextSaturday();
        TramServiceDate date = new TramServiceDate(nextSaturday);
        Set<Service> results = transportData.getServicesOnDate(date);

        assertFalse(results.isEmpty());
        long onCorrectDate = results.stream().filter(svc -> svc.operatesOn(nextSaturday)).count();
        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        LocalDate noTramsDate = TestEnv.LocalNow().plusMonths(36).toLocalDate(); //transportData.getFeedInfo().validUntil().plusMonths(12);
        results = transportData.getServicesOnDate(new TramServiceDate(noTramsDate));
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldHaveSundayServicesFromCornbrook() {
        LocalDate nextSunday = TestEnv.nextSunday();

        Set<Service> sundayServices = transportData.getServicesOnDate(new TramServiceDate(nextSunday));

        IdSet<Service> sundayServiceIds = sundayServices.stream().collect(IdSet.collector());

        Set<Trip> cornbrookTrips = transportData.getTrips().stream().
                filter(trip -> trip.getStops().callsAt(Stations.Cornbrook)).collect(Collectors.toSet());

        Set<Trip> sundayTrips = cornbrookTrips.stream().filter(trip -> sundayServiceIds.
                contains(trip.getService().getId())).collect(Collectors.toSet());

        assertFalse(sundayTrips.isEmpty());
    }

    @Disabled("Only useful when issues arise with expiry")
    @DataExpiryCategory
    @Test
    void shouldHaveServiceEndDatesBeyondNextNDays() {
        LocalDate queryDate = LocalDate.now().plusDays(DAYS_AHEAD);

        Collection<Service> services = transportData.getServices();
        Set<Service> expiringServices = services.stream().
                filter(svc -> !svc.operatesOn(queryDate)).collect(Collectors.toSet());
        Set<Route> routes = expiringServices.stream().map(Service::getRoutes).flatMap(Collection::stream).collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), expiringServices, HasId.asIds(routes) + " with expiring svcs " +HasId.asIds(expiringServices));
    }

    @DataExpiryCategory
    @Test
    void shouldHaveServicesRunningAtReasonableTimesNDaysAhead() {

        // temporary 23 -> 22, 6->7
        int latestHour = 22;
        int earlistHour = 7;

        int maxwait = 25;

        for (int day = 0; day < DAYS_AHEAD; day++) {
            LocalDate date = TestEnv.testDay().plusDays(day);
            TramServiceDate tramServiceDate = new TramServiceDate(date);
            Set<Service> servicesOnDate = transportData.getServicesOnDate(tramServiceDate);

            IdSet<Service> servicesOnDateIds = servicesOnDate.stream().collect(IdSet.collector());
            transportData.getStations().stream().forEach(station -> {
                Set<Trip> callingTripsOnDate = transportData.getTrips().stream().
                        filter(trip -> trip.getStops().callsAt(station)).
                        filter(trip -> servicesOnDateIds.contains(trip.getService().getId())).
                        collect(Collectors.toSet());
                assertFalse(callingTripsOnDate.isEmpty(), String.format("%s %s", date, station));

                IdSet<Service> callingServicesIds = callingTripsOnDate.stream().map(Trip::getService).collect(IdSet.collector());

                for (int hour = earlistHour; hour < latestHour; hour++) {
                    TramTime tramTime = TramTime.of(hour,0);
                    Set<Service> runningAtTime = servicesOnDate.stream().
                            filter(svc -> callingServicesIds.contains(svc.getId())).
                            filter(svc -> tramTime.between(svc.earliestDepartTime(), svc.latestDepartTime())).collect(Collectors.toSet());

                    assertFalse(runningAtTime.isEmpty(), String.format("%s %s %s", date, tramTime, station.getName()));

                    Set<StopCall> calling = new HashSet<>();
                    callingTripsOnDate.forEach(trip -> {
                        Set<StopCall> onTime = trip.getStops().stream().
                                filter(stop -> stop.getStation().equals(station)).
                                filter(stop -> tramTime.between(stop.getArrivalTime().minusMinutes(maxwait), stop.getArrivalTime())).
                                collect(Collectors.toSet());
                        calling.addAll(onTime);
                    });
                    assertFalse(calling.isEmpty(), String.format("Stops %s %s %s %s", date.getDayOfWeek(), date, tramTime, station.getName()));
                }

            });
        }
    }

    @Test
    void shouldHaveAtLeastOnePlatformForEveryStation() {
        Set<Station> stations = transportData.getStations();
        Set<Station> noPlatforms = stations.stream().filter(station -> station.getPlatforms().isEmpty()).collect(Collectors.toSet());
        assertEquals(Collections.emptySet(),noPlatforms);
    }

    @Test
    void shouldGetStation() {
        assertTrue(transportData.hasStationId(Stations.Altrincham.getId()));
        Station station = transportData.getStationById(Stations.Altrincham.getId());
        assertEquals("Altrincham", station.getName());

        assertTrue(station.hasPlatforms());
        // only one platform at alty, well according to the timetable anyway....
        assertEquals(1, station.getPlatforms().size());
        Platform platformOne = station.getPlatforms().get(0);
        assertEquals( Stations.Altrincham.forDTO()+"1", platformOne.getId().forDTO());
        assertEquals( "1", platformOne.getPlatformNumber());
        assertEquals( "Altrincham platform 1", platformOne.getName());
    }

    @Test
    void shouldHavePlatform() {
        IdFor<Platform> id = IdFor.createId(Stations.StPetersSquare.forDTO() + "3");

        assertTrue(transportData.hasPlatformId(id));
        Platform platform = transportData.getPlatform(id);
        assertEquals("St Peter's Square platform 3", platform.getName());
        assertEquals(Stations.StPetersSquare.forDTO()+"3", platform.getId().forDTO());
    }

    @Test
    void shouldHaveAllEndOfLineTramStations() {
        List<Station> filteredStations = transportData.getStations().stream()
                .filter(station -> Stations.EndOfTheLine.contains(station)).collect(Collectors.toList());

        assertEquals(Stations.EndOfTheLine.size(), filteredStations.size());
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
    void shouldLoadExceptionalDates() {
        Set<String> servicesToLoad = allServices.stream().map(Service::getId).map(IdFor::forDTO).collect(Collectors.toSet());

        TransportDataReaderFactory dataReaderFactory = dependencies.get(TransportDataReaderFactory.class);
        List<TransportDataReader> transportDataReaders = dataReaderFactory.getReaders();
        TransportDataReader transportDataReader = transportDataReaders.get(0);
        Stream<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates(new CalendarDatesDataMapper(servicesToLoad));

        calendarsDates.forEach(exception -> {
            Service service = transportData.getServiceById(exception.getServiceId());
            LocalDate exceptionDate = exception.getDate();
            int exceptionType = exception.getExceptionType();
            if (exceptionType == CalendarDateData.ADDED) {
                assertTrue(service.operatesOn(exceptionDate));
            } else if (exceptionType == CalendarDateData.REMOVED) {
                assertFalse(service.operatesOn(exceptionDate));
            }
        });
        calendarsDates.close();
    }

    @Test
    void shouldReproIssueAtMediaCityWithBranchAtCornbrook() {
        Set<Trip> allTrips = getTripsFor(transportData.getTrips(), Stations.Cornbrook);

        IdSet<Service> toMediaCity = allTrips.stream().
                filter(trip -> trip.getStops().callsAt(Stations.Cornbrook)).
                filter(trip -> trip.getStops().callsAt(Stations.MediaCityUK)).
                filter(trip -> trip.getRoute().getId().equals(RoutesForTesting.ASH_TO_ECCLES.getId())).
                map(trip -> trip.getService().getId()).collect(IdSet.idCollector());

        Set<Service> services = toMediaCity.stream().
                map(svc->transportData.getServiceById(svc)).collect(Collectors.toSet());

        LocalDate nextTuesday = TestEnv.testDay();

        Set<Service> onDay = services.stream().
                filter(service -> service.operatesOn(nextTuesday)).
                collect(Collectors.toSet());

        ServiceTime time = ServiceTime.of(12, 0);

        Set<Service> onTime = onDay.stream().
                filter(svc -> svc.latestDepartTime().isAfter(time) && svc.earliestDepartTime().isBefore(time)).
                collect(Collectors.toSet());

        assertFalse(onTime.isEmpty(), "next tuesday at 12 missing"); // at least one service (likely is just one)
    }

    @DataExpiryCategory
    @Test
    void shouldHaveCorrectDataForTramsCallingAtVeloparkMonday8AM() {
        Set<Trip> origTrips = getTripsFor(transportData.getTrips(), Stations.VeloPark);
        //List<String> ashtonXRoutes = Collections.singletonList(RoutesForTesting.ASH_TO_ECCLES.getId());

        LocalDate aMonday = TestEnv.nextMonday();
        assertEquals(DayOfWeek.MONDAY, aMonday.getDayOfWeek());

        // TODO Due to exception dates makes no sense to use getDays
        IdSet<Service> mondayAshToManServices = allServices.stream()
                .filter(svc -> svc.operatesOn(aMonday))
                .filter(svc -> svc.getRoutes().contains(RoutesForTesting.ASH_TO_ECCLES))
                .collect(IdSet.collector());

        // reduce the trips to the ones for the right route on the monday by filtering by service ID
        List<Trip> filteredTrips = origTrips.stream().filter(trip -> mondayAshToManServices.contains(trip.getService().getId())).
                collect(Collectors.toList());

        assertTrue(filteredTrips.size()>0);

        // find the stops, invariant is now that each trip ought to contain a velopark stop
        List<StopCall> stoppingAtVelopark = filteredTrips.stream()
                .filter(trip -> mondayAshToManServices.contains(trip.getService().getId()))
                .map(trip -> getStopsFor(trip, Stations.VeloPark.getId()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertEquals(filteredTrips.size(), stoppingAtVelopark.size());

        // finally check there are trams stopping within 15 mins of 8AM on Monday
        stoppingAtVelopark.removeIf(stop -> {
            ServiceTime arrivalTime = stop.getArrivalTime();
            return arrivalTime.asLocalTime().isAfter(LocalTime.of(7,59)) &&
                    arrivalTime.asLocalTime().isBefore(LocalTime.of(8,16));
        });

        assertTrue(stoppingAtVelopark.size()>=1); // at least 1
        assertNotEquals(filteredTrips.size(), stoppingAtVelopark.size());
    }

    private List<StopCall> getStopsFor(Trip trip, IdFor<Station> stationId) {
        return trip.getStops().stream().filter(stopCall -> stopCall.getStation().getId().equals(stationId)).collect(Collectors.toList());
    }

}
