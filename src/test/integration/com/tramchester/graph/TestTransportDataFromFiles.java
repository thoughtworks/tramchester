package com.tramchester.graph;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestTransportDataFromFiles {
    public static final String ASH_TO_MANCHESTER = "MET:MET3:O:";
    public static final int MINUTES_FROM_MIDNIGHT_8AM = 8 * 60;

    private static Dependencies dependencies;

    private TransportDataFromFiles transportData;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTestConfig());
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        transportData = dependencies.get(TransportDataFromFiles.class);
    }

    @Test
    public void shouldGetFeedInfo() {
        FeedInfo result = transportData.getFeedInfo();
        assertEquals("http://www.tfgm.com", result.getPublisherUrl());
    }

    @Test
    public void shouldGetRoute() {
        Route result = transportData.getRoute(ASH_TO_MANCHESTER);
        assertEquals("Ashton - Manchester - Rochdale", result.getName());
    }

    @Test
    public void shouldGetRoutes() {
        Collection<Route> results = transportData.getRoutes();
        assertEquals(12, results.size());
    }

    @Test
    public void shouldThrowOnMissingSvc() {
        try {
            transportData.getService("doesnotExist");
            fail("Should have thrown");
        } catch (NoSuchElementException expected) {
            // no-op expected
        }
    }

    @Test
    public void shouldGetServicesForLineAndStop() {
        Collection<Service> services = transportData.getServices();

        // select servcies for one route
        services.removeIf(svc -> !svc.getRouteId().equals(ASH_TO_MANCHESTER));
        assertFalse(services.isEmpty());

        List<Trip> trips = services.stream()
                .map(svc -> svc.getTrips())
                .flatMap(svcTrips -> svcTrips.stream())
                .collect(Collectors.toList());

        // find trips calling at Velo
        trips.removeIf(trip -> !trip.travelsBetween(Stations.Ashton, Stations.VeloPark, MINUTES_FROM_MIDNIGHT_8AM));
        assertFalse(trips.isEmpty());

        List<String> callingServices = trips.stream()
                .map(trip -> trip.getServiceId())
                .collect(Collectors.toList());

        // find one service id from trips
        String callingService = callingServices.get(0);

        // check can now get service
        Service velopark8AMSvc = transportData.getService(callingService);

        assertEquals(ASH_TO_MANCHESTER, velopark8AMSvc.getRouteId());

        // now check can get trips using times instead
        List<ServiceTime> tripsByTime = transportData.getTimes(velopark8AMSvc.getServiceId(),
                Stations.Ashton, Stations.VeloPark, MINUTES_FROM_MIDNIGHT_8AM, 10);
        assertFalse(tripsByTime.isEmpty());
    }

    @Test
    public void shouldGetStation() {
        Station result = transportData.getStation(Stations.Altrincham);
        assertEquals("Altrincham", result.getName());
    }

    @Test
    public void shouldHaveAllEndOfLineStations() {
        List<Station> allStations = transportData.getStations();

        List<String> endsOfTheLines = Arrays.asList(Stations.EndOfTheLine);

        List<String> filteredStations = allStations.stream()
                .map(station -> station.getId())
                .filter(station -> endsOfTheLines.contains(station))
                .collect(Collectors.toList());

        assertEquals(Stations.EndOfTheLine.length, filteredStations.size());
    }

    @Test
    public void shouldFindTripsForStation() {
        List<Trip> altyTrips = transportData.getTripsFor(Stations.Altrincham);
        altyTrips.removeIf(trip -> !trip.travelsBetween(Stations.Altrincham, Stations.Piccadily, MINUTES_FROM_MIDNIGHT_8AM));

        assertFalse(altyTrips.isEmpty());
        assertEquals(13, altyTrips.get(0).getStops().size());
    }

    @Test
    public void shouldTestValidityOfCalendarImport() {
        List<Service> mondayServices = new LinkedList<>();
        Collection<Service> svcs = transportData.getServices();
        for(Service svc : svcs) {
            HashMap<DaysOfWeek, Boolean> days = svc.getDays();
            boolean monday = days.get(DaysOfWeek.Monday);
            if (monday) {
                mondayServices.add(svc);
            }
        }

        List<Service> veloToPiccadily = new LinkedList<>();

        for(Service svc : mondayServices) {
            if (transportData.getTimes(svc.getServiceId(),
                    Stations.VeloPark, Stations.Piccadily, MINUTES_FROM_MIDNIGHT_8AM, 10).size()>0) {
                veloToPiccadily.add(svc);
            }
        }

        assertTrue(veloToPiccadily.size() > 0);
    }

    @Test
    public void shouldHaveConsistencyOfRouteAndTripAndServiceIds() {
        Collection<Route> allRoutes = transportData.getRoutes();
        Set<Service> allSvcs = new HashSet<>();
        allRoutes.forEach(route -> allSvcs.addAll(route.getServices()));

        assertEquals(allSvcs.size(), transportData.getServices().size());

        List<Station> allsStations = transportData.getStations();

        Set<Trip> allTrips = new HashSet<>();
        allsStations.forEach(station -> allTrips.addAll(transportData.getTripsFor(station.getId())));

        Set<String> tripServicesId = new HashSet<>();
        allTrips.forEach(trip -> tripServicesId.add(trip.getServiceId()));

        assertEquals(allSvcs.size(), tripServicesId.size());
    }

    @Test
    public void shouldHaveCorrectDataForTramsCallingAtVeloparkMonday8AM() {
        List<Trip> trips = transportData.getTripsFor(Stations.VeloPark);

        Collection<Service> allServices = transportData.getServices();

        Set<String> mondayAshToManServices = allServices.stream()
                .filter(svc -> svc.getDays().get(DaysOfWeek.Monday))
                .filter(svc -> svc.getRouteId().equals(ASH_TO_MANCHESTER))
                .map(svc -> svc.getServiceId())
                .collect(Collectors.toSet());

        // reduce the trips to the ones for the right route on the monday by filtering by service ID
        trips.removeIf(trip -> !mondayAshToManServices.contains(trip.getServiceId()));

        // find the stops, invariant is now that each trip ought to contain a velopark stop
        List<Stop> stoppingAtVelopark = trips.stream()
                .filter(trip -> mondayAshToManServices.contains(trip.getServiceId()))
                .map(trip -> trip.getStop(Stations.VeloPark))
                .collect(Collectors.toList());

        assertEquals(trips.size(), stoppingAtVelopark.size());

        // finally check there are trams stopping within 15 mins of 8AM on Monday
        stoppingAtVelopark.removeIf(stop -> {
            int mins = stop.getMinutesFromMidnight();
            return !((mins>=MINUTES_FROM_MIDNIGHT_8AM) && (mins-MINUTES_FROM_MIDNIGHT_8AM<=15));
        });

        assertTrue(stoppingAtVelopark.size()>=1); // at least 1
        assertNotEquals(trips.size(), stoppingAtVelopark.size());
    }

}
