package com.tramchester.graph;

import com.tramchester.domain.*;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Stops;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.DaysOfWeek;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.TransportData;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.GraphStaticKeys.RouteStation.ROUTE_NAME;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

///
// Station-[enter]->Platform-[board]->RouteStation-[toHour]->Hour-[toMinute]->Minute-[toService]->
//          Service-[GoesTo]->RouteStation-[depart]->Platform-[leave]->Station
//
// RouteStation-[onRoute]->RouteStation
///

public class TransportGraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TransportGraphBuilder.class);

    private static final int INTERCHANGE_DEPART_COST = 1;
    private static final int INTERCHANGE_BOARD_COST = 1;

    private static final int DEPARTS_COST = 1;
    private static final int BOARDING_COST = 2;

    // TODO compute actual costs depend on physical configuration of platforms at the station? No data available yet.
    private static final int ENTER_PLATFORM_COST = 0;
    private static final int LEAVE_PLATFORM_COST = 0;
    private static final int ENTER_INTER_PLATFORM_COST = 0;
    private static final int LEAVE_INTER_PLATFORM_COST = 0;

    private int numberNodes = 0;
    private int numberRelationships = 0;

    public enum Labels implements Label
    {
        ROUTE_STATION, STATION, AREA, PLATFORM, QUERY_NODE, SERVICE, HOUR, MINUTE
    }

    private Map<String,TransportRelationshipTypes> boardings;
    private Map<String,TransportRelationshipTypes> departs;
    private List<String> platforms;

    private final GraphDatabaseService graphDatabaseService;
    private final TransportData transportData;
    private final NodeIdLabelMap nodeIdLabelMap;
    private final StationIndexs stationIndexs;

    public TransportGraphBuilder(GraphDatabaseService graphDatabaseService, TransportData transportData,
                                 NodeIdLabelMap nodeIdLabelMap, StationIndexs stationIndexs) {
        this.graphDatabaseService = graphDatabaseService;
        this.transportData = transportData;
        this.nodeIdLabelMap = nodeIdLabelMap;
        this.stationIndexs = stationIndexs;

        boardings = new HashMap<>();
        departs = new HashMap<>();
        platforms = new LinkedList<>();
    }

    public void buildGraphwithFilter(GraphFilter filter) {
        logger.info("Building graph from " + transportData.getFeedInfo());
        LocalTime start = LocalTime.now();

        createIndexs();

        try (Transaction tx = graphDatabaseService.beginTx()) {
            logger.info("Rebuilding the graph...");
            for (Route route : transportData.getRoutes()) {
                if (filter.shouldInclude(route)) {
                    for (Service service : route.getServices()) {
                        if (filter.shouldInclude(service)) {
                            for (Trip trip : service.getTrips()) {
                                AddRouteServiceTrip(route, service, trip, filter);
                            }
                        }
                    }
                }
            }
            tx.success();
            Duration duration = Duration.between(start, LocalTime.now());
            logger.info("Graph rebuild finished, took " + duration.getSeconds());

        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
        }
        reportStats();
    }

    public void buildGraph() {
        logger.info("Building graph from " + transportData.getFeedInfo());
        LocalTime start = LocalTime.now();

        createIndexs();

        Transaction tx = graphDatabaseService.beginTx();
        try {
            logger.info("Rebuilding the graph...");
            for (Route route : transportData.getRoutes()) {
                for (Service service : route.getServices()) {
                    for (Trip trip : service.getTrips()) {
                        AddRouteServiceTrip(route, service, trip);
                    }
                    // performance
                    tx.success();
                    tx.close();
                    tx = graphDatabaseService.beginTx();
                }
            }

            graphDatabaseService.schema().awaitIndexesOnline(5, TimeUnit.SECONDS);

            graphDatabaseService.schema().getIndexes().forEach(indexDefinition -> {
                logger.info(String.format("Index label %s keys %s",
                        indexDefinition.getLabels(), indexDefinition.getPropertyKeys()));
            });

            tx.success();

            Duration duration = Duration.between(start, LocalTime.now());
            logger.info("Graph rebuild finished, took " + duration.getSeconds());



        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
        } finally {
            tx.close();
        }
        reportStats();
    }

    private void reportStats() {
        logger.info("Nodes created: " + numberNodes);
        logger.info("Relationships created: " + numberRelationships);
    }

    private void createIndexs() {
        try ( Transaction tx = graphDatabaseService.beginTx() )
        {
            Schema schema = graphDatabaseService.schema();
            schema.indexFor(Labels.STATION).on(GraphStaticKeys.ID).create();
            schema.indexFor(Labels.ROUTE_STATION).on(GraphStaticKeys.ID).create();
            schema.indexFor(Labels.PLATFORM).on(GraphStaticKeys.ID).create();
            // edge per trip indexs
            schema.indexFor(Labels.SERVICE).on(GraphStaticKeys.ID).create();
            schema.indexFor(Labels.SERVICE).on(SERVICE_ID).create();
            schema.indexFor(Labels.HOUR).on(GraphStaticKeys.ID).create();
            schema.indexFor(Labels.HOUR).on(GraphStaticKeys.HOUR).create();
            schema.indexFor(Labels.MINUTE).on(GraphStaticKeys.ID).create();
            schema.indexFor(Labels.MINUTE).on(GraphStaticKeys.TIME).create();
            schema.indexFor(Labels.MINUTE).on(TRIP_ID).create();

            tx.success();
        }
    }

    private void AddRouteServiceTrip(Route route, Service service, Trip trip, GraphFilter filter) {
        Stops stops = filter.filterStops(trip.getStops());
        int lastStopNum = stops.size(); // sequence runs from 1

        AddRouteServiceTripStops(route, service, trip, stops, lastStopNum);
    }

    private void AddRouteServiceTrip(Route route, Service service, Trip trip) {
        Stops stops = trip.getStops();
        int lastStopNum = stops.size(); // sequence runs from 1

        AddRouteServiceTripStops(route, service, trip, stops, lastStopNum);
    }

    private void AddRouteServiceTripStops(Route route, Service service, Trip trip, Stops stops, int lastStopNum) {
        for (int stopIndex = 0; stopIndex < stops.size() - 1; stopIndex++) {
            Stop currentStop = stops.get(stopIndex);
            Stop nextStop = stops.get(stopIndex + 1);

            boolean firstStop = (currentStop.getGetSequenceNumber() == 1); //stop seq num, not index
            boolean lastStop = nextStop.getGetSequenceNumber() == lastStopNum;

            Node fromRouteStation = getOrCreateCallingPointAndStation(currentStop, route, service, firstStop, false);
            Node toRouteStation = getOrCreateCallingPointAndStation(nextStop, route, service, false, lastStop);

            int cost = TramTime.diffenceAsMinutes(currentStop.getDepartureTime(), nextStop.getArrivalTime());

            if (runsAtLeastADay(service.getDays())) {
                createRouteRelationship(fromRouteStation, toRouteStation, route, cost);
                createRelationships(fromRouteStation, toRouteStation, currentStop, nextStop, route, service, trip);
            }
        }
    }

    private void createRouteRelationship(Node from, Node to, Route route, int cost) {
        if (from.hasRelationship(TransportRelationshipTypes.ON_ROUTE, OUTGOING)) {
            return;
        }
        Relationship onRoute = from.createRelationshipTo(to, TransportRelationshipTypes.ON_ROUTE);
        onRoute.setProperty(ROUTE_ID, route.getId());
        onRoute.setProperty(COST, cost);
    }

    private Node getOrCreateStation(Location station) {

        String id = station.getId();
        String stationName = station.getName();
        Node stationNode = stationIndexs.getStationNode(id);

        if (stationNode == null) {
            logger.info(format("Creating station node: %s ",station));
            stationNode = createGraphNode(Labels.STATION);
            stationNode.setProperty(GraphStaticKeys.ID, id);
            stationNode.setProperty(GraphStaticKeys.Station.NAME, stationName);
            LatLong latLong = station.getLatLong();
            setLatLongFor(stationNode, latLong);

            stationIndexs.getSpatialLayer().add(stationNode);
        }

//        Node areaNode = getAreaNode(station.getArea());
//        if (areaNode == null ) {
//
//        }

        return stationNode;
    }

    private void setLatLongFor(Node node, LatLong latLong) {
        node.setProperty(GraphStaticKeys.Station.LAT, latLong.getLat());
        node.setProperty(GraphStaticKeys.Station.LONG, latLong.getLon());
    }

    private Node createGraphNode(Labels label) {
        numberNodes++;
        Node node = graphDatabaseService.createNode(label);
        nodeIdLabelMap.put(node.getId(), label);
        return node;
    }

    private Node getOrCreatePlatform(Stop stop) {
        String stopId = stop.getId();

        Node platformNode = stationIndexs.getPlatformNode(stopId);
        if (platformNode==null) {
            platformNode = createGraphNode(Labels.PLATFORM);
            platformNode.setProperty(GraphStaticKeys.ID, stopId);
            String platformName = getPlatformName(stopId);
            platformNode.setProperty(GraphStaticKeys.Station.NAME,
                    format("%s Platform %s",stop.getStation().getName(),platformName));
            setLatLongFor(platformNode, stop.getStation().getLatLong());
        }
        return platformNode;
    }

    private String getPlatformName(String stopId) {
        return stopId.substring(stopId.length()-1); // the final digit of the ID
    }

    private Node getOrCreateCallingPointAndStation(Stop stop, Route route, Service service,
                                                   boolean firstStop, boolean lastStop) {
        Location station = stop.getStation();
        String stationId = station.getId();
        String routeStationId = createRouteStationId(station, route);

        Node routeStationNode = stationIndexs.getRouteStationNode(routeStationId);
        if ( routeStationNode == null) {
             routeStationNode = createRouteStationNode(station, route, routeStationId, service);
        }
        boolean isInterchange = TramInterchanges.has(station);
        int enterPlatformCost = ENTER_PLATFORM_COST;
        int leavePlatformCost = LEAVE_PLATFORM_COST;

        if (isInterchange) {
            enterPlatformCost = ENTER_INTER_PLATFORM_COST;
            leavePlatformCost = LEAVE_INTER_PLATFORM_COST;
        }

        Node stationNode = getOrCreateStation(station);

        Node platformNode = stationNode;
        String stationOrPlatformID = stationId;
        if (station.isTram()) {
            // add a platform node between station and calling points
            platformNode = getOrCreatePlatform(stop);
            stationOrPlatformID = stop.getId();
            // station -> platform & platform -> station
            if (!hasPlatform(stationId, stationOrPlatformID)) {
                // station -> platform
                Relationship crossToPlatform = createRelationships(stationNode, platformNode,
                        TransportRelationshipTypes.ENTER_PLATFORM);
                crossToPlatform.setProperty(COST, enterPlatformCost);
                crossToPlatform.setProperty(GraphStaticKeys.PLATFORM_ID, stationOrPlatformID);

                // platform -> station
                Relationship crossFromPlatform = createRelationships(platformNode, stationNode,
                        TransportRelationshipTypes.LEAVE_PLATFORM);
                crossFromPlatform.setProperty(COST, leavePlatformCost);
                crossFromPlatform.setProperty(STATION_ID, stationId);
                // always create together
                platforms.add(stationId + stationOrPlatformID);
            }
        }

        TransportRelationshipTypes boardType;
        TransportRelationshipTypes departType;
        int boardCost;
        int departCost;

        if (isInterchange) {
            boardType = TransportRelationshipTypes.INTERCHANGE_BOARD;
            boardCost = INTERCHANGE_BOARD_COST;
            departType = TransportRelationshipTypes.INTERCHANGE_DEPART;
            departCost = INTERCHANGE_DEPART_COST;
        } else {
            boardType = TransportRelationshipTypes.BOARD;
            boardCost = BOARDING_COST;
            departType = TransportRelationshipTypes.DEPART;
            departCost = DEPARTS_COST;
        }

        // boarding: platform/station ->  callingPoint
        // no boarding at the last stop of a trip
        if (!lastStop) {
            if (!hasBoarding(stationOrPlatformID, routeStationId, boardType)) {
                stationOrPlatformID = stop.getId();
                Relationship boardRelationship = createRelationships(platformNode, routeStationNode, boardType);
                boardRelationship.setProperty(COST, boardCost);
                boardRelationship.setProperty(GraphStaticKeys.ID, routeStationId);
                boardRelationship.setProperty(ROUTE_ID, route.getId());
                boardRelationship.setProperty(ROUTE_NAME, route.getName());
                boardRelationship.setProperty(STATION_ID, station.getId());
                boardRelationship.setProperty(PLATFORM_ID, stationOrPlatformID);
                boardings.put(boardKey(routeStationId, stationOrPlatformID), boardType);
            }
        }

        // leave: route station -> platform/station
        // no towardsStation at first stop of a trip
        if (!firstStop) {
            if (!hasDeparting(routeStationId, stationOrPlatformID, departType)) {
                Relationship departRelationship = createRelationships(routeStationNode, platformNode, departType);
                departRelationship.setProperty(COST, departCost);
                departRelationship.setProperty(GraphStaticKeys.ID, routeStationId);
                departRelationship.setProperty(GraphStaticKeys.STATION_ID, station.getId());
                departs.put(departKey(routeStationId, stationOrPlatformID), departType);
            }
        }

        return  routeStationNode;
    }

    private Relationship createRelationships(Node node, Node platformNode, TransportRelationshipTypes relationshipType) {
        numberRelationships++;
        return node.createRelationshipTo(platformNode, relationshipType);
    }


    private boolean hasPlatform(String stationId, String platformId) {
        String key = stationId + platformId;
        return platforms.contains(key);
    }

    private String departKey(String callingPointId, String id) {
        return callingPointId + "->" + id;
    }

    private String boardKey(String callingPointId, String id) {
        return id+"->"+callingPointId;
    }

    private boolean hasBoarding(String id, String callingPointId, TransportRelationshipTypes type) {
        String key = boardKey(callingPointId, id);
        if (boardings.containsKey(key)) {
            return boardings.get(key).equals(type);
        }
        return false;
    }

    private boolean hasDeparting(String callingPointId, String id, TransportRelationshipTypes type) {
        String key = departKey(callingPointId, id);
        if (departs.containsKey(key)) {
            return departs.get(key).equals(type);
        }
        return false;
    }

    private Node createRouteStationNode(Location station, Route route, String routeStationId, Service service) {
        Node routeStation = createGraphNode(Labels.ROUTE_STATION);

        logger.info(format("Creating route station %s route %s service %s nodeId %s", station.getId(),route.getId(),
                service.getServiceId(), routeStation.getId()));
        routeStation.setProperty(GraphStaticKeys.ID, routeStationId);
        routeStation.setProperty(GraphStaticKeys.RouteStation.STATION_NAME, station.getName());
        routeStation.setProperty(STATION_ID, station.getId());
        routeStation.setProperty(ROUTE_NAME, route.getName());
        routeStation.setProperty(ROUTE_ID, route.getId());
        setLatLongFor(routeStation, station.getLatLong());
        return routeStation;
    }

    private String createRouteStationId(Location station, Route route) {
        return station.getId() + route.getId();
    }

    //// edge per trip, experimental
    private void createRelationships(Node routeStationStart, Node routeStationEnd, Stop beginStop, Stop endStop,
                                     Route route, Service service, Trip trip) {
        Location startLocation = beginStop.getStation();
        LatLong destinationLatLong = endStop.getStation().getLatLong();

        // Node for the service
        // -route ID here as some towardsServices can go via multiple routes, this seems to be associated with the depots
        // -some towardsServices can go in two different directions from a station i.e. around Media City UK
        String routeIdClean = route.getId().replaceAll(" ", "");
        String beginSvcNodeId = format("%s_%s_%s_%s", startLocation.getId(), endStop.getStation().getId(),
                service.getServiceId(), routeIdClean);

        Node beginServiceNode = stationIndexs.getServiceNode(beginSvcNodeId);
        String tripId = trip.getTripId();

        if (beginServiceNode==null) {
            beginServiceNode = createGraphNode(Labels.SERVICE);
            beginServiceNode.setProperty(GraphStaticKeys.ID, beginSvcNodeId);
            beginServiceNode.setProperty(GraphStaticKeys.SERVICE_ID, service.getServiceId());
            beginServiceNode.setProperty(GraphStaticKeys.ROUTE_ID, route.getId());

            setLatLongFor(beginServiceNode, destinationLatLong);

            // start route station -> svc node
            Relationship svcRelationship = createRelationships(routeStationStart, beginServiceNode, TransportRelationshipTypes.TO_SERVICE);
            svcRelationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getServiceId());
            svcRelationship.setProperty(COST, 0);
            svcRelationship.setProperty(GraphStaticKeys.TRIPS, tripId);
            svcRelationship.setProperty(GraphStaticKeys.ROUTE_ID, route.getId());

        } else {
            beginServiceNode.getRelationships(INCOMING, TransportRelationshipTypes.TO_SERVICE).forEach(
                    relationship -> {
                        String tripIds = relationship.getProperty(GraphStaticKeys.TRIPS).toString();
                        if (!tripIds.contains(tripId)) {
                            relationship.setProperty(GraphStaticKeys.TRIPS, tripId + tripIds);
                        }
                    });
        }

        TramTime departureTime = beginStop.getDepartureTime();
        Node hourNode = getOrCreateHourNode(beginServiceNode, beginSvcNodeId, departureTime);
        Node timeNode = getOrCreateTimeNode(hourNode, beginSvcNodeId, departureTime, tripId);

        TransportRelationshipTypes transportRelationshipType =
                route.isTram() ? TransportRelationshipTypes.TRAM_GOES_TO : TransportRelationshipTypes.BUS_GOES_TO;

        // endSvcNode node -> end route station
        Relationship goesToRelationship = createRelationships(timeNode, routeStationEnd, transportRelationshipType);
        goesToRelationship.setProperty(GraphStaticKeys.TRIP_ID, tripId);

        // common properties
        int cost = TramTime.diffenceAsMinutes(endStop.getArrivalTime(), departureTime);
        addCommonProperties(goesToRelationship, transportRelationshipType, route, service, cost);
    }

    private Node getOrCreateHourNode(Node previousNode, String beginSvcNodeId, TramTime departureTime) {
        // Node for the hour
        int hourOfDay = departureTime.getHourOfDay();
        String hourNodeId = format("%s_%s", beginSvcNodeId, hourOfDay);
        Node hourNode = stationIndexs.getHourNode(hourNodeId);
        if (hourNode==null) {
            hourNode = createGraphNode(Labels.HOUR);
            hourNode.setProperty(GraphStaticKeys.ID, hourNodeId);
            hourNode.setProperty(HOUR, hourOfDay);

            // service node -> time node
            Relationship fromPrevious = createRelationships(previousNode, hourNode, TransportRelationshipTypes.TO_HOUR);
            fromPrevious.setProperty(COST, 0);
            fromPrevious.setProperty(HOUR, hourOfDay);
        }
        return hourNode;
    }

    private Node getOrCreateTimeNode(Node previousNode, String baseId, TramTime departureTime, String tripId) {
        // Node for the departure time
        String timeNodeId = format("%s_%s", baseId, departureTime.toPattern());
        Node timeNode = stationIndexs.getTimeNode(timeNodeId);
        if (timeNode==null) {
            LocalTime time = departureTime.asLocalTime();

            timeNode = createGraphNode(Labels.MINUTE);
            timeNode.setProperty(GraphStaticKeys.ID, timeNodeId);
            timeNode.setProperty(TIME, time);
            timeNode.setProperty(TRIP_ID, tripId);

            // hour node -> time node
            Relationship fromPrevious = createRelationships(previousNode, timeNode, TransportRelationshipTypes.TO_MINUTE);
            fromPrevious.setProperty(COST, 0);
            fromPrevious.setProperty(TIME, time);
            fromPrevious.setProperty(TRIP_ID, tripId);
        }
        return timeNode;
    }

    private void addCommonProperties(Relationship relationship, TransportRelationshipTypes transportRelationshipType,
                                     Route route, Service service, int cost) {
        relationship.setProperty(COST, cost);
        relationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getServiceId());
        relationship.setProperty(GraphStaticKeys.ROUTE_ID, route.getId());

        if (transportRelationshipType.equals(TransportRelationshipTypes.BUS_GOES_TO)) {
            relationship.setProperty(ROUTE_NAME, route.getName());
        }
    }

    private boolean runsAtLeastADay(HashMap<DaysOfWeek, Boolean> days) {
        return days.containsValue(true);
    }

}
