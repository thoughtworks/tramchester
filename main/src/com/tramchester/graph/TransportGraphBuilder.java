package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.TransportData;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;

import static com.tramchester.graph.GraphStaticKeys.*;
import static org.neo4j.graphdb.Direction.INCOMING;

///
// Station-[enter]->Platform-[board]->RouteStation-[toHour]->Hour-[toMinute]->Minute-[toService]->
//          Service-[GoesTo]->RouteStation-[depart]->Platform-[leave]->Station
//
// RouteStation-[onRoute]->RouteStation
///

public class TransportGraphBuilder extends GraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TransportGraphBuilder.class);

    // caching of various id's for peformance
    private final Map<String,TransportRelationshipTypes> boardings;
    private final Map<String,TransportRelationshipTypes> departs;
    private final List<String> platforms;
    private final HashSet<String> timeNodeIds;
    private final Set<Long> nodesWithRouteRelationship;
    private final TransportData transportData;
    private final InterchangeRepository interchangeRepository;

    public TransportGraphBuilder(GraphDatabase graphDatabase, GraphFilter graphFilter, TransportData transportData,
                                 NodeIdLabelMap nodeIdLabelMap, NodeIdQuery nodeIdQuery,
                                 InterchangeRepository interchangeRepository, TramchesterConfig config) {
        super(graphDatabase, config, graphFilter, nodeIdLabelMap, nodeIdQuery);
        this.transportData = transportData;
        this.interchangeRepository = interchangeRepository;

        boardings = new HashMap<>();
        departs = new HashMap<>();
        platforms = new LinkedList<>();
        timeNodeIds = new HashSet<>();
        nodesWithRouteRelationship = new HashSet<>();
    }

    protected void buildGraphwithFilter(GraphFilter filter, GraphDatabase graphDatabase) {
        logger.info("Building graph from " + transportData.getFeedInfo());
        long start = System.currentTimeMillis();
        logMemory("Before graph build");

        graphDatabase.createIndexs();

        Transaction tx = graphDatabase.beginTx();
        try {
            logger.info("Rebuilding the graph...");
            for (Agency agency : transportData.getAgencies()) {
                logger.info("Add routes for agency " + agency.getId());
                for (Route route : agency.getRoutes()) {
                    logger.info("Add nodes for route " + route.getId());
                    if (filter.shouldInclude(route)) {
                        for (Service service : route.getServices()) {
                            if (filter.shouldInclude(service)) {
                                for (Trip trip : service.getTrips()) {
                                    AddRouteServiceTrip(graphDatabase, route, service, trip, filter);
                                }
                                // performance & memory use control, specifically on prod box with limited ram
                                tx.success();
                                tx.close();
                                tx = graphDatabase.beginTx();
                            }
                        }
                    }
                }
            }
            logger.info("Wait for indexes online");
            graphDatabase.waitForIndexesReady();

            long duration = System.currentTimeMillis()-start;
            logger.info("Graph rebuild finished, took " + duration + "ms");

        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
            return;
        }
        reportStats();
        clearBuildCaches();
        logMemory("After graph build");
        System.gc();
    }

    protected void buildGraph(GraphDatabase graphDatabase) {
        buildGraphwithFilter(new IncludeAllFilter(), graphDatabase);
//        logger.info("Building graph from " + transportData.getFeedInfo());
//        logMemory("Before graph build");
//        long start = System.currentTimeMillis();
//
//        graphDatabase.createIndexs();
//
//        Transaction tx = graphDatabase.beginTx();
//        try {
//            logger.info("Rebuilding the graph...");
//            for(Agency agency : transportData.getAgencies()) {
//                logger.info("Add routes for agency " + agency.getId());
//
//                for (Route route : agency.getRoutes()) {
//                    logger.info("Add nodes for route " + route.getId());
//                    for (Service service : route.getServices()) {
//                        for (Trip trip : service.getTrips()) {
//                            AddRouteServiceTrip(graphDatabase, route, service, trip);
//                        }
//                        // performance & memory use control, specifically on prod box with limited ram
//                        tx.success();
//                        tx.close();
//                        tx = graphDatabase.beginTx();
//                    }
//                }
//            }
//
//            logger.info("Wait for indexes online");
//            graphDatabase.waitForIndexesReady();
//
//            long duration = System.currentTimeMillis()-start;
//            logger.info("Graph rebuild finished, took " + duration);
//
//        } catch (Exception except) {
//            logger.error("Exception while rebuilding the graph", except);
//            return;
//        }
//        reportStats();
//        clearBuildCaches();
//        logMemory("After graph build");
//        System.gc();
    }

    private void clearBuildCaches() {
        nodesWithRouteRelationship.clear();
        timeNodeIds.clear();
        platforms.clear();
        boardings.clear();
        departs.clear();
        nodeIdQuery.clearAfterGraphBuild();
    }

    private void AddRouteServiceTrip(GraphDatabase graphBasebase, Route route, Service service, Trip trip, GraphFilter filter) {

        StopCalls stops = trip.getStops();
        byte lastStopNum = (byte) stops.size(); // sequence runs from 1

        AddRouteServiceTripStops(graphBasebase, filter, route, service, trip, stops, lastStopNum);
    }

