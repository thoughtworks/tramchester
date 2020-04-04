package com.tramchester.integration.repository;


import com.tramchester.Dependencies;
import com.tramchester.domain.*;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.domain.time.DaysOfWeek;
import com.tramchester.domain.time.TimeWindow;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.RouteCodesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.testSupport.TestEnv;
import org.junit.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TransportDataFromFilesTest {
    private static final TimeWindow MINUTES_FROM_MIDNIGHT_8AM = new TimeWindow(TramTime.of(8,0), 45);
    private static final List<String> ashtonRoutes = Collections.singletonList(RouteCodesForTesting.ASH_TO_ECCLES);

    private static Dependencies dependencies;

    private TransportDataFromFiles transportData;
    private Collection<Service> allServices;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        transportData = dependencies.get(TransportDataFromFiles.class);
        allServices = transportData.getServices();
    }

    @Test
    public void shouldGetFeedInfo() {
        FeedInfo result = transportData.getFeedInfo();
        assertEquals("http://www.tfgm.com", result.getPublisherUrl());
    }

    @Test
    public void shouldGetRoute() {
        Route result = transportData.getRoute(RouteCodesForTesting.ASH_TO_ECCLES);
        assertEquals("Ashton-under-Lyne - Manchester - Eccles", result.getName());
        assertEquals(TestEnv.MetAgency(),result.getAgency());
        assertEquals("MET:   3:I:",result.getId());
        assertTrue(result.isTram());

        Set<String> headsigns = result.getHeadsigns();
        assertEquals("expected headsigns", 2, headsigns.size());
        assertTrue(headsigns.contains("Eccles"));
    }

    @Test
    public void shouldGetTramRoutes() {
        Collection<Route> results = transportData.getRoutes();
        long tramRoutes = results.stream().filter(route -> route.getAgency().equals(TestEnv.MetAgency())).count();

        assertEquals(14, tramRoutes);
    }

    @Test
    public void shouldGetServicesByDate() {
        LocalDate nextSaturday = TestEnv.nextSaturday();
        TramServiceDate date = new TramServiceDate(nextSaturday);
        Set<Service> results = transportData.getServicesOnDate(date);

        assertFalse(results.isEmpty());
        long saturdays = results.stream().filter(svc -> svc.getDays().get(DaysOfWeek.Saturday)).count();
        assertEquals(results.size(), saturdays);
        long onDate = results.stream().filter(svc -> svc.operatesOn(nextSaturday)).count();
        assertEquals(results.size(), onDate);

        LocalDate noTramsDate = transportData.getFeedInfo().validUntil().plusMonths(12);
        results = transportData.getServicesOnDate(new TramServiceDate(noTramsDate));
        assertTrue(results.isEmpty());
    }

    @Test
    public void shouldThrowOnMissingSvc() {
        try {
            transportData.getServiceById("doesnotExist");
            fail("Should have thrown");
        } catch (NoSuchElementException expected) {
            // no-op expected
        }
    }

    @Test
    public void shouldGetServiceForLineAndStop() {

        // select servcies for one route
        List<Service> filtered = allServices.stream().
                filter(svc -> ashtonRoutes.contains(svc.getRouteId())).
                collect(Collectors.toList());
        assertFalse(filtered.isEmpty());

        List<Trip> trips = filtered.stream()
                .map(Service::getTrips)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // find trips calling at Velo
        trips.removeIf(trip -> !trip.travelsBetween(Stations.Ashton.getId(), Stations.VeloPark.getId(), MINUTES_FROM_MIDNIGHT_8AM));
        assertFalse(trips.isEmpty());

        List<String> callingServices = trips.stream()
                .map(trip -> trip.getService().getId())
                .collect(Collectors.toList());

        // find one service id from trips
        String callingService = callingServices.get(0);

        // check can now getPlatformById service
        Service velopark8AMSvc = transportData.getServiceById(callingService);

        assertTrue(ashtonRoutes.contains(velopark8AMSvc.getRouteId()));

        // now check can getPlatformById trips using times instead
//        Optional<ServiceTime> tripsByTime = transportData.getFirstServiceTime(velopark8AMSvc.getServiceId(),
//                Stations.Ashton, Stations.VeloPark, MINUTES_FROM_MIDNIGHT_8AM);
//        assertTrue(tripsByTime.isPresent());
    }

    @Test
    public void shouldGetAreas() {
        List<AreaDTO> results = transportData.getAreas();
        assertTrue(results.size() > 0 );
        AreaDTO area = new AreaDTO("Manchester Airport");
        assertTrue(results.contains(area));
        // only once
        long count = results.stream().filter(item -> item.equals(area)).count();
        assertEquals(1, count);
    }

    @Test
    public void shouldHaveSundayServicesFromCornbrook() {
        LocalDate nextSunday = TestEnv.nextSunday();

        Set<Service> sundayServices = transportData.getServicesOnDate(new TramServiceDate(nextSunday));

        Set<String> sundayServiceIds = sundayServices.stream().map(Service::getId).collect(Collectors.toSet());

        Set<Trip> cornbrookTrips = transportData.getTrips().stream().
                filter(trip -> trip.callsAt(Stations.Cornbrook.getId())).collect(Collectors.toSet());

        Set<Trip> sundayTrips = cornbrookTrips.stream().filter(trip -> sundayServiceIds.contains(trip.getService().getId())).collect(Collectors.toSet());

        assertFalse(sundayTrips.isEmpty());
    }

    @Test
    public void shouldHaveServicesRunningAtReasonableTimes() {

        // temporary 23 -> 22, 6->7
        int latestHour = 22;
        int earlistHour = 7;

        int maxwait = 25;

        for (int day = 0; day < 7; day++) {
            LocalDate date = TestEnv.nextTuesday(day);
            TramServiceDate tramServiceDate = new TramServiceDate(date);
            Set<Service> servicesOnDate = transportData.getServicesOnDate(tramServiceDate);

            Set<String> servicesOnDateIds = servicesOnDate.stream().map(Service::getId).collect(Collectors.toSet());
            transportData.getStations().stream().filter(station -> checkForNewRouteStationOpen(date, station)).forEach(station -> {
                Set<Trip> callingTripsOnDate = transportData.getTrips().stream().
                        filter(trip -> trip.callsAt(station.getId())).
                        filter(trip -> servicesOnDateIds.contains(trip.getService().getId())).
                        collect(Collectors.toSet());
                assertFalse(String.format("%s %s", date, station), callingTripsOnDate.isEmpty());

                Set<String> callingServicesIds = callingTripsOnDate.stream().map(trip -> trip.getService().getId()).collect(Collectors.toSet());

                for (int hour = earlistHour; hour < latestHour; hour++) {
                    TramTime tramTime = TramTime.of(hour,00);
                    Set<Service> runningAtTime = servicesOnDate.stream().
                            filter(svc -> callingServicesIds.contains(svc.getId())).
                            filter(svc -> tramTime.between(svc.earliestDepartTime(), svc.latestDepartTime())).collect(Collectors.toSet());

                    assertFalse(String.format("%s %s %s", date, tramTime, station.getName()), runningAtTime.isEmpty());

                    Set<StopCall> calling = new HashSet<>();
                    callingTripsOnDate.forEach(trip -> {
                        Set<StopCall> onTime = trip.getStops().stream().
                                filter(stop -> stop.getStation().equals(station)).
                                filter(stop -> tramTime.between(stop.getArrivalTime().minusMinutes(maxwait), stop.getArrivalTime())).
                                collect(Collectors.toSet());
                        calling.addAll(onTime);
                    });
                    assertFalse(String.format("Stops %s %s %s %s", date.getDayOfWeek(), date, tramTime, station.getName()), calling.isEmpty());
                }

            });
        }
    }

    //////
    // Temporary until 22/March/2020
    //////
    private boolean checkForNewRouteStationOpen(LocalDate date, Station station) {
        if (date.isAfter(LocalDate.of(2020,3, 21))) {
            return true;
        }
        Set<String> newRoutesServed = station.getRoutes().stream().
                map(Route::getId).
                filter(RouteCodesForTesting.RouteSeven::contains).
                collect(Collectors.toSet());
        return newRoutesServed.isEmpty();
    }

    @Test
    public void shouldHaveWeekendServicesDeansgateToAshton() {
        Set<Trip> deansgateTrips = transportData.getTripsFor(Stations.Deansgate.getId());

        TimeWindow timeWindow = new TimeWindow(TramTime.of(9,0),30);
        List<String> servicesIds = deansgateTrips.stream().
                filter(trip -> trip.callsAt(Stations.Ashton.getId())).
                filter(trip -> trip.travelsBetween(Stations.Deansgate.getId(),Stations.Ashton.getId(),timeWindow)).
                map(trip->trip.getService().getId()).collect(Collectors.toList());
        assertTrue(servicesIds.size()>0);

        List<Service> sundays = transportData.getServices().stream().
                filter(svc -> servicesIds.contains(svc.getId())).
                filter(svc -> svc.getDays().get(DaysOfWeek.Sunday)).collect(Collectors.toList());
        assertTrue(sundays.size()>0);

        List<Service> saturdays = transportData.getServices().stream().
                filter(svc -> servicesIds.contains(svc.getId())).
                filter(svc -> svc.getDays().get(DaysOfWeek.Saturday)).collect(Collectors.toList());
        assertTrue(saturdays.size()>0);


        List<Trip> sundayTrips = sundays.stream().map(Service::getTrips).flatMap(Collection::stream).collect(Collectors.toList());

        List<Trip> atRequiredTimed = sundayTrips.stream().
                filter(trip -> trip.travelsBetween(Stations.Deansgate.getId(), Stations.Ashton.getId(), timeWindow)).
                collect(Collectors.toList());

        // not date specific
        assertEquals(4, atRequiredTimed.size());
    }

    @Test
    public void shouldHaveAtLeastOnePlatformForEveryStation() {
        Set<Station> stations = transportData.getStations();
        Set<Station> noPlatforms = stations.stream().filter(station -> station.getPlatforms().isEmpty()).collect(Collectors.toSet());
        assertEquals(Collections.emptySet(),noPlatforms);
    }

    @Test
    public void shouldGetStation() {
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
    public void shouldHavePlatform() {
        Optional<Platform> result = transportData.getPlatformById(Stations.StPetersSquare.getId()+"3");
        assertTrue(result.isPresent());
        Platform platform = result.get();
        assertEquals("St Peter's Square platform 3", platform.getName());
        assertEquals(Stations.StPetersSquare.getId()+"3", platform.getId());
    }

    @Test
    public void shouldHaveAllEndOfLineTramStations() {
        List<Station> filteredStations = transportData.getStations().stream()
                .filter(station -> Stations.EndOfTheLine.contains(station)).collect(Collectors.toList());

        assertEquals(Stations.EndOfTheLine.size(), filteredStations.size());
    }

    @Test
    public void shouldFindTripsForTramStation() {
        Set<Trip> altyTrips = transportData.getTripsFor(Stations.Altrincham.getId());
        altyTrips.removeIf(trip -> !trip.travelsBetween(Stations.Altrincham.getId(), Stations.Deansgate.getId(),
                MINUTES_FROM_MIDNIGHT_8AM));

        assertFalse(altyTrips.isEmpty());
        Trip trip = altyTrips.stream().findFirst().get();

        int count = 0;
        for (StopCall stop : trip.getStops()) {
            count++;
            if (stop.getStation().getId().equals(Stations.Deansgate.getId())) {
                break;
            }
        }
        assertEquals(11, count);
    }

    @Test
    public void shouldTestValidityOfCalendarImport() {
        List<Service> mondayServices = new LinkedList<>();

        for(Service svc : allServices) {
            HashMap<DaysOfWeek, Boolean> days = svc.getDays();
            boolean monday = days.get(DaysOfWeek.Monday);
            if (monday) {
                mondayServices.add(svc);
            }
        }
        assertTrue(mondayServices.size()>0);

    }

    @Test
    public void shouldHaveConsistencyOfRouteAndTripAndServiceIds() {
        Collection<Route> allRoutes = transportData.getRoutes();
        List<Integer> svcSizes = new LinkedList<>();

        allRoutes.forEach(route -> svcSizes.add(route.getServices().size()));

        int allSvcs = svcSizes.stream().reduce(0, (total,current) -> total+current);

        assertEquals(allSvcs, allServices.size());

        Set<Station> allsStations = transportData.getStations();

        Set<Trip> allTrips = new HashSet<>();
        allsStations.forEach(station -> allTrips.addAll(transportData.getTripsFor(station.getId())));

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
    public void shouldReproIssueAtMediaCityWithBranchAtCornbrook() {
        Set<Trip> allTrips = transportData.getTripsFor(Stations.Cornbrook.getId());

        Set<String> toMediaCity = allTrips.stream().
                filter(trip -> trip.callsAt(Stations.Cornbrook.getId())).
                filter(trip -> trip.callsAt(Stations.MediaCityUK.getId())).
                filter(trip -> trip.getRoute().getId().equals(RouteCodesForTesting.ASH_TO_ECCLES)).
                map(trip -> trip.getService().getId()).collect(Collectors.toSet());

        Set<Service> services = toMediaCity.stream().
                map(svc->transportData.getServiceById(svc)).collect(Collectors.toSet());

        LocalDate nextTuesday = TestEnv.nextTuesday(0);

        Set<Service> onDay = services.stream().
                filter(service -> service.operatesOn(nextTuesday)).
                filter(service -> service.getDays().get(DaysOfWeek.Tuesday)).
                collect(Collectors.toSet());

        TramTime time = TramTime.of(12, 0);

        Set<Service> onTime = onDay.stream().filter(svc -> svc.latestDepartTime().isAfter(time) && svc.earliestDepartTime().isBefore(time)).collect(Collectors.toSet());

        assertFalse(onTime.isEmpty()); // at least one service (likely is just one)
    }

    @Test
    public void shouldReproIssueStPetersToPomona() {
        TramTime problemTime = TramTime.of(19, 51);
        Set<Trip> allTrips = transportData.getTripsFor(Stations.StPetersSquare.getId());

        TimeWindow timeWindow = new TimeWindow(problemTime, 25);

        Set<Trip> trips = allTrips.stream().
                filter(trip -> trip.callsAt(Stations.Pomona.getId())).
                filter(trip -> trip.getRoute().getId().equals(RouteCodesForTesting.ASH_TO_ECCLES)).
                filter(trip -> trip.earliestDepartTime().isBefore(problemTime) || trip.earliestDepartTime().equals(problemTime)).
                filter(trip -> trip.latestDepartTime().isAfter(problemTime) || trip.latestDepartTime().equals(problemTime)).
                filter(trip -> trip.travelsBetween(Stations.StPetersSquare.getId(),Stations.Pomona.getId(), timeWindow)).
                collect(Collectors.toSet());


        Set<Service> svcs = trips.stream().map(trip -> trip.getService().getId()).
                map(svcId -> transportData.getServiceById(svcId)).
                collect(Collectors.toSet());

        List<String> runTuesday = svcs.stream().
                filter(service -> service.getDays().get(DaysOfWeek.Tuesday)).
                map(service -> service.getId()).
                collect(Collectors.toList());

        List<Trip> runningTrips = trips.stream().filter(trip -> runTuesday.contains(trip.getService().getId())).collect(Collectors.toList());

        assertTrue(runTuesday.size()>0);

    }

    @Test
    public void shouldHaveCorrectDataForTramsCallingAtVeloparkMonday8AM() {
        Set<Trip> origTrips = transportData.getTripsFor(Stations.VeloPark.getId());

        Set<String> mondayAshToManServices = allServices.stream()
                .filter(svc -> svc.getDays().get(DaysOfWeek.Monday))
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
                .map(trip -> trip.getStopsFor(Stations.VeloPark.getId()))
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

}
