package com.tramchester.graph;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.RouteCodes;
import com.tramchester.Stations;
import com.tramchester.domain.Service;
import com.tramchester.domain.Trip;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.*;
import com.tramchester.repository.TransportDataFromFiles;
import org.junit.*;
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
    public void shouldHaveCorrectInboundsAtMediaCity() throws TramchesterException {

        List<TransportRelationship> inbounds = calculator.getInboundRouteStationRelationships(
                Stations.MediaCityUK.getId() + RouteCodes.ECCLES_TO_ASH );

        List<BoardRelationship> boards = new LinkedList<>();
        List<TramGoesToRelationship> svcsToMediaCity = new LinkedList<>();
        inbounds.forEach(in -> {
            if (in instanceof BoardRelationship) boards.add((BoardRelationship) in);
            if (in instanceof TramGoesToRelationship) svcsToMediaCity.add((TramGoesToRelationship) in);
        });

        assertEquals(1, boards.size());
        assertEquals(7, svcsToMediaCity.size());
    }

    @Test
    public void shouldHaveHarbourCityStation() {

        TramNode tramNode = calculator.getStation(Stations.HarbourCity.getId());

        StationNode stationNode = (StationNode) tramNode;
        assertNotNull(stationNode);

        assertEquals(Stations.HarbourCity.getId(), stationNode.getId());
        assertFalse(stationNode.isRouteStation());
        assertTrue(stationNode.isStation());
        assertEquals("Harbour City", stationNode.getName());
    }

    @Test
    public void shouldReproduceIssueWithDeansgateToVictoriaTrams() throws TramchesterException {
        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(
                Stations.Deansgate.getId() + RouteCodes.EAST_DIDS_TO_DEANSGATE);

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

    @Test
    public void shouldValidateGraphRepresentationMatchesTransportData() throws TramchesterException {

        String station = Stations.VeloPark.getId();
        String route = RouteCodes.ASH_TO_ECCLES;

        List<TransportRelationship> relationships = calculator.getOutboundRouteStationRelationships(station + route);

        // check on departs relationship & services
        List<TransportRelationship> departs = new LinkedList<>();
        List<GoesToRelationship> outbounds = new LinkedList<>();
        relationships.forEach(relationship -> {
            if (relationship instanceof DepartRelationship) departs.add(relationship);
            if (relationship instanceof TramGoesToRelationship) outbounds.add((TramGoesToRelationship) relationship);
        });

        assertEquals(relationships.size()-1, (outbounds.size())); // rest should be tram services
        assertEquals(1, departs.size()); // one way to get off the tram

        Set<Trip> trips = transportData.getTripsFor(station);
        Set<String> fileSvcs = new HashSet<>(); // all trips both ways

        trips.forEach(trip -> {
            String serviceId = trip.getServiceId();
            Service serviceById = transportData.getServiceById(serviceId);
            if (serviceById.getRouteId().equals(route)
                    && serviceById.isRunning()) {
                fileSvcs.add(serviceId);
            }
        });

        outbounds.forEach(outbound -> {
            String svcId = outbound.getService();
            assertTrue(svcId,fileSvcs.contains(svcId));
        });

    }

    @Test
    public void shouldReportServicesAtHarbourCityWithTimes() throws TramchesterException {

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.HarbourCity.getId()
                + RouteCodes.ECCLES_TO_ASH);
        reportServices(outbounds);
    }

    @Test
    public void shouldReportServicesCorrectlyAtVeloparkTimes() throws TramchesterException {

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(
                Stations.VeloPark.getId() + RouteCodes.ASH_TO_ECCLES);
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
                Stations.VeloPark.getId() + RouteCodes.ASH_TO_ECCLES);

        List<TramGoesToRelationship> svcsFromVelopark = new LinkedList<>();
        outbounds.forEach(out -> {
            if (out instanceof TramGoesToRelationship) svcsFromVelopark.add((TramGoesToRelationship) out);
        });
        // filter by day and then direction/route
        assertTrue(!svcsFromVelopark.isEmpty());
        svcsFromVelopark.removeIf(svc -> !svc.getDaysTramRuns()[0]); // monday
        assertTrue(!svcsFromVelopark.isEmpty());
        svcsFromVelopark.removeIf(svc -> !transportData.getServiceById(
                svc.getService()).getRouteId().equals(RouteCodes.ASH_TO_ECCLES));
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

}
