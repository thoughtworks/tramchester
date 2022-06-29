package com.tramchester.graph.graphbuild;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.HasGraphDBConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.StationTime;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.AddNeighboursGraphBuilder;
import com.tramchester.graph.AddWalksForClosedGraphBuilder;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.databaseManagement.GraphDatabaseMetaInfo;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.metrics.TimedTransaction;
import com.tramchester.metrics.Timing;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StopCallRepository;
import com.tramchester.repository.TransportData;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.graphbuild.GraphLabel.INTERCHANGE;
import static com.tramchester.graph.graphbuild.GraphProps.*;
import static java.lang.String.format;
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
    private final GraphDatabaseMetaInfo databaseMetaInfo;
    private final StopCallRepository stopCallRepository;

    // force contsruction via guice to generate ready token, needed where no direct code dependency on this class
    public Ready getReady() {
        return new Ready();
    }

    @Inject
    public StagedTransportGraphBuilder(GraphDatabase graphDatabase, HasGraphDBConfig config, GraphFilter graphFilter,
                                       TransportData transportData, InterchangeRepository interchangeRepository,
                                       GraphBuilderCache builderCache,
                                       StationsAndLinksGraphBuilder.Ready stationAndLinksBuilt,
                                       AddNeighboursGraphBuilder.Ready neighboursReady,
                                       AddWalksForClosedGraphBuilder.Ready walksForClosedReady,
                                       GraphDatabaseMetaInfo databaseMetaInfo, StopCallRepository stopCallRepository) {
        super(graphDatabase, graphFilter, config, builderCache);
        this.transportData = transportData;
        this.interchangeRepository = interchangeRepository;
        this.databaseMetaInfo = databaseMetaInfo;
        this.stopCallRepository = stopCallRepository;
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (graphDatabase.isCleanDB()) {
            logger.info("Rebuild of TimeTable graph DB for " + graphDBConfig.getDbPath());
            if (graphFilter.isFiltered()) {
                logger.warn("Graph is filtered " + graphFilter);
            }
            buildGraphwithFilter(graphDatabase, builderCache);
            graphDatabase.waitForIndexes();
            logger.info("Graph rebuild is finished for " + graphDBConfig.getDbPath());
        } else {
            logger.info("No rebuild of graph");
            graphDatabase.waitForIndexes();
        }
        logger.info("started");
    }

    private void buildGraphwithFilter(GraphDatabase graphDatabase, GraphBuilderCache builderCache) {
        logger.info("Building graph for data source: " + transportData.getDataSourceInfo());
        logMemory("Before graph build");

        try(Timing ignored = new Timing(logger, "Graph rebuild")) {

            // just for tfgm trams currently
            linkStationsAndPlatforms(builderCache);

            // TODO Agencies could be done in parallel as should be no overlap except at station level?
            for(Agency agency : transportData.getAgencies()) {
                if (graphFilter.shouldIncludeAgency(agency)) {
                    try (Timing unused = new Timing(logger,"Add agency " + agency.getId() + " " + agency.getName())) {
                        buildForAgency(graphDatabase, agency, builderCache);
                    }
                }
            }

            // only add version node if we manage to build graph, so partial builds that fail cause a rebuild
            addVersionNode(graphDatabase, transportData.getDataSourceInfo());

        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
            throw new RuntimeException("Unable to build graph", except);
        }

        builderCache.fullClear();
        reportStats();
        System.gc(); // for testing, was causing issue on the main test run
        logMemory("After graph build");
    }

    private void linkStationsAndPlatforms(GraphBuilderCache builderCache) {

        try(TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "link stations & platfoms")) {
            Transaction txn = timedTransaction.transaction();
            transportData.getActiveStationStream().
                    filter(Station::hasPlatforms).
                    filter(graphFilter::shouldInclude).
                    forEach(station -> linkStationAndPlatforms(txn, station, builderCache));
            timedTransaction.commit();
        }
    }

    private void addVersionNode(GraphDatabase graphDatabase, Set<DataSourceInfo> infos) {
        if (infos.isEmpty()) {
            logger.error("No data source info was provided, version will not be set in the DB");
            return;
        }

        try(Transaction tx = graphDatabase.beginTx()) {
            logger.info("Adding version node to the DB");
            databaseMetaInfo.createVersionNode(tx, infos);
            tx.commit();
        }
    }

    private void buildForAgency(GraphDatabase graphDatabase, Agency agency, GraphBuilderCache builderCache) {

        if (getRoutesForAgency(agency).findAny().isEmpty()) {
            return;
        }

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "onRoute for " + agency.getId())) {
            Transaction tx = timedTransaction.transaction();
            getRoutesForAgency(agency).forEach(route -> createOnRouteRelationships(tx, route, builderCache));
            timedTransaction.commit();
        }

        try(Timing ignored = new Timing(logger,"service, hour for " + agency.getId())) {
            getRoutesForAgency(agency).parallel().forEach(route -> {
                try (Transaction tx = graphDatabase.beginTx()) {
                    createServiceAndHourNodesForRoute(tx, route, builderCache);
                    tx.commit();
                }
            });
        }

        try(Timing ignored = new Timing(logger,"time and update for trips for " + agency.getId())) {
            getRoutesForAgency(agency).parallel().forEach(route -> {
                try (Transaction tx = graphDatabase.beginTx()) {
                    createMinuteNodesAndRecordUpdatesForTrips(tx, route, builderCache);
                    tx.commit();
                }
            });
        }

        try (TimedTransaction timedTransaction = new TimedTransaction(graphDatabase, logger, "boards & departs for " + agency.getId())) {
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

    private void createMinuteNodesAndRecordUpdatesForTrips(Transaction tx, Route route,
                                                           GraphBuilderCache routeBuilderCache) {

        // time nodes and relationships for trips
        for (Trip trip : route.getTrips()) {
            Map<StationTime, Node> timeNodes = createMinuteNodes(tx, trip, routeBuilderCache);
            createTripRelationships(tx, route, trip, routeBuilderCache, timeNodes);
            timeNodes.clear();
        }
    }

    private void createTripRelationships(Transaction tx, Route route, Trip trip, GraphBuilderCache routeBuilderCache,
                                         Map<StationTime, Node> timeNodes) {
        StopCalls stops = trip.getStopCalls();

        stops.getLegs(graphFilter.isFiltered()).forEach(leg -> {
            if (includeBothStops(leg)) {
                StopCall first = leg.getFirst();
                StopCall second = leg.getSecond();
                createRelationshipTimeNodeToRouteStation(tx, route, trip, first, second, routeBuilderCache, timeNodes);
            }
        });
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
                List<StopCalls.StopLeg> legs = stops.getLegs(graphFilter.isFiltered());
                legs.forEach(leg -> {

                    if (includeBothStops(leg)) {
                        if (!leg.getDepartureTime().isValid()) {
                            throw new RuntimeException("Invalid dept time for " + leg);
                        }

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

        Node svcNode = createGraphNode(tx, GraphLabel.SERVICE);
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
        setCostProp(svcRelationship, Duration.ZERO);
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

        Map<StopCalls.StopLeg, Duration> pairs = new HashMap<>();
        route.getTrips().forEach(trip -> {
            StopCalls stops = trip.getStopCalls();
            stops.getLegs(graphFilter.isFiltered()).forEach(leg -> {
                if (includeBothStops(leg)) {
                    if (!pairs.containsKey(leg)) {
                        // TODO need cost representative of the route as whole
                        Duration cost = leg.getCost();
                        if (cost.isZero() && route.getTransportMode() != TransportMode.Bus) {
                            // this can happen a lot for buses
                            logger.warn(format("Zero cost for trip %s for %s", trip.getId(), leg));
                        }
                        pairs.put(leg, cost);
                    }
                }
            });
        });

        pairs.forEach((leg, unused) -> {
            IdFor<Station> beginId = leg.getFirstStation().getId();
            IdFor<Station> endId = leg.getSecondStation().getId();
            if (!routeBuilderCache.hasRouteStation(route, beginId)) {
                String message = format("Missing first route station (%s, %s) in cache for route: %s and leg: %s",
                        route.getId(), beginId, route, leg);
                throw new RuntimeException(message);
            }
            if (!routeBuilderCache.hasRouteStation(route, endId)) {
                String message = format("Missing second route station (%s, %s) in cache for %s and %s",
                        route.getId(), beginId, route, leg.getFirst());
                throw new RuntimeException(message);
            }
            Node startNode = routeBuilderCache.getRouteStation(tx, route, beginId);
            Node endNode = routeBuilderCache.getRouteStation(tx, route, endId);

            StopCallRepository.Costs costs = stopCallRepository.getCostsBetween(route, leg.getFirstStation(), leg.getSecondStation());

            createOnRouteRelationship(startNode, endNode, route, costs);
        });
}

    private boolean includeBothStops(StopCalls.StopLeg leg) {
        return graphFilter.shouldInclude(leg.getFirst()) && graphFilter.shouldInclude(leg.getSecond());
    }

    private void createBoardingAndDepart(Transaction tx, GraphBuilderCache routeBuilderCache, StopCall stopCall,
                                         Route route, Trip trip) {

        if (!stopCall.callsAtStation()) {
            if (route.getTransportMode()==Tram) {
                logger.warn("No pickup or drop-off for " + stopCall);
            }
            return;
        }

        boolean pickup = stopCall.getPickupType().equals(GTFSPickupDropoffType.Regular);
        boolean dropoff = stopCall.getDropoffType().equals(GTFSPickupDropoffType.Regular);

        Station station = stopCall.getStation();

        // TODO when filtering this isn't really valid, we might only see a small segment of a larger trip....
        // In unfiltered situations (i.e. not testing) it is fine

        boolean isFirstStop = stopCall.equals(trip.getStopCalls().getFirstStop());
        if (isFirstStop && dropoff && !trip.isFiltered()) {
            String msg = "Drop off at first station for stop " + station.getId() + " trip " + trip.getId() + " " + stopCall.getDropoffType()
                    + " seq:" + stopCall.getGetSequenceNumber();
            logger.info(msg);
        }

        boolean isLastStop = stopCall.equals(trip.getStopCalls().getLastStop());
        if (isLastStop && pickup && !trip.isFiltered()) {
            String msg = "Pick up at last station for stop " + station.getId() + " trip " + trip.getId() + " " + stopCall.getPickupType()
                    + " seq:" + stopCall.getGetSequenceNumber();
            logger.info(msg);
        }

        boolean isInterchange = interchangeRepository.isInterchange(station);

        // If bus we board to/from station, for trams it is from the platform
        Node platformOrStation = station.hasPlatforms() ? routeBuilderCache.getPlatform(tx, stopCall.getPlatform().getId())
                : routeBuilderCache.getStation(tx, station.getId());
        IdFor<RouteStation> routeStationId = RouteStation.createId(station.getId(), route.getId());
        Node routeStationNode = routeBuilderCache.getRouteStation(tx, routeStationId);

        if (isInterchange) {
            routeStationNode.addLabel(INTERCHANGE);
        }

        // boarding: platform/station ->  callingPoint , NOTE: no boarding at the last stop of a trip
        if (pickup && !routeBuilderCache.hasBoarding(platformOrStation.getId(), routeStationNode.getId())) {
            createBoarding(routeBuilderCache, stopCall, route, station, isInterchange, platformOrStation, routeStationId,
                    routeStationNode);
        }

        // leave: route station -> platform/station , NOTE: no towardsStation at first stop of a trip
        if (dropoff && !routeBuilderCache.hasDeparts(platformOrStation.getId(), routeStationNode.getId()) ) {
            createDeparts(routeBuilderCache, station, isInterchange, platformOrStation, routeStationId, routeStationNode);
        }

    }

    private void createDeparts(GraphBuilderCache routeBuilderCache, Station station, boolean isInterchange,
                               Node boardingNode, IdFor<RouteStation> routeStationId, Node routeStationNode) {
        TransportRelationshipTypes departType = isInterchange ? INTERCHANGE_DEPART : DEPART;

        Duration departCost = Duration.ZERO;
        //int departCost = isInterchange ? INTERCHANGE_DEPART_COST : DEPARTS_COST;

        Relationship departRelationship = createRelationship(routeStationNode, boardingNode, departType);
        setCostProp(departRelationship, departCost);
        setRouteStationProp(departRelationship, routeStationId);
        setProperty(departRelationship, station);
        routeBuilderCache.putDepart(boardingNode.getId(), routeStationNode.getId());
    }

    private void createBoarding(GraphBuilderCache routeBuilderCache, StopCall stop, Route route, Station station,
                                boolean isInterchange, Node platformOrStation, IdFor<RouteStation> routeStationId,
                                Node routeStationNode) {
        TransportRelationshipTypes boardType = isInterchange ? INTERCHANGE_BOARD : BOARD;
        Relationship boardRelationship = createRelationship(platformOrStation, routeStationNode, boardType);

        Duration boardCost = Duration.ZERO;
//        int boardCost = isInterchange ? INTERCHANGE_BOARD_COST : BOARDING_COST;

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

    private void createOnRouteRelationship(Node from, Node to, Route route, StopCallRepository.Costs costs) {
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

            setCostProp(onRoute, costs.average());
            setMaxCostProp(onRoute, costs.max());
            setProperty(onRoute, route.getTransportMode());
        }
    }

    private void createPlatformStationRelationships(Station station, Node stationNode, Platform platform, Node platformNode) {

        // station -> platform
        Duration enterPlatformCost = station.getMinChangeDuration();

        Relationship crossToPlatform = createRelationship(stationNode, platformNode, ENTER_PLATFORM);
        setCostProp(crossToPlatform, enterPlatformCost);
        setProperty(crossToPlatform, platform);

        // platform -> station
        Duration leavePlatformCost = Duration.ZERO;

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

        Duration cost = TramTime.difference(endStop.getArrivalTime(), departureTime);
        setCostProp(goesToRelationship, cost);
        setProperty(goesToRelationship, trip.getService()); // TODO Still useful?
        setProperty(goesToRelationship, route);
        setStopSequenceNumber(goesToRelationship, endStop.getGetSequenceNumber());
    }

    private Map<StationTime, Node> createMinuteNodes(Transaction tx, Trip trip, GraphBuilderCache builderCache) {

        Map<StationTime, Node> timeNodes = new HashMap<>();

        StopCalls stops = trip.getStopCalls();
        stops.getLegs(graphFilter.isFiltered()).forEach(leg -> {
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

        Node timeNode = createGraphNode(tx, GraphLabel.MINUTE);
        setTimeProp(timeNode, departureTime);
        setProperty(timeNode, trip);

        // hour node -> time node
        Node hourNode = builderCache.getHourNode(tx, trip.getRoute().getId(), trip.getService(), startId, departureTime.getHourOfDay());
        Relationship fromPrevious = createRelationship(hourNode, timeNode, TransportRelationshipTypes.TO_MINUTE);
        setCostProp(fromPrevious, Duration.ZERO);
        setTimeProp(fromPrevious, departureTime);
        setProperty(fromPrevious, trip);

        return timeNode;
    }

    private void createHourNodeAndRelationshipFromService(Transaction tx, IdFor<Route> routeId, Service service, IdFor<Station> startId,
                                                          Integer hour, GraphBuilderCache builderCache, Node serviceNode) {

        if (!builderCache.hasHourNode(routeId, service, startId, hour)) {
            Node hourNode = createGraphNode(tx, GraphLabel.HOUR);
            setHourProp(hourNode, hour);

            hourNode.addLabel(GraphLabel.getHourLabel(hour));
            builderCache.putHour(routeId, service, startId, hour, hourNode);

            // service node -> time node
            //Node serviceNode = stationCache.getServiceNode(tx, routeId, service, startId, endId);
            Relationship serviceNodeToHour = createRelationship(serviceNode, hourNode, TransportRelationshipTypes.TO_HOUR);
            setCostProp(serviceNodeToHour, Duration.ZERO);
            setHourProp(serviceNodeToHour, hour);
        }

    }

    public static class Ready {
        private Ready() {
            // prevent guice creating this, want to create dependency on the Builder
        }
    }

}
