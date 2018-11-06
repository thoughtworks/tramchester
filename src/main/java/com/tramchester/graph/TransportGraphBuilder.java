package com.tramchester.graph;

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

import static java.lang.String.format;

public class TransportGraphBuilder extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(TransportGraphBuilder.class);

    private static final int INTERCHANGE_DEPART_COST = 1;
    private static final int INTERCHANGE_BOARD_COST = 1;
    private static final int DEPARTS_COST = 1;
    public static final int BOARDING_COST = 2;

    public enum Labels implements Label
    {
        ROUTE_STATION, STATION, AREA, PLATFORM, QUERY_NODE
    }

    private Map<String,TransportRelationshipTypes> boardings;
    private Map<String,TransportRelationshipTypes> departs;
    private List<String> platforms;
    private Map<Long, String> relationToSvcId;
    private Map<Long, LocalTime[]> timesForRelationship;

    private TransportData transportData;

    public TransportGraphBuilder(GraphDatabaseService graphDatabaseService, TransportData transportData,
                                 RelationshipFactory relationshipFactory, SpatialDatabaseService spatialDatabaseService) {
        super(graphDatabaseService, relationshipFactory, spatialDatabaseService, false);
        this.transportData = transportData;
        boardings = new HashMap<>();
        departs = new HashMap<>();
        relationToSvcId = new HashMap<>();
        timesForRelationship = new HashMap<>();
        platforms = new LinkedList<>();
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
    }

    private void createIndexs() {
        try ( Transaction tx = graphDatabaseService.beginTx() )
        {
            Schema schema = graphDatabaseService.schema();
            schema.indexFor(Labels.ROUTE_STATION)
                    .on(GraphStaticKeys.ID)
                    .create();
            schema.indexFor(Labels.STATION)
                    .on(GraphStaticKeys.ID)
                    .create();
            tx.success();
        }
    }

    private void AddRouteServiceTrip(Route route, Service service, Trip trip) {
       Stops stops = trip.getStops();
        for (int i = 0; i < stops.size() - 1; i++) {
            Stop currentStop = stops.get(i);
            Stop nextStop = stops.get(i + 1);

            Node from = getOrCreateRouteStation(currentStop, route, service);
            Node to = getOrCreateRouteStation(nextStop, route, service);

            if (runsAtLeastADay(service.getDays())) {
                createOrUpdateRelationship(from, to, currentStop, nextStop, service, route);
            }
        }
    }

    private Node getOrCreateStation(Location station) {

        String id = station.getId();
        String stationName = station.getName();
        Node stationNode = getStationNode(id);

        if (stationNode == null) {
            logger.info(format("Creating station node: %s ",station));
            stationNode = graphDatabaseService.createNode(Labels.STATION);

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

    private Node getOrCreatePlatform(Stop stop) {
        String id = stop.getId();

        Node platformNode = getPlatformNode(id);
        if (platformNode==null) {
            platformNode = graphDatabaseService.createNode(Labels.PLATFORM);
            platformNode.setProperty(GraphStaticKeys.ID, id);
            String platformName = id.substring(id.length()-1); // the final digit of the ID
            platformNode.setProperty(GraphStaticKeys.Station.NAME, format("%s Platform %s",stop.getStation().getName(),platformName));
        }
        return platformNode;
    }

    private Node getOrCreateRouteStation(Stop stop, Route route, Service service) {
        Location station = stop.getStation();
        String callingPointId = createCallingPointId(station, route);
        Node callingPoint = getRouteStationNode(callingPointId);

        if ( callingPoint == null) {
             callingPoint = createCallingPoint(station, route, callingPointId, service);
        }

        Node stationNode = getOrCreateStation(station);

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

        String stationId = station.getId();

        Node platformNode = stationNode;

        String stationOrPlatformID = stationId;
        if (station.isTram()) {
            // add a platform node between station and calling points
            platformNode = getOrCreatePlatform(stop);
            stationOrPlatformID = stop.getId();
            // station -> platform & platform -> station
            if (!hasPlatform(stationId, stationOrPlatformID)) {
                Relationship crossToPlatform = stationNode.createRelationshipTo(platformNode, TransportRelationshipTypes.ENTER_PLATFORM);
                crossToPlatform.setProperty(GraphStaticKeys.COST, 0);
                Relationship crossFromPlatform = platformNode.createRelationshipTo(stationNode, TransportRelationshipTypes.LEAVE_PLATFORM);
                crossFromPlatform.setProperty(GraphStaticKeys.COST, 0);
                // always create together
                platforms.add(stationId + stationOrPlatformID);
            }
        }

        // boarding: platform/station ->  callingPoint
        if (!hasBoarding(stationOrPlatformID, callingPointId, boardType)) {
            Relationship interchangeRelationshipTo = platformNode.createRelationshipTo( callingPoint, boardType);
            interchangeRelationshipTo.setProperty(GraphStaticKeys.COST, boardCost);
            interchangeRelationshipTo.setProperty(GraphStaticKeys.ID, callingPointId);
            boardings.put(boardKey(callingPointId, stationOrPlatformID), boardType);
        }

        // leave: route station -> platform/station
        if (!hasDeparting(callingPointId, stationOrPlatformID, departType)) {
            Relationship departRelationship =  callingPoint.createRelationshipTo(platformNode, departType);
            departRelationship.setProperty(GraphStaticKeys.COST, departCost);
            departRelationship.setProperty(GraphStaticKeys.ID, callingPointId);
            departs.put(departKey(callingPointId, stationOrPlatformID), departType);
        }

        return  callingPoint;
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
        Node routeStation = graphDatabaseService.createNode(Labels.ROUTE_STATION);
        routeStation.setProperty(GraphStaticKeys.ID, routeStationId);
        routeStation.setProperty(GraphStaticKeys.RouteStation.STATION_NAME, station.getName());
        routeStation.setProperty(GraphStaticKeys.RouteStation.ROUTE_NAME, route.getName());
        routeStation.setProperty(GraphStaticKeys.RouteStation.ROUTE_ID, route.getId());
        return routeStation;
    }

    private String createCallingPointId(Location station, Route route) {
        return station.getId() + route.getId();
    }

    private void createOrUpdateRelationship(Node start, Node end,
                                            Stop beginStop, Stop endStop, Service service, Route route) {

        // Confusingly some services can go different routes and hence have different outbound GOES relationships from
        // the same node, so we have to check both start and end nodes for each relationship

        LocalTime departTime = beginStop.getDepartureTime().asLocalTime();
        Relationship relationship = getRelationship(service, start, end);

        String dest = endStop.getStation().getName();

        if (relationship == null) {
            //int cost = endStop.getArriveMinsFromMidnight()-beginStop.getDepartureMinFromMidnight();
            int cost = TramTime.diffenceAsMinutes(endStop.getArrivalTime(), beginStop.getArrivalTime());
            TransportRelationshipTypes transportRelationshipType;
            if (route.isTram()) {
                transportRelationshipType = TransportRelationshipTypes.TRAM_GOES_TO;
            } else {
                transportRelationshipType = TransportRelationshipTypes.BUS_GOES_TO;
            }
            createRelationship(start, end, transportRelationshipType, beginStop, cost, service, route, dest);
        } else {
            // add the time of this stop to the service relationship
            //int[] array = (int[]) relationship.getProperty(GraphStaticKeys.TIMES);
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
                                    Stop begin, int cost, Service service,
                                    Route route, String dest) {
        if (service.isRunning()) {
            Relationship relationship = start.createRelationshipTo(end, transportRelationshipType);

            LocalTime[] times = new LocalTime[]{begin.getDepartureTime().asLocalTime()};   // initial contents
            timesForRelationship.put(relationship.getId(), times);
            relationship.setProperty(GraphStaticKeys.TIMES, times);
            relationship.setProperty(GraphStaticKeys.COST, cost);
            relationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getServiceId());
            relationship.setProperty(GraphStaticKeys.DAYS, toBoolArray(service.getDays()));
            relationship.setProperty(GraphStaticKeys.ID, end.getProperty(GraphStaticKeys.ID));
            relationship.setProperty(GraphStaticKeys.DESTINATION, dest);
            relationship.setProperty(GraphStaticKeys.SERVICE_START_DATE, service.getStartDate().getStringDate());
            relationship.setProperty(GraphStaticKeys.SERVICE_END_DATE, service.getEndDate().getStringDate());

            if (transportRelationshipType.equals(TransportRelationshipTypes.BUS_GOES_TO)) {
                relationship.setProperty(GraphStaticKeys.RouteStation.ROUTE_NAME, route.getName());
            }

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

    private Relationship getRelationship(Service service, Node startNode, Node end) {
        String serviceId = service.getServiceId();
        long endId = end.getId();

        Iterable<Relationship> existing = startNode.getRelationships(Direction.OUTGOING,
                TransportRelationshipTypes.TRAM_GOES_TO, TransportRelationshipTypes.BUS_GOES_TO);
        Stream<Relationship> existingStream = StreamSupport.stream(existing.spliterator(), false);
        Optional<Relationship> match = existingStream
                .filter(out -> out.getEndNode().getId() == endId)
                .filter(out -> getSvcIdForRelationship(out).equals(serviceId))
                .limit(1)
                .findAny();

        return match.orElse(null);

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
