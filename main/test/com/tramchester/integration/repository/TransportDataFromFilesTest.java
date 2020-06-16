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
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

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
    private static final List<String> ashtonRoutes = Collections.singletonList(RoutesForTesting.ASH_TO_ECCLES.getId());

    private static Dependencies dependencies;

    private TransportDataFromFiles transportData;
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
        transportData = dependencies.get(TransportDataFromFiles.class);
        allServices = transportData.getServices();
    }

    @Test
    void shouldGetFeedInfo() {
        FeedInfo result = transportData.getFeedInfo();
        assertEquals("http://www.tfgm.com", result.getPublisherUrl());
    }

    @Test
    void shouldGetRoute() {
        Route result = transportData.getRoute(RoutesForTesting.ASH_TO_ECCLES.getId());
        assertEquals("Ashton-under-Lyne - Manchester - Eccles", result.getName());
        assertEquals(TestEnv.MetAgency(),result.getAgency());
        assertEquals("MET:   3:I:",result.getId());
        assertTrue(result.isTram());

        Set<String> headsigns = result.getHeadsigns();
        assertEquals(2, headsigns.size(), "expected headsigns");
        assertTrue(headsigns.contains("Eccles"));
    }

    @Test
    void shouldGetTramRoutes() {
        Collection<Route> results = transportData.getRoutes();
        long tramRoutes = results.stream().filter(route -> route.getAgency().equals(TestEnv.MetAgency())).count();

        // lockdown 14->12
        assertEquals(14, tramRoutes);
    }

    @Test
    void shouldGetServicesByDate() {
        LocalDate nextSaturday = TestEnv.nextSaturday();
        TramServiceDate date = new TramServiceDate(nextSaturday);
        Set<Service> results = transportData.getServicesOnDate(date);

        assertFalse(results.isEmpty());
        long onCorrectDate = results.stream().filter(svc -> svc.operatesOn(nextSaturday)).count();
        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        LocalDate noTramsDate = transportData.getFeedInfo().validUntil().plusMonths(12);
        results = transportData.getServicesOnDate(new TramServiceDate(noTramsDate));
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldThrowOnMissingSvc() {
        try {
            transportData.getServiceById("doesnotExist");
            fail("Should have thrown");
        } catch (NoSuchElementException expected) {
            // no-op expected
        }
    }

    @Test
    void shouldHaveSundayServicesFromCornbrook() {
        LocalDate nextSunday = TestEnv.nextSunday();

        Set<Service> sundayServices = transportData.getServicesOnDate(new TramServiceDate(nextSunday));

        Set<String> sundayServiceIds = sundayServices.stream().map(Service::getId).collect(Collectors.toSet());

        Set<Trip> cornbrookTrips = transportData.getTrips().stream().
                filter(trip -> trip.getStops().callsAt(Stations.Cornbrook)).collect(Collectors.toSet());

        Set<Trip> sundayTrips = cornbrookTrips.stream().filter(trip -> sundayServiceIds.
                contains(trip.getService().getId())).collect(Collectors.toSet());

        assertFalse(sundayTrips.isEmpty());
    }

    @Test
    void shouldHaveServiceEndDatesBeyondNextNDays() {
        LocalDate queryDate = LocalDate.now().plusDays(DAYS_AHEAD);

        Collection<Service> services = transportData.getServices();
        Set<Service> expiringServices = services.stream().
                filter(svc -> {
                    return !svc.operatesOn(queryDate);
                }).collect(Collectors.toSet());
        Set<String> routes = expiringServices.stream().map(Service::getRouteId).collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), expiringServices, routes.toString() + " with expiring svcs " +HasId.asIds(expiringServices));
    }

    @Test
    void shouldHaveServicesRunningAtReasonableTimesNDaysAhead() {

        // temporary 23 -> 22, 6->7
        int latestHour = 22;
        int earlistHour = 7;

        int maxwait = 25;

        for (int day = 0; day < DAYS_AHEAD; day++) {
            LocalDate date = TestEnv.nextTuesday(day);
            TramServiceDate tramServiceDate = new TramServiceDate(date);
            Set<Service> servicesOnDate = transportData.getServicesOnDate(tramServiceDate);

            Set<String> servicesOnDateIds = servicesOnDate.stream().map(Service::getId).collect(Collectors.toSet());
            transportData.getStations().stream().forEach(station -> {
                Set<Trip> callingTripsOnDate = transportData.getTrips().stream().
                        filter(trip -> trip.getStops().callsAt(station)).
                        filter(trip -> servicesOnDateIds.contains(trip.getService().getId())).
                        collect(Collectors.toSet());
                assertFalse(callingTripsOnDate.isEmpty(), String.format("%s %s", date, station));

                Set<String> callingServicesIds = callingTripsOnDate.stream().map(trip -> trip.getService().getId()).collect(Collectors.toSet());

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
        Station station = transportData.getStation(Stations.Altrincham.getId());
        assertEquals("Altrincham", station.getName());

        assertTrue(station.hasPlatforms());
        // only one platform at alty, well according to the timetable anyway....
        assertEquals(1, station.getPlatforms().size());
        Platform platformOne = station.getPlatforms().get(0);
        assertEquals( Stations.Altrincham.getId()+"1", platformOne.getId());
        assertEquals( "1", platformOne.getPlatformNumber());
        assertEquals( "Altrincham platform 1", platformOne.getName());
    }

    @Test
    void shouldHavePlatform() {
        Optional<Platform> result = transportData.getPlatformById(Stations.StPetersSquare.getId()+"3");
        assertTrue(result.isPresent());
        Platform platform = result.get();
        assertEquals("St Peter's Square platform 3", platform.getName());
        assertEquals(Stations.StPetersSquare.getId()+"3", platform.getId());
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

        Set<String> tripIdsFromSvcs = transportData.getServices().stream().map(Service::getTrips).
                flatMap(Collection::stream).
                map(Trip::getId).collect(Collectors.toSet());
        assertEquals(tripsSize, tripIdsFromSvcs.size());

        Set<String> tripServicesId = new HashSet<>();
        allTrips.forEach(trip -> tripServicesId.add(trip.getService().getId()));
        assertEquals(allSvcs, tripServicesId.size());
    }

    @Test
    void shouldLoadExceptionalDates() {
        Set<String> servicesToLoad = allServices.stream().map(Service::getId).collect(Collectors.toSet());

        TransportDataReaderFactory dataReaderFactory = dependencies.get(TransportDataReaderFactory.class);
        TransportDataReader transportDataReader = dataReaderFactory.getForLoader();
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

        Set<String> toMediaCity = allTrips.stream().
                filter(trip -> trip.getStops().callsAt(Stations.Cornbrook)).
                filter(trip -> trip.getStops().callsAt(Stations.MediaCityUK)).
                filter(trip -> trip.getRoute().getId().equals(RoutesForTesting.ASH_TO_ECCLES.getId())).
                map(trip -> trip.getService().getId()).collect(Collectors.toSet());

        Set<Service> services = toMediaCity.stream().
                map(svc->transportData.getServiceById(svc)).collect(Collectors.toSet());

        LocalDate nextTuesday = TestEnv.nextTuesday(0);

        Set<Service> onDay = services.stream().
                filter(service -> service.operatesOn(nextTuesday)).
                collect(Collectors.toSet());

        TramTime time = TramTime.of(12, 0);

        Set<Service> onTime = onDay.stream().filter(svc -> svc.latestDepartTime().isAfter(time) && svc.earliestDepartTime().isBefore(time)).collect(Collectors.toSet());

        assertFalse(onTime.isEmpty()); // at least one service (likely is just one)
    }

    @Test
    void shouldHaveCorrectDataForTramsCallingAtVeloparkMonday8AM() {
        Set<Trip> origTrips = getTripsFor(transportData.getTrips(), Stations.VeloPark);

        LocalDate aMonday = TestEnv.nextTuesday(0).minusDays(1);
        assertEquals(DayOfWeek.MONDAY, aMonday.getDayOfWeek());

        // TOOD Due to exception dates makes no sense to use getDays
        Set<String> mondayAshToManServices = allServices.stream()
                .filter(svc -> svc.operatesOn(aMonday))
                .filter(svc -> ashtonRoutes.contains(svc.getRouteId()))
                .map(Service::getId)
                .collect(Collectors.toSet());

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
            TramTime arrivalTime = stop.getArrivalTime();
            return arrivalTime.asLocalTime().isAfter(LocalTime.of(7,59)) &&
                    arrivalTime.asLocalTime().isBefore(LocalTime.of(8,16));
        });

        assertTrue(stoppingAtVelopark.size()>=1); // at least 1
        assertNotEquals(filteredTrips.size(), stoppingAtVelopark.size());
    }

    private List<StopCall> getStopsFor(Trip trip, String stationId) {
        return trip.getStops().stream().filter(stopCall -> stopCall.getStation().getId().equals(stationId)).collect(Collectors.toList());
    }

}