//    private void AddRouteServiceTrip(GraphDatabase graphBasebase, Route route, Service service, Trip trip) {
//        StopCalls stops = trip.getStops();
//        byte lastStopNum = (byte) stops.size(); // sequence runs from 1
//
//        AddRouteServiceTripStops(graphBasebase, filter, route, service, trip, stops, lastStopNum);
//    }

    private void AddRouteServiceTripStops(GraphDatabase graphDatabase, GraphFilter filter, Route route, Service service, Trip trip, StopCalls stops, byte lastStopNum) {
        for (int stopIndex = 0; stopIndex < stops.size() - 1; stopIndex++) {
            StopCall currentStop = stops.get(stopIndex);
            StopCall nextStop = stops.get(stopIndex + 1);

            if (filter.shouldInclude(currentStop) && filter.shouldInclude(nextStop)) {
                boolean firstStop = (currentStop.getGetSequenceNumber() == (byte)1); //stop seq num, not index
                boolean lastStop = nextStop.getGetSequenceNumber() == lastStopNum;

                Node fromRouteStation = getOrCreateCallingPointAndStation(currentStop, route, firstStop, false);
                Node toRouteStation = getOrCreateCallingPointAndStation(nextStop, route, false, lastStop);

                int cost = TramTime.diffenceAsMinutes(currentStop.getDepartureTime(), nextStop.getArrivalTime());

                createRouteRelationship(fromRouteStation, toRouteStation, route, cost);
                createRelationships(graphDatabase, fromRouteStation, toRouteStation, currentStop, nextStop, route, service, trip);
            }


        }
    }

    private void createRouteRelationship(Node from, Node to, Route route, int cost) {
        long fromNodeId = from.getId();
        if (nodesWithRouteRelationship.contains(fromNodeId)) {
            return;
        }
        Relationship onRoute = from.createRelationshipTo(to, TransportRelationshipTypes.ON_ROUTE);
        onRoute.setProperty(ROUTE_ID, route.getId());
        onRoute.setProperty(COST, cost);
        nodesWithRouteRelationship.add(fromNodeId);
    }

    private Node getOrCreateStation(Location station) {
        String id = station.getId();
        Node stationNode = station.isTram() ? nodeIdQuery.getTramStationNode(id) : nodeIdQuery.getBusStationNode(id);

        if (stationNode == null) {
            stationNode = createStationNode(station);
        }

        return stationNode;
    }

    private Node getOrCreatePlatform(StopCall stop) {
        String platformId = stop.getPlatformId();

        Node platformNode = nodeIdQuery.getPlatformNode(platformId);
        if (platformNode==null) {
            platformNode = createGraphNode(Labels.PLATFORM);
            platformNode.setProperty(GraphStaticKeys.ID, platformId);
        }
        return platformNode;
    }

    private Node getOrCreateCallingPointAndStation(StopCall stop, Route route,
                                                   boolean firstStop, boolean lastStop) {
        Station station = stop.getStation();
        String stationId = station.getId();
        String routeStationId = RouteStation.formId(station, route);

        Node routeStationNode = nodeIdQuery.getRouteStationNode(routeStationId);
        if ( routeStationNode == null) {
             routeStationNode = createRouteStationNode(station, route);
        }

        boolean isInterchange = interchangeRepository.isInterchange(station);
        int enterPlatformCost = ENTER_PLATFORM_COST;
        int leavePlatformCost = LEAVE_PLATFORM_COST;

        if (isInterchange) {
            enterPlatformCost = ENTER_INTER_PLATFORM_COST;
            leavePlatformCost = LEAVE_INTER_PLATFORM_COST;
        }

        Node stationNode = getOrCreateStation(station);

        Node platformNode = stationNode;

        String stationOrPlatformID;
        if (station.isTram()) {
            stationOrPlatformID = stop.getPlatformId();
        } else {
            stationOrPlatformID = stationId;
        }

        if (station.isTram()) {
            // add a platform node between station and calling points
            platformNode = getOrCreatePlatform(stop);

            // station -> platform AND platform -> station
            if (!hasPlatform(stationId, stationOrPlatformID)) {
                // station -> platform
                Relationship crossToPlatform = createRelationship(stationNode, platformNode,
                        TransportRelationshipTypes.ENTER_PLATFORM);
                crossToPlatform.setProperty(COST, enterPlatformCost);
                crossToPlatform.setProperty(GraphStaticKeys.PLATFORM_ID, stationOrPlatformID);

                // platform -> station
                Relationship crossFromPlatform = createRelationship(platformNode, stationNode,
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
                Relationship boardRelationship = createRelationship(platformNode, routeStationNode, boardType);
                boardRelationship.setProperty(COST, boardCost);
                boardRelationship.setProperty(GraphStaticKeys.ID, routeStationId);
                boardRelationship.setProperty(ROUTE_ID, route.getId());
                boardRelationship.setProperty(STATION_ID, station.getId());
                // No platform ID on buses
                if (route.isTram()) {
                    boardRelationship.setProperty(PLATFORM_ID, stationOrPlatformID);
                }
                boardings.put(boardKey(routeStationId, stationOrPlatformID), boardType);
            }
        }

        // leave: route station -> platform/station
        // no towardsStation at first stop of a trip
        if (!firstStop) {
            if (!hasDeparting(routeStationId, stationOrPlatformID, departType)) {
                Relationship departRelationship = createRelationship(routeStationNode, platformNode, departType);
                departRelationship.setProperty(COST, departCost);
                departRelationship.setProperty(GraphStaticKeys.ID, routeStationId);
                departRelationship.setProperty(GraphStaticKeys.STATION_ID, station.getId());
                departs.put(departKey(routeStationId, stationOrPlatformID), departType);
            }
        }

        return  routeStationNode;
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

    private void createRelationships(GraphDatabase graphDatabase, Node routeStationStart, Node routeStationEnd,
                                     StopCall beginStop, StopCall endStop, Route route, Service service, Trip trip) {
        Location startLocation = beginStop.getStation();

        // Node for the service
        // -route ID here as some towardsServices can go via multiple routes, this seems to be associated with the depots
        // -some towardsServices can go in two different directions from a station i.e. around Media City UK
        String beginSvcNodeId = startLocation.getId()+"_"+endStop.getStation().getId()+"_"+
                service.getId();

        Node beginServiceNode = nodeIdQuery.getServiceNode(beginSvcNodeId);
        String tripId = trip.getId();

        if (beginServiceNode==null) {
            beginServiceNode = createGraphNode(Labels.SERVICE);
            beginServiceNode.setProperty(GraphStaticKeys.ID, beginSvcNodeId);
            beginServiceNode.setProperty(GraphStaticKeys.SERVICE_ID, service.getId());
            beginServiceNode.setProperty(GraphStaticKeys.ROUTE_ID, route.getId());

            // start route station -> svc node
            Relationship svcRelationship = createRelationship(routeStationStart, beginServiceNode, TransportRelationshipTypes.TO_SERVICE);
            svcRelationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getId());
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
        Node timeNode = getOrCreateTimeNode(graphDatabase, hourNode, beginSvcNodeId, departureTime, tripId);

        TransportRelationshipTypes transportRelationshipType =
                route.isTram() ? TransportRelationshipTypes.TRAM_GOES_TO : TransportRelationshipTypes.BUS_GOES_TO;

        // endSvcNode node -> end route station
        Relationship goesToRelationship = createRelationship(timeNode, routeStationEnd, transportRelationshipType);
        goesToRelationship.setProperty(GraphStaticKeys.TRIP_ID, tripId);

        // common properties
        int cost = TramTime.diffenceAsMinutes(endStop.getArrivalTime(), departureTime);
        addCommonProperties(goesToRelationship, route, service, cost);
    }

    private Node getOrCreateHourNode(Node previousNode, String beginSvcNodeId, TramTime departureTime) {
        // Node for the hour
        int hourOfDay = departureTime.getHourOfDay();
        String hourNodeId = beginSvcNodeId +"_"+ hourOfDay;
        Node hourNode = nodeIdQuery.getHourNode(hourNodeId);
        if (hourNode==null) {
            hourNode = createGraphNode(Labels.HOUR);
            hourNode.setProperty(GraphStaticKeys.ID, hourNodeId);
            hourNode.setProperty(HOUR, hourOfDay);

            // service node -> time node
            Relationship fromPrevious = createRelationship(previousNode, hourNode, TransportRelationshipTypes.TO_HOUR);
            fromPrevious.setProperty(COST, 0);
            fromPrevious.setProperty(HOUR, hourOfDay);
        }
        return hourNode;
    }

    private Node getOrCreateTimeNode(GraphDatabase graphDatabase, Node previousNode, String baseId, TramTime departureTime, String tripId) {
        // Node for the departure time
        String timeNodeId = baseId +"_"+ departureTime.toPattern();
        Node timeNode;
        if (!timeNodeIds.contains(timeNodeId)) {
            LocalTime time = departureTime.asLocalTime();

            timeNode = createGraphNode(Labels.MINUTE);
            timeNode.setProperty(GraphStaticKeys.ID, timeNodeId);
            timeNode.setProperty(TIME, time);
            timeNode.setProperty(TRIP_ID, tripId);

            // hour node -> time node
            Relationship fromPrevious = createRelationship(previousNode, timeNode, TransportRelationshipTypes.TO_MINUTE);
            fromPrevious.setProperty(COST, 0);
            fromPrevious.setProperty(TIME, time);
            fromPrevious.setProperty(TRIP_ID, tripId);
            timeNodeIds.add(timeNodeId);
        } else {
            timeNode = graphDatabase.findNode(Labels.MINUTE, GraphStaticKeys.ID, timeNodeId);
        }
        return timeNode;
    }

    private void addCommonProperties(Relationship relationship, Route route, Service service, int cost) {
        relationship.setProperty(COST, cost);
        relationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getId());
        relationship.setProperty(GraphStaticKeys.ROUTE_ID, route.getId());
    }

}
