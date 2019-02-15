package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Service;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.Nodes.ServiceNode;
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

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class TramGraphBuilderEdgePerTripTest {
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
    public void shouldCheckOutboundSvcRelationships() throws TramchesterException {
        assumeTrue(edgePerTrip);

        graphAndFileConsistencyCheckOutbounds(Stations.Deansgate.getId(), RouteCodesForTesting.ALTY_TO_BURY);
        graphAndFileConsistencyCheckOutbounds(Stations.StPetersSquare.getId(), RouteCodesForTesting.ALTY_TO_BURY);
        graphAndFileConsistencyCheckOutbounds(Stations.StPetersSquare.getId(), RouteCodesForTesting.BURY_TO_ALTY);
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
}
