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
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.StationTime;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
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
                                       NodeTypeRepository nodeIdLabelMap, TransportData transportData,
                                       InterchangeRepository interchangeRepository, GraphBuilderCache builderCache,
                                       StationsAndLinksGraphBuilder.Ready readyToken) {
        super(graphDatabase, graphFilter, config, builderCache, nodeIdLabelMap);
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
            buildGraphwithFilter(graphFilter, graphDatabase, builderCache);
            logger.info("Graph rebuild is finished for " + config.getDbPath());
        } else {
            logger.info("No rebuild of graph, using existing data");
            nodeTypeRepository.populateNodeLabelMap(graphDatabase);
        }
    }

    private void buildGraphwithFilter(GraphFilter graphFilter, GraphDatabase graphDatabase, GraphBuilderCache builderCache) {
        logger.info("Building graph for feedinfo: " + transportData.getDataSourceInfo());
        logMemory("Before graph build");

        graphDatabase.createIndexs();

        try(Timing ignored = new Timing(logger, "Graph rebuild")) {
            // TODO Agencies could be done in parallel as should be no overlap except at route station level?
            // Performance only really an issue for buses currently.
            for(Agency agency : transportData.getAgencies()) {
                    if (graphFilter.shouldInclude(agency)) {
                        try (Timing agencyTiming = new Timing(logger,"Add agency " + agency.getId())) {
                            Stream<Route> routes = agency.getRoutes().stream().filter(
                                    route -> graphFilter.shouldInclude(transportData, route));
                            buildGraphForRoutes(graphDatabase, graphFilter, routes, builderCache);
                        }
                    } else {
                        logger.warn("Filter out: " + agency);
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

    private void addVersionNode(GraphDatabase graphDatabase, Set<DataSourceInfo> infos) {
        try(Transaction tx = graphDatabase.beginTx()) {
            logger.info("Adding version node to the DB");
            Node node = graphDatabase.createNode(tx, Labels.VERSION);
            infos.forEach(nameAndVersion -> setProp(node, nameAndVersion));
            tx.commit();
        }
    }

    private void buildGraphForRoutes(GraphDatabase graphDatabase, final GraphFilter filter, Stream<Route> routes,
                                     GraphBuilderCache builderCache) {
        Set<Station> filteredStations = filter.isFiltered() ?
                transportData.getStations().stream().filter(filter::shouldInclude).collect(Collectors.toSet()) :
                transportData.getStations();

        routes.forEach(route -> {
            IdFor<Route> asId = route.getId();
            try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger,"routes " +asId)) {
                Transaction tx = timedTransaction.transaction();
                // create or cache stations and platforms for route, create route stations
                filteredStations.stream().filter(station -> station.servesRoute(route)).
                        forEach(station -> createStationAndPlatforms(tx, station, builderCache));

                // route relationships
                createRouteRelationships(tx, filter, route, builderCache);

                buildGraphForServiceHourAndMinutes(filter, route, builderCache, tx);

                timedTransaction.commit();
            }
            builderCache.routeClear();
            logger.debug("Route " + asId + " added ");
        });
    }

    private void buildGraphForServiceHourAndMinutes(GraphFilter filter, Route route, GraphBuilderCache routeBuilderCache,
                                                    Transaction tx) {

            // nodes for services and hours
            createServiceAndHourNodesForServices(tx, filter, route, route.getTrips(), routeBuilderCache);
            // time nodes and relationships for trips
            for (Trip trip : route.getTrips()) {
                Map<StationTime, Node> timeNodes = createMinuteNodes(tx, filter, trip, routeBuilderCache);
                createBoardingAndDeparts(tx, filter, route, trip, routeBuilderCache);
                createTripRelationships(tx, filter, route, trip, routeBuilderCache, timeNodes);
                timeNodes.clear();
            }
    }

    private void createServiceAndHourNodesForServices(Transaction tx, GraphFilter filter, Route route, Set<Trip> trips,
                                                      GraphBuilderCache stationCache) {
        trips.forEach(trip -> {
                StopCalls stops = trip.getStopCalls();
                List<StopCalls.StopLeg> legs = stops.getLegs();
                legs.forEach(leg -> {
                    if (includeBothStops(filter, leg)) {
                        IdFor<Station> beginId = leg.getFirstStation().getId();
                        Station end = leg.getSecondStation();

                        createServiceNodeAndRelationship(tx, route, trip.getService(),
                                beginId, end, stationCache);

                        createHourNode(tx, route.getId(), trip.getService(), beginId,
                                end.getId(), leg.getDepartureTime().getHourOfDay(), stationCache);
                    }
                });
        });
    }

    private void createServiceNodeAndRelationship(Transaction tx, Route route, Service service, IdFor<Station> begin, Station end,
                                                  GraphBuilderCache routeBuilderCache) {

        // Node for the service
        // -route ID here as some towardsServices can go via multiple routes, this seems to be associated with the depots
        // -some towardsServices can go in two different directions from a station i.e. around Media City UK

        IdFor<Station> endId = end.getId();
        if (!routeBuilderCache.hasServiceNode(route.getId(), service, begin, endId)) {
            Node svcNode = createGraphNode(tx, Labels.SERVICE);
            setProperty(svcNode, service);
            setProperty(svcNode, route);

            // TODO This is used to look up station and hence lat/long for distance ordering, store
            //  org.neo4j.graphdb.spatial.Point instead?
            setTowardsProp(svcNode, endId);

            routeBuilderCache.putService(route.getId(), service, begin, endId, svcNode);

            // start route station -> svc node
            Node routeStationStart = routeBuilderCache.getRouteStation(tx, route, begin);
            Relationship svcRelationship = createRelationship(routeStationStart, svcNode, TransportRelationshipTypes.TO_SERVICE);
            setProperty(svcRelationship, service);
            setCostProp(svcRelationship, 0);
            setProperty(svcRelationship, route);
        }
    }

    private Node getPlatformNode(Transaction txn, Platform platform) {
        return graphDatabase.findNode(txn, Labels.PLATFORM, platform.getProp().getText(), platform.getId().getGraphId());
    }

    private Node getStationNode(Transaction txn, Station station) {
        Set<GraphBuilder.Labels> labels = GraphBuilder.Labels.forMode(station.getTransportModes());
        // ought to be able find with any of the labels, os use the first one
        GraphBuilder.Labels label = labels.iterator().next();

        return graphDatabase.findNode(txn, label, station.getProp().getText(), station.getId().getGraphId());
    }

    private void createStationAndPlatforms(Transaction txn, Station station, GraphBuilderCache routeBuilderCache) {

        Node stationNode = getStationNode(txn, station);
        if (stationNode!=null) {
            routeBuilderCache.putStation(station.getId(), stationNode);
            for (Platform platform : station.getPlatforms()) {
                Node platformNode = getPlatformNode(txn, platform);
                if (platformNode==null) {
                    platformNode = createPlatformNode(txn, platform);
                    routeBuilderCache.putPlatform(platform.getId(), platformNode);
                    createPlatformStationRelationships(station, stationNode, platform, platformNode);
                }
                routeBuilderCache.putPlatform(platform.getId(), platformNode);
            }
        } else {
            throw new RuntimeException("Missing station node for " + station);
        }
    }

    private void createRouteRelationships(Transaction tx, GraphFilter filter, Route route, GraphBuilderCache routeBuilderCache) {
        //Stream<Service> services = getServices(filter, route);

        Map<StationIdPair, Integer> pairs = new HashMap<>();
        //services.forEach(service -> service.getTrips().forEach(trip -> {
        route.getTrips().forEach(trip -> {
            StopCalls stops = trip.getStopCalls();
            stops.getLegs().forEach(leg -> {
                if (includeBothStops(filter, leg)) {
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
            createRouteRelationship(startNode, endNode, route, cost);
        });
}

    private boolean includeBothStops(GraphFilter filter, StopCalls.StopLeg leg) {
        return filter.shouldInclude(leg.getFirst()) && filter.shouldInclude(leg.getSecond());
    }

    private void createBoardingAndDeparts(Transaction tx, GraphFilter filter, Route route, Trip trip, GraphBuilderCache routeBuilderCache) {
        StopCalls stops = trip.getStopCalls();

        stops.stream().filter(filter::shouldInclude).forEach(stopCall ->
                createBoardingAndDepart(tx, routeBuilderCache, stopCall, route, trip));
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

        if ((!(pickup||dropoff)) && (!TransportMode.isTrain(route))) {
            // this is normal for trains, timetable lists all passed stations, whether train stops or not
            logger.warn("No pickup or dropoff for " + stopCall);
        }
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

    private void createTripRelationships(Transaction tx, GraphFilter filter, Route route, Trip trip,
                                         GraphBuilderCache routeBuilderCache,
                                         Map<StationTime, Node> timeNodes) {
        StopCalls stops = trip.getStopCalls();

        stops.getLegs().forEach(leg -> {
            if (includeBothStops(filter, leg)) {
                updateTripRelationship(tx, route, trip, leg.getFirst(), leg.getSecond(), routeBuilderCache, timeNodes);
            }
        });
    }

    private void createRouteRelationship(Node from, Node to, Route route, int cost) {
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
            Relationship onRoute = from.createRelationshipTo(to, TransportRelationshipTypes.ON_ROUTE);
            setProperty(onRoute, route);
            setCostProp(onRoute, cost);
            setProperty(onRoute, route.getTransportMode());
        }
    }

    private Node createPlatformNode(Transaction tx, Platform platform) {
        Node platformNode = createGraphNode(tx, Labels.PLATFORM);
        setProperty(platformNode, platform);
        return platformNode;
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

    private void updateTripRelationship(Transaction tx, Route route, Trip trip, StopCall beginStop, StopCall endStop,
                                        GraphBuilderCache routeBuilderCache, Map<StationTime, Node> timeNodes) {
        Station startStation = beginStop.getStation();

        IdFor<Trip> tripId = trip.getId();
        Node beginServiceNode = routeBuilderCache.getServiceNode(tx, route.getId(), trip.getService(), startStation.getId(),
                endStop.getStation().getId());

        beginServiceNode.getRelationships(INCOMING, TransportRelationshipTypes.TO_SERVICE).forEach(
                relationship -> {
                    IdSet<Trip> tripIds = getTrips(relationship);
                    if (!tripIds.contains(tripId)) {
                        tripIds.add(tripId);
                        setTripsProp(relationship, tripIds);
                    }
                });

        TramTime departureTime = beginStop.getDepartureTime();

        TransportRelationshipTypes transportRelationshipType = TransportRelationshipTypes.forMode(route.getTransportMode());

        Node routeStationEnd = routeBuilderCache.getRouteStation(tx, route, endStop.getStation().getId());

        // time node -> end route station
        Node timeNode = timeNodes.get(StationTime.of(startStation, beginStop.getDepartureTime()));

        Relationship goesToRelationship = createRelationship(timeNode, routeStationEnd, transportRelationshipType);
        setProperty(goesToRelationship, trip);

        int cost = TramTime.diffenceAsMinutes(endStop.getArrivalTime(), departureTime);
        setCostProp(goesToRelationship, cost);
        setProperty(goesToRelationship, trip.getService()); // TODO Still useful?
        setProperty(goesToRelationship, route);
        setStopSequenceNumber(goesToRelationship, endStop.getGetSequenceNumber());
    }

    private Map<StationTime, Node> createMinuteNodes(Transaction tx, GraphFilter filter,
                                                     Trip trip, GraphBuilderCache builderCache) {

        Map<StationTime, Node> timeNodes = new HashMap<>();

        StopCalls stops = trip.getStopCalls();
        stops.getLegs().forEach(leg -> {
            if (includeBothStops(filter, leg)) {
                Station start = leg.getFirstStation();
                TramTime departureTime = leg.getDepartureTime();
                Node timeNode = createMinuteNode(tx, trip, start.getId(), departureTime, builderCache);
                timeNodes.put(StationTime.of(start, departureTime), timeNode);
            }
        });

        return timeNodes;
    }

    private Node createMinuteNode(Transaction tx, Trip trip, IdFor<Station> startId, TramTime departureTime,
                                  GraphBuilderCache builderCache) {
        //LocalTime time = departureTime.asLocalTime();

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

    private void createHourNode(Transaction tx, IdFor<Route> routeId, Service service, IdFor<Station> startId, IdFor<Station> endId,
                                Integer hour, GraphBuilderCache stationCache) {

        if (!stationCache.hasHourNode(routeId, service, startId, hour)) {
            Node hourNode = createGraphNode(tx, Labels.HOUR);
            setHourProp(hourNode, hour);
            stationCache.putHour(routeId, service, startId, hour, hourNode);

            // service node -> time node
            Node serviceNode = stationCache.getServiceNode(tx, routeId, service, startId, endId);
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

}
