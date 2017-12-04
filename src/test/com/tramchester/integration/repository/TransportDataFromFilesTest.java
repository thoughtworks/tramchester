package com.tramchester.integration.repository;


import com.tramchester.Dependencies;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.domain.Platform;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.RouteCodesForTesting;
import com.tramchester.integration.Stations;
import com.tramchester.repository.TransportDataFromFiles;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TransportDataFromFilesTest {
    public static final TimeWindow MINUTES_FROM_MIDNIGHT_8AM = new TimeWindow(8 * 60, 45);
    public static final List<String> ashtonRoutes = Arrays.asList(new String[]{RouteCodesForTesting.ASH_TO_ECCLES});

    private static Dependencies dependencies;

    private TransportDataFromFiles transportData;
    // use JourneyPlannerResourceTest.shouldFindRouteDeansgateToVictoria to find svc id
    private final String svcDeansgateToVic = "Serv000940";
    // use JourneyPlannerResourceTest.shouldFindEndOfDayThreeStageJourney to find svc id
    private String svcShawAndCrompton = "Serv000916";

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
        assertEquals("Ashton-under-Lyne - MediaCityUK - Eccles", result.getName());
        assertEquals("MET",result.getAgency());
        assertEquals("MET:   E:O:",result.getId());
        assertTrue(result.isTram());

        Set<String> headsigns = result.getHeadsigns();
        assertEquals(3, headsigns.size());
        assertTrue(headsigns.contains("Eccles"));
    }

    @Test
    public void shouldGetTramRoutes() {
        Collection<Route> results = transportData.getRoutes();
        long tramRoutes = results.stream().filter(route -> route.getAgency().equals("MET")).count();
        assertEquals(16, tramRoutes);
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
                .map(svc -> svc.getTrips())
                .flatMap(svcTrips -> svcTrips.stream())
                .collect(Collectors.toList());

        // find trips calling at Velo
        trips.removeIf(trip -> !trip.travelsBetween(Stations.Ashton.getId(), Stations.VeloPark.getId(), MINUTES_FROM_MIDNIGHT_8AM));
        assertFalse(trips.isEmpty());

        List<String> callingServices = trips.stream()
                .map(trip -> trip.getServiceId())
                .collect(Collectors.toList());

        // find one service id from trips
        String callingService = callingServices.get(0);

        // check can now getPlatformById service
        Service velopark8AMSvc = transportData.getServiceById(callingService);

        assertTrue(ashtonRoutes.contains(velopark8AMSvc.getRouteId()));

        // now check can getPlatformById trips using times instead
        Optional<ServiceTime> tripsByTime = transportData.getFirstServiceTime(velopark8AMSvc.getServiceId(),
                Stations.Ashton, Stations.VeloPark, MINUTES_FROM_MIDNIGHT_8AM);
        assertTrue(tripsByTime.isPresent());
    }

    @Test
    public void shouldGetAreas() {
        List<AreaDTO> results = transportData.getAreas();
        assertTrue(results.size() > 0 );
        AreaDTO area = new AreaDTO("Altrincham");
        assertTrue(results.contains(area));
        // only once
        long count = results.stream().filter(item -> item.equals(area)).count();
        assertEquals(1, count);
    }

    @Test
    public void shouldGetTripsAfter() {
        // use JourneyPlannerResourceTest.shouldFindRouteDeansgateToVictoria
        Service svc = transportData.getServiceById(svcDeansgateToVic);
        Optional<Trip> trips = svc.getFirstTripAfter(Stations.Deansgate.getId(), Stations.Victoria.getId(),
                new TimeWindow((23 * 60) +41,30));
        assertTrue(trips.isPresent());
    }

    @Test
    public void shouldGetTripsCrossingMidnight() {
        // use JourneyPlannerResourceTest.shouldFindRouteVicToShawAndCrompton to find svc Id
        Service svc = transportData.getServiceById(svcShawAndCrompton);
        Optional<Trip> trips = svc.getFirstTripAfter(Stations.Victoria.getId(), Stations.ShawAndCrompton.getId(),
                new TimeWindow(((23 * 60) + 41), 30));
        assertTrue(trips.isPresent());
    }

    @Test
    public void shouldGetTripCrossingMidnight() {
        // use JourneyPlannerResourceTest.shouldFindRouteVicToShawAndCrompton to find svc Id
        Service svc = transportData.getServiceById(svcShawAndCrompton);
        Optional<Trip> trips = svc.getFirstTripAfter(Stations.Victoria.getId(), Stations.ShawAndCrompton.getId(),
                new TimeWindow(((23 * 60) + 41), 30));
        assertTrue(trips.isPresent());
    }

    @Test
    public void shouldGetStation() {
        Optional<Station> result = transportData.getStation(Stations.Altrincham.getId());
        assertTrue(result.isPresent());
        assertEquals("Altrincham", result.get().getName());
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
        for (Stop stop : trip.getStops()) {
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

        List<Service> veloToPiccadily = new LinkedList<>();

        for(Service svc : mondayServices) {
            if (transportData.getFirstServiceTime(svc.getServiceId(),
                    Stations.VeloPark, Stations.Piccadilly, MINUTES_FROM_MIDNIGHT_8AM).isPresent()) {
                veloToPiccadily.add(svc);
            }
        }

        assertTrue(veloToPiccadily.size() > 0);
    }

    @Test
    public void shouldHaveConsistencyOfRouteAndTripAndServiceIds() {
        Collection<Route> allRoutes = transportData.getRoutes();
        List<Integer> svcSizes = new LinkedList<>();

        allRoutes.forEach(route -> svcSizes.add(route.getServices().size()));

        int allSvcs = svcSizes.stream().reduce(0, (total,current) -> total+current);

        assertEquals(allSvcs, allServices.size());

        List<Station> allsStations = transportData.getStations();

        Set<Trip> allTrips = new HashSet<>();
        allsStations.forEach(station -> allTrips.addAll(transportData.getTripsFor(station.getId())));

        Set<String> tripServicesId = new HashSet<>();
        allTrips.forEach(trip -> tripServicesId.add(trip.getServiceId()));

        assertEquals(allSvcs, tripServicesId.size());
    }

    @Test
    public void shouldHaveCorrectDataForTramsCallingAtVeloparkMonday8AM() {
        Set<Trip> origTrips = transportData.getTripsFor(Stations.VeloPark.getId());

        Set<String> mondayAshToManServices = allServices.stream()
                .filter(svc -> svc.getDays().get(DaysOfWeek.Monday))
                .filter(svc -> ashtonRoutes.contains(svc.getRouteId()))
                .map(svc -> svc.getServiceId())
                .collect(Collectors.toSet());

        // reduce the trips to the ones for the right route on the monday by filtering by service ID
        List<Trip> filteredTrips = origTrips.stream().filter(trip -> mondayAshToManServices.contains(trip.getServiceId())).
                collect(Collectors.toList());

        assertTrue(filteredTrips.size()>0);

        // find the stops, invariant is now that each trip ought to contain a velopark stop
        List<Stop> stoppingAtVelopark = filteredTrips.stream()
                .filter(trip -> mondayAshToManServices.contains(trip.getServiceId()))
                .map(trip -> trip.getStopsFor(Stations.VeloPark.getId()))
                .flatMap(stops -> stops.stream())
                .collect(Collectors.toList());

        assertEquals(filteredTrips.size(), stoppingAtVelopark.size());

        // finally check there are trams stopping within 15 mins of 8AM on Monday
        stoppingAtVelopark.removeIf(stop -> {
            int mins = stop.getArriveMinsFromMidnight();
            int expected = MINUTES_FROM_MIDNIGHT_8AM.minsFromMidnight();
            return !((mins>=expected) && (mins-expected<=15));
        });

        assertTrue(stoppingAtVelopark.size()>=1); // at least 1
        assertNotEquals(filteredTrips.size(), stoppingAtVelopark.size());
    }

}
