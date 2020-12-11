package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.graph.*;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportData;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
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
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

@LazySingleton
public class StagedTransportGraphBuilder extends GraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(StagedTransportGraphBuilder.class);

    ///
    // Station -[enter]-> Platform -[board]-> RouteStation -[toSvc]-> Service -> Hour-[toMinute]->
    //          -> Minute -> RouteStation-[depart]-> Platform -[leave]-> Station
    //
    // RouteStation-[onRoute]->RouteStation
    //
    ///

    private final TransportData transportData;
    private final InterchangeRepository interchangeRepository;

    public Ready getReady() {
        return new Ready();
    }

    @Inject
    public StagedTransportGraphBuilder(GraphDatabase graphDatabase, TramchesterConfig config, GraphFilter graphFilter,
                                       NodeTypeRepository nodeIdLabelMap, TransportData transportData,
                                       InterchangeRepository interchangeRepository) {
        super(graphDatabase, graphFilter, config, nodeIdLabelMap);
        this.transportData = transportData;
        this.interchangeRepository = interchangeRepository;
    }

    @PostConstruct
    public void run() {
        super.start();
    }

    @Deprecated
    @Override
    protected void buildGraph(GraphDatabase graphDatabase) {
        buildGraphwithFilter(new IncludeAllFilter(), graphDatabase);
    }

    @Override
    protected void buildGraphwithFilter(GraphFilter graphFilter, GraphDatabase graphDatabase) {
        logger.info("Building graph for feedinfo: " + transportData.getDataSourceInfo());
        logMemory("Before graph build");
        long start = System.currentTimeMillis();

        graphDatabase.createIndexs();

        addVersionNode(graphDatabase, transportData.getDataSourceInfo());

        try {
            logger.info("Rebuilding the graph...");

            for(Agency agency : transportData.getAgencies()) {
                logger.info("Adding agency " + agency.getId());
                Stream<Route> routes = agency.getRoutes().stream().filter(graphFilter::shouldInclude);
                buildGraphForRoutes(graphDatabase, graphFilter, routes);
                logger.info("Finished agency " + agency.getId());
            }

            try(Transaction tx = graphDatabase.beginTx()) {
                logger.info("Wait for indexes online");
                graphDatabase.waitForIndexesReady(tx);
            }

            long duration = System.currentTimeMillis()-start;
            logger.info("Graph rebuild finished, took " + duration);

        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
            throw new RuntimeException("Unable to build graph", except);
        }
        reportStats();
        logMemory("After graph build");
        System.gc();
    }

    private void addVersionNode(GraphDatabase graphDatabase, Set<DataSourceInfo> infos) {
        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, Labels.VERSION);
            infos.forEach(nameAndVersion -> setProp(node, nameAndVersion));
            tx.commit();
        }
    }

    private void buildGraphForRoutes(GraphDatabase graphDatabase, final GraphFilter filter, Stream<Route> routes) {
        Set<Station> filteredStations = filter.isFiltered() ?
                transportData.getStations().stream().filter(filter::shouldInclude).collect(Collectors.toSet()) :
                transportData.getStations();

        routes.forEach(route -> {
            RouteBuilderCache routeBuilderCache = new RouteBuilderCache();
            IdFor<Route> asId = route.getId();
            logger.debug("Adding route " + asId);

            try(Transaction tx = graphDatabase.beginTx()) {
                // create or cache stations and platforms for route, create route stations
                filteredStations.stream().filter(station -> station.servesRoute(route)).
                        forEach(station -> createStationAndPlatforms(tx, route, station, routeBuilderCache));

                // route relationships
                createRouteRelationships(tx, filter, route, routeBuilderCache);
                tx.commit();
            }

            Stream<Service> services = getServices(filter, route);
            buildGraphForServices(graphDatabase, filter, route, routeBuilderCache, services);

            routeBuilderCache.clear();
            logger.debug("Route " + asId + " added ");
        });
    }

    private void buildGraphForServices(GraphDatabase graphDatabase, GraphFilter filter, Route route,
                                       RouteBuilderCache routeBuilderCache, Stream<Service> services) {
        services.forEach(service -> {
            try(Transaction tx = graphDatabase.beginTx()) {
                Set<Trip> serviceTrips = service.getTripsFor(route);
                // nodes for services and hours
                createServiceAndHourNodesForServices(tx, filter, route, service, serviceTrips, routeBuilderCache);

                // time nodes and relationships for trips
                for (Trip trip : serviceTrips) {
                    Map<Pair<Station, ServiceTime>, Node> timeNodes = createMinuteNodes(tx, filter, service, trip, routeBuilderCache);
                    createBoardingAndDeparts(tx, filter, route, trip, routeBuilderCache);
                    createTripRelationships(tx, filter, route, service, trip, routeBuilderCache, timeNodes);
                    timeNodes.clear();
                }
                tx.commit();
            }

        });
    }

    private void createServiceAndHourNodesForServices(Transaction tx, GraphFilter filter, Route route, Service service, Set<Trip> trips,
                                                      RouteBuilderCache stationCache) {
        Set<Pair<Station, Station>> services = new HashSet<>();
        Set<Triple<Station, Station, Integer>> hours = new HashSet<>();

        trips.forEach(trip -> {
                StopCalls stops = trip.getStops();
                List<StopCalls.StopLeg> legs = stops.getLegs();
                legs.forEach(leg -> {
                    if (includeBothStops(filter, leg)) {
                        services.add(Pair.of(leg.getFirstStation(), leg.getSecondStation()));
                        hours.add(Triple.of(leg.getFirstStation(), leg.getSecondStation(), leg.getDepartureTime().getHourOfDay()));
                    }
                });
        });
        services.forEach(pair -> createServiceNodeAndRelationship(tx, route, service, pair.getLeft(), pair.getRight(), stationCache));
        hours.forEach(triple -> createHourNode(tx, service, triple.getLeft(), triple.getMiddle(), triple.getRight(), stationCache));
    }


    private void createServiceNodeAndRelationship(Transaction tx, Route route, Service service, Station begin, Station end,
                                                  RouteBuilderCache routeBuilderCache) {

        // Node for the service
        // -route ID here as some towardsServices can go via multiple routes, this seems to be associated with the depots
        // -some towardsServices can go in two different directions from a station i.e. around Media City UK

//        String beginSvcNodeId = begin.getId().getGraphId()+"_"+end.getId().getGraphId()+"_"+ service.getId().getGraphId();

        Node routeStationStart = routeBuilderCache.getRouteStation(tx, route, begin);

        Node svcNode = createGraphNode(tx, Labels.SERVICE);
//        setIdProp(svcNode, beginSvcNodeId);
        setProperty(svcNode, service);
        setProperty(svcNode, route);
        setTowardsProp(svcNode, end.getId());

        routeBuilderCache.putService(service, begin, end, svcNode);

        // start route station -> svc node
        Relationship svcRelationship = createRelationship(routeStationStart, svcNode, TransportRelationshipTypes.TO_SERVICE);
        setProperty(svcRelationship, service);
        setCostProp(svcRelationship, 0);
        setProperty(svcRelationship, route);
        setTripsProp(svcRelationship, "");

    }

    private Node getPlatformNode(Transaction txn, Platform platform) {
        return graphDatabase.findNode(txn, Labels.PLATFORM, platform.getProp().getText(), platform.getId().getGraphId());
    }

    private Node getStationNode(Transaction txn, Station station) {
        return graphDatabase.findNode(txn, Labels.forMode(station.getTransportMode()),
                station.getProp().getText(), station.getId().getGraphId());
    }

    private void createStationAndPlatforms(Transaction txn, Route route, Station station, RouteBuilderCache routeBuilderCache) {

        RouteStation routeStation = transportData.getRouteStation(station, route);
        createRouteStationNode(txn, routeStation, routeBuilderCache);

        Node stationNode = getStationNode(txn, station);
        if (stationNode!=null) {
            routeBuilderCache.putStation(station, stationNode);
            for (Platform platform : station.getPlatforms()) {
                Node platformNode = getPlatformNode(txn, platform);
                routeBuilderCache.putPlatform(platform.getId(), platformNode);
            }
        } else {
            stationNode = createStationNode(txn, station);
            routeBuilderCache.putStation(station, stationNode);
            for (Platform platform : station.getPlatforms()) {
                Node platformNode = createPlatformNode(txn, platform);
                routeBuilderCache.putPlatform(platform.getId(), platformNode);
                createPlatformStationRelationships(station, stationNode, platform, platformNode);
            }
        }
    }

    private void createRouteRelationships(Transaction tx, GraphFilter filter, Route route, RouteBuilderCache routeBuilderCache) {
        Stream<Service> services = getServices(filter, route);

        Map<Pair<Station, Station>, Integer> pairs = new HashMap<>();
        services.forEach(service -> service.getTripsFor(route).forEach(trip -> {
                StopCalls stops = trip.getStops();
                stops.getLegs().forEach(leg -> {
                    if (includeBothStops(filter, leg)) {
                        if (!pairs.containsKey(Pair.of(leg.getFirstStation(), leg.getSecondStation()))) {
                            int cost = leg.getCost();
                            pairs.put(Pair.of(leg.getFirstStation(), leg.getSecondStation()), cost);
                        }
                    }
                });
            }));
        pairs.forEach((pair, cost) -> createRouteRelationship(
                routeBuilderCache.getRouteStation(tx, route, pair.getLeft()),
                routeBuilderCache.getRouteStation(tx, route, pair.getRight()), route, cost));

    }

    private boolean includeBothStops(GraphFilter filter, StopCalls.StopLeg leg) {
        return filter.shouldInclude(leg.getFirst()) && filter.shouldInclude(leg.getSecond());
    }

    @NotNull
    private Stream<Service> getServices(GraphFilter filter, Route route) {
        return route.getServices().stream().filter(filter::shouldInclude);
    }

    private void createBoardingAndDeparts(Transaction tx, GraphFilter filter, Route route, Trip trip, RouteBuilderCache routeBuilderCache) {
        StopCalls stops = trip.getStops();

        stops.stream().filter(filter::shouldInclude).forEach(stopCall ->
                createBoardingAndDepart(tx, routeBuilderCache, stopCall, route, trip));
    }

    private void createBoardingAndDepart(Transaction tx, RouteBuilderCache routeBuilderCache, StopCall stopCall,
                                         Route route, Trip trip) {

        // TODO when filtering this isn't really valid, we might only see a small segment of a larger trip....
        boolean isFirstStop = stopCall.getGetSequenceNumber() == trip.getSeqNumOfFirstStop(); //stop seq num, not index
        boolean isLastStop = stopCall.getGetSequenceNumber() == trip.getSeqNumOfLastStop();

        boolean pickup = stopCall.getPickupType().equals(GTFSPickupDropoffType.Regular);
        boolean dropoff = stopCall.getDropoffType().equals(GTFSPickupDropoffType.Regular);

        if (isFirstStop && dropoff) {
            String msg = "Drop off at first station for stop " + stopCall.getStation().getId() + " dep time " + stopCall.getDepartureTime();
            logger.info(msg);
        }

        if (isLastStop && pickup) {
            String msg = "Pick up at last station for stop " + stopCall.getStation().getId() + " dep time " + stopCall.getDepartureTime();
            logger.info(msg);
        }

        Station station = stopCall.getStation();
        boolean isInterchange = interchangeRepository.isInterchange(station);

        // If bus we board to/from station, for trams its from the platform
        Node boardingNode = station.hasPlatforms() ? routeBuilderCache.getPlatform(tx, stopCall.getPlatform().getId())
                : routeBuilderCache.getStation(tx, station);
        IdFor<RouteStation> routeStationId = IdFor.createId(station, route);
        Node routeStationNode = routeBuilderCache.getRouteStation(tx, route, stopCall.getStation());

        // boarding: platform/station ->  callingPoint , NOTE: no boarding at the last stop of a trip
        if (pickup && !routeBuilderCache.hasBoarding(boardingNode.getId(), routeStationNode.getId())) {
            createBoarding(routeBuilderCache, stopCall, route, station, isInterchange, boardingNode, routeStationId, routeStationNode);
        }

        // leave: route station -> platform/station , NOTE: no towardsStation at first stop of a trip
        if (dropoff && !routeBuilderCache.hasDeparts(routeStationNode.getId(), boardingNode.getId()) ) {
            createDeparts(routeBuilderCache, station, isInterchange, boardingNode, routeStationId, routeStationNode);
        }

        if ((!(pickup||dropoff)) && (!TransportMode.isTrain(route))) {
            logger.warn("No pickup or dropoff for " + stopCall.toString());
        }
    }

    private void createDeparts(RouteBuilderCache routeBuilderCache, Station station, boolean isInterchange,
                               Node boardingNode, IdFor<RouteStation> routeStationId, Node routeStationNode) {
        TransportRelationshipTypes departType = isInterchange ? INTERCHANGE_DEPART : DEPART;
        int departCost = isInterchange ? INTERCHANGE_DEPART_COST : DEPARTS_COST;

        Relationship departRelationship = createRelationship(routeStationNode, boardingNode, departType);
        setCostProp(departRelationship, departCost);
        setRouteStationProp(departRelationship, routeStationId);
        setProperty(departRelationship, station);
        routeBuilderCache.putDepart(boardingNode.getId(), routeStationNode.getId());
    }

    private void createBoarding(RouteBuilderCache routeBuilderCache, StopCall stop, Route route, Station station,
                                boolean isInterchange, Node boardingNode, IdFor<RouteStation> routeStationId, Node routeStationNode) {
        TransportRelationshipTypes boardType = isInterchange ? INTERCHANGE_BOARD : BOARD;
        int boardCost = isInterchange ? INTERCHANGE_BOARD_COST : BOARDING_COST;
        Relationship boardRelationship = createRelationship(boardingNode, routeStationNode, boardType);
        setCostProp(boardRelationship, boardCost);
        setRouteStationProp(boardRelationship, routeStationId);
        setProperty(boardRelationship, route);
        setProperty(boardRelationship, station);
        // No platform ID on buses
        if (stop.hasPlatfrom()) {
            setProperty(boardRelationship, stop.getPlatform());
        }
        routeBuilderCache.putBoarding(boardingNode.getId(), routeStationNode.getId());
    }

    private void createTripRelationships(Transaction tx, GraphFilter filter, Route route, Service service, Trip trip,
                                         RouteBuilderCache routeBuilderCache,
                                         Map<Pair<Station, ServiceTime>, Node> timeNodes) {
        StopCalls stops = trip.getStops();

        stops.getLegs().forEach(leg -> {
            if (includeBothStops(filter, leg)) {
                updateTripRelationship(tx, route, service, trip, leg.getFirst(), leg.getSecond(), routeBuilderCache, timeNodes);
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
                // normal situation, where (especially) trains go via different paths even thought route is the "same"

//                logger.info(format("Existing outbounds at %s for same route %s currently has %s, new is %s",
//                        from.getProperty(STATION_ID), route.getId(),
//                        current.getEndNode().getProperty(STATION_ID),
//                        to.getProperty(STATION_ID)));
            }

        }

        if (!endNodes.contains(to)) {
            Relationship onRoute = from.createRelationshipTo(to, TransportRelationshipTypes.ON_ROUTE);
            setProperty(onRoute, route);
            setCostProp(onRoute, cost);
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

    private void updateTripRelationship(Transaction tx, Route route, Service service, Trip trip, StopCall beginStop, StopCall endStop,
                                        RouteBuilderCache routeBuilderCache, Map<Pair<Station, ServiceTime>, Node> timeNodes) {
        Station startStation = beginStop.getStation();

        IdFor<Trip> tripId = trip.getId();
        Node beginServiceNode = routeBuilderCache.getServiceNode(tx, service, startStation, endStop.getStation());

        beginServiceNode.getRelationships(INCOMING, TransportRelationshipTypes.TO_SERVICE).forEach(
                relationship -> {
                    String tripIds = GraphProps.getTrips(relationship);
                    if (!tripIds.contains(tripId.getGraphId())) {
                        setTripsProp(relationship, tripId.getGraphId() + tripIds);
                    }
                });

        ServiceTime departureTime = beginStop.getDepartureTime();

        TransportRelationshipTypes transportRelationshipType = TransportRelationshipTypes.from(route.getTransportMode());

        Node routeStationEnd = routeBuilderCache.getRouteStation(tx, route, endStop.getStation());

        // time node -> end route station
        Node timeNode = timeNodes.get(Pair.of(startStation, beginStop.getDepartureTime()));
        Relationship goesToRelationship = createRelationship(timeNode, routeStationEnd, transportRelationshipType);
        setProperty(goesToRelationship, trip);

        int cost = ServiceTime.diffenceAsMinutes(endStop.getArrivalTime(), departureTime);
        setCostProp(goesToRelationship, cost);
        setProperty(goesToRelationship, service);
        setProperty(goesToRelationship, route);
    }

    private Map<Pair<Station, ServiceTime>, Node> createMinuteNodes(Transaction tx, GraphFilter filter, Service service,
                                                                    Trip trip, RouteBuilderCache routeBuilderCache) {

        Map<Pair<Station, ServiceTime>, Node> timeNodes = new HashMap<>();

        StopCalls stops = trip.getStops();
        stops.getLegs().forEach(leg -> {
            if (includeBothStops(filter, leg)) {
                Station start = leg.getFirstStation();
                ServiceTime departureTime = leg.getDepartureTime();
                Node timeNode = createMinuteNode(tx, service, trip, start, departureTime, routeBuilderCache);
                timeNodes.put(Pair.of(start, departureTime), timeNode);
            }
        });

        return timeNodes;
    }

    private Node createMinuteNode(Transaction tx, Service service, Trip trip, Station start, ServiceTime departureTime,
                                  RouteBuilderCache stationCache) {
        //LocalTime time = departureTime.asLocalTime();

        Node timeNode = createGraphNode(tx, Labels.MINUTE);
        setTimeProp(timeNode, departureTime);
        setProperty(timeNode, trip);

        // hour node -> time node
        Node hourNode = stationCache.getHourNode(tx, service, start, departureTime.getHourOfDay());
        Relationship fromPrevious = createRelationship(hourNode, timeNode, TransportRelationshipTypes.TO_MINUTE);
        setCostProp(fromPrevious, 0);
        setTimeProp(fromPrevious, departureTime);
        setProperty(fromPrevious, trip);

        return timeNode;
    }

    private void createHourNode(Transaction tx, Service service, Station start, Station end, Integer hour, RouteBuilderCache stationCache) {
        Node hourNode = createGraphNode(tx, Labels.HOUR);

        setHourProp(hourNode, hour);

        stationCache.putHour(service, start, hour, hourNode);

        // service node -> time node
        Node serviceNode = stationCache.getServiceNode(tx, service, start, end);
        Relationship fromServiceNode = createRelationship(serviceNode, hourNode, TransportRelationshipTypes.TO_HOUR);
        setCostProp(fromServiceNode, 0);
        setHourProp(fromServiceNode, hour);
    }

    private void createRouteStationNode(Transaction tx, RouteStation routeStation, RouteBuilderCache routeBuilderCache) {
        Node routeStationNode = createGraphNode(tx, Labels.ROUTE_STATION);

        logger.debug(format("Creating route station %s nodeId %s", routeStation.getId(), routeStationNode.getId()));
        GraphProps.setProperty(routeStationNode, routeStation);
        setProperty(routeStationNode, routeStation.getStation());
        setProperty(routeStationNode, routeStation.getRoute());

        routeBuilderCache.putRouteStation(routeStation.getId(), routeStationNode);

    }

    private Node createStationNode(Transaction tx, Station station) {

        Labels label = Labels.forMode(station.getTransportMode());
        logger.debug(format("Creating station node: %s with label: %s ", station, label));
        Node stationNode = createGraphNode(tx, label);
        setProperty(stationNode, station);
        return stationNode;
    }

    private static class RouteBuilderCache {
        private final Map<IdFor<RouteStation>, Long> routeStations;
        private final Map<Station, Long> stations;
        private final Map<IdFor<Platform>, Long> platforms;
        private final Map<String, Long> svcNodes;
        private final Map<String, Long> hourNodes;
        private final Set<Pair<Long,Long>> boardings;
        private final Set<Pair<Long,Long>> departs;

        private RouteBuilderCache() {
            stations = new HashMap<>();
            routeStations = new HashMap<>();
            platforms = new HashMap<>();
            svcNodes = new HashMap<>();
            hourNodes = new HashMap<>();
            boardings = new HashSet<>();
            departs = new HashSet<>();
        }

        protected void clear() {
            routeStations.clear();
            stations.clear();
            platforms.clear();
            svcNodes.clear();
            hourNodes.clear();
        }

        protected void putRouteStation(IdFor<RouteStation> id, Node routeStationNode) {
            routeStations.put(id, routeStationNode.getId());
        }

        protected void putStation(Station station, Node stationNode) {
            stations.put(station, stationNode.getId());
        }

        protected Node getRouteStation(Transaction txn, Route route, Station station) {
            IdFor<RouteStation> id = IdFor.createId(station,route);
            if (!routeStations.containsKey(id)) {
                String message = "Cannot find routestation node in cache " + id + " station "
                        + station.getId() + " route " + route.getId();
                logger.error(message);
                throw new RuntimeException(message);
            }
            return txn.getNodeById(routeStations.get(id));
        }

        protected Node getStation(Transaction txn, Station station) {
            return txn.getNodeById(stations.get(station));
        }

        protected Node getPlatform(Transaction txn, IdFor<Platform> platformId) {
            return txn.getNodeById(platforms.get(platformId));
        }

        protected void putPlatform(IdFor<Platform> platformId, Node platformNode) {
            platforms.put(platformId, platformNode.getId());
        }

        protected Node getServiceNode(Transaction txn, Service service, Station startStation, Station endStation) {
            String id = CreateKeys.getServiceKey(service, startStation, endStation);
            return txn.getNodeById(svcNodes.get(id));
        }

        protected void putService(Service service, Station begin, Station end, Node svcNode) {
            svcNodes.put(CreateKeys.getServiceKey(service, begin, end), svcNode.getId());
        }

        protected void putHour(Service service, Station station, Integer hour, Node node) {
            hourNodes.put(CreateKeys.getHourKey(service, station, hour), node.getId());
        }

        protected Node getHourNode(Transaction txn, Service service, Station station, Integer hour) {
            String key = CreateKeys.getHourKey(service, station, hour);
            if (!hourNodes.containsKey(key)) {
                throw new RuntimeException(format("Missing hour node for key %s service %s station %s hour %s",
                        key, service.getId(), station.getId(), hour.toString()));
            }
            return txn.getNodeById(hourNodes.get(key));
        }

        protected boolean hasBoarding(long boardingNodeId, long routeStationNodeId) {
            return boardings.contains(Pair.of(boardingNodeId, routeStationNodeId));
        }

        protected void putBoarding(long boardingNodeId, long routeStationNodeId) {
            boardings.add(Pair.of(boardingNodeId, routeStationNodeId));
        }

        protected boolean hasDeparts(long routeStationNodeId, long boardingNodeId) {
            return departs.contains(Pair.of(routeStationNodeId, boardingNodeId));
        }

        protected void putDepart(long boardingNodeId, long routeStationNodeId) {
            departs.add(Pair.of(routeStationNodeId, boardingNodeId));
        }
    }

    private static class CreateKeys {
        protected static String getServiceKey(Service service, Station startStation, Station endStation) {
            return startStation.getId().getGraphId()+"_"+endStation.getId().getGraphId()+"_"+ service.getId().getGraphId();
        }

        protected static String getHourKey(Service service, Station station, Integer hour) {
            return service.getId().getGraphId()+"_"+station.getId().getGraphId()+"_"+hour.toString();
        }

//        public static String getMinuteKey(Trip trip, Station start, ServiceTime time) {
//            return trip.getId().getGraphId() +"_"+ start.getId().getGraphId() + "_" + time.toPattern();
//        }
    }

}
