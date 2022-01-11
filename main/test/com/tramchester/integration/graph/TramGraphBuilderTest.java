package com.tramchester.integration.graph;

import com.google.common.collect.Lists;
import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.graphbuild.StagedTransportGraphBuilder;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
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

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class TramGraphBuilderTest {
    private static ComponentContainer componentContainer;

    private TransportData transportData;
    private Transaction txn;
    private GraphQuery graphQuery;
    private StationRepository stationRepository;
    private Set<Route> tramRoutesAshtonEccles;
    private Set<Route> tramRoutesEcclesAshton;
    private Set<Route> tramRoutesAltPicc;
    private TramRouteHelper tramRouteHelper;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        tramRouteHelper = new TramRouteHelper(componentContainer);
        tramRoutesAshtonEccles = tramRouteHelper.get(AshtonUnderLyneManchesterEccles);
        tramRoutesEcclesAshton = tramRouteHelper.get(EcclesManchesterAshtonUnderLyne);
        tramRoutesAltPicc = tramRouteHelper.get(AltrinchamPiccadilly);

        graphQuery = componentContainer.get(GraphQuery.class);
        transportData = componentContainer.get(TransportData.class);
        stationRepository = componentContainer.get(StationRepository.class);
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
    void shouldHaveCorrectPlatformCosts() {
        Station piccadilly = Piccadilly.getFrom(stationRepository);
        Set<Platform> platforms = piccadilly.getPlatforms();

        int expectedCost = piccadilly.getMinimumChangeCost();

        platforms.forEach(platform -> {
            Node node = graphQuery.getPlatformNode(txn, platform);
            Relationship leave = node.getSingleRelationship(TransportRelationshipTypes.LEAVE_PLATFORM, Direction.OUTGOING);
            int leaveCost = GraphProps.getCost(leave);
            assertEquals(0, leaveCost, "leave cost wrong for " + platform);

            Relationship enter = node.getSingleRelationship(TransportRelationshipTypes.ENTER_PLATFORM, Direction.INCOMING);
            int enterCost = GraphProps.getCost(enter);
            assertEquals(expectedCost, enterCost, "wrong cost for " + platform.getId());
        });

        platforms.forEach(platform -> {
            Node node = graphQuery.getPlatformNode(txn, platform);
            Iterable<Relationship> boards = node.getRelationships(Direction.OUTGOING, INTERCHANGE_BOARD);
            boards.forEach(board -> {
                int boardCost = GraphProps.getCost(board);
                assertEquals(expectedCost, boardCost, "board cost wrong for " + platform);
            });

            Iterable<Relationship> departs = node.getRelationships(Direction.OUTGOING, INTERCHANGE_DEPART);
            departs.forEach(depart -> {
                int enterCost = GraphProps.getCost(depart);
                assertEquals(0, enterCost, "depart wrong cost for " + platform.getId());
            });

        });
    }

    @Test
    void shouldHaveCorrectRouteStationToStationRouteCosts() {

        Set<RouteStation> routeStations = stationRepository.getRouteStationsFor(Piccadilly.getId());

        routeStations.forEach(routeStation -> {
            Node node = graphQuery.getRouteStationNode(txn, routeStation);

            Relationship toStation = node.getSingleRelationship(ROUTE_TO_STATION, Direction.OUTGOING);
            int costToStation = GraphProps.getCost(toStation);
            assertEquals(0, costToStation, "wrong cost for " + routeStation);

            Relationship fromStation = node.getSingleRelationship(STATION_TO_ROUTE, Direction.INCOMING);
            int costFromStation = GraphProps.getCost(fromStation);
            int expected = routeStation.getStation().getMinimumChangeCost();
            assertEquals(expected, costFromStation, "wrong cost for " + routeStation);
        });
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
    void shouldHaveOneNodePerRouteStation() {
        stationRepository.getRouteStations().forEach(routeStation -> {
            Node found = graphQuery.getRouteStationNode(txn, routeStation);
            assertNotNull(found, routeStation.getId().forDTO());
        });
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

        List<Relationship> outbounds = new ArrayList<>();

        tramRoutesEcclesAshton.forEach(tramRouteEcclesAshton -> {
            RouteStation routeStationMediaCityA = stationRepository.getRouteStation(mediaCityUK, tramRouteEcclesAshton);
            outbounds.addAll(graphQuery.getRouteStationRelationships(txn, routeStationMediaCityA, Direction.OUTGOING));
        });

        tramRoutesAshtonEccles.forEach(tramRouteAshtonEccles -> {
            RouteStation routeStationMediaCityB = stationRepository.getRouteStation(mediaCityUK, tramRouteAshtonEccles);
            outbounds.addAll(graphQuery.getRouteStationRelationships(txn, routeStationMediaCityB, Direction.OUTGOING));
        });

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

        tramRoutesAltPicc.forEach(tramRouteAltPicc -> {
            RouteStation routeStationCornbrookAltyPiccRoute = stationRepository.getRouteStation(of(Cornbrook), tramRouteAltPicc);
            List<Relationship> outbounds = graphQuery.getRouteStationRelationships(txn, routeStationCornbrookAltyPiccRoute, Direction.OUTGOING);

            assertTrue(outbounds.size()>1, "have at least one outbound");
        });

        tramRoutesAshtonEccles.forEach(tramRouteAshtonEccles -> {

            RouteStation routeStationCornbrookAshtonEcclesRoute = stationRepository.getRouteStation(of(Cornbrook), tramRouteAshtonEccles);
            List<Relationship> outbounds = graphQuery.getRouteStationRelationships(txn, routeStationCornbrookAshtonEcclesRoute, Direction.OUTGOING);

            assertTrue(outbounds.size()>1);
        });

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

        checkOutboundConsistency(StPetersSquare, AltrinchamManchesterBury);
        checkOutboundConsistency(StPetersSquare, BuryManchesterAltrincham);

        checkOutboundConsistency(Cornbrook, BuryManchesterAltrincham);
        checkOutboundConsistency(Cornbrook, AltrinchamManchesterBury);

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
        Set<Route> routes = tramRouteHelper.get(knownRoute);

        routes.forEach(route -> checkOutboundConsistency(station, route));
    }

    private void checkOutboundConsistency(Station station, Route route) {
        RouteStation routeStation = stationRepository.getRouteStation(station, route);

        List<Relationship> routeStationOutbounds = graphQuery.getRouteStationRelationships(txn, routeStation, Direction.OUTGOING);

        assertTrue(routeStationOutbounds.size()>0);

        // since can have 'multiple' route stations due to dup routes use set here
       IdSet<Service> serviceRelatIds = routeStationOutbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                map(GraphProps::getServiceId).
                collect(IdSet.idCollector());

        Set<Trip> fileCallingTrips =
                transportData.getRouteById(route.getId()).getTrips().stream().

                filter(trip -> trip.getStopCalls().callsAt(station)).
                collect(Collectors.toSet());

        IdSet<Service> fileSvcIdFromTrips = fileCallingTrips.stream().
                map(trip -> trip.getService().getId()).
                collect(IdSet.idCollector());

        // NOTE: Check clean target that and graph has been rebuilt if see failure here
        assertEquals(fileSvcIdFromTrips.size(), serviceRelatIds.size(),
                "Did not match " + fileSvcIdFromTrips + " and " + serviceRelatIds);
        assertTrue(fileSvcIdFromTrips.containsAll(serviceRelatIds));

        long connectedToRouteStation = routeStationOutbounds.stream().filter(relationship -> relationship.isType(ROUTE_TO_STATION)).count();
        assertNotEquals(0, connectedToRouteStation);

        List<Relationship> incomingToRouteStation = graphQuery.getRouteStationRelationships(txn, routeStation, Direction.INCOMING);
        long fromStation = Streams.stream(incomingToRouteStation).filter(relationship -> relationship.isType(STATION_TO_ROUTE)).count();
        assertNotEquals(0, fromStation);
    }

    private void checkInboundConsistency(TramStations tramStation, KnownTramRoute knownRoute) {
        Set<Route> routes = tramRouteHelper.get(knownRoute);
        Station station = of(tramStation);

        routes.forEach(route -> checkInboundConsistency(station, route));
    }

    private void checkInboundConsistency(Station station, Route route) {
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
