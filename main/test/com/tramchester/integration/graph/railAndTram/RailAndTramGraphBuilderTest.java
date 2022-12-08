package com.tramchester.integration.graph.railAndTram;

import com.google.common.collect.Lists;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.TramAndTrainGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.testTags.GMTest;
import org.assertj.core.util.Streams;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
class RailAndTramGraphBuilderTest {
    private static ComponentContainer componentContainer;

    private TransportData transportData;
    private Transaction txn;
    private GraphQuery graphQuery;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new TramAndTrainGreaterManchesterConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {

        transportData = componentContainer.get(TransportData.class);

        graphQuery = componentContainer.get(GraphQuery.class);
        stationRepository = componentContainer.get(StationRepository.class);
        GraphDatabase graphDatabase = componentContainer.get(GraphDatabase.class);

        StagedTransportGraphBuilder builder = componentContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();
        txn = graphDatabase.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForInterchange() {
        Station cornbrook = Cornbrook.from(stationRepository);
        Node cornbrookNode = graphQuery.getStationNode(txn, cornbrook);
        Iterable<Relationship> outboundLinks = cornbrookNode.getRelationships(Direction.OUTGOING, LINKED);

        List<Relationship> list = Lists.newArrayList(outboundLinks);
        assertEquals(3, list.size());

        Set<IdFor<Station>> destinations = list.stream().map(Relationship::getEndNode).
                map(GraphProps::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(TraffordBar.getId()));
        assertTrue(destinations.contains(Pomona.getId()));
        assertTrue(destinations.contains(Deansgate.getId()));
    }

    @Test
    void shouldHaveCorrectRouteStationToStationRouteCosts() {

        Set<RouteStation> routeStations = stationRepository.getRouteStationsFor(Piccadilly.getId());

        routeStations.forEach(routeStation -> {
            Node node = graphQuery.getRouteStationNode(txn, routeStation);

            Relationship toStation = node.getSingleRelationship(ROUTE_TO_STATION, Direction.OUTGOING);
            Duration costToStation = GraphProps.getCost(toStation);
            assertEquals(Duration.ZERO, costToStation, "wrong cost for " + routeStation);

            Relationship fromStation = node.getSingleRelationship(STATION_TO_ROUTE, Direction.INCOMING);
            Duration costFromStation = GraphProps.getCost(fromStation);
            Duration expected = routeStation.getStation().getMinChangeDuration();
            assertEquals(expected, costFromStation, "wrong cost for " + routeStation);
        });
    }

    @Test
    void shouldHaveExpectedRelationshipsBetweenTramAndTrainStations() {
        Station altyTram = Altrincham.from(stationRepository);
        Station altyTrain = RailStationIds.Altrincham.from(stationRepository);

        Duration expectedCost = Duration.ofMinutes(1);

        Node altyTramNode = graphQuery.getStationNode(txn, altyTram);
        Node altyTrainNode = graphQuery.getStationNode(txn, altyTrain);

        assertNotNull(altyTramNode);
        assertNotNull(altyTrainNode);

        List<Relationship> fromTram = Streams.stream(altyTramNode.getRelationships(Direction.OUTGOING, NEIGHBOUR)).collect(Collectors.toList());
        assertEquals(1, fromTram.size(), "Wrong number of neighbours " + fromTram);

        Relationship tramNeighbour = fromTram.get(0);
        assertEquals(altyTrainNode.getId(), tramNeighbour.getEndNode().getId());
        assertEquals(expectedCost, GraphProps.getCost(tramNeighbour));

        List<Relationship> fromTrain = Streams.stream(altyTrainNode.getRelationships(Direction.OUTGOING, NEIGHBOUR)).collect(Collectors.toList());
        assertEquals(1, fromTrain.size(), "Wrong number of neighbours " + fromTram);

        Relationship trainNeighbour = fromTrain.get(0);
        assertEquals(altyTramNode.getId(), trainNeighbour.getEndNode().getId());
        assertEquals(expectedCost, GraphProps.getCost(trainNeighbour));

    }

    @Test
    void shouldHaveOneNodePerRouteStation() {
        Set<RouteStation> routeStations = stationRepository.getRouteStations();

        IdSet<RouteStation> noTramRouteStationNode = routeStations.stream().
                filter(routeStation -> routeStation.getTransportModes().contains(TransportMode.Tram)).
                filter(routeStation -> graphQuery.getRouteStationNode(txn, routeStation) == null).
                collect(IdSet.collector());

        assertTrue(noTramRouteStationNode.isEmpty(), noTramRouteStationNode.toString());

        Set<RouteStation> trainRouteStations = routeStations.stream().
                filter(routeStation -> routeStation.getTransportModes().contains(TransportMode.Train)).
                filter(RouteStation::isActive). // rail data has 'passed' stations
                collect(Collectors.toSet());

        IdSet<RouteStation> noTrainRouteStationNode = trainRouteStations.stream().
                filter(routeStation -> graphQuery.getRouteStationNode(txn, routeStation) == null).
                collect(IdSet.collector());

        int numRouteStations = trainRouteStations.size();
        assertTrue(noTrainRouteStationNode.isEmpty(), "Not empty, num route stations is " + numRouteStations
                + " without nodes is " + noTrainRouteStationNode.size());
    }

    @Test
    void shouldHaveExpectedInterchangesInTheGraph() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);

