package com.tramchester.integration.graph;

import com.tramchester.Dependencies;
import com.tramchester.domain.HasId;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.KnownRoute;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.TramStations;
import org.junit.jupiter.api.*;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.KnownRoute.*;
import static com.tramchester.testSupport.RoutesForTesting.createTramRoute;
import static com.tramchester.testSupport.TramStations.*;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;

class TramGraphBuilderTest {
    private static Dependencies dependencies;

    private TransportData transportData;
    private Transaction txn;
    private GraphQuery graphQuery;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
        dependencies.initialise(testConfig);
    }

    @BeforeEach
    void beforeEachTestRuns() {
        graphQuery = dependencies.get(GraphQuery.class);
        transportData = dependencies.get(TransportData.class);
        stationRepository = dependencies.get(StationRepository.class);
        GraphDatabase service = dependencies.get(GraphDatabase.class);
        txn = service.beginTx();
    }

    @AfterEach
    void afterEachTestRuns() {
        txn.close();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    void shouldHaveCorrectOutboundsAtMediaCity() {

        Station mediaCityUK = TramStations.of(MediaCityUK);

        List<Relationship> outbounds = getOutboundRouteStationRelationships(txn,
                stationRepository.getRouteStation(mediaCityUK, createTramRoute(EcclesManchesterAshtonunderLyne)));
        outbounds.addAll(getOutboundRouteStationRelationships(txn,
                stationRepository.getRouteStation(mediaCityUK, createTramRoute(AshtonunderLyneManchesterEccles))));

        Set<IdFor<Service>> graphSvcIds = outbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.TO_SERVICE)).
                map(GraphProps::getServiceId).
                collect(Collectors.toSet());

        // check number of outbound services matches services in transport data files
        Set<IdFor<Service>> fileSvcIds = getTripsFor(transportData.getTrips(), mediaCityUK).stream().
                map(trip -> trip.getService().getId()).
                collect(Collectors.toSet());
        fileSvcIds.removeAll(graphSvcIds);

        Assertions.assertEquals(0, fileSvcIds.size());
    }

    private List<Relationship> getOutboundRouteStationRelationships(Transaction txn, HasId<RouteStation> routeStationId) {
        return graphQuery.getRouteStationRelationships(txn, routeStationId, Direction.OUTGOING);
    }

    @Test
    void shouldHaveCorrectRelationshipsAtCornbrook() {

        List<Relationship> outbounds = getOutboundRouteStationRelationships(txn,
                stationRepository.getRouteStation(of(Cornbrook), createTramRoute(AltrinchamPiccadilly)));

        Assertions.assertTrue(outbounds.size()>1, "have at least one outbound");

        outbounds = getOutboundRouteStationRelationships(txn,
                stationRepository.getRouteStation(of(Cornbrook), createTramRoute(AshtonunderLyneManchesterEccles)));

        Assertions.assertTrue(outbounds.size()>1);

    }

    @Test
    void shouldHaveCorrectInboundsAtMediaCity() {
        checkInboundConsistency(MediaCityUK, EcclesManchesterAshtonunderLyne);
        checkInboundConsistency(MediaCityUK, AshtonunderLyneManchesterEccles);

        checkInboundConsistency(HarbourCity, EcclesManchesterAshtonunderLyne);
        checkInboundConsistency(HarbourCity, AshtonunderLyneManchesterEccles);

        checkInboundConsistency(Broadway, EcclesManchesterAshtonunderLyne);
        checkInboundConsistency(Broadway, AshtonunderLyneManchesterEccles);
    }

    @Test
    void shouldCheckOutboundSvcRelationships() {

        // TODO Lockdown - Route 1 is gone for now
//        checkOutboundConsistency(Stations.StPetersSquare, RoutesForTesting.ALTY_TO_BURY);
//        checkOutboundConsistency(Stations.StPetersSquare, RoutesForTesting.BURY_TO_ALTY);
//
//        checkOutboundConsistency(Stations.Cornbrook, RoutesForTesting.BURY_TO_ALTY);
//        checkOutboundConsistency(Stations.Cornbrook, RoutesForTesting.ALTY_TO_BURY);

        checkOutboundConsistency(StPetersSquare, AshtonunderLyneManchesterEccles);
        checkOutboundConsistency(StPetersSquare, EcclesManchesterAshtonunderLyne);

        checkOutboundConsistency(MediaCityUK, AshtonunderLyneManchesterEccles);
        checkOutboundConsistency(MediaCityUK, EcclesManchesterAshtonunderLyne);

        // consistent heading away from Media City ONLY, see below
        checkOutboundConsistency(HarbourCity, EcclesManchesterAshtonunderLyne);
        checkOutboundConsistency(Broadway, AshtonunderLyneManchesterEccles);

        // these two are not consistent because same svc can go different ways while still having same route code
        // i.e. service from harbour city can go to media city or to Broadway with same svc and route id
        // => end up with two outbound services instead of one, hence numbers looks different
        // graphAndFileConsistencyCheckOutbounds(Stations.Broadway.getId(), RouteCodesForTesting.ECCLES_TO_ASH);
        // graphAndFileConsistencyCheckOutbounds(Stations.HarbourCity.getId(), RouteCodesForTesting.ASH_TO_ECCLES);
    }

    private void checkOutboundConsistency(TramStations tramStation, KnownRoute knownRoute) {
        Station station = of(tramStation);
        Route route = createTramRoute(knownRoute);

        RouteStation routeStation = stationRepository.getRouteStation(station, route);

        List<Relationship> graphOutbounds = getOutboundRouteStationRelationships(txn, routeStation);

        Assertions.assertTrue(graphOutbounds.size()>0);

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
        Assertions.assertEquals(fileSvcIdFromTrips.size(), serviceRelatIds.size());
        Assertions.assertTrue(fileSvcIdFromTrips.containsAll(serviceRelatIds));
    }

    private void checkInboundConsistency(TramStations tramStation, KnownRoute knownRoute) {
        Route route = RoutesForTesting.createTramRoute(knownRoute);
        Station station = of(tramStation);

        RouteStation routeStation = stationRepository.getRouteStation(station, route);
        List<Relationship> inbounds = graphQuery.getRouteStationRelationships(txn, routeStation, Direction.INCOMING);

        List<Relationship> graphTramsIntoStation = inbounds.stream().
                filter(inbound -> inbound.isType(TransportRelationshipTypes.TRAM_GOES_TO)).collect(Collectors.toList());

        long boardingCount = inbounds.stream().
                filter(relationship -> relationship.isType(TransportRelationshipTypes.BOARD)
                        || relationship.isType(TransportRelationshipTypes.INTERCHANGE_BOARD)).count();
        Assertions.assertEquals(1, boardingCount);

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

        Assertions.assertEquals(svcIdsFromCallingTrips, graphInboundSvcIds);

        Set<IdFor<Trip>> graphInboundTripIds = graphTramsIntoStation.stream().
                map(GraphProps::getTripId).
                collect(Collectors.toSet());

        Assertions.assertEquals(graphTramsIntoStation.size(), graphInboundTripIds.size()); // should have an inbound link per trip

        Set<IdFor<Trip>> tripIdsFromFile = callingTrips.stream().
                map(Trip::getId).
                collect(Collectors.toSet());

        tripIdsFromFile.removeAll(graphInboundTripIds);
        Assertions.assertEquals(0, tripIdsFromFile.size());
    }
}
