package com.tramchester.graph;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.Service;
import com.tramchester.domain.TransportData;
import com.tramchester.domain.Trip;
import com.tramchester.graph.Relationships.DepartRelationship;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.TramRelationship;
import com.tramchester.services.DateTimeService;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class GraphBuilderTest {
    private static final int MINUTES_FROM_MIDNIGHT_8AM = 8 * 60;
    private static Dependencies dependencies;

    private RouteCalculator calculator;
    private TransportData transportData;
    public static final String ASH_TO_ECCLES_SVC = "MET:MET4:O:";

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
        transportData = dependencies.get(TransportData.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    // this test is data specific and could fail if change to routes happen
    @Test
    public void shouldValidateGraphRepresentationMatchesTransportData() {
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
    public void shouldCheckServiceThatNormallyGoesToEcclesCallsAtTraffordBar() {
        String svcId = "Serv000069";  // can go to eccles, media city or trafford bar

        // trips for this journey are currently Trip001632 and Trip001633
        List<TramRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.Cornbrook + ASH_TO_ECCLES_SVC);
        checkNumberOfServices(svcId, outbounds, 2);
    }

    @Test
    public void shouldRepresentBranchingServicesCorrectlyAtHarbourCity() {
        String svcId = "Serv000069";  // can go to eccles, media city or trafford bar

        List<TramRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.HarbourCity + ASH_TO_ECCLES_SVC);
        checkNumberOfServices(svcId, outbounds, 2);
    }

    @Test
    public void shouldHaveCorrectGraphRelationshipsFromVeloparkNodeMonday8Am() {

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

        //GraphDatabaseService graphDb = dependencies.get(GraphDatabaseService.class);
        //Transaction tx = graphDb.beginTx();
        //Relationship relationship = svcsFromVelopark.get(0).getRelationship();
        //Node target = relationship.getEndNode();
        //String type = target.getProperty(GraphStaticKeys.STATION_TYPE).toString();
        //String name = target.getProperty(GraphStaticKeys.STATION_NAME).toString();
        //tx.close();

        //assertEquals(GraphStaticKeys.ROUTE_STATION, type);
        //assertEquals("Etihad Campus", name);

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