        IdSet<Station> fromConfigAndDiscovered = interchangeRepository.getAllInterchanges().stream().
                map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        ResourceIterator<Node> interchangeNodes = txn.findNodes(GraphLabel.INTERCHANGE);

        IdSet<Station> fromDB = interchangeNodes.stream().map(GraphProps::getStationId).collect(IdSet.idCollector());

        assertEquals(fromConfigAndDiscovered, fromDB, "Graph clean and rebuild needed?");
    }

//    private void checkOutboundConsistency(TramStations tramStation, KnownTramRoute knownRoute) {
//        Station station = tramStation.from(stationRepository);
//        Route route = tramRouteHelper.getOneRoute(knownRoute, when);
//
//        checkOutboundConsistency(station, route);
//    }

//    private void checkOutboundConsistency(Station station, Route route) {
//        RouteStation routeStation = stationRepository.getRouteStation(station, route);
//
//        List<Relationship> routeStationOutbounds = graphQuery.getRouteStationRelationships(txn, routeStation, Direction.OUTGOING);
//
//        assertTrue(routeStationOutbounds.size()>0);
//
//        // since can have 'multiple' route stations due to dup routes use set here
//       IdSet<Service> serviceRelatIds = routeStationOutbounds.stream().
//                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
//                map(GraphProps::getServiceId).
//                collect(IdSet.idCollector());
//
//        Set<Trip> fileCallingTrips =
//                transportData.getRouteById(route.getId()).getTrips().stream().
//
//                filter(trip -> trip.callsAt(station)).
//                collect(Collectors.toSet());
//
//        IdSet<Service> fileSvcIdFromTrips = fileCallingTrips.stream().
//                map(trip -> trip.getService().getId()).
//                collect(IdSet.idCollector());
//
//        // NOTE: Check clean target that and graph has been rebuilt if see failure here
//        assertEquals(fileSvcIdFromTrips.size(), serviceRelatIds.size(),
//                "Did not match " + fileSvcIdFromTrips + " and " + serviceRelatIds);
//        assertTrue(fileSvcIdFromTrips.containsAll(serviceRelatIds));
//
//        long connectedToRouteStation = routeStationOutbounds.stream().filter(relationship -> relationship.isType(ROUTE_TO_STATION)).count();
//        assertNotEquals(0, connectedToRouteStation);
//
//        List<Relationship> incomingToRouteStation = graphQuery.getRouteStationRelationships(txn, routeStation, Direction.INCOMING);
//        long fromStation = Streams.stream(incomingToRouteStation).filter(relationship -> relationship.isType(STATION_TO_ROUTE)).count();
//        assertNotEquals(0, fromStation);
//    }

//    private void checkInboundConsistency(TramStations tramStation, KnownTramRoute knownRoute) {
//        Route route = tramRouteHelper.getOneRoute(knownRoute, when);
//        Station station = tramStation.from(stationRepository);
//
//        checkInboundConsistency(station, route);
//    }

    private void checkInboundConsistency(Station station, Route route) {
        RouteStation routeStation = stationRepository.getRouteStation(station, route);
        assertNotNull(routeStation, "Could not find a route for " + station.getId() + " and  " + route.getId());
        List<Relationship> inbounds = graphQuery.getRouteStationRelationships(txn, routeStation, Direction.INCOMING);

        List<Relationship> graphTramsIntoStation = inbounds.stream().
                filter(inbound -> inbound.isType(TransportRelationshipTypes.TRAM_GOES_TO)).collect(Collectors.toList());

        long boardingCount = inbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.BOARD)
                        || relationship.isType(TransportRelationshipTypes.INTERCHANGE_BOARD)).count();
        assertEquals(1, boardingCount);

        SortedSet<IdFor<Service>> graphInboundSvcIds = graphTramsIntoStation.stream().
                map(GraphProps::getServiceId).collect(Collectors.toCollection(TreeSet::new));

        Set<Trip> callingTrips =
                transportData.getRouteById(route.getId()).getTrips().stream().
                filter(trip -> trip.callsAt(station)). // calls at , but not starts at because no inbound for these
                //filter(trip -> !trip.getStopCalls().getStopBySequenceNumber(trip.getSeqNumOfFirstStop()).getStation().equals(station)).
                filter(trip -> !trip.getStopCalls().getFirstStop().getStation().equals(station)).
                collect(Collectors.toSet());

        SortedSet<IdFor<Service>> svcIdsFromCallingTrips = callingTrips.stream().
                map(trip -> trip.getService().getId()).collect(Collectors.toCollection(TreeSet::new));

        assertEquals(svcIdsFromCallingTrips, graphInboundSvcIds);

        Set<IdFor<Trip>> graphInboundTripIds = graphTramsIntoStation.stream().
                map(GraphProps::getTripId).
                collect(Collectors.toSet());

        assertEquals(graphTramsIntoStation.size(), graphInboundTripIds.size()); // should have an inbound link per trip

        Set<IdFor<Trip>> tripIdsFromFile = callingTrips.stream().
                map(Trip::getId).
                collect(Collectors.toSet());

        tripIdsFromFile.removeAll(graphInboundTripIds);
        assertEquals(0, tripIdsFromFile.size());
    }
}
