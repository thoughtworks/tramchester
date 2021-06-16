package com.tramchester.integration.graph;

import com.google.common.collect.Lists;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.reference.TramStations;
import org.assertj.core.util.Streams;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class TramGraphBuilderTest {
    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig testConfig;

    private TransportData transportData;
    private Transaction txn;
    private GraphQuery graphQuery;
    private StationRepository stationRepository;
    private Route tramRouteAshtonEccles;
    private Route tramRouteEcclesAshton;
    private Route tramRouteAltPicc;
    private TramRouteHelper tramRouteHelper;
    private InterchangeRepository interchangeRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        tramRouteHelper = new TramRouteHelper(componentContainer);
        tramRouteAshtonEccles = tramRouteHelper.get(AshtonUnderLyneManchesterEccles);
        tramRouteEcclesAshton = tramRouteHelper.get(EcclesManchesterAshtonUnderLyne);
        tramRouteAltPicc = tramRouteHelper.get(AltrinchamPiccadilly);

        graphQuery = componentContainer.get(GraphQuery.class);
        transportData = componentContainer.get(TransportData.class);
        stationRepository = componentContainer.get(StationRepository.class);
        interchangeRepository = componentContainer.get(InterchangeRepository.class);
        GraphDatabase service = componentContainer.get(GraphDatabase.class);

        StagedTransportGraphBuilder builder = componentContainer.get(StagedTransportGraphBuilder.class);
        builder.getReady();
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
    void shouldHaveLinkRelationshipsCorrectForInterchange() {
        Station cornbrook = TramStations.of(Cornbrook);
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
    void shouldHaveLinkRelationshipsCorrectForEndOfLine() {
        Station alty = TramStations.of(Altrincham);
        Node altyNode = graphQuery.getStationNode(txn, alty);
        Iterable<Relationship> outboundLinks = altyNode.getRelationships(Direction.OUTGOING, LINKED);

        List<Relationship> list = Lists.newArrayList(outboundLinks);
        assertEquals(1, list.size());

        Set<IdFor<Station>> destinations = list.stream().map(Relationship::getEndNode).
                map(GraphProps::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(NavigationRoad.getId()));
    }

    @Test
    void shouldHaveLinkRelationshipsCorrectForNonInterchange() {
        Station exchangeSq = TramStations.of(ExchangeSquare);
        Node exchangeSqNode = graphQuery.getStationNode(txn, exchangeSq);
        Iterable<Relationship> outboundLinks = exchangeSqNode.getRelationships(Direction.OUTGOING, LINKED);

        List<Relationship> list = Lists.newArrayList(outboundLinks);
        assertEquals(2, list.size());

        Set<IdFor<Station>> destinations = list.stream().map(Relationship::getEndNode).
                map(GraphProps::getStationId).collect(Collectors.toSet());

        assertTrue(destinations.contains(Victoria.getId()));
        assertTrue(destinations.contains(StPetersSquare.getId()));
    }

    @Test
    void shouldHaveCorrectOutboundsAtMediaCity() {

        Station mediaCityUK = TramStations.of(MediaCityUK);

        RouteStation routeStationMediaCityA = stationRepository.getRouteStation(mediaCityUK, tramRouteEcclesAshton);
        List<Relationship> outbounds = graphQuery.getRouteStationRelationships(txn, routeStationMediaCityA, Direction.OUTGOING);

        RouteStation routeStationMediaCityB = stationRepository.getRouteStation(mediaCityUK, tramRouteAshtonEccles);
        outbounds.addAll(graphQuery.getRouteStationRelationships(txn, routeStationMediaCityB, Direction.OUTGOING));

        Set<IdFor<Service>> graphSvcIds = outbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                map(GraphProps::getServiceId).
                collect(Collectors.toSet());

        // check number of outbound services matches services in transport data files
        Set<IdFor<Service>> fileSvcIds = getTripsFor(transportData.getTrips(), mediaCityUK).stream().
                map(trip -> trip.getService().getId()).
                collect(Collectors.toSet());
        fileSvcIds.removeAll(graphSvcIds);

        assertEquals(0, fileSvcIds.size());
    }

    @Test
    void shouldHaveCorrectRelationshipsAtCornbrook() {

        RouteStation routeStationCornbrookAltyPiccRoute = stationRepository.getRouteStation(of(Cornbrook), tramRouteAltPicc);
        List<Relationship> outbounds = graphQuery.getRouteStationRelationships(txn, routeStationCornbrookAltyPiccRoute, Direction.OUTGOING);

        assertTrue(outbounds.size()>1, "have at least one outbound");

        RouteStation routeStationCornbrookAshtonEcclesRoute = stationRepository.getRouteStation(of(Cornbrook), tramRouteAshtonEccles);
        outbounds = graphQuery.getRouteStationRelationships(txn, routeStationCornbrookAshtonEcclesRoute, Direction.OUTGOING);

        assertTrue(outbounds.size()>1);
    }

    @Test
    void shouldHaveCorrectInboundsAtMediaCity() {
        checkInboundConsistency(MediaCityUK, EcclesManchesterAshtonUnderLyne);
        checkInboundConsistency(MediaCityUK, AshtonUnderLyneManchesterEccles);

        checkInboundConsistency(HarbourCity, EcclesManchesterAshtonUnderLyne);
        checkInboundConsistency(HarbourCity, AshtonUnderLyneManchesterEccles);

        checkInboundConsistency(Broadway, EcclesManchesterAshtonUnderLyne);
        checkInboundConsistency(Broadway, AshtonUnderLyneManchesterEccles);
    }

    @Test
    void shouldCheckOutboundSvcRelationships() {

        // TODO Lockdown - Route 1 is gone for now
//        checkOutboundConsistency(Stations.StPetersSquare, RoutesForTesting.ALTY_TO_BURY);
//        checkOutboundConsistency(Stations.StPetersSquare, RoutesForTesting.BURY_TO_ALTY);
//
//        checkOutboundConsistency(Stations.Cornbrook, RoutesForTesting.BURY_TO_ALTY);
//        checkOutboundConsistency(Stations.Cornbrook, RoutesForTesting.ALTY_TO_BURY);

        checkOutboundConsistency(StPetersSquare, AshtonUnderLyneManchesterEccles);
        checkOutboundConsistency(StPetersSquare, EcclesManchesterAshtonUnderLyne);

        checkOutboundConsistency(MediaCityUK, AshtonUnderLyneManchesterEccles);
        checkOutboundConsistency(MediaCityUK, EcclesManchesterAshtonUnderLyne);

        // consistent heading away from Media City ONLY, see below
        checkOutboundConsistency(HarbourCity, EcclesManchesterAshtonUnderLyne);
        checkOutboundConsistency(Broadway, AshtonUnderLyneManchesterEccles);

        // these two are not consistent because same svc can go different ways while still having same route code
        // i.e. service from harbour city can go to media city or to Broadway with same svc and route id
        // => end up with two outbound services instead of one, hence numbers looks different
        // graphAndFileConsistencyCheckOutbounds(Stations.Broadway.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        // graphAndFileConsistencyCheckOutbounds(Stations.HarbourCity.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
    }

    private void checkOutboundConsistency(TramStations tramStation, KnownTramRoute knownRoute) {
        Station station = of(tramStation);
        Route route = tramRouteHelper.get(knownRoute);

        RouteStation routeStation = stationRepository.getRouteStation(station, route);

        List<Relationship> routeStationOutbounds = graphQuery.getRouteStationRelationships(txn, routeStation, Direction.OUTGOING);

        assertTrue(routeStationOutbounds.size()>0);

        List<IdFor<Service>> serviceRelatIds = routeStationOutbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                map(GraphProps::getServiceId).
                collect(Collectors.toList());

        Set<Trip> fileCallingTrips =
                transportData.getRouteById(route.getId()).getTrips().stream().

                filter(trip -> trip.getStopCalls().callsAt(station)).
                collect(Collectors.toSet());

        Set<IdFor<Service>> fileSvcIdFromTrips = fileCallingTrips.stream().
                map(trip -> trip.getService().getId()).
                collect(Collectors.toSet());

        // NOTE: Check clean target that and graph has been rebuilt if see failure here
        // each svc should be one outbound, no dups, so use list not set of ids
        assertEquals(fileSvcIdFromTrips.size(), serviceRelatIds.size());
        assertTrue(fileSvcIdFromTrips.containsAll(serviceRelatIds));

//        long routeConnected = routeStationOutbounds.stream().
//                filter(relationship -> relationship.isType(CONNECT_ROUTES)).count();
//        if (testConfig.getChangeAtInterchangeOnly() && interchangeRepository.isInterchange(TramStations.of(tramStation))) {
//            assertNotEquals(0, routeConnected);
//        } else {
//            assertEquals(0, routeConnected);
//        }

        long connectedToRouteStation = routeStationOutbounds.stream().filter(relationship -> relationship.isType(ROUTE_TO_STATION)).count();
        assertNotEquals(0, connectedToRouteStation);

        List<Relationship> incomingToRouteStation = graphQuery.getRouteStationRelationships(txn, routeStation, Direction.INCOMING);
        long fromStation = Streams.stream(incomingToRouteStation).filter(relationship -> relationship.isType(STATION_TO_ROUTE)).count();
        assertNotEquals(0, fromStation);
    }

    private void checkInboundConsistency(TramStations tramStation, KnownTramRoute knownRoute) {
        Route route = tramRouteHelper.get(knownRoute);
        Station station = of(tramStation);

        RouteStation routeStation = stationRepository.getRouteStation(station, route);
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
                filter(trip -> trip.getStopCalls().callsAt(station)). // calls at , but not starts at because no inbound for these
                filter(trip -> !trip.getStopCalls().getStopBySequenceNumber(trip.getSeqNumOfFirstStop()).getStation().equals(station)).
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
