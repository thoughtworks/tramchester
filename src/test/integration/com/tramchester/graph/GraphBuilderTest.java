package com.tramchester.graph;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.Service;
import com.tramchester.domain.TransportDataFromFiles;
import com.tramchester.domain.Trip;
import com.tramchester.graph.Nodes.RouteStationNode;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class GraphBuilderTest {
    private static final int MINUTES_FROM_MIDNIGHT_8AM = 8 * 60;
    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private TransportDataFromFiles transportData;
    public static final String ASH_TO_ECCLES_SVC = "MET:MET4:O:";

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
        transportData = dependencies.get(TransportDataFromFiles.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldHaveCorrectInboundsAtMediaCity() throws UnknownStationException {

        List<TramRelationship> inbounds = calculator.getInboundRouteStationRelationships(
                Stations.MediaCityUK + ASH_TO_ECCLES_SVC);

        List<BoardRelationship> boards = new LinkedList<>();
        List<GoesToRelationship> svcsToMediaCity = new LinkedList<>();
        inbounds.forEach(in -> {
            if (in instanceof BoardRelationship) boards.add((BoardRelationship) in);
            if (in instanceof GoesToRelationship) svcsToMediaCity.add((GoesToRelationship) in);
        });

        assertEquals(1, boards.size());
        assertEquals(13, svcsToMediaCity.size());
    }

    @Test
    public void shouldHaveService69BranchAtHarbourCityAndGoToMediaCityAndBroadway() throws UnknownStationException {
        String svcId = "Serv000069";  // can go to eccles, media city or trafford bar

        List<TramRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.HarbourCity + ASH_TO_ECCLES_SVC);
        checkNumberOfServices(svcId, outbounds, 2);
    }

    @Test
      public void shouldHaveHarbourCityStation() throws UnknownStationException {

        TramNode tramNode = calculator.getStation(Stations.HarbourCity);

        StationNode stationNode = (StationNode) tramNode;
        assertNotNull(stationNode);

        assertEquals(Stations.HarbourCity, stationNode.getId());
        assertFalse(stationNode.isRouteStation());
        assertTrue(stationNode.isStation());
        assertEquals("Harbour City", stationNode.getName());
    }

    @Test
    public void shouldHaveHarbourCityRouteStation() throws UnknownStationException {

        TramNode tramNode = calculator.getRouteStation(Stations.HarbourCity + ASH_TO_ECCLES_SVC);

        RouteStationNode routeStationNode = (RouteStationNode) tramNode;
        assertNotNull(routeStationNode);

        assertEquals(Stations.HarbourCity + ASH_TO_ECCLES_SVC, routeStationNode.getId());
        assertFalse(routeStationNode.isStation());
        assertTrue(routeStationNode.isRouteStation());
        assertEquals("Ashton-Under-Lyne - Manchester - Eccles", routeStationNode.getRouteName());
        assertEquals(ASH_TO_ECCLES_SVC, routeStationNode.getRouteId());
    }


    // this test is data specific and could fail if change to routes happen
    @Test
    public void shouldValidateGraphRepresentationMatchesTransportData() throws UnknownStationException {
        String svcId = "Serv000070";

        List<TramRelationship> outbounds =
                calculator.getOutboundRouteStationRelationships(Stations.VeloPark + ASH_TO_ECCLES_SVC);
        // check on departs relationship & services
        List<TramRelationship> departs = new LinkedList<>();
        List<GoesToRelationship> svcsFromVelopark = new LinkedList<>();
        outbounds.forEach(out -> {
            if (out instanceof DepartRelationship) departs.add(out);
            if (out instanceof GoesToRelationship) svcsFromVelopark.add((GoesToRelationship) out);
        });

        assertEquals(1, departs.size()); // one way to get off the tram
        assertEquals(outbounds.size()-1, (svcsFromVelopark.size())); // rest should be tram services

        // check particular svc is present, we want one that calls at mediacity, currently: 63,65,66,67 or 69
        checkNumberOfServices(svcId, outbounds, 1);

        GoesToRelationship svcFromVelopark = null;
        for(GoesToRelationship svc : svcsFromVelopark) {
            if (svc.getService().equals(svcId)) {
                svcFromVelopark  = svc;
            }
        }
        assertNotNull(svcFromVelopark);

        Service rawService = transportData.getService(svcId);

        // number of times tram runs should match up with number of trips from velopark
        List<Trip> trips = rawService.getTrips();
        List<Trip> callingTrips = new LinkedList<>();
        trips.forEach(trip -> trip.getStops().forEach(stop -> {
            if (stop.getStation().getId().equals(Stations.VeloPark)) callingTrips.add(trip);
        }));
        // create list of calling not matching the graph (could compare sizes but makes diagnosis of issues hard)
        List<Trip> notInGraph = new LinkedList<>();
        int[] timesTramRuns = svcFromVelopark.getTimesTramRuns();
        callingTrips.forEach(trip -> {
            int min = trip.getStop(Stations.VeloPark).getMinutesFromMidnight();
            if (Arrays.binarySearch(timesTramRuns, min)<0) {
                notInGraph.add(trip);
            }
        });
        assertEquals(0, notInGraph.size());

        // check at least one of the services calls at media city
        List<Trip> callsAtMediaCity = new LinkedList<>();
        trips.forEach(trip -> trip.getStops().forEach(stop -> {
            if (stop.getStation().getId().equals(Stations.MediaCityUK) ) callsAtMediaCity.add(trip);
        }));

        assertTrue(callsAtMediaCity.size()>0);
    }



    @Test
    public void shouldReportServicesAtHarbourCityWithTimes() throws UnknownStationException {

        List<TramRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.HarbourCity + ASH_TO_ECCLES_SVC);
        reportServices(outbounds);
    }

    @Test
    public void shouldReportServicesCorrectlyAtVeloparkTimes() throws UnknownStationException {

        List<TramRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.VeloPark + ASH_TO_ECCLES_SVC);
        reportServices(outbounds);
    }

    private void reportServices(List<TramRelationship> outbounds) {
        outbounds.forEach(outbound -> {
            if (outbound.isGoesTo()) {
                GoesToRelationship goesToRelationship = (GoesToRelationship) outbound;
                int[] runsAt = goesToRelationship.getTimesTramRuns();
                assertTrue(runsAt.length >0 );
                System.out.print(String.format("%s (%s): ", goesToRelationship.getService(), goesToRelationship.getDest()));
                display(runsAt);
                boolean[] days = goesToRelationship.getDaysTramRuns();
                System.out.println();
                display(days);
                System.out.println();
            }
        });
    }

    private void display(boolean[] days) {
        for(boolean runs : days) {
            System.out.print(" " + (runs?"Y":"N"));
        }
    }

    private void display(int[] runsAt) {
        for (int i : runsAt) {
            System.out.print(" " + i);
        }
    }

    @Test
    public void shouldHaveCorrectGraphRelationshipsFromVeloparkNodeMonday8Am() throws UnknownStationException {

        List<TramRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.VeloPark + ASH_TO_ECCLES_SVC);

        List<GoesToRelationship> svcsFromVelopark = new LinkedList<>();
        outbounds.forEach(out -> {
            if (out instanceof GoesToRelationship) svcsFromVelopark.add((GoesToRelationship) out);

        });
        // filter by day and then direction/route
        svcsFromVelopark.removeIf(svc -> !svc.getDaysTramRuns()[0]); // monday
        svcsFromVelopark.removeIf(svc -> !transportData.getService(svc.getService()).getRouteId().equals(ASH_TO_ECCLES_SVC));

        assertEquals(5, svcsFromVelopark.size());

        svcsFromVelopark.removeIf(svc -> {
            for (int mins : svc.getTimesTramRuns()) {
                if ((mins>=MINUTES_FROM_MIDNIGHT_8AM) && (mins-MINUTES_FROM_MIDNIGHT_8AM<=15)) return false;
            }
            return true;
        });

        assertEquals(1, svcsFromVelopark.size()); // one service calls mondays at this time, 59

    }

    private void checkNumberOfServices(final String svcId, List<TramRelationship> outbounds, int num) {
        outbounds.removeIf(new Predicate<TramRelationship>() {
            @Override
            public boolean test(TramRelationship svc) {
                if (!(svc instanceof GoesToRelationship)) return true;
                return !((GoesToRelationship)svc).getService().equals(svcId);
            }
        });
        assertEquals(num, outbounds.size());
    }
}
