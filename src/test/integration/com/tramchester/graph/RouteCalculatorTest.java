package com.tramchester.graph;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.*;
import com.tramchester.graph.Relationships.TramRelationship;
import com.tramchester.services.DateTimeService;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class RouteCalculatorTest {

    public static final String ASH_TO_ECCLES_SVC = "MET:MET4:O:";
    public static final int MINUTES_FROM_MIDNIGHT_8AM = 8 * 60;
    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private DateTimeService dateTimeService;
    private TransportDataFromFiles transportData;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
        dateTimeService = dependencies.get(DateTimeService.class);
        transportData = dependencies.get(TransportDataFromFiles.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void testJourneyFromAltyToAirport() throws Exception {
        int minutes = dateTimeService.getMinutesFromMidnight("11:43:00");
        Set<Journey> results = calculator.calculateRoute(Stations.Altrincham, Stations.ManAirport, minutes, DaysOfWeek.Sunday);

        assertEquals(1, results.size());    // results is iterator
        for (Journey result : results) {
            List<Stage> stages = result.getStages();
            assertEquals(2, stages.size());
            Stage firstStage = stages.get(0);
            assertEquals(Stations.Altrincham, firstStage.getFirstStation());
            assertEquals(Stations.TraffordBar, firstStage.getLastStation());
            Stage secondStage = stages.get(1);
            assertEquals(Stations.TraffordBar, secondStage.getFirstStation());
            assertEquals(Stations.ManAirport, secondStage.getLastStation());
        }
    }

    @Test
    public void shouldGetToRouteStopsAtVelopark() throws UnknownStationException {
        List<TramRelationship> boarding = calculator.getOutboundStationRelationships(Stations.VeloPark);
        assertEquals(2, boarding.size());
        assertTrue(boarding.get(0).isBoarding());  // we can get to either platform
        assertTrue(boarding.get(1).isBoarding());
    }

    @Test
    public void shouldGetCorrectNumberOfTripsVeloToMediaCityDirectSaturdayService() {
        String svcId = "Serv000069";

        int minutesFromMidnight = MINUTES_FROM_MIDNIGHT_8AM;
        int numTrips = 10;
        List<ServiceTime> times = transportData.getTimes(svcId, Stations.VeloPark, Stations.MediaCityUK,
                minutesFromMidnight, numTrips);
        assertEquals(numTrips, times.size());
    }

    @Test
    public void shouldTestNumberOfTripsFromVeloToPiccadilyMondayMorning() {
        String svcId = "Serv000059";

        int minutesFromMidnight = MINUTES_FROM_MIDNIGHT_8AM;
        int numTrips = 10;
        List<ServiceTime> times = transportData.getTimes(svcId, Stations.VeloPark, Stations.Piccadily,
                minutesFromMidnight, numTrips);
        assertEquals(numTrips, times.size());

    }

    @Test
    public void shouldTestNumberOfTripsFromVeloToHarbourCityMondayMorning() {
        String svcId = "Serv000059";

        int minutesFromMidnight = MINUTES_FROM_MIDNIGHT_8AM;
        int numTrips = 10;
        List<ServiceTime> times = transportData.getTimes(svcId, Stations.VeloPark, Stations.HarbourCity,
                minutesFromMidnight, numTrips);
        assertEquals(numTrips, times.size());
    }

    @Test
    public void shouldTestNumberOfTripsFromHarbourCityToMediaCityMondayMorning() {
        String svcId = "Serv000067";

        int minutesFromMidnight = MINUTES_FROM_MIDNIGHT_8AM;
        int numTrips = 10;
        List<ServiceTime> times = transportData.getTimes(svcId, Stations.HarbourCity, Stations.MediaCityUK,
                minutesFromMidnight, numTrips);
        assertEquals(numTrips, times.size());
    }

    @Test
    public void shouldGetCorrectNumberOfTripsVeloToPiccadilyWeekdayService() {
        String svcId = "Serv000059";  // velo to piccadily on weekday

        int minutesFromMidnight = MINUTES_FROM_MIDNIGHT_8AM;
        int numTrips = 10;
        List<ServiceTime> times = transportData.getTimes(svcId, Stations.VeloPark, Stations.Piccadily,
                minutesFromMidnight, numTrips);
        assertEquals(numTrips, times.size());
    }

    @Test
    public void shouldGetCorrectNumberOfTripsPiccadilyToMediaCityWeekdayService() {
        String svcId = "Serv000067";  // piccadily to mediacity

        int minutesFromMidnight = MINUTES_FROM_MIDNIGHT_8AM;
        int numTrips = 10;
        List<ServiceTime> times = transportData.getTimes(svcId, Stations.Piccadily, Stations.MediaCityUK,
                minutesFromMidnight, numTrips);
        assertEquals(numTrips, times.size());
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

        List<Service> veloToMediaCity = new LinkedList<>();

        for(Service svc : mondayServices) {
            if (transportData.getTimes(svc.getServiceId(),
                    Stations.VeloPark, Stations.MediaCityUK, MINUTES_FROM_MIDNIGHT_8AM, 10).size()>0) {
                veloToMediaCity.add(svc);
            }
        }

        assertTrue(veloToMediaCity.size()>0);
    }

    @Test
    public void shouldHaveCorrectDataForTramsCallingAtVeloparkMonday8AM() {
        // trips that call
        List<Trip> trips = transportData.getTripsFor(Stations.VeloPark);
        Set<String> serviceIds = new HashSet<>();
        // services for the trips
        trips.forEach(trip -> serviceIds.add(trip.getServiceId()));
        // filter by day and then direction/route
        serviceIds.removeIf(serviceId -> !transportData.getService(serviceId).getDays().get(DaysOfWeek.Monday));
        serviceIds.removeIf(serviceId -> !transportData.getService(serviceId).getRouteId().equals(ASH_TO_ECCLES_SVC));

        // reduce the trips to the ones for the right route on the monday by filtering by service ID
        trips.removeIf(trip -> !serviceIds.contains(trip.getServiceId()));

        // find the stops, invariant is now that each trip ought to contain a velopark stop
        List<Stop> stoppingAtVelopark = new LinkedList<>();
        trips.forEach(trip -> stoppingAtVelopark.add(trip.getStop(Stations.VeloPark)));
        assertEquals(trips.size(), stoppingAtVelopark.size());

        // finally check there are trams stopping within 15 mins of 8AM on Monday
        stoppingAtVelopark.removeIf(stop -> {
            int mins = stop.getMinutesFromMidnight();
            return !((mins>=MINUTES_FROM_MIDNIGHT_8AM) && (mins-MINUTES_FROM_MIDNIGHT_8AM<=15));
        });

        assertEquals(2, stoppingAtVelopark.size());
        assertNotEquals(trips.size(), stoppingAtVelopark.size());
    }



}