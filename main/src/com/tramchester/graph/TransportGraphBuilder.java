package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Stops;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.repository.TransportData;

import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.tramchester.graph.GraphStaticKeys.COST;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;

public class TransportGraphBuilder extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(TransportGraphBuilder.class);

    public static final int INTERCHANGE_DEPART_COST = 1;
    public static final int INTERCHANGE_BOARD_COST = 1;

    public static final int DEPARTS_COST = 1;
    public static final int BOARDING_COST = 2;

    private static final int PLATFORM_COST = 0;

    private int numberNodes = 0;
    private int numberRelationships = 0;

    private final boolean edgePerTrip;

    public enum Labels implements Label
    {
        ROUTE_STATION, STATION, AREA, PLATFORM, QUERY_NODE, SERVICE, HOUR, MINUTE
    }

    private Map<String,TransportRelationshipTypes> boardings;
    private Map<String,TransportRelationshipTypes> departs;
    private List<String> platforms;
    private Map<Long, String> relationToSvcId;
    private Map<Long, LocalTime[]> timesForRelationship;

    private TransportData transportData;

    public TransportGraphBuilder(GraphDatabaseService graphDatabaseService, TransportData transportData,
                                 RelationshipFactory relationshipFactory, SpatialDatabaseService spatialDatabaseService,
                                 TramchesterConfig config) {
        super(graphDatabaseService, relationshipFactory, spatialDatabaseService, false);
        this.transportData = transportData;
        boardings = new HashMap<>();
        departs = new HashMap<>();
        relationToSvcId = new HashMap<>();
        timesForRelationship = new HashMap<>();
        platforms = new LinkedList<>();
        edgePerTrip = config.getEdgePerTrip();
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

        try (Transaction tx = graphDatabaseService.beginTx()) {
            logger.info("Rebuilding the graph...");
            for (Route route : transportData.getRoutes()) {
                for (Service service : route.getServices()) {
                    for (Trip trip : service.getTrips()) {
                        AddRouteServiceTrip(route, service, trip);
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
            if (edgePerTrip) {
                schema.indexFor(Labels.SERVICE).on(GraphStaticKeys.ID).create();
                schema.indexFor(Labels.SERVICE).on(GraphStaticKeys.DAYS).create();
                schema.indexFor(Labels.SERVICE).on(GraphStaticKeys.SERVICE_START_DATE).create();
                schema.indexFor(Labels.SERVICE).on(GraphStaticKeys.SERVICE_END_DATE).create();
                schema.indexFor(Labels.HOUR).on(GraphStaticKeys.ID).create();
                schema.indexFor(Labels.HOUR).on(GraphStaticKeys.HOUR).create();
                schema.indexFor(Labels.MINUTE).on(GraphStaticKeys.ID).create();
                schema.indexFor(Labels.MINUTE).on(GraphStaticKeys.TIME).create();
            }
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

            boolean firstStop = (currentStop.getGetSequenceNumber() == 1);
            boolean lastStop = nextStop.getGetSequenceNumber() == lastStopNum;

            Node from = getOrCreateRouteStation(currentStop, route, service, firstStop, false);
            Node to = getOrCreateRouteStation(nextStop, route, service, false, lastStop);

            if (runsAtLeastADay(service.getDays())) {
                if (edgePerTrip) {
                    createRelationship(from, to, currentStop, nextStop, route, service, trip);
                } else {
                    createOrUpdateRelationship(from, to, currentStop, nextStop, route, service);
                }
            }
        }
    }

    private Node getOrCreateStation(Location station) {

        String id = station.getId();
        String stationName = station.getName();
        Node stationNode = getStationNode(id);

        if (stationNode == null) {
            logger.info(format("Creating station node: %s ",station));
            stationNode = createGraphNode(Labels.STATION);
            stationNode.setProperty(GraphStaticKeys.ID, id);
            stationNode.setProperty(GraphStaticKeys.Station.NAME, stationName);
            LatLong latLong = station.getLatLong();
            stationNode.setProperty(GraphStaticKeys.Station.LAT, latLong.getLat());
            stationNode.setProperty(GraphStaticKeys.Station.LONG, latLong.getLon());

            getSpatialLayer().add(stationNode);
        }

//        Node areaNode = getAreaNode(station.getArea());
//        if (areaNode == null ) {
//
//        }

        return stationNode;
    }

    private Node createGraphNode(Labels label) {
        numberNodes++;
        return graphDatabaseService.createNode(label);
    }

    private Node getOrCreatePlatform(Stop stop) {
        String stopId = stop.getId();

        Node platformNode = getPlatformNode(stopId);
        if (platformNode==null) {
            platformNode = createGraphNode(Labels.PLATFORM);
            platformNode.setProperty(GraphStaticKeys.ID, stopId);
            String platformName = stopId.substring(stopId.length()-1); // the final digit of the ID
            platformNode.setProperty(GraphStaticKeys.Station.NAME, format("%s Platform %s",stop.getStation().getName(),platformName));
        }
        return platformNode;
    }

    private Node getOrCreateRouteStation(Stop stop, Route route, Service service, boolean firstStop, boolean lastStop) {
        Location station = stop.getStation();
        String stationId = station.getId();
        String callingPointId = createCallingPointId(station, route);

        Node callingPoint = getCallingPointNode(callingPointId);
        if ( callingPoint == null) {
             callingPoint = createCallingPoint(station, route, callingPointId, service);
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
                Relationship crossToPlatform = createRelationship(stationNode, platformNode, TransportRelationshipTypes.ENTER_PLATFORM);
                crossToPlatform.setProperty(COST, PLATFORM_COST);

                // platform -> station
                Relationship crossFromPlatform = createRelationship(platformNode, stationNode, TransportRelationshipTypes.LEAVE_PLATFORM);
                crossFromPlatform.setProperty(COST, PLATFORM_COST);
                // always create together
                platforms.add(stationId + stationOrPlatformID);
            }
        }

        TransportRelationshipTypes boardType;
        TransportRelationshipTypes departType;
        int boardCost;
        int departCost;
        if (TramInterchanges.has(station)) {
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
            if (!hasBoarding(stationOrPlatformID, callingPointId, boardType)) {
                Relationship interchangeRelationshipTo = createRelationship(platformNode, callingPoint, boardType);
                interchangeRelationshipTo.setProperty(COST, boardCost);
                interchangeRelationshipTo.setProperty(GraphStaticKeys.ID, callingPointId);
                boardings.put(boardKey(callingPointId, stationOrPlatformID), boardType);
            }
        }

        // leave: route station -> platform/station
        // no depart at first stop of a trip
        if (!firstStop) {
            if (!hasDeparting(callingPointId, stationOrPlatformID, departType)) {
                Relationship departRelationship = createRelationship(callingPoint, platformNode, departType);
                departRelationship.setProperty(COST, departCost);
                departRelationship.setProperty(GraphStaticKeys.ID, callingPointId);
                departs.put(departKey(callingPointId, stationOrPlatformID), departType);
            }
        }

        return  callingPoint;
    }

    private Relationship createRelationship(Node node, Node platformNode, TransportRelationshipTypes relationshipType) {
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

    private Node createCallingPoint(Location station, Route route, String routeStationId, Service service) {
        logger.info(format("Creating route station %s route %s service %s", station.getId(),route.getId(),
                service.getServiceId()));
        Node routeStation = createGraphNode(Labels.ROUTE_STATION);
        routeStation.setProperty(GraphStaticKeys.ID, routeStationId);
        routeStation.setProperty(GraphStaticKeys.RouteStation.STATION_NAME, station.getName());
        routeStation.setProperty(GraphStaticKeys.RouteStation.ROUTE_NAME, route.getName());
        routeStation.setProperty(GraphStaticKeys.RouteStation.ROUTE_ID, route.getId());
        return routeStation;
    }

    private String createCallingPointId(Location station, Route route) {
        return station.getId() + route.getId();
    }

    //// edge per trip, experimental
    private void createRelationship(Node routeStationStart, Node routeStationEnd, Stop beginStop, Stop endStop,
                                    Route route, Service service, Trip trip) {
        Location startLocation = beginStop.getStation();

        // Node for the service
        // -route ID here as some services can go via multiple routes, this seems to be associated with the depots
        // -some services can go in two different directions from a station i.e. around Media City UK
        String svcNodeId = format("%s_%s_%s_%s", startLocation.getId(), endStop.getStation().getId(),
                service.getServiceId(), route.getId().replaceAll(" ",""));

        Node serviceNode = graphQuery.getServiceNode(svcNodeId);
        String tripId = trip.getTripId();

        if (serviceNode==null) {
            serviceNode = createGraphNode(Labels.SERVICE);
            serviceNode.setProperty(GraphStaticKeys.ID, svcNodeId);
            serviceNode.setProperty(GraphStaticKeys.SERVICE_ID, service.getServiceId());

            // TODO these 3 no longer needed
            serviceNode.setProperty(GraphStaticKeys.DAYS, toBoolArray(service.getDays()));
            serviceNode.setProperty(GraphStaticKeys.SERVICE_START_DATE, service.getStartDate().getStringDate());
            serviceNode.setProperty(GraphStaticKeys.SERVICE_END_DATE, service.getEndDate().getStringDate());

            serviceNode.setProperty(GraphStaticKeys.SERVICE_EARLIEST_TIME, service.earliestDepartTime().asLocalTime());
            serviceNode.setProperty(GraphStaticKeys.SERVICE_LATEST_TIME, service.latestDepartTime().asLocalTime());

            // start route station -> svc node
            Relationship svcRelationship = createRelationship(routeStationStart, serviceNode, TransportRelationshipTypes.TO_SERVICE);
            svcRelationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getServiceId());
            svcRelationship.setProperty(COST, 0);
            svcRelationship.setProperty(GraphStaticKeys.TRIPS, tripId);

        } else {
            serviceNode.getRelationships(INCOMING, TransportRelationshipTypes.TO_SERVICE).forEach(
                    relationship -> {
                        String tripIds = relationship.getProperty(GraphStaticKeys.TRIPS).toString();
                        if (!tripIds.contains(tripId)) {
                            relationship.setProperty(GraphStaticKeys.TRIPS, tripId + tripIds);
                        }
                    });
        }

        TramTime departureTime = beginStop.getDepartureTime();

        // Node for the hour
        String hourNodeId = format("%s_%s", svcNodeId, departureTime.getHourOfDay());
        Node hourNode = graphQuery.getTimeNode(hourNodeId);
        if (hourNode==null) {
            hourNode = createGraphNode(Labels.HOUR);
            hourNode.setProperty(GraphStaticKeys.ID, hourNodeId);
            hourNode.setProperty(GraphStaticKeys.HOUR, departureTime.getHourOfDay());

            // service node -> time node
            Relationship toHourRelationship = createRelationship(serviceNode, hourNode, TransportRelationshipTypes.TO_HOUR);
            toHourRelationship.setProperty(COST, 0);
        }

        // Node for the departure time
        String timeNodeId = format("%s_%s", svcNodeId, departureTime.toPattern());
        Node timeNode = graphQuery.getTimeNode(timeNodeId);
        if (timeNode==null) {
            timeNode = createGraphNode(Labels.MINUTE);
            timeNode.setProperty(GraphStaticKeys.ID, timeNodeId);
            timeNode.setProperty(GraphStaticKeys.TIME, departureTime.asLocalTime());

            // hour node -> time node
            Relationship toHourRelationship = createRelationship(hourNode, timeNode, TransportRelationshipTypes.TO_MINUTE);
            toHourRelationship.setProperty(COST, 0);
        }

        TransportRelationshipTypes transportRelationshipType =
                route.isTram() ? TransportRelationshipTypes.TRAM_GOES_TO : TransportRelationshipTypes.BUS_GOES_TO;

        // time node -> end route station
        Relationship goesToRelationship = createRelationship(timeNode, routeStationEnd, transportRelationshipType);
        goesToRelationship.setProperty(GraphStaticKeys.TRIP_ID, tripId);

        // TODO should not need depart time as using check on the Node instead
        // but for now it is used in the mapper code
        goesToRelationship.setProperty(GraphStaticKeys.DEPART_TIME, departureTime.asLocalTime());

        // common properties
        int cost = TramTime.diffenceAsMinutes(endStop.getArrivalTime(), beginStop.getArrivalTime());
        addCommonProperties(goesToRelationship, transportRelationshipType, route, service, cost);
    }

    private void createOrUpdateRelationship(Node start, Node end,
                                            Stop beginStop, Stop endStop, Route route, Service service) {

        // Confusingly some services can go different routes and hence have different outbound GOES relationships from
        // the same node, so we have to check both start and end nodes for each relationship

        LocalTime departTime = beginStop.getDepartureTime().asLocalTime();
        Relationship relationship = getRelationshipForService(service, start, end);

        if (relationship == null) {
            int cost = TramTime.diffenceAsMinutes(endStop.getArrivalTime(), beginStop.getArrivalTime());
            TransportRelationshipTypes transportRelationshipType;
            if (route.isTram()) {
                transportRelationshipType = TransportRelationshipTypes.TRAM_GOES_TO;
            } else {
                transportRelationshipType = TransportRelationshipTypes.BUS_GOES_TO;
            }
            createRelationship(start, end, transportRelationshipType, beginStop, route, service, cost);
        } else {
            // add the time of this stop to the service relationship
            LocalTime[] array = timesForRelationship.get(relationship.getId());
            if (Arrays.binarySearch(array, departTime) < 0) {
                LocalTime[] newTimes = Arrays.copyOf(array, array.length + 1);
                newTimes[array.length] = departTime;
                // keep times sorted
                Arrays.sort(newTimes);
                timesForRelationship.put(relationship.getId(), newTimes);
                relationship.setProperty(GraphStaticKeys.TIMES, newTimes);
            }
        }
    }

    private void createRelationship(Node start, Node end, TransportRelationshipTypes transportRelationshipType,
                                    Stop begin, Route route, Service service, int cost) {
        if (service.isRunning()) {
            Relationship relationship = createRelationship(start, end, transportRelationshipType);

            // times
            LocalTime[] times = new LocalTime[]{begin.getDepartureTime().asLocalTime()};   // initial contents
            timesForRelationship.put(relationship.getId(), times);
            relationship.setProperty(GraphStaticKeys.TIMES, times);

            // common properties
            addCommonProperties(relationship, transportRelationshipType, route, service, cost);

        }
    }

    private void addCommonProperties(Relationship relationship, TransportRelationshipTypes transportRelationshipType,
                                     Route route, Service service, int cost) {
        relationship.setProperty(COST, cost);
        relationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getServiceId());

        relationship.setProperty(GraphStaticKeys.DAYS, toBoolArray(service.getDays()));
        relationship.setProperty(GraphStaticKeys.SERVICE_START_DATE, service.getStartDate().getStringDate());
        relationship.setProperty(GraphStaticKeys.SERVICE_END_DATE, service.getEndDate().getStringDate());

        if (transportRelationshipType.equals(TransportRelationshipTypes.BUS_GOES_TO)) {
            relationship.setProperty(GraphStaticKeys.RouteStation.ROUTE_NAME, route.getName());
        }
    }

    private boolean runsAtLeastADay(HashMap<DaysOfWeek, Boolean> days) {
        return days.values().contains(true);
    }

    private boolean[] toBoolArray(HashMap<DaysOfWeek, Boolean> days) {
        boolean[] daysArray = new boolean[7];
        daysArray[0] = days.get(DaysOfWeek.Monday);
        daysArray[1] = days.get(DaysOfWeek.Tuesday);
        daysArray[2] = days.get(DaysOfWeek.Wednesday);
        daysArray[3] = days.get(DaysOfWeek.Thursday);
        daysArray[4] = days.get(DaysOfWeek.Friday);
        daysArray[5] = days.get(DaysOfWeek.Saturday);
        daysArray[6] = days.get(DaysOfWeek.Sunday);
        return daysArray;
    }

    private Relationship getRelationshipForService(Service service, Node startNode, Node end) {
        String serviceId = service.getServiceId();
        long endId = end.getId();

        Iterable<Relationship> existing = getOutboundJourneyRelationships(startNode);
        Stream<Relationship> existingStream = StreamSupport.stream(existing.spliterator(), false);
        Optional<Relationship> match = existingStream
                .filter(out -> out.getEndNode().getId() == endId)
                .filter(out -> {
                    String svcIdForRelationship = getSvcIdForRelationship(out);
                    return svcIdForRelationship.equals(serviceId);
                })
                .limit(1)
                .findAny();

        return match.orElse(null);
    }

    private Iterable<Relationship> getOutboundJourneyRelationships(Node startNode) {
        return startNode.getRelationships(OUTGOING,
                TransportRelationshipTypes.TRAM_GOES_TO, TransportRelationshipTypes.BUS_GOES_TO);
    }

    private String getSvcIdForRelationship(Relationship outgoing) {
        long id = outgoing.getId();
        if (relationToSvcId.containsKey(id)) {
            return relationToSvcId.get(id);
        }
        String svcId = outgoing.getProperty(GraphStaticKeys.SERVICE_ID).toString();
        relationToSvcId.put(id, svcId);
        return svcId;
    }

}
