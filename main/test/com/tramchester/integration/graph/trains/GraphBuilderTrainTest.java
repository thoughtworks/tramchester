package com.tramchester.integration.graph.trains;

import com.google.common.collect.Lists;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.LINKED;
import static com.tramchester.testSupport.reference.TrainStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class GraphBuilderTrainTest {
    private static ComponentContainer componentContainer;

    private TransportData transportData;
    private Transaction txn;
    private GraphQuery graphQuery;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new IntegrationTrainTestConfig();
        componentContainer = new ComponentsBuilder<>().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        graphQuery = componentContainer.get(GraphQuery.class);
        transportData = componentContainer.get(TransportData.class);
        stationRepository = componentContainer.get(StationRepository.class);
        GraphDatabase service = componentContainer.get(GraphDatabase.class);
        txn = service.beginTx();
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
    void shouldHaveLinkRelationshipsCorrectForNonInterchange() {
        Station mobberley = TrainStations.of(TrainStations.Mobberley);
        Node mobberleyNode = graphQuery.getStationNode(txn, mobberley);
        Iterable<Relationship> outboundLinks = mobberleyNode.getRelationships(Direction.OUTGOING, LINKED);

        List<Relationship> list = Lists.newArrayList(outboundLinks);
        assertEquals(2, list.size(), list.toString());

        Set<IdFor<Station>> destinations = list.stream().map(Relationship::getEndNode).
                map(GraphProps::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(Knutsford.getId()));
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForInterchange() {
        Station cornbrook = TrainStations.of(ManchesterPiccadilly);
        Node manPiccNode = graphQuery.getStationNode(txn, cornbrook);
        Iterable<Relationship> outboundLinks = manPiccNode.getRelationships(Direction.OUTGOING, LINKED);

        List<Relationship> list = Lists.newArrayList(outboundLinks);
        assertEquals(3, list.size());

        Set<IdFor<Station>> destinations = list.stream().map(Relationship::getEndNode).
                map(GraphProps::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(Stockport.getId()));
;
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForEndOfLine() {
       // todo
    }

    @Test
    void shouldCheckOutboundSvcRelationships() {

        // todo

//        checkOutboundConsistency(StPetersSquare, AshtonunderLyneManchesterEccles);
//        checkOutboundConsistency(StPetersSquare, EcclesManchesterAshtonunderLyne);
//
//        checkOutboundConsistency(MediaCityUK, AshtonunderLyneManchesterEccles);
//        checkOutboundConsistency(MediaCityUK, EcclesManchesterAshtonunderLyne);
//
//        // consistent heading away from Media City ONLY, see below
//        checkOutboundConsistency(HarbourCity, EcclesManchesterAshtonunderLyne);
//        checkOutboundConsistency(Broadway, AshtonunderLyneManchesterEccles);

        // these two are not consistent because same svc can go different ways while still having same route code
        // i.e. service from harbour city can go to media city or to Broadway with same svc and route id
        // => end up with two outbound services instead of one, hence numbers looks different
        // graphAndFileConsistencyCheckOutbounds(Stations.Broadway.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        // graphAndFileConsistencyCheckOutbounds(Stations.HarbourCity.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
    }

    private void checkOutboundConsistency(TrainStations trainStation, Route route) {
        Station station = of(trainStation);
        //Route route = createTramRoute(knownRoute);

        RouteStation routeStation = stationRepository.getRouteStation(station, route);

        List<Relationship> graphOutbounds = graphQuery.getRouteStationRelationships(txn, routeStation, Direction.OUTGOING);

        assertTrue(graphOutbounds.size()>0);

        List<IdFor<Service>> serviceRelatIds = graphOutbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                map(GraphProps::getServiceId).
                collect(Collectors.toList());

        Set<Trip> fileCallingTrips = transportData.getServices().stream().
                filter(svc -> svc.getRoutes().contains(route)).
                map(Service::getAllTrips).
                flatMap(Collection::stream).
                filter(trip -> trip.getStops().callsAt(station)).
                collect(Collectors.toSet());

        Set<IdFor<Service>> fileSvcIdFromTrips = fileCallingTrips.stream().
                map(trip -> trip.getService().getId()).
                collect(Collectors.toSet());

        // NOTE: Check clean target that and graph has been rebuilt if see failure here
        // each svc should be one outbound, no dups, so use list not set of ids
        assertEquals(fileSvcIdFromTrips.size(), serviceRelatIds.size());
        assertTrue(fileSvcIdFromTrips.containsAll(serviceRelatIds));
    }

    private void checkInboundConsistency(TrainStations trainStations, Route route) {
        Station station = of(trainStations);

        RouteStation routeStation = stationRepository.getRouteStation(station, route);
        List<Relationship> inbounds = graphQuery.getRouteStationRelationships(txn, routeStation, Direction.INCOMING);

        List<Relationship> graphTramsIntoStation = inbounds.stream().
                filter(inbound -> inbound.isType(TransportRelationshipTypes.TRAIN_GOES_TO)).collect(Collectors.toList());

        long boardingCount = inbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.BOARD)
                        || relationship.isType(TransportRelationshipTypes.INTERCHANGE_BOARD)).count();
        assertEquals(1, boardingCount);

        SortedSet<IdFor<Service>> graphInboundSvcIds = graphTramsIntoStation.stream().
                map(GraphProps::getServiceId).collect(Collectors.toCollection(TreeSet::new));

        Set<Trip> callingTrips = transportData.getServices().stream().
                filter(svc -> svc.getRoutes().contains(route)).
                map(Service::getAllTrips).
                flatMap(Collection::stream).
                filter(trip -> trip.getStops().callsAt(station)). // calls at , but not starts at because no inbound for these
                filter(trip -> !trip.getStops().getStopBySequenceNumber(trip.getSeqNumOfFirstStop()).getStation().equals(station)).
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
