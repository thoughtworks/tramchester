package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.Service;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.input.Trip;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import org.junit.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TramGraphBuilderTest {
    private static Dependencies dependencies;

    private TransportDataFromFiles transportData;
    private Transaction transaction;
    private GraphQuery graphQuery;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @Before
    public void beforeEachTestRuns() {
        graphQuery = dependencies.get(GraphQuery.class);
        transportData = dependencies.get(TransportDataFromFiles.class);
        GraphDatabase service = dependencies.get(GraphDatabase.class);
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
    public void shouldHaveCorrectOutboundsAtMediaCity() {

        List<Relationship> outbounds = getOutboundRouteStationRelationships(
                RouteStation.formId(Stations.MediaCityUK, RoutesForTesting.ECCLES_TO_ASH));
        outbounds.addAll(getOutboundRouteStationRelationships(RouteStation.formId(Stations.MediaCityUK,
                RoutesForTesting.ASH_TO_ECCLES )));

        Set<String> graphSvcIds = outbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                map(relationship -> relationship.getProperty(GraphStaticKeys.SERVICE_ID).toString()).
                collect(Collectors.toSet());

        // check number of outbound services matches services in transport data files
        Set<String> fileSvcIds = getTripsFor(transportData.getTrips(), Stations.MediaCityUK).stream().
                filter(trip -> trip.getService().isRunning())
                .map(trip -> trip.getService().getId()).
                collect(Collectors.toSet());
        fileSvcIds.removeAll(graphSvcIds);

        assertEquals(0, fileSvcIds.size());
    }

    private List<Relationship> getOutboundRouteStationRelationships(String routeStationId) {
        return graphQuery.getRouteStationRelationships(routeStationId, Direction.OUTGOING);
    }

    @Test
    public void shouldHaveCorrectRelationshipsAtCornbrook() {

        List<Relationship> outbounds = getOutboundRouteStationRelationships(RouteStation.formId(Stations.Cornbrook,
                RoutesForTesting.ALTY_TO_PICC));

        assertTrue("have at least one outbound", outbounds.size()>1);

        outbounds = getOutboundRouteStationRelationships(RouteStation.formId(Stations.Cornbrook, RoutesForTesting.ASH_TO_ECCLES));

        assertTrue(outbounds.size()>1);

    }

    @Test
    public void shouldHaveCorrectInboundsAtMediaCity() {

        checkInboundConsistency(Stations.MediaCityUK, RoutesForTesting.ECCLES_TO_ASH);
        checkInboundConsistency(Stations.MediaCityUK, RoutesForTesting.ASH_TO_ECCLES);

        checkInboundConsistency(Stations.HarbourCity, RoutesForTesting.ECCLES_TO_ASH);
        checkInboundConsistency(Stations.HarbourCity, RoutesForTesting.ASH_TO_ECCLES);

        checkInboundConsistency(Stations.Broadway, RoutesForTesting.ECCLES_TO_ASH);
        checkInboundConsistency(Stations.Broadway, RoutesForTesting.ASH_TO_ECCLES);
    }

    @Test
    public void shouldCheckOutboundSvcRelationships() {

        // TODO Lockdown - Route 1 is gone for now
//        checkOutboundConsistency(Stations.StPetersSquare, RoutesForTesting.ALTY_TO_BURY);
//        checkOutboundConsistency(Stations.StPetersSquare, RoutesForTesting.BURY_TO_ALTY);
//
//        checkOutboundConsistency(Stations.Cornbrook, RoutesForTesting.BURY_TO_ALTY);
//        checkOutboundConsistency(Stations.Cornbrook, RoutesForTesting.ALTY_TO_BURY);

        checkOutboundConsistency(Stations.StPetersSquare, RoutesForTesting.ASH_TO_ECCLES);
        checkOutboundConsistency(Stations.StPetersSquare, RoutesForTesting.ECCLES_TO_ASH);

        checkOutboundConsistency(Stations.MediaCityUK, RoutesForTesting.ASH_TO_ECCLES);
        checkOutboundConsistency(Stations.MediaCityUK, RoutesForTesting.ECCLES_TO_ASH);

        // consistent heading away from Media City ONLY, see below
        checkOutboundConsistency(Stations.HarbourCity, RoutesForTesting.ECCLES_TO_ASH);
        checkOutboundConsistency(Stations.Broadway, RoutesForTesting.ASH_TO_ECCLES);

        // these two are not consistent because same svc can go different ways while still having same route code
        // i.e. service from harbour city can go to media city or to Broadway with same svc and route id
        // => end up with two outbound services instead of one, hence numbers looks different
        // graphAndFileConsistencyCheckOutbounds(Stations.Broadway.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        // graphAndFileConsistencyCheckOutbounds(Stations.HarbourCity.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
    }

    private void checkOutboundConsistency(Station station, Route route) {
        List<Relationship> graphOutbounds = getOutboundRouteStationRelationships(RouteStation.formId(station, route));

        assertTrue(graphOutbounds.size()>0);

        List<String> serviceRelatIds = graphOutbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                map(relationship -> relationship.getProperty(GraphStaticKeys.SERVICE_ID).toString()).
                collect(Collectors.toList());

        Set<Trip> fileCallingTrips = transportData.getServices().stream().
                filter(svc -> svc.getRouteId().equals(route.getId())).
                filter(Service::isRunning).
                map(Service::getTrips).
                flatMap(Collection::stream).
                filter(trip -> trip.getStops().callsAt(station)).
                collect(Collectors.toSet());

        Set<String> fileSvcIdFromTrips = fileCallingTrips.stream().
                map(trip -> trip.getService().getId()).
                collect(Collectors.toSet());

        // NOTE: Check clean target that and graph has been rebuilt if see failure here
        // each svc should be one outbound, no dups, so use list not set of ids
        assertEquals(fileSvcIdFromTrips.size(), serviceRelatIds.size());
        assertTrue(fileSvcIdFromTrips.containsAll(serviceRelatIds));

    }

    private void checkInboundConsistency(Station station, Route route) {
        List<Relationship> inbounds = graphQuery.getRouteStationRelationships(RouteStation.formId(station, route), Direction.INCOMING);

        List<Relationship> graphTramsIntoStation = inbounds.stream().
                filter(inbound -> inbound.isType(TransportRelationshipTypes.TRAM_GOES_TO)).collect(Collectors.toList());

        long boardingCount = inbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.BOARD)
                        || relationship.isType(TransportRelationshipTypes.INTERCHANGE_BOARD)).count();
        assertEquals(1, boardingCount);

        SortedSet<String> graphInboundSvcIds = new TreeSet<>();
        graphInboundSvcIds.addAll(graphTramsIntoStation.stream().
                map(relationship -> relationship.getProperty(GraphStaticKeys.SERVICE_ID).toString()).
                collect(Collectors.toSet()));

        Set<Trip> callingTrips = transportData.getServices().stream().
                filter(svc -> svc.isRunning()).
                filter(svc -> svc.getRouteId().equals(route.getId())).
                map(Service::getTrips).
                flatMap(Collection::stream).
                filter(trip -> trip.getStops().callsAt(station)). // calls at , but not starts at because no inbound for these
                filter(trip -> !trip.getStops().get(0).getStation().getId().equals(station.getId())).
                collect(Collectors.toSet());

        SortedSet<String> svcIdsFromCallingTrips = new TreeSet<>();
        svcIdsFromCallingTrips.addAll(callingTrips.stream().
                map(trip -> trip.getService().getId()).
                collect(Collectors.toSet()));

        assertEquals(svcIdsFromCallingTrips, graphInboundSvcIds);

        Set<String> graphInboundTripIds = graphTramsIntoStation.stream().
                map(relationship -> relationship.getProperty(GraphStaticKeys.TRIP_ID).toString()).
                collect(Collectors.toSet());

        assertEquals(graphTramsIntoStation.size(), graphInboundTripIds.size()); // should have an inbound link per trip

        Set<String> tripIdsFromFile = callingTrips.stream().map(Trip::getId).collect(Collectors.toSet());

        tripIdsFromFile.removeAll(graphInboundTripIds);
        assertEquals(0, tripIdsFromFile.size());
    }
}
