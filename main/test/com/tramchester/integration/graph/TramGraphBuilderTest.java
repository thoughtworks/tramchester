package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Service;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.Nodes.ServiceNode;
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
import static org.junit.Assume.assumeTrue;

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
        assertEquals(7, svcsToMediaCity.size());
    }

    @Test
    public void shouldHaveCorrectOutboundsAtMediaCity() throws TramchesterException {
        assumeTrue(edgePerTrip);

        String mediaCityUKId = Stations.MediaCityUK.getId();
        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(
                mediaCityUKId + RouteCodesForTesting.ECCLES_TO_ASH );
        outbounds.addAll(calculator.getOutboundRouteStationRelationships(
                mediaCityUKId + RouteCodesForTesting.ASH_TO_ECCLES ));

        List<ServiceRelationship> graphServicesRelationships = new LinkedList<>();
        outbounds.forEach(out -> {
            if (out instanceof ServiceRelationship) graphServicesRelationships.add((ServiceRelationship) out);
        });

        Set<String> graphSvcIds = graphServicesRelationships.stream().map(svc -> svc.getServiceId()).collect(Collectors.toSet());

        Set<String> fileSvcIds = transportData.getTripsFor(mediaCityUKId).stream().map(svc -> svc.getServiceId()).collect(Collectors.toSet());
        fileSvcIds.removeAll(graphSvcIds);

        assertEquals(0, fileSvcIds.size());
    }

    @Test
    public void shouldHaveCorrectInboundsAtMediaCityEdgePerTrip() throws TramchesterException {
        assumeTrue(edgePerTrip);

        String stationId = Stations.MediaCityUK.getId();
        String routeId = RouteCodesForTesting.ECCLES_TO_ASH;

        List<TransportRelationship> inbounds = calculator.getInboundRouteStationRelationships(
                stationId + routeId);

        List<BoardRelationship> graphBoardAtStation = new LinkedList<>();
        List<TramGoesToRelationship> graphTramsIntoStation = new LinkedList<>();
        inbounds.forEach(in -> {
            if (in instanceof BoardRelationship) graphBoardAtStation.add((BoardRelationship) in);
            if (in instanceof TramGoesToRelationship) graphTramsIntoStation.add((TramGoesToRelationship) in);
        });
        assertEquals(1, graphBoardAtStation.size());

        Set<String> graphInboundSvcIds = graphTramsIntoStation.stream().
                map(GoesToRelationship::getServiceId).
                collect(Collectors.toSet());

        Set<Trip> callingTrips = transportData.getServices().stream().
                filter(svc -> svc.getRouteId().equals(routeId)).
                map(Service::getTrips).
                flatMap(Collection::stream).
                filter(trip -> trip.callsAt(stationId)). // calls at , but not starts at because no inbound for these
                filter(trip -> !trip.getStops().get(0).getStation().getId().equals(stationId)).
                collect(Collectors.toSet());

        Set<String> svcIdsFromCallingTrips = callingTrips.stream().
                map(Trip::getServiceId).
                collect(Collectors.toSet());

        assertEquals(svcIdsFromCallingTrips.size(), graphInboundSvcIds.size());

        Set<String> graphInboundTripIds = graphTramsIntoStation.stream().map(GoesToRelationship::getTripId).collect(Collectors.toSet());

        assertEquals(graphTramsIntoStation.size(), graphInboundTripIds.size()); // should have an inbound link per trip

        Set<String> tripIdsFromFile = callingTrips.stream().map(Trip::getTripId).collect(Collectors.toSet());

        tripIdsFromFile.removeAll(graphInboundTripIds);
        assertEquals(0, tripIdsFromFile.size());
    }

    @Test
    public void shouldRepdroduceIssueWithWeekendsAtDeansgateToAshtonWithEdgePerService() throws TramchesterException {
        assumeTrue(edgePerTrip);

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.Deansgate.getId()
                + RouteCodesForTesting.ECCLES_TO_ASH);
        List<ServiceNode> serviceNodes = outbounds.stream().filter(TransportRelationship::isServiceLink).
                map(relationship -> (ServiceNode)relationship.getEndNode()).collect(Collectors.toList());

        List<ServiceNode> sundays = serviceNodes.stream().filter(node -> node.getDaysServiceRuns()[6]).collect(Collectors.toList());
        assertTrue(sundays.size()>0);
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
    public void shouldCheckOutboundSvcRelationships() throws TramchesterException {
        assumeTrue(edgePerTrip);

        graphAndFileConsistencyCheckOutbounds(Stations.Deansgate.getId(), RouteCodesForTesting.ALTY_TO_BURY);
        graphAndFileConsistencyCheckOutbounds(Stations.StPetersSquare.getId(), RouteCodesForTesting.ALTY_TO_BURY);
        graphAndFileConsistencyCheckOutbounds(Stations.StPetersSquare.getId(), RouteCodesForTesting.BURY_TO_ALTY);
    }

    public void graphAndFileConsistencyCheckOutbounds(String stationId, String routeId) throws TramchesterException {
        List<TransportRelationship> graphOutbounds = calculator.getOutboundRouteStationRelationships(
                stationId + routeId);

        assertTrue(graphOutbounds.size()>0);

        List<TramNode> graphOutboundSvcs = graphOutbounds.stream().
                filter(TransportRelationship::isServiceLink).
                map(svc -> (ServiceRelationship) svc).
                map(TransportCostRelationship::getEndNode).
                collect(Collectors.toList());

        Set<Trip> callingTrips = transportData.getServices().stream().
                filter(svc -> svc.getRouteId().equals(routeId)).
                map(Service::getTrips).
                flatMap(Collection::stream).
                filter(trip -> trip.callsAt(stationId)).
                collect(Collectors.toSet());

        Set<String> svcIdsFromCallingTrips = callingTrips.stream().
                map(Trip::getServiceId).
                collect(Collectors.toSet());

        // each svc should be one outbound, no dups, so use list not set of ids
        assertEquals(svcIdsFromCallingTrips.size(), graphOutboundSvcs.size());

        // service earliest/latest at nodes should match those from trips/stops
        graphOutboundSvcs.stream().map(node -> (ServiceNode)node).
               forEach(serviceNode -> {
                   Service fileService = transportData.getServiceById(serviceNode.getServiceId());
                   LocalTime nodeEarliest = serviceNode.getEarliestTime();
                   assertEquals(fileService.getServiceId(), fileService.earliestDepartTime().asLocalTime(), nodeEarliest);
               });
        graphOutboundSvcs.stream().map(node -> (ServiceNode)node).
                forEach(serviceNode -> {
                    Service fileService = transportData.getServiceById(serviceNode.getServiceId());
                    LocalTime nodeLatest = serviceNode.getLatestTime();
                    assertEquals(fileService.getServiceId(), fileService.latestDepartTime().asLocalTime(), nodeLatest);
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
    public void shouldValidateGraphRepresentationMatchesTransportDataEdgePerTrip() throws TramchesterException {
        assumeTrue(edgePerTrip);

        String station = Stations.VeloPark.getId();
        String route = RouteCodesForTesting.ASH_TO_ECCLES;

        List<TransportRelationship> relationships = calculator.getOutboundRouteStationRelationships(station + route);

        // check on departs relationship & services
        List<TransportRelationship> departs = new LinkedList<>();
        List<ServiceRelationship> outbounds = new LinkedList<>();
        relationships.forEach(relationship -> {
            if (relationship instanceof DepartRelationship) departs.add(relationship);
            if (relationship instanceof ServiceRelationship) outbounds.add((ServiceRelationship) relationship);
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

    @Test
    public void shouldHaveCorrectGraphRelationshipsFromVeloparkNodeMonday8AmEdgePerTrip() throws TramchesterException {
        assumeTrue(edgePerTrip);

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(
                Stations.VeloPark.getId() + RouteCodesForTesting.ASH_TO_ECCLES);

        List<ServiceRelationship> svcRelationshipsFromVeloPark = new LinkedList<>();
        outbounds.forEach(out -> {
            if (out instanceof ServiceRelationship) svcRelationshipsFromVeloPark.add((ServiceRelationship) out);
        });
        // filter by day and then direction/route
        assertTrue(!svcRelationshipsFromVeloPark.isEmpty());
        List<ServiceNode> serviceNodes = svcRelationshipsFromVeloPark.stream().
                map(relationship -> (ServiceNode) relationship.getEndNode()).collect(Collectors.toList());
        serviceNodes.removeIf(svc -> !svc.getDaysServiceRuns()[0]); // monday
        assertTrue(!serviceNodes.isEmpty());
        svcRelationshipsFromVeloPark.removeIf(svc -> !transportData.getServiceById(
                svc.getServiceId()).getRouteId().equals(RouteCodesForTesting.ASH_TO_ECCLES));

        assertTrue(!svcRelationshipsFromVeloPark.isEmpty());

    }

}
