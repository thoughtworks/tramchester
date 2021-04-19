package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.StationTime;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.caches.NodeTypeRepository;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportData;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.graphbuild.GraphProps.*;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

@LazySingleton
public class StagedTransportGraphBuilder extends GraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(StagedTransportGraphBuilder.class);

    ///
    // Station -[enter]-> Platform -[board]-> RouteStation -[toSvc]-> Service -> Hour-[toMinute]->
    //          -> Minute -> RouteStation-[depart]-> Platform -[leave]-> Station
    //
    // OR
    //
    // Station -[board]-> RouteStation -[toSvc]-> Service -> Hour-[toMinute]->
    //          -> Minute -> RouteStation-[depart]-> Station
    //
    // RouteStation-[onRoute]->RouteStation
    //
    ///

    private final TransportData transportData;
    private final InterchangeRepository interchangeRepository;

    // force contsruction via guide to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    @Inject
    public StagedTransportGraphBuilder(GraphDatabase graphDatabase, TramchesterConfig config, GraphFilter graphFilter,
                                       NodeTypeRepository nodeTypeRepository, TransportData transportData,
                                       InterchangeRepository interchangeRepository, GraphBuilderCache builderCache,
                                       StationsAndLinksGraphBuilder.Ready readyToken) {
        super(graphDatabase, graphFilter, config, builderCache, nodeTypeRepository);
        this.transportData = transportData;
        this.interchangeRepository = interchangeRepository;
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (graphDatabase.isCleanDB()) {
            logger.info("Rebuild of TimeTable graph DB for " + config.getDbPath());
            if (graphFilter.isFiltered()) {
                logger.warn("Graph is filtered " + graphFilter);
            }
            buildGraphwithFilter(graphDatabase, builderCache);
            logger.info("Graph rebuild is finished for " + config.getDbPath());
        } else {
            logger.info("No rebuild of graph, using existing data");
            nodeTypeRepository.populateNodeLabelMap(graphDatabase);
        }
    }

    private void buildGraphwithFilter(GraphDatabase graphDatabase, GraphBuilderCache builderCache) {
        logger.info("Building graph for feedinfo: " + transportData.getDataSourceInfo());
        logMemory("Before graph build");

        graphDatabase.createIndexs();

        try(Timing ignored = new Timing(logger, "Graph rebuild")) {

            // just for tfgm trams currently
            linkStationsAndPlatforms(builderCache);

            // TODO Agencies could be done in parallel as should be no overlap except at station level?
            for(Agency agency : transportData.getAgencies()) {
                if (graphFilter.shouldIncludeAgency(agency)) {
                    try (Timing agencyTiming = new Timing(logger,"Add agency " + agency.getId())) {
                        buildForAgency(graphDatabase, agency, builderCache);
                    }
                }
            }

            // only add version node if we manage to build graph, so partial builds that fail we cause a rebuild
            addVersionNode(graphDatabase, transportData.getDataSourceInfo());

            try(Transaction tx = graphDatabase.beginTx()) {
                logger.info("Wait for indexes online");
                graphDatabase.waitForIndexesReady(tx);
            }

        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
            throw new RuntimeException("Unable to build graph", except);
        }

        builderCache.fullClear();
        reportStats();
        System.gc();
        logMemory("After graph build");
    }

    private void linkStationsAndPlatforms(GraphBuilderCache builderCache) {

        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "link stations & platfoms")) {
            Transaction txn = timedTransaction.transaction();
            transportData.getStations().stream().
                    filter(Station::hasPlatforms).
                    filter(graphFilter::shouldInclude).
                    filter(station -> graphFilter.shouldIncludeRoutes(station.getRoutes())).
                    forEach(station -> linkStationAndPlatforms(txn, station, builderCache));
            timedTransaction.commit();
        }

    }

    private void addVersionNode(GraphDatabase graphDatabase, Set<DataSourceInfo> infos) {
        try(Transaction tx = graphDatabase.beginTx()) {
            logger.info("Adding version node to the DB");
            Node node = createGraphNode(tx, Labels.VERSION);
            infos.forEach(nameAndVersion -> setProp(node, nameAndVersion));
            tx.commit();
        }
    }

    private void buildForAgency(GraphDatabase graphDatabase, Agency agency, GraphBuilderCache builderCache) {

        if (getRoutesForAgency(agency).findAny().isEmpty()) {
            return;
        }

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "onRoutes")) {
            Transaction tx = timedTransaction.transaction();
            getRoutesForAgency(agency).forEach(route -> createOnRouteRelationships(tx, route, builderCache));
            timedTransaction.commit();
        }

        try(Timing timing = new Timing(logger,"service, hour")) {
            getRoutesForAgency(agency).parallel().forEach(route -> {
                try (Transaction tx = graphDatabase.beginTx()) {
                    createServiceAndHourNodesForRoute(tx, route, builderCache);
                    tx.commit();
                }
            });
        }

        try(Timing timing = new Timing(logger,"time and update for trips")) {
            getRoutesForAgency(agency).parallel().forEach(route -> {
                try (Transaction tx = graphDatabase.beginTx()) {
                    ServiceRelationshipTrips serviceRelationshipTrips = new ServiceRelationshipTrips();
                    createMinuteNodesAndRecordUpdatesForTrips(tx, route, serviceRelationshipTrips, builderCache);
                    serviceRelationshipTrips.updateAll(tx);
                    tx.commit();
                }
            });
        }

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "boards & departs")) {
            Transaction tx = timedTransaction.transaction();
            getRoutesForAgency(agency).forEach(route -> buildGraphForBoardsAndDeparts(route, builderCache, tx));
            timedTransaction.commit();
        }

        builderCache.routeClear();

    }

    @NotNull
    private Stream<Route> getRoutesForAgency(Agency agency) {
        return agency.getRoutes().stream().filter(graphFilter::shouldIncludeRoute);
    }

    private void createMinuteNodesAndRecordUpdatesForTrips(Transaction tx, Route route, ServiceRelationshipTrips serviceRelationshipTrips,
                                                           GraphBuilderCache routeBuilderCache) {

        // time nodes and relationships for trips
        for (Trip trip : route.getTrips()) {
            Map<StationTime, Node> timeNodes = createMinuteNodes(tx, trip, routeBuilderCache);
            createTripRelationships(tx, route, trip, routeBuilderCache, timeNodes, serviceRelationshipTrips);
            timeNodes.clear();
        }
    }

    private void createTripRelationships(Transaction tx, Route route, Trip trip, GraphBuilderCache routeBuilderCache,
                                         Map<StationTime, Node> timeNodes, ServiceRelationshipTrips serviceRelationshipTrips) {
        StopCalls stops = trip.getStopCalls();

        stops.getLegs().forEach(leg -> {
            if (includeBothStops(leg)) {
                StopCall first = leg.getFirst();
                StopCall second = leg.getSecond();
                updateIncomingToServiceNodeWithTrip(tx, route, trip, first, second, routeBuilderCache, serviceRelationshipTrips);
                createRelationshipTimeNodeToRouteStation(tx, route, trip, first, second, routeBuilderCache, timeNodes);
            }
        });
    }

    private void updateIncomingToServiceNodeWithTrip(Transaction tx, Route route, Trip trip, StopCall beginStop, StopCall endStop,
                                                     GraphBuilderCache routeBuilderCache, ServiceRelationshipTrips serviceRelationshipTrips) {

        IdFor<Trip> tripId = trip.getId();
        IdFor<Station> startStationId = beginStop.getStation().getId();
        IdFor<Station> endStationId = endStop.getStation().getId();

        Node serviceNode = routeBuilderCache.getServiceNode(tx, route.getId(), trip.getService(), startStationId,
                endStationId);

        serviceNode.getRelationships(INCOMING, TO_SERVICE).forEach(
                relationship -> serviceRelationshipTrips.add(relationship.getId(), tripId));
    }

    private void buildGraphForBoardsAndDeparts(Route route, GraphBuilderCache routeBuilderCache,
                                               Transaction tx) {
        for (Trip trip : route.getTrips()) {
            trip.getStopCalls().stream().
                    filter(graphFilter::shouldInclude).
                    forEach(stopCall -> createBoardingAndDepart(tx, routeBuilderCache, stopCall, route, trip));
        }
    }

    private void createServiceAndHourNodesForRoute(Transaction tx, Route route, GraphBuilderCache stationCache) {
        route.getTrips().forEach(trip -> {
                StopCalls stops = trip.getStopCalls();
                List<StopCalls.StopLeg> legs = stops.getLegs();
                legs.forEach(leg -> {
                    if (includeBothStops(leg)) {
                        IdFor<Station> beginId = leg.getFirstStation().getId();
                        IdFor<Station> endId = leg.getSecondStation().getId();

                        Service service = trip.getService();
                        Node serviceNode = createServiceNodeAndRelationshipFromRouteStation(tx, route, service,
                                beginId, endId, stationCache);

                        createHourNodeAndRelationshipFromService(tx, route.getId(), service,
                                beginId, leg.getDepartureTime().getHourOfDay(), stationCache, serviceNode);
                    }
                });
        });
    }

    private Node createServiceNodeAndRelationshipFromRouteStation(Transaction tx, Route route, Service service,
                                                                  IdFor<Station> beginId, IdFor<Station> endId,
                                                                  GraphBuilderCache routeBuilderCache) {

        if (routeBuilderCache.hasServiceNode(route.getId(), service, beginId, endId)) {
            return routeBuilderCache.getServiceNode(tx, route.getId(), service, beginId, endId);
        }

        // Node for the service
        // -route ID here as some towardsServices can go via multiple routes, this seems to be associated with the depots
        // -some towardsServices can go in two different directions from a station i.e. around Media City UK

        Node svcNode = createGraphNode(tx, Labels.SERVICE);
        setProperty(svcNode, service);
        setProperty(svcNode, route);
        // TODO This is used to look up station and hence lat/long for distance ordering, store
        //  org.neo4j.graphdb.spatial.Point instead?
        setTowardsProp(svcNode, endId);
        routeBuilderCache.putService(route.getId(), service, beginId, endId, svcNode);

        // start route station -> svc node
        Node routeStationStart = routeBuilderCache.getRouteStation(tx, route, beginId);
        Relationship svcRelationship = createRelationship(routeStationStart, svcNode, TO_SERVICE);
        setProperty(svcRelationship, service);
        setCostProp(svcRelationship, 0);
        setProperty(svcRelationship, route);
        return svcNode;

    }

    private void linkStationAndPlatforms(Transaction txn, Station station, GraphBuilderCache routeBuilderCache) {

        Node stationNode = routeBuilderCache.getStation(txn, station.getId());
        if (stationNode!=null) {
            for (Platform platform : station.getPlatforms()) {
                Node platformNode = routeBuilderCache.getPlatform(txn, platform.getId());
                createPlatformStationRelationships(station, stationNode, platform, platformNode);
            }
        } else {
            throw new RuntimeException("Missing station node for " + station);
        }
    }

    private void createOnRouteRelationships(Transaction tx, Route route, GraphBuilderCache routeBuilderCache) {

        Map<StationIdPair, Integer> pairs = new HashMap<>();
        route.getTrips().forEach(trip -> {
            StopCalls stops = trip.getStopCalls();
            stops.getLegs().forEach(leg -> {
                if (includeBothStops(leg)) {
                    if (!pairs.containsKey(StationIdPair.of(leg.getFirstStation(), leg.getSecondStation()))) {
                        int cost = leg.getCost();
                        pairs.put(StationIdPair.of(leg.getFirstStation(), leg.getSecondStation()), cost);
                    }
                }
            });
        });

        pairs.forEach((pair, cost) -> {
            Node startNode = routeBuilderCache.getRouteStation(tx, route, pair.getBeginId());
            Node endNode = routeBuilderCache.getRouteStation(tx, route, pair.getEndId());
            createOnRouteRelationship(startNode, endNode, route, cost);
        });
}

    private boolean includeBothStops(StopCalls.StopLeg leg) {
        return graphFilter.shouldInclude(leg.getFirst()) && graphFilter.shouldInclude(leg.getSecond());
    }

    private void createBoardingAndDepart(Transaction tx, GraphBuilderCache routeBuilderCache, StopCall stopCall,
                                         Route route, Trip trip) {

        // TODO when filtering this isn't really valid, we might only see a small segment of a larger trip....
        boolean isFirstStop = stopCall.getGetSequenceNumber() == trip.getSeqNumOfFirstStop(); //stop seq num, not index
        boolean isLastStop = stopCall.getGetSequenceNumber() == trip.getSeqNumOfLastStop();

        boolean pickup = stopCall.getPickupType().equals(GTFSPickupDropoffType.Regular);
        boolean dropoff = stopCall.getDropoffType().equals(GTFSPickupDropoffType.Regular);

        Station station = stopCall.getStation();
        TramTime departureTime = stopCall.getDepartureTime();

        if (isFirstStop && dropoff) {
            String msg = "Drop off at first station for stop " + station.getId() + " dep time " + departureTime;
            logger.info(msg);
        }

        if (isLastStop && pickup) {
            String msg = "Pick up at last station for stop " + station.getId() + " dep time " + departureTime;
            logger.info(msg);
        }

        boolean isInterchange = interchangeRepository.isInterchange(station);

        // If bus we board to/from station, for trams its from the platform
        Node platformOrStation = station.hasPlatforms() ? routeBuilderCache.getPlatform(tx, stopCall.getPlatform().getId())
                : routeBuilderCache.getStation(tx, station.getId());
        IdFor<RouteStation> routeStationId = RouteStation.createId(station.getId(), route.getId());
        Node routeStationNode = routeBuilderCache.getRouteStation(tx, routeStationId);

        // boarding: platform/station ->  callingPoint , NOTE: no boarding at the last stop of a trip
        if (pickup && !routeBuilderCache.hasBoarding(platformOrStation.getId(), routeStationNode.getId())) {
            createBoarding(routeBuilderCache, stopCall, route, station, isInterchange, platformOrStation, routeStationId,
                    routeStationNode);
        }

        // leave: route station -> platform/station , NOTE: no towardsStation at first stop of a trip
        if (dropoff && !routeBuilderCache.hasDeparts(platformOrStation.getId(), routeStationNode.getId()) ) {
            createDeparts(routeBuilderCache, station, isInterchange, platformOrStation, routeStationId, routeStationNode);
        }

        // TODO seems normal in most data sets
//        if ((!(pickup||dropoff)) && (!TransportMode.isTrain(route))) {
//            // this is normal for trains, timetable lists all passed stations, whether train stops or not
//            logger.warn("No pickup or dropoff for " + stopCall);
//        }
    }

    private void createDeparts(GraphBuilderCache routeBuilderCache, Station station, boolean isInterchange,
                               Node boardingNode, IdFor<RouteStation> routeStationId, Node routeStationNode) {
        TransportRelationshipTypes departType = isInterchange ? INTERCHANGE_DEPART : DEPART;
        int departCost = isInterchange ? INTERCHANGE_DEPART_COST : DEPARTS_COST;

        Relationship departRelationship = createRelationship(routeStationNode, boardingNode, departType);
        setCostProp(departRelationship, departCost);
        setRouteStationProp(departRelationship, routeStationId);
        setProperty(departRelationship, station);
        routeBuilderCache.putDepart(boardingNode.getId(), routeStationNode.getId());
    }

    private void createBoarding(GraphBuilderCache routeBuilderCache, StopCall stop, Route route, Station station,
                                boolean isInterchange, Node platformOrStation, IdFor<RouteStation> routeStationId, Node routeStationNode) {
        TransportRelationshipTypes boardType = isInterchange ? INTERCHANGE_BOARD : BOARD;
        int boardCost = isInterchange ? INTERCHANGE_BOARD_COST : BOARDING_COST;
        Relationship boardRelationship = createRelationship(platformOrStation, routeStationNode, boardType);
        setCostProp(boardRelationship, boardCost);
        setRouteStationProp(boardRelationship, routeStationId);
        setProperty(boardRelationship, route);
        setProperty(boardRelationship, station);
        // No platform ID on buses
        if (stop.hasPlatfrom()) {
            setProperty(boardRelationship, stop.getPlatform());
        }
        routeBuilderCache.putBoarding(platformOrStation.getId(), routeStationNode.getId());
    }


    private void createOnRouteRelationship(Node from, Node to, Route route, int cost) {
        Set<Node> endNodes = new HashSet<>();

        if (from.hasRelationship(OUTGOING, ON_ROUTE)) {
            // legit for some routes when trams return to depot, or at media city where they branch, etc
            Iterable<Relationship> relationships = from.getRelationships(OUTGOING, ON_ROUTE);

            for (Relationship current : relationships) {
                endNodes.add(current.getEndNode());
                // diff outbounds for same route actually a normal situation, where (especially) trains go via
                // different paths even thought route is the "same"
            }
        }

        if (!endNodes.contains(to)) {
            Relationship onRoute = createRelationship(from, to, ON_ROUTE);
            setProperty(onRoute, route);
            setCostProp(onRoute, cost);
            setProperty(onRoute, route.getTransportMode());
        }
    }

    private void createPlatformStationRelationships(Station station, Node stationNode, Platform platform, Node platformNode) {
        boolean isInterchange = interchangeRepository.isInterchange(station);

        // station -> platform
        int enterPlatformCost = isInterchange ? ENTER_INTER_PLATFORM_COST : ENTER_PLATFORM_COST;
        Relationship crossToPlatform = createRelationship(stationNode, platformNode, ENTER_PLATFORM);
        setCostProp(crossToPlatform, enterPlatformCost);
        setProperty(crossToPlatform, platform);

        // platform -> station
        int leavePlatformCost = isInterchange ? LEAVE_INTER_PLATFORM_COST : LEAVE_PLATFORM_COST;
        Relationship crossFromPlatform = createRelationship(platformNode, stationNode, LEAVE_PLATFORM);
        setCostProp(crossFromPlatform, leavePlatformCost);
        setProperty(crossFromPlatform, station);
    }


    private void createRelationshipTimeNodeToRouteStation(Transaction tx, Route route, Trip trip, StopCall beginStop, StopCall endStop,
                                                          GraphBuilderCache routeBuilderCache, Map<StationTime, Node> timeNodes) {
        Station startStation = beginStop.getStation();
        TramTime departureTime = beginStop.getDepartureTime();

        // time node -> end route station
        Node routeStationEnd = routeBuilderCache.getRouteStation(tx, route, endStop.getStation().getId());
        Node timeNode = timeNodes.get(StationTime.of(startStation, beginStop.getDepartureTime()));
        TransportRelationshipTypes transportRelationshipType = TransportRelationshipTypes.forMode(route.getTransportMode());
        Relationship goesToRelationship = createRelationship(timeNode, routeStationEnd, transportRelationshipType);
        // properties on relationship
        setProperty(goesToRelationship, trip);
        int cost = TramTime.diffenceAsMinutes(endStop.getArrivalTime(), departureTime);
        setCostProp(goesToRelationship, cost);
        setProperty(goesToRelationship, trip.getService()); // TODO Still useful?
        setProperty(goesToRelationship, route);
        setStopSequenceNumber(goesToRelationship, endStop.getGetSequenceNumber());
    }

    private Map<StationTime, Node> createMinuteNodes(Transaction tx, Trip trip, GraphBuilderCache builderCache) {

        Map<StationTime, Node> timeNodes = new HashMap<>();

        StopCalls stops = trip.getStopCalls();
        stops.getLegs().forEach(leg -> {
            if (includeBothStops(leg)) {
                Station start = leg.getFirstStation();
                TramTime departureTime = leg.getDepartureTime();
                Node timeNode = createTimeNodeAndRelationshipFromHour(tx, trip, start.getId(), departureTime, builderCache);
                timeNodes.put(StationTime.of(start, departureTime), timeNode);
            }
        });

        return timeNodes;
    }

    private Node createTimeNodeAndRelationshipFromHour(Transaction tx, Trip trip, IdFor<Station> startId, TramTime departureTime,
                                                       GraphBuilderCache builderCache) {

        Node timeNode = createGraphNode(tx, Labels.MINUTE);
        setTimeProp(timeNode, departureTime);
        setProperty(timeNode, trip);

        // hour node -> time node
        Node hourNode = builderCache.getHourNode(tx, trip.getRoute().getId(), trip.getService(), startId, departureTime.getHourOfDay());
        Relationship fromPrevious = createRelationship(hourNode, timeNode, TransportRelationshipTypes.TO_MINUTE);
        setCostProp(fromPrevious, 0);
        setTimeProp(fromPrevious, departureTime);
        setProperty(fromPrevious, trip);

        return timeNode;
    }

    private void createHourNodeAndRelationshipFromService(Transaction tx, IdFor<Route> routeId, Service service, IdFor<Station> startId,
                                                          Integer hour, GraphBuilderCache builderCache, Node serviceNode) {

        if (!builderCache.hasHourNode(routeId, service, startId, hour)) {
            Node hourNode = createGraphNode(tx, Labels.HOUR);
            setHourProp(hourNode, hour);
            builderCache.putHour(routeId, service, startId, hour, hourNode);

            // service node -> time node
            //Node serviceNode = stationCache.getServiceNode(tx, routeId, service, startId, endId);
            Relationship serviceNodeToHour = createRelationship(serviceNode, hourNode, TransportRelationshipTypes.TO_HOUR);
            setCostProp(serviceNodeToHour, 0);
            setHourProp(serviceNodeToHour, hour);
        }

    }

    public static class Ready {
        private Ready() {
            // prevent guice creating this, want to create dependency on the Builder
        }
    }

    public static class ServiceRelationshipTrips {
        private final ConcurrentMap<Long, IdSet<Trip>> map;

        public ServiceRelationshipTrips() {
            this.map = new ConcurrentHashMap<>();
        }

        public void updateAll(Transaction txn) {
            map.entrySet().forEach(entry -> update(txn, entry));
            map.values().forEach(IdSet::clear);
            map.clear();
        }

        private void update(Transaction txn, Map.Entry<Long, IdSet<Trip>> entry) {
            Relationship relationship = txn.getRelationshipById(entry.getKey());
            setTripsProp(relationship, entry.getValue());
        }

        public void add(long id, IdFor<Trip> tripId) {
            map.computeIfAbsent(id, key -> new IdSet<>());
            map.computeIfPresent(id, (key,ids) -> ids.add(tripId));
//            if (map.containsKey(id)) {
//                map.get(id).add(tripId);
//                return;
//            }
//            map.put(id, new IdSet<>(tripId));
        }
    }

}
