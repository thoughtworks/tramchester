package com.tramchester.graph;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
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
    // use TramJourneyPlannerTest.
    private final String svcDeansgateToVic = "Serv001724";

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
    public void shouldGetTramRoutes() {
        Collection<Route> results = transportData.getRoutes();
        long tramRoutes = results.stream().filter(route -> route.getAgency().equals(Route.METROLINK)).count();
        assertEquals(34, tramRoutes); // both old format and new form routes present
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
        Service velopark8AMSvc = transportData.getServiceById(callingService);

        assertEquals(ASH_TO_MANCHESTER, velopark8AMSvc.getRouteId());

        // now check can get trips using times instead
        List<ServiceTime> tripsByTime = transportData.getTimes(velopark8AMSvc.getServiceId(),
                Stations.Ashton, Stations.VeloPark, MINUTES_FROM_MIDNIGHT_8AM, 10);
        assertFalse(tripsByTime.isEmpty());
    }

    @Test
    public void shouldGetTripsAfter() {
        // use TramJourneyPlannerTest.shouldFindRouteDeansgateToVictoria
        Service svc = transportData.getServiceById(svcDeansgateToVic);
        List<Trip> trips = svc.getTripsAfter(Stations.Deansgate, Stations.Victoria, (23 * 60) +41 , 10);
        assertFalse(trips.isEmpty());
    }

    @Test
    public void shouldGetTripCrossingMidnight() {
        // use TramJourneyPlannerTest.shouldFindRouteVicToShawAndCrompton to find svc Id
        Service svc = transportData.getServiceById("Serv001110");
        List<Trip> trips = svc.getTripsAfter(Stations.Victoria, Stations.ShawAndCrompton, (23 * 60) + 44, 10);
        assertFalse(trips.isEmpty());
    }

    @Test
    public void shouldGetStation() {
        Station result = transportData.getStation(Stations.Altrincham);
        assertEquals("Altrincham", result.getName());
    }

    @Test
    public void shouldHaveAllEndOfLineTramStations() {
        List<Station> allStations = transportData.getStations();

        List<String> endsOfTheLines = Arrays.asList(Stations.EndOfTheLine);

        List<String> filteredStations = allStations.stream()
                .map(station -> station.getId())
                .filter(station -> endsOfTheLines.contains(station))
                .collect(Collectors.toList());

        assertEquals(Stations.EndOfTheLine.length, filteredStations.size());
    }

    @Test
    public void shouldFindTripsForTramStation() {
        Set<Trip> altyTrips = transportData.getTripsFor(Stations.Altrincham);
        altyTrips.removeIf(trip -> !trip.travelsBetween(Stations.Altrincham, Stations.Piccadily,
                MINUTES_FROM_MIDNIGHT_8AM));

        assertFalse(altyTrips.isEmpty());
        Trip trip = altyTrips.stream().findFirst().get();

        int count = 0;
        for (Stop stop : trip.getStops()) {
            count++;
            if (stop.getStation().getId().equals(Stations.Piccadily)) {
                break;
            }
        }
        assertEquals(13, count); // this number will change when st peters square re-opens
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
        List<Integer> svcSizes = new LinkedList<>();

        allRoutes.forEach(route -> svcSizes.add(route.getServices().size()));

        int allSvcs = svcSizes.stream().reduce(0, (total,current) -> total+current);

        assertEquals(allSvcs, transportData.getServices().size());

        List<Station> allsStations = transportData.getStations();

        Set<Trip> allTrips = new HashSet<>();
        allsStations.forEach(station -> allTrips.addAll(transportData.getTripsFor(station.getId())));

        Set<String> tripServicesId = new HashSet<>();
        allTrips.forEach(trip -> tripServicesId.add(trip.getServiceId()));

        assertEquals(allSvcs, tripServicesId.size());
    }

    @Test
    public void shouldHaveCorrectDataForTramsCallingAtVeloparkMonday8AM() {
        Set<Trip> trips = transportData.getTripsFor(Stations.VeloPark);

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
                .map(trip -> trip.getStopsFor(Stations.VeloPark))
                .flatMap(stops -> stops.stream())
                .collect(Collectors.toList());

        assertEquals(trips.size(), stoppingAtVelopark.size());

        // finally check there are trams stopping within 15 mins of 8AM on Monday
        stoppingAtVelopark.removeIf(stop -> {
            int mins = stop.getArriveMinsFromMidnight();
            return !((mins>=MINUTES_FROM_MIDNIGHT_8AM) && (mins-MINUTES_FROM_MIDNIGHT_8AM<=15));
        });

        assertTrue(stoppingAtVelopark.size()>=1); // at least 1
        assertNotEquals(trips.size(), stoppingAtVelopark.size());
    }

}
