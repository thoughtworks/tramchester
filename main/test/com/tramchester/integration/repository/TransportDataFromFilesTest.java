package com.tramchester.integration.repository;


import com.tramchester.Dependencies;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.domain.time.DaysOfWeek;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static org.junit.Assert.*;

public class TransportDataFromFilesTest {
    private static final List<String> ashtonRoutes = Collections.singletonList(RoutesForTesting.ASH_TO_ECCLES.getId());

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
        Route result = transportData.getRoute(RoutesForTesting.ASH_TO_ECCLES.getId());
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

        // TODO lockdown 14->12
        assertEquals(12, tramRoutes);
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
            transportData.getStations().stream().forEach(station -> {
                Set<Trip> callingTripsOnDate = transportData.getTrips().stream().
                        filter(trip -> trip.callsAt(station.getId())).
                        filter(trip -> servicesOnDateIds.contains(trip.getService().getId())).
                        collect(Collectors.toSet());
                assertFalse(String.format("%s %s", date, station), callingTripsOnDate.isEmpty());

                Set<String> callingServicesIds = callingTripsOnDate.stream().map(trip -> trip.getService().getId()).collect(Collectors.toSet());

                for (int hour = earlistHour; hour < latestHour; hour++) {
                    TramTime tramTime = TramTime.of(hour,0);
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

        int allSvcs = svcSizes.stream().reduce(0, Integer::sum);

        assertEquals(allSvcs, allServices.size());

        Set<Station> allsStations = transportData.getStations();

        Set<Trip> allTrips = new HashSet<>();
        allsStations.forEach(station -> allTrips.addAll(getTripsFor(transportData.getTrips(), station.getId())));

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
        Set<Trip> allTrips = getTripsFor(transportData.getTrips(), Stations.Cornbrook.getId());

        Set<String> toMediaCity = allTrips.stream().
                filter(trip -> trip.callsAt(Stations.Cornbrook.getId())).
                filter(trip -> trip.callsAt(Stations.MediaCityUK.getId())).
                filter(trip -> trip.getRoute().getId().equals(RoutesForTesting.ASH_TO_ECCLES.getId())).
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

    // TODO Lockdown
    @Ignore("Lockdown, no trams at this time currently")
    @Test
    public void shouldHaveCorrectDataForTramsCallingAtVeloparkMonday8AM() {
        Set<Trip> origTrips = getTripsFor(transportData.getTrips(), Stations.VeloPark.getId());

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
