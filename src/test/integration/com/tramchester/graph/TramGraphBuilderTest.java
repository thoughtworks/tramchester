package com.tramchester.graph;

import com.tramchester.*;
import com.tramchester.dataimport.ErrorCount;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.domain.Service;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.exceptions.UnknownStationException;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.domain.Trip;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.BoardRelationship;
import com.tramchester.graph.Relationships.DepartRelationship;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TramGraphBuilderTest {
    private static final Logger logger = LoggerFactory.getLogger(TramGraphBuilderTest.class);
    private static final int MINUTES_FROM_MIDNIGHT_8AM = 8 * 60;
    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private TransportDataFromFiles transportData;
    private GraphDatabaseService service;
    private Transaction transaction;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
        transportData = dependencies.get(TransportDataFromFiles.class);
        service = dependencies.get(GraphDatabaseService.class);
        transaction = service.beginTx();
    }

    @After
    public void afterEachTestRuns() {
        transaction.close();
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

//    @Test
//    public void shouldHaveZeroErrorCountOnImport() {
//        ErrorCount errorCount = dependencies.get(ErrorCount.class);
//        assertTrue(errorCount.noErrors());
//    }

    @Test
    @Category({ClosureTest.class})
    @Ignore("summer 2016 closure")
    public void shouldHaveCorrectInboundsAtMediaCity() throws TramchesterException {

        List<TransportRelationship> inbounds = calculator.getInboundRouteStationRelationships(
                Stations.MediaCityUK );
                        // this route not present during summer 2016 closure
                        // + RouteCodes.PICC_TO_ECCLES);

        List<BoardRelationship> boards = new LinkedList<>();
        List<TramGoesToRelationship> svcsToMediaCity = new LinkedList<>();
        inbounds.forEach(in -> {
            if (in instanceof BoardRelationship) boards.add((BoardRelationship) in);
            if (in instanceof TramGoesToRelationship) svcsToMediaCity.add((TramGoesToRelationship) in);
        });

        assertEquals(1, boards.size());
        //assertEquals(4, svcsToMediaCity.size());
        // 8 during the clousures
        assertEquals(8, svcsToMediaCity.size());
    }

    @Test
    public void shouldHaveHarbourCityStation() throws UnknownStationException {

        TramNode tramNode = calculator.getStation(Stations.HarbourCity.getId());

        StationNode stationNode = (StationNode) tramNode;
        assertNotNull(stationNode);

        assertEquals(Stations.HarbourCity.getId(), stationNode.getId());
        assertFalse(stationNode.isRouteStation());
        assertTrue(stationNode.isStation());
        assertEquals("Harbour City", stationNode.getName());
    }

    @Test
    @Category({ClosureTest.class})
    @Ignore("summer 2016 closure")
    public void shouldHaveHarbourCityRouteStation() throws UnknownStationException {
//        TramNode tramNode = calculator.getRouteStation(Stations.HarbourCity + RouteCodes.PICC_TO_ECCLES);
//
//        RouteStationNode routeStationNode = (RouteStationNode) tramNode;
//        assertNotNull(routeStationNode);
//
//        assertEquals(Stations.HarbourCity + RouteCodes.PICC_TO_ECCLES, routeStationNode.getId());
//        assertFalse(routeStationNode.isStation());
//        assertTrue(routeStationNode.isRouteStation());
//
//        // for data as of 2nd June 2016 this is shown as "Deansgate-Castlefield - MediaCityUK"
//        // this is an known (i.e. tfgm know) issue with the data
//        //assertEquals("Piccadilly - MediaCityUK - Eccles", routeStationNode.getRouteName());
//        assertEquals("Deansgate-Castlefield - MediaCityUK", routeStationNode.getRouteName());
//
//        assertEquals(RouteCodes.PICC_TO_ECCLES, routeStationNode.getRouteId());
    }

    @Test
    @Category({ClosureTest.class})
    @Ignore("summer 2016 closure")
    public void shouldReproduceIssueWithDeansgateToVictoriaTrams() throws TramchesterException {
        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(
                Stations.Deansgate.getId() );
                        //+ RouteCodes.EAST_DIDS_TO_BURY);

        List<String> deansAndNext = Arrays.asList(Stations.Deansgate.getId(), Stations.MarketStreet.getId());

        outbounds.stream().filter(out->out.isGoesTo()).forEach(out -> {
                    TramGoesToRelationship goesTo = (TramGoesToRelationship) out;
                    String svcId = goesTo.getService();
                    Service svc = transportData.getServiceById(svcId);
                    Set<Trip> trips = svc.getTrips();
                    List<Trip> tripsThatCall = trips.stream().filter(trip -> trip.getStops().stream().
                            map(stop -> stop.getStation().getId()).
                                collect(Collectors.toList()).
                                containsAll(deansAndNext)).
                            collect(Collectors.toList());
                    int[] timesTramRuns = goesTo.getTimesTramRuns();
                    // number of outbounds from should match calling trip from the data
                    assertEquals(svcId, tripsThatCall.size(), timesTramRuns.length);

                    List<Integer> times = tripsThatCall.stream().
                            map(trip -> trip.getStopsFor(Stations.Deansgate.getId())).
                            flatMap(stops -> stops.stream()).
                            map(stop -> stop.getDepartureMinFromMidnight()).
                            collect(Collectors.toList());
                    assertEquals(svcId, times.size(), timesTramRuns.length);

                    for (int timesTramRun : timesTramRuns) {
                        assertTrue(svcId + " " + timesTramRun, times.contains(timesTramRun));
                    }

                });
    }

    // this test is data specific and could fail if change to routes happen
    @Ignore("No service direct from velo to media city during st peters square closure")
    @Test
    public void shouldValidateGraphRepresentationMatchesTransportData() throws TramchesterException {
        String svcId = "Serv001180";

        List<TransportRelationship> outbounds =
                calculator.getOutboundRouteStationRelationships(Stations.VeloPark.toString() + RouteCodes.ASH_TO_BURY);
        // check on departs relationship & services
        List<TransportRelationship> departs = new LinkedList<>();
        List<TramGoesToRelationship> svcsFromVelopark = new LinkedList<>();
        outbounds.forEach(out -> {
            if (out instanceof DepartRelationship) departs.add(out);
            if (out instanceof TramGoesToRelationship) svcsFromVelopark.add((TramGoesToRelationship) out);
        });

        assertEquals(1, departs.size()); // one way to get off the tram
        assertEquals(outbounds.size()-1, (svcsFromVelopark.size())); // rest should be tram services

        // check particular svc is present, we want one that calls at mediacity, currently: 63,65,66,67 or 69
        checkNumberOfServices(svcId, outbounds, 1);

        TramGoesToRelationship svcFromVelopark = null;
        for(TramGoesToRelationship svc : svcsFromVelopark) {
            if (svc.getService().equals(svcId)) {
                svcFromVelopark  = svc;
            }
        }
        assertNotNull(svcFromVelopark);

        Service rawService = transportData.getServiceById(svcId);

        // number of times tram runs should match up with number of trips from velopark
        Set<Trip> trips = rawService.getTrips();
        List<Trip> callingTrips = new LinkedList<>();
        trips.forEach(trip -> trip.getStops().forEach(stop -> {
            if (stop.getStation().getId().equals(Stations.VeloPark)) callingTrips.add(trip);
        }));
        // create list of calling not matching the graph (could compare sizes but makes diagnosis of issues hard)
        List<Trip> notInGraph = new LinkedList<>();
        int[] timesTramRuns = svcFromVelopark.getTimesTramRuns();
        callingTrips.forEach(trip -> {
            trip.getStopsFor(Stations.VeloPark.getId()).forEach(stop -> {
                        if (Arrays.binarySearch(timesTramRuns, stop.getDepartureMinFromMidnight()) < 0) {
                            notInGraph.add(trip);
                        }
                    });
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
    @Category({ClosureTest.class})
    @Ignore("summer 2016 closure")
    public void shouldReportServicesAtHarbourCityWithTimes() throws TramchesterException {

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.HarbourCity.getId());
                //+ RouteCodes.PICC_TO_ECCLES);
        reportServices(outbounds);
    }

    @Test
    public void shouldReportServicesCorrectlyAtVeloparkTimes() throws TramchesterException {

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(
                Stations.VeloPark.getId() + RouteCodes.ASH_TO_BURY);
        reportServices(outbounds);
    }

    private void reportServices(List<TransportRelationship> outbounds) {
        outbounds.forEach(outbound -> {
            if (outbound.isGoesTo()) {
                TramGoesToRelationship tramGoesToRelationship = (TramGoesToRelationship) outbound;
                int[] runsAt = tramGoesToRelationship.getTimesTramRuns();
                assertTrue(runsAt.length >0 );
                logger.info(String.format("%s (%s): ", tramGoesToRelationship.getService(), tramGoesToRelationship.getDest()));
                logger.info(display(runsAt));
                boolean[] days = tramGoesToRelationship.getDaysTramRuns();
                logger.info(display(days));
            }
        });
    }

    private String display(boolean[] days) {
        StringBuilder builder = new StringBuilder();
        for(boolean runs : days) {
            builder.append(" " + (runs?"Y":"N"));
        }
        return builder.toString();
    }

    private String display(int[] runsAt) {
        StringBuilder builder = new StringBuilder();
        for (int i : runsAt) {
            builder.append(" " + i);
        }
        return builder.toString();
    }

    @Test
    public void shouldHaveCorrectGraphRelationshipsFromVeloparkNodeMonday8Am() throws TramchesterException {

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(
                Stations.VeloPark.getId() + RouteCodes.ASH_TO_BURY);

        List<TramGoesToRelationship> svcsFromVelopark = new LinkedList<>();
        outbounds.forEach(out -> {
            if (out instanceof TramGoesToRelationship) svcsFromVelopark.add((TramGoesToRelationship) out);
        });
        // filter by day and then direction/route
        assertTrue(!svcsFromVelopark.isEmpty());
        svcsFromVelopark.removeIf(svc -> !svc.getDaysTramRuns()[0]); // monday
        assertTrue(!svcsFromVelopark.isEmpty());
        svcsFromVelopark.removeIf(svc -> !transportData.getServiceById(
                svc.getService()).getRouteId().equals(RouteCodes.ASH_TO_BURY));
        assertTrue(!svcsFromVelopark.isEmpty());

        assertTrue(svcsFromVelopark.size() >=1 );

        svcsFromVelopark.removeIf(svc -> {
            for (int mins : svc.getTimesTramRuns()) {
                if ((mins>=MINUTES_FROM_MIDNIGHT_8AM) && (mins-MINUTES_FROM_MIDNIGHT_8AM<=15)) return false;
            }
            return true;
        });

        assertTrue(svcsFromVelopark.size() >=1 ); // at least one service calls mondays at this time, 59

    }

    private void checkNumberOfServices(final String svcId, List<TransportRelationship> outbounds, int num) {
        outbounds.removeIf(svc -> {
            if (!(svc instanceof TramGoesToRelationship)) return true;
            return !((TramGoesToRelationship)svc).getService().equals(svcId);
        });
        assertEquals(num, outbounds.size());
    }
}
