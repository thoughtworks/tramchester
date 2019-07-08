package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Service;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.*;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.RouteCodesForTesting;
import com.tramchester.integration.Stations;
import com.tramchester.repository.TransportDataFromFiles;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class TramGraphBuilderTest {
    private static final Logger logger = LoggerFactory.getLogger(TramGraphBuilderTest.class);
    private static Dependencies dependencies;
    private static boolean edgePerTrip;

    private RouteCalculator calculator;
    private TransportDataFromFiles transportData;
    private Transaction transaction;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        edgePerTrip = testConfig.getEdgePerTrip();
        dependencies.initialise(testConfig);
    }

    @Before
    public void beforeEachTestRuns() {
        calculator = dependencies.get(RouteCalculator.class);
        transportData = dependencies.get(TransportDataFromFiles.class);
        GraphDatabaseService service = dependencies.get(GraphDatabaseService.class);
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

    @Test
    @Ignore("Work In Progress")
    public void shouldHaveAnAreaWithStations() throws TramchesterException {
        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.PiccadillyGardens.getId()
                + RouteCodesForTesting.ALTY_TO_BURY);
    }

    @Test
    public void shouldHaveCorrectInboundsAtMediaCity() throws TramchesterException {
        assumeFalse(edgePerTrip);

        List<TransportRelationship> inbounds = calculator.getInboundRouteStationRelationships(
                Stations.MediaCityUK.getId() + RouteCodesForTesting.ECCLES_TO_ASH );

        List<BoardRelationship> boards = new LinkedList<>();
        List<TramGoesToRelationship> svcsToMediaCity = new LinkedList<>();
        inbounds.forEach(in -> {
            if (in instanceof BoardRelationship) boards.add((BoardRelationship) in);
            if (in instanceof TramGoesToRelationship) svcsToMediaCity.add((TramGoesToRelationship) in);
        });

        assertEquals(1, boards.size());
        assertEquals(19, svcsToMediaCity.size());
    }

    @Test
    public void shouldRepdroduceIssueWithWeekendsAtDeansgateToAshton() throws TramchesterException {
        assumeFalse(edgePerTrip);

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.Deansgate.getId()
                + RouteCodesForTesting.ECCLES_TO_ASH);
        List<TramGoesToRelationship> trams = outbounds.stream().
                filter(TransportRelationship::isGoesTo).
                map(relationship -> ((TramGoesToRelationship) relationship)).collect(Collectors.toList());
        List<TramGoesToRelationship> sundays = trams.stream().
                filter(tram -> tram.getDaysServiceRuns()[6]).collect(Collectors.toList());
        assertTrue(sundays.size()>0);

        List<LocalTime> allTimes = new LinkedList<>();

        sundays.forEach(svc -> {
            LocalTime[] times = svc.getTimesServiceRuns();
            allTimes.addAll(Arrays.asList(times));
        });

        assertTrue(allTimes.size()>0);
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
        assumeFalse(edgePerTrip);

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(
                Stations.Deansgate.getId() + RouteCodesForTesting.ALTY_TO_BURY);

        assertTrue(outbounds.size()>0);

        List<String> deansAndNext = Arrays.asList(Stations.Deansgate.getId(), Stations.MarketStreet.getId());

        outbounds.stream().filter(TransportRelationship::isGoesTo).forEach(out -> {
                    TramGoesToRelationship goesTo = (TramGoesToRelationship) out;
                    String svcId = goesTo.getServiceId();
                    Service svc = transportData.getServiceById(svcId);
                    Set<Trip> trips = svc.getTrips();
                    List<Trip> tripsThatCall = trips.stream().filter(trip -> trip.getStops().stream().
                            map(stop -> stop.getStation().getId()).
                                collect(Collectors.toList()).
                                containsAll(deansAndNext)).
                            collect(Collectors.toList());
                    LocalTime[] timesTramRuns = goesTo.getTimesServiceRuns();
                    // number of outbounds from should match calling trip from the data
                    assertEquals(svcId, tripsThatCall.size(), timesTramRuns.length);

                    List<LocalTime> times = tripsThatCall.stream().
                            map(trip -> trip.getStopsFor(Stations.Deansgate.getId())).
                            flatMap(Collection::stream).
                            map(stop -> stop.getDepartureTime().asLocalTime()).
                            collect(Collectors.toList());
                    assertEquals(svcId, times.size(), timesTramRuns.length);

                    for (LocalTime timesTramRun : timesTramRuns) {
                        assertTrue(svcId + " " + timesTramRun, times.contains(timesTramRun));
                    }

                });
    }

    @Test
    public void shouldValidateGraphRepresentationMatchesTransportData() throws TramchesterException {
        assumeFalse(edgePerTrip);

        String station = Stations.VeloPark.getId();
        String route = RouteCodesForTesting.ASH_TO_ECCLES;

        List<TransportRelationship> relationships = calculator.getOutboundRouteStationRelationships(station + route);

        // check on departs relationship & services
        List<TransportRelationship> departs = new LinkedList<>();
        List<GoesToRelationship> outbounds = new LinkedList<>();
        relationships.forEach(relationship -> {
            if (relationship instanceof DepartRelationship) departs.add(relationship);
            if (relationship instanceof TramGoesToRelationship) outbounds.add((TramGoesToRelationship) relationship);
        });

        assertEquals(relationships.size()-1, (outbounds.size())); // rest should be tram services
        assertEquals(1, departs.size()); // one way to getPlatformById off the tram

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
            String svcId = outbound.getServiceId();
            assertTrue(svcId,fileSvcs.contains(svcId));
        });
    }

    @Test
    public void shouldReportServicesAtHarbourCityWithTimes() throws TramchesterException {

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.HarbourCity.getId()
                + RouteCodesForTesting.ECCLES_TO_ASH);
        reportServices(outbounds);
    }

    @Test
    public void shouldReportServicesCorrectlyAtVeloparkTimes() throws TramchesterException {

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(
                Stations.VeloPark.getId() + RouteCodesForTesting.ASH_TO_ECCLES);
        reportServices(outbounds);
    }

    private void reportServices(List<TransportRelationship> outbounds) {
        outbounds.forEach(outbound -> {
            if (outbound.isGoesTo()) {
                TramGoesToRelationship tramGoesToRelationship = (TramGoesToRelationship) outbound;
                LocalTime[] runsAt = tramGoesToRelationship.getTimesServiceRuns();
                assertTrue(runsAt.length >0 );
                logger.info(String.format("%s", tramGoesToRelationship.getServiceId()));
                logger.info(display(runsAt));
                boolean[] days = tramGoesToRelationship.getDaysServiceRuns();
                logger.info(display(days));
            }
        });
    }

    private String display(boolean[] days) {
        StringBuilder builder = new StringBuilder();
        for(boolean runs : days) {
            builder.append(" ").append(runs?"Y":"N");
        }
        return builder.toString();
    }

    private String display(LocalTime[] runsAt) {
        StringBuilder builder = new StringBuilder();
        for (LocalTime i : runsAt) {
            builder.append(" ").append(i);
        }
        return builder.toString();
    }

    @Test
    public void shouldHaveCorrectGraphRelationshipsFromVeloparkNodeMonday8Am() throws TramchesterException {
        assumeFalse(edgePerTrip);

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(
                Stations.VeloPark.getId() + RouteCodesForTesting.ASH_TO_ECCLES);

        List<TramGoesToRelationship> svcsFromVelopark = new LinkedList<>();
        outbounds.forEach(out -> {
            if (out instanceof TramGoesToRelationship) svcsFromVelopark.add((TramGoesToRelationship) out);
        });
        // filter by day and then direction/route
        assertTrue(!svcsFromVelopark.isEmpty());
        svcsFromVelopark.removeIf(svc -> !svc.getDaysServiceRuns()[0]); // monday
        assertTrue(!svcsFromVelopark.isEmpty());
        svcsFromVelopark.removeIf(svc -> !transportData.getServiceById(
                svc.getServiceId()).getRouteId().equals(RouteCodesForTesting.ASH_TO_ECCLES));
        assertTrue(!svcsFromVelopark.isEmpty());

        assertTrue(svcsFromVelopark.size() >=1 );

        svcsFromVelopark.removeIf(svc -> {
            for (LocalTime time : svc.getTimesServiceRuns()) {
                //if ((mins>=MINUTES_FROM_MIDNIGHT_8AM) && (mins-MINUTES_FROM_MIDNIGHT_8AM<=15))
                if (time.isAfter(LocalTime.of(7,59)) && time.isBefore(LocalTime.of(8,16)))
                    return false;
            }
            return true;
        });

        assertTrue(svcsFromVelopark.size() >=1 ); // at least one service calls mondays at this time, 59
    }


}
