package com.tramchester.graph.graphbuild;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
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

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

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

    public StagedTransportGraphBuilder(GraphDatabase graphDatabase, TramchesterConfig config, GraphFilter graphFilter,
                                       GraphQuery graphQuery, NodeTypeRepository nodeIdLabelMap, TransportData transportData,
                                       InterchangeRepository interchangeRepository) {
        super(graphDatabase, graphQuery, graphFilter, config, nodeIdLabelMap);
        this.transportData = transportData;
        this.interchangeRepository = interchangeRepository;
    }

    @Override
    protected void buildGraph(GraphDatabase graphDatabase) {
        buildGraphwithFilter(new IncludeAllFilter(), graphDatabase);
    }

    @Override
    protected void buildGraphwithFilter(GraphFilter graphFilter, GraphDatabase graphDatabase) {
        logger.info("Building graph from " + transportData.getVersion());
        logMemory("Before graph build");
        long start = System.currentTimeMillis();

        graphDatabase.createIndexs();

        addVersionNode(graphDatabase, transportData.getVersion());

        try {
            logger.info("Rebuilding the graph...");

            for(Agency agency : transportData.getAgencies()) {
                logger.info("Adding agency " + agency.getId());
                Stream<Route> routes = agency.getRoutes().stream().filter(graphFilter::shouldInclude);
                buildGraphForRoutes(graphDatabase, graphFilter, routes);
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

    // todo change to version
    private void addVersionNode(GraphDatabase graphDatabase, String version) {
        try(Transaction tx = graphDatabase.beginTx()) {
            Node node = graphDatabase.createNode(tx, Labels.VERSION);
            node.setProperty(TIME, version);

            tx.commit();
        }
    }

    private void buildGraphForRoutes(GraphDatabase graphDatabase, final GraphFilter filter, Stream<Route> routes) {
        Set<Station> filteredStations = filter.isFiltered() ?
                transportData.getStations().stream().filter(filter::shouldInclude).collect(Collectors.toSet()) :
                transportData.getStations();

        routes.forEach(route -> {
            RouteBuilderCache routeBuilderCache = new RouteBuilderCache();
            String asId = route.getId();
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
                for (int stopIndex = 0; stopIndex < stops.size() - 1; stopIndex++) {
                    StopCall currentStop = stops.get(stopIndex);
                    StopCall nextStop = stops.get(stopIndex + 1);

                    if (includeBothStops(filter, currentStop, nextStop)) {
                        services.add(Pair.of(currentStop.getStation(), nextStop.getStation()));
                        hours.add(Triple.of(currentStop.getStation(), nextStop.getStation(), currentStop.getDepartureTime().getHourOfDay()));
                    }
                }
        });
        services.forEach(pair -> createServiceNodeAndRelationship(tx, route, service, pair.getLeft(), pair.getRight(), stationCache));
        hours.forEach(triple -> createHourNode(tx, service, triple.getLeft(), triple.getMiddle(), triple.getRight(), stationCache));
    }


    private void createServiceNodeAndRelationship(Transaction tx, Route route, Service service, Station begin, Station end,
                                                  RouteBuilderCache routeBuilderCache) {

        // Node for the service
        // -route ID here as some towardsServices can go via multiple routes, this seems to be associated with the depots
        // -some towardsServices can go in two different directions from a station i.e. around Media City UK

        //TODO
        String beginSvcNodeId = begin.getId()+"_"+end.getId()+"_"+ service.getId();

        Node routeStationStart = routeBuilderCache.getRouteStation(tx, route, begin);

        Node svcNode = createGraphNode(tx, Labels.SERVICE);
        svcNode.setProperty(GraphStaticKeys.ID, beginSvcNodeId);
        svcNode.setProperty(GraphStaticKeys.SERVICE_ID, service.getId());
        svcNode.setProperty(GraphStaticKeys.ROUTE_ID, route.getId());
        svcNode.setProperty(GraphStaticKeys.TOWARDS_STATION_ID, end.getId());

        routeBuilderCache.putService(service, begin, end, svcNode);

        // start route station -> svc node
        Relationship svcRelationship = createRelationship(routeStationStart, svcNode, TransportRelationshipTypes.TO_SERVICE);
        svcRelationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getId());
        svcRelationship.setProperty(COST, 0);
        svcRelationship.setProperty(GraphStaticKeys.ROUTE_ID, route.getId());
        svcRelationship.setProperty(GraphStaticKeys.TRIPS, "");

    }

    private void createStationAndPlatforms(Transaction txn, Route route, Station station, RouteBuilderCache routeBuilderCache) {
        Node routeStationNode = createRouteStationNode(txn, station, route);
        routeBuilderCache.putRouteStation(route, station, routeStationNode);

        Node stationNode = graphQuery.getStationNode(txn, station);

        if (stationNode==null) {
            stationNode = createStationNode(txn, station);
            routeBuilderCache.putStation(station, stationNode);
            for (Platform platform : station.getPlatforms()) {
                Node platformNode = createPlatformNode(txn, platform);
                routeBuilderCache.putPlatform(platform.getId(), platformNode);
                createPlatformStationRelationships(station, stationNode, platform, platformNode);
            }
        } else {
            routeBuilderCache.putStation(station, stationNode);
            for (Platform platform : station.getPlatforms()) {
                Node platformNode = graphQuery.getPlatformNode(txn, platform.getId());
                routeBuilderCache.putPlatform(platform.getId(), platformNode);
            }
        }
    }

    private void createRouteRelationships(Transaction tx, GraphFilter filter, Route route, RouteBuilderCache routeBuilderCache) {
        Stream<Service> services = getServices(filter, route);

        Map<Pair<Station, Station>, Integer> pairs = new HashMap<>();
        services.forEach(service -> {

            service.getTripsFor(route).forEach(trip -> {
                    StopCalls stops = trip.getStops();
                    for (int stopIndex = 0; stopIndex < stops.size() - 1; stopIndex++) {
                        StopCall currentStop = stops.get(stopIndex);
                        StopCall nextStop = stops.get(stopIndex + 1);

                        if (includeBothStops(filter, currentStop, nextStop)) {
                            if (!pairs.containsKey(Pair.of(currentStop.getStation(), nextStop.getStation()))) {
                                int cost = ServiceTime.diffenceAsMinutes(currentStop.getDepartureTime(), nextStop.getArrivalTime());
                                pairs.put(Pair.of(currentStop.getStation(), nextStop.getStation()), cost);
                            }
                        }
                    }
                });
        });
        pairs.forEach((pair, cost) -> createRouteRelationship(
                routeBuilderCache.getRouteStation(tx, route, pair.getLeft()),
                routeBuilderCache.getRouteStation(tx, route, pair.getRight()), route, cost));

    }

    private boolean includeBothStops(GraphFilter filter, StopCall currentStop, StopCall nextStop) {
        return filter.shouldInclude(currentStop) && filter.shouldInclude(nextStop);
    }

    @NotNull
    private Stream<Service> getServices(GraphFilter filter, Route route) {
        return route.getServices().stream().filter(filter::shouldInclude);
    }

    private void createBoardingAndDeparts(Transaction tx, GraphFilter filter, Route route, Trip trip, RouteBuilderCache routeBuilderCache) {
        StopCalls stops = trip.getStops();
        int lastStopNum = stops.size(); // sequence runs from 1

        for (int stopIndex = 0; stopIndex < stops.size(); stopIndex++) {
            StopCall currentStop = stops.get(stopIndex);

            if (filter.shouldInclude(currentStop)) {
                createBoardingAndDepart(tx, routeBuilderCache, currentStop, route, lastStopNum);
            }

        }
    }

    private void createBoardingAndDepart(Transaction tx, RouteBuilderCache routeBuilderCache, StopCall stopCall,
                                         Route route, int lastStopNum) {

        boolean isFirstStop = (stopCall.getGetSequenceNumber() == 1); //stop seq num, not index
        boolean isLastStop = stopCall.getGetSequenceNumber() == lastStopNum;

        if (isFirstStop && isLastStop) {
            throw new RuntimeException("first and last true for " + stopCall);
        }

        boolean pickup = stopCall.getPickupType().equals(GTFSPickupDropoffType.Regular);
        boolean dropoff = stopCall.getDropoffType().equals(GTFSPickupDropoffType.Regular);

        if (isFirstStop && dropoff) {
            String msg = "Drop off at first station for stop " + stopCall.toString();
            logger.error(msg);
            // TODO THROW
        }

        if (isLastStop && pickup) {
            String msg = "Pick up at last station for stop " + stopCall.toString();
            logger.error(msg);
            // TODO THROW
        }

        Station station = stopCall.getStation();
        boolean isInterchange = interchangeRepository.isInterchange(station);

        // If bus we board to/from station, for trams its from the platform
        Node boardingNode = station.hasPlatforms() ? routeBuilderCache.getPlatform(tx, stopCall.getPlatformId())
                : routeBuilderCache.getStation(tx, station);
        String routeStationId = RouteStation.formId(station, route);
        Node routeStationNode = routeBuilderCache.getRouteStation(tx, route, stopCall.getStation());

        // boarding: platform/station ->  callingPoint , NOTE: no boarding at the last stop of a trip
        if (pickup && !routeBuilderCache.hasBoarding(boardingNode.getId(), routeStationNode.getId())) {
            createBoarding(routeBuilderCache, stopCall, route, station, isInterchange, boardingNode, routeStationId, routeStationNode);
        }

        // leave: route station -> platform/station , NOTE: no towardsStation at first stop of a trip
        if (dropoff && !routeBuilderCache.hasDeparts(routeStationNode.getId(), boardingNode.getId()) ) {
            createDeparts(routeBuilderCache, station, isInterchange, boardingNode, routeStationId, routeStationNode);
        }
    }

    private void createDeparts(RouteBuilderCache routeBuilderCache, Station station, boolean isInterchange,
                               Node boardingNode, String routeStationId, Node routeStationNode) {
        TransportRelationshipTypes departType = isInterchange ? INTERCHANGE_DEPART : DEPART;
        int departCost = isInterchange ? INTERCHANGE_DEPART_COST : DEPARTS_COST;

        Relationship departRelationship = createRelationship(routeStationNode, boardingNode, departType);
        departRelationship.setProperty(COST, departCost);
        departRelationship.setProperty(GraphStaticKeys.ID, routeStationId);
        departRelationship.setProperty(GraphStaticKeys.STATION_ID, station.getId());
        routeBuilderCache.putDepart(boardingNode.getId(), routeStationNode.getId());
    }

    private void createBoarding(RouteBuilderCache routeBuilderCache, StopCall stop, Route route, Station station,
                                boolean isInterchange, Node boardingNode, String routeStationId, Node routeStationNode) {
        TransportRelationshipTypes boardType = isInterchange ? INTERCHANGE_BOARD : BOARD;
        int boardCost = isInterchange ? INTERCHANGE_BOARD_COST : BOARDING_COST;
        Relationship boardRelationship = createRelationship(boardingNode, routeStationNode, boardType);
        boardRelationship.setProperty(COST, boardCost);
        boardRelationship.setProperty(GraphStaticKeys.ID, routeStationId);
        boardRelationship.setProperty(ROUTE_ID, route.getId());
        boardRelationship.setProperty(STATION_ID, station.getId());
        // No platform ID on buses
        if (stop.hasPlatfrom()) {
            boardRelationship.setProperty(PLATFORM_ID, stop.getPlatformId());
        }
        routeBuilderCache.putBoarding(boardingNode.getId(), routeStationNode.getId());
    }

    private void createTripRelationships(Transaction tx, GraphFilter filter, Route route, Service service, Trip trip, RouteBuilderCache routeBuilderCache,
                                         Map<Pair<Station, ServiceTime>, Node> timeNodes) {
        StopCalls stops = trip.getStops();

        for (int stopIndex = 0; stopIndex < stops.size() - 1; stopIndex++) {
            StopCall currentStop = stops.get(stopIndex);
            StopCall nextStop = stops.get(stopIndex + 1);

            if (includeBothStops(filter, currentStop, nextStop)) {
                updateTripRelationship(tx, route, service, trip, currentStop, nextStop, routeBuilderCache, timeNodes);
            }
        }
    }

    private void createRouteRelationship(Node from, Node to, Route route, int cost) {
        Set<Node> endNodes = new HashSet<>();

        // normal situation, where (especially) trains go via different paths even thought route is the "same"
//        if (from.hasRelationship(OUTGOING, ON_ROUTE)) {
//            // legit for some routes when trams return to depot, or at media city where they branch, etc
//            Iterable<Relationship> relationships = from.getRelationships(OUTGOING, ON_ROUTE);
//
//            for (Relationship current : relationships) {
//                endNodes.add(current.getEndNode());
//                logger.info(format("Existing outbounds at %s for same route %s currently has %s, new is %s",
//                        from.getProperty(STATION_ID), route.getId(),
//                        current.getEndNode().getProperty(STATION_ID),
//                        to.getProperty(STATION_ID)));
//            }
//
//        }
        if (!endNodes.contains(to)) {
            Relationship onRoute = from.createRelationshipTo(to, TransportRelationshipTypes.ON_ROUTE);
            onRoute.setProperty(ROUTE_ID, route.getId());
            onRoute.setProperty(COST, cost);
        }

    }

    private Node createPlatformNode(Transaction tx, Platform platform) {
        Node platformNode = createGraphNode(tx, Labels.PLATFORM);
        platformNode.setProperty(GraphStaticKeys.ID, platform.getId());
        return platformNode;
    }

    private void createPlatformStationRelationships(Station station, Node stationNode, Platform platform, Node platformNode) {
        boolean isInterchange = interchangeRepository.isInterchange(station);

        // station -> platform
        int enterPlatformCost = isInterchange ? ENTER_INTER_PLATFORM_COST : ENTER_PLATFORM_COST;
        Relationship crossToPlatform = createRelationship(stationNode, platformNode, ENTER_PLATFORM);
        crossToPlatform.setProperty(COST, enterPlatformCost);
        crossToPlatform.setProperty(GraphStaticKeys.PLATFORM_ID, platform.getId());

        // platform -> station
        int leavePlatformCost = isInterchange ? LEAVE_INTER_PLATFORM_COST : LEAVE_PLATFORM_COST;
        Relationship crossFromPlatform = createRelationship(platformNode, stationNode, LEAVE_PLATFORM);
        crossFromPlatform.setProperty(COST, leavePlatformCost);
        crossFromPlatform.setProperty(STATION_ID, station.getId());
    }

    private void updateTripRelationship(Transaction tx, Route route, Service service, Trip trip, StopCall beginStop, StopCall endStop,
                                        RouteBuilderCache routeBuilderCache, Map<Pair<Station, ServiceTime>, Node> timeNodes) {
        Station startStation = beginStop.getStation();

        String tripId = trip.getId();
        Node beginServiceNode = routeBuilderCache.getServiceNode(tx, service, startStation, endStop.getStation());

        beginServiceNode.getRelationships(INCOMING, TransportRelationshipTypes.TO_SERVICE).forEach(
                relationship -> {
                    String tripIds = relationship.getProperty(GraphStaticKeys.TRIPS).toString();
                    if (!tripIds.contains(tripId)) {
                        relationship.setProperty(GraphStaticKeys.TRIPS, tripId + tripIds);
                    }
                });

        ServiceTime departureTime = beginStop.getDepartureTime();

        TransportRelationshipTypes transportRelationshipType = TransportRelationshipTypes.from(route.getTransportMode());

        Node routeStationEnd = routeBuilderCache.getRouteStation(tx, route, endStop.getStation());

        // time node -> end route station
        Node timeNode = timeNodes.get(Pair.of(startStation, beginStop.getDepartureTime()));
        Relationship goesToRelationship = createRelationship(timeNode, routeStationEnd, transportRelationshipType);
        goesToRelationship.setProperty(GraphStaticKeys.TRIP_ID, tripId);

        int cost = ServiceTime.diffenceAsMinutes(endStop.getArrivalTime(), departureTime);
        goesToRelationship.setProperty(COST, cost);
        goesToRelationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getId());
        goesToRelationship.setProperty(GraphStaticKeys.ROUTE_ID, route.getId());
    }

    private Map<Pair<Station, ServiceTime>, Node> createMinuteNodes(Transaction tx, GraphFilter filter, Service service, Trip trip, RouteBuilderCache routeBuilderCache) {
        Map<Pair<Station, ServiceTime>, Node> timeNodes = new HashMap<>();

        StopCalls stops = trip.getStops();
        // TODO -1 here?
        for (int stopIndex = 0; stopIndex < stops.size() - 1; stopIndex++) {
            StopCall currentStop = stops.get(stopIndex);
            StopCall nextStop = stops.get(stopIndex+1);
            if (includeBothStops(filter, currentStop, nextStop)) {
                Station start = currentStop.getStation();
                ServiceTime departureTime = currentStop.getDepartureTime();
                Node timeNode = createMinuteNode(tx, service, trip, start, departureTime, routeBuilderCache);
                timeNodes.put(Pair.of(start, departureTime), timeNode);
            }
        }

        return timeNodes;
    }

    private Node createMinuteNode(Transaction tx, Service service, Trip trip, Station start, ServiceTime departureTime, RouteBuilderCache stationCache) {
        LocalTime time = departureTime.asLocalTime();
        String tripId = trip.getId();

        String timeNodeId = CreateKeys.getMinuteKey(trip, start, departureTime);
        Node timeNode = createGraphNode(tx, Labels.MINUTE);
        timeNode.setProperty(GraphStaticKeys.ID, timeNodeId);
        timeNode.setProperty(TIME, time);
        timeNode.setProperty(TRIP_ID, tripId);

        // hour node -> time node
        Node hourNode = stationCache.getHourNode(tx, service, start, departureTime.getHourOfDay());
        Relationship fromPrevious = createRelationship(hourNode, timeNode, TransportRelationshipTypes.TO_MINUTE);
        fromPrevious.setProperty(COST, 0);
        fromPrevious.setProperty(TIME, time);
        fromPrevious.setProperty(TRIP_ID, tripId);

        return timeNode;
    }

    private void createHourNode(Transaction tx, Service service, Station start, Station end, Integer hour, RouteBuilderCache stationCache) {
        Node hourNode = createGraphNode(tx, Labels.HOUR);
        String hourNodeId = createHourNodeKey(service, start, hour);
        hourNode.setProperty(GraphStaticKeys.ID, hourNodeId);
        hourNode.setProperty(HOUR, hour);

        stationCache.putHour(service, start, hour, hourNode);

        // service node -> time node
        Node serviceNode = stationCache.getServiceNode(tx, service, start, end);
        Relationship fromServiceNode = createRelationship(serviceNode, hourNode, TransportRelationshipTypes.TO_HOUR);
        fromServiceNode.setProperty(COST, 0);
        fromServiceNode.setProperty(HOUR, hour);
    }

    private String createHourNodeKey(Service service, Station start, Integer hour) {
        return service.getId() + "_" + start.getId() + "_" + hour.toString();
    }

    private static class RouteBuilderCache {
        private final Map<String, Long> routeStations;
        private final Map<Station, Long> stations;
        private final Map<String, Long> platforms;
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

        public void clear() {
            routeStations.clear();
            stations.clear();
            platforms.clear();
            svcNodes.clear();
            hourNodes.clear();
        }

        public void putRouteStation(Route route, Station station, Node routeStationNode) {
            String id = RouteStation.formId(station,route);
            routeStations.put(id, routeStationNode.getId());
        }

        public void putStation(Station station, Node stationNode) {
            stations.put(station, stationNode.getId());
        }

        public Node getRouteStation(Transaction txn, Route route, Station station) {
            String id = RouteStation.formId(station,route);
            if (!routeStations.containsKey(id)) {
                String message = "Cannot find routestation node in cache " + id;
                logger.error(message);
                throw new RuntimeException(message);
            }
            return txn.getNodeById(routeStations.get(id));
        }

        public Node getStation(Transaction txn, Station station) {
            return txn.getNodeById(stations.get(station));
        }

        public Node getPlatform(Transaction txn, String platformId) {
            return txn.getNodeById(platforms.get(platformId));
        }

        public void putPlatform(String platformId, Node platformNode) {
            platforms.put(platformId, platformNode.getId());
        }

        public Node getServiceNode(Transaction txn, Service service, Station startStation, Station endStation) {
            String id = CreateKeys.getServiceKey(service, startStation, endStation);
            return txn.getNodeById(svcNodes.get(id));
        }

        public void putService(Service service, Station begin, Station end, Node svcNode) {
            svcNodes.put(CreateKeys.getServiceKey(service, begin, end), svcNode.getId());
        }

        public void putHour(Service service, Station station, Integer hour, Node node) {
            hourNodes.put(CreateKeys.getHourKey(service, station, hour), node.getId());
        }

        public Node getHourNode(Transaction txn, Service service, Station station, Integer hour) {
            String key = CreateKeys.getHourKey(service, station, hour);
            if (!hourNodes.containsKey(key)) {
                throw new RuntimeException(format("Missing hour node for key %s service %s station %s hour %s",
                        key, service.getId(), station.getId(), hour.toString()));
            }
            return txn.getNodeById(hourNodes.get(key));
        }

        public boolean hasBoarding(long boardingNodeId, long routeStationNodeId) {
            return boardings.contains(Pair.of(boardingNodeId, routeStationNodeId));
        }

        public void putBoarding(long boardingNodeId, long routeStationNodeId) {
            boardings.add(Pair.of(boardingNodeId, routeStationNodeId));
        }

        public boolean hasDeparts(long routeStationNodeId, long boardingNodeId) {
            return departs.contains(Pair.of(routeStationNodeId, boardingNodeId));
        }

        public void putDepart(long boardingNodeId, long routeStationNodeId) {
            departs.add(Pair.of(routeStationNodeId, boardingNodeId));
        }
    }

    private static class CreateKeys {
        public static String getServiceKey(Service service, Station startStation, Station endStation) {
            return startStation.getId()+"_"+endStation.getId()+"_"+ service.getId();
        }

        public static String getHourKey(Service service, Station station, Integer hour) {
            return service.getId()+"_"+station.getId()+"_"+hour.toString();
        }

        public static String getMinuteKey(Trip trip, Station start, ServiceTime time) {
            return trip.getId() +"_"+ start.getId() + "_" + time.toPattern();
        }
    }

}
