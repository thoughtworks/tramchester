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
    public void shouldHaveCorrectRelationshipsAtCornbrook() throws TramchesterException {
        assumeTrue(edgePerTrip);

        List<TransportRelationship> outbounds = calculator.getOutboundRouteStationRelationships(Stations.Cornbrook.getId()
                + RouteCodesForTesting.ALTY_TO_BURY);

        assertTrue(outbounds.size()>1);

        outbounds = calculator.getOutboundRouteStationRelationships(Stations.Cornbrook.getId()
                + RouteCodesForTesting.ASH_TO_ECCLES);

        assertTrue(outbounds.size()>1);

    }

    @Test
    public void shouldHaveCorrectInboundsAtMediaCityEdgePerTrip() throws TramchesterException {
        assumeTrue(edgePerTrip);

        checkInboundConsistency(Stations.MediaCityUK.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        checkInboundConsistency(Stations.MediaCityUK.getId(), RouteCodesForTesting.ASH_TO_ECCLES);

        checkInboundConsistency(Stations.HarbourCity.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        checkInboundConsistency(Stations.HarbourCity.getId(), RouteCodesForTesting.ASH_TO_ECCLES);

        checkInboundConsistency(Stations.Broadway.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        checkInboundConsistency(Stations.Broadway.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
    }

    @Test
    public void shouldCheckOutboundSvcRelationships() throws TramchesterException {
        assumeTrue(edgePerTrip);

        checkOutboundConsistency(Stations.StPetersSquare.getId(), RouteCodesForTesting.ALTY_TO_BURY);
        checkOutboundConsistency(Stations.StPetersSquare.getId(), RouteCodesForTesting.BURY_TO_ALTY);

        checkOutboundConsistency(Stations.Cornbrook.getId(), RouteCodesForTesting.BURY_TO_ALTY);
        checkOutboundConsistency(Stations.Cornbrook.getId(), RouteCodesForTesting.ALTY_TO_BURY);

        checkOutboundConsistency(Stations.StPetersSquare.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
        checkOutboundConsistency(Stations.StPetersSquare.getId(), RouteCodesForTesting.ECCLES_TO_ASH);

        checkOutboundConsistency(Stations.MediaCityUK.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
        checkOutboundConsistency(Stations.MediaCityUK.getId(), RouteCodesForTesting.ECCLES_TO_ASH);

        // consistent heading away from Media City ONLY, see below
        checkOutboundConsistency(Stations.HarbourCity.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        checkOutboundConsistency(Stations.Broadway.getId(), RouteCodesForTesting.ASH_TO_ECCLES);

        // these two are not consistent because same svc can go different ways while still having same route code
        // i.e. service from harbour city can go to media city or to Broadway with same svc and route id
        // => end up with two outbound services instead of one, hence numbers looks different
        // graphAndFileConsistencyCheckOutbounds(Stations.Broadway.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        // graphAndFileConsistencyCheckOutbounds(Stations.HarbourCity.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
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

    public void checkOutboundConsistency(String stationId, String routeId) throws TramchesterException {
        List<TransportRelationship> graphOutbounds = calculator.getOutboundRouteStationRelationships(
                stationId + routeId);

        assertTrue(graphOutbounds.size()>0);

        List<String> serviceRelatIds = graphOutbounds.stream().
                filter(TransportRelationship::isServiceLink).
                map(relationship -> (ServiceRelationship) relationship).
                map(ServiceRelationship::getServiceId).
                collect(Collectors.toList());

        Set<Trip> fileCallingTrips = transportData.getServices().stream().
                filter(svc -> svc.getRouteId().equals(routeId)).
                filter(svc -> svc.isRunning()).
                map(Service::getTrips).
                flatMap(Collection::stream).
                filter(trip -> trip.callsAt(stationId)).
                collect(Collectors.toSet());

        Set<String> fileSvcIdFromTrips = fileCallingTrips.stream().
                map(Trip::getServiceId).
                collect(Collectors.toSet());

        // each svc should be one outbound, no dups, so use list not set of ids
        assertEquals(fileSvcIdFromTrips.size(), serviceRelatIds.size());
        assertTrue(fileSvcIdFromTrips.containsAll(serviceRelatIds));

        Set<TramNode> graphOutboundSvcs = graphOutbounds.stream().
                filter(TransportRelationship::isServiceLink).
                map(relationship -> (ServiceRelationship) relationship).
                map(svc -> svc.getEndNode()).
                collect(Collectors.toSet());
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

    private void checkInboundConsistency(String stationId, String routeId) throws TramchesterException {
        List<TransportRelationship> inbounds = calculator.getInboundRouteStationRelationships(stationId + routeId);

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
}
