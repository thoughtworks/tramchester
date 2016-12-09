package com.tramchester.graph;


import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.repository.TransportData;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.schema.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;

public class TransportGraphBuilder extends StationIndexs {
    private static final Logger logger = LoggerFactory.getLogger(TransportGraphBuilder.class);

    public static final int INTERCHANGE_DEPART_COST = 1;
    public static final int INTERCHANGE_BOARD_COST = 1;
    public static final int BOARDING_COST = 2;
    public static final int DEPARTS_COST = 1;

    public enum Labels implements Label
    {
        ROUTE_STATION, STATION
    }

    private Map<String,TransportRelationshipTypes> boardings;
    private Map<String,TransportRelationshipTypes> departs;
    private Map<Long, String> relationToSvcId;
    private Map<Long, int[]> timesForRelationship;

    private TransportData transportData;

    public TransportGraphBuilder(GraphDatabaseService graphDatabaseService, TransportData transportData,
                                 RelationshipFactory relationshipFactory, SpatialDatabaseService spatialDatabaseService) {
        super(graphDatabaseService, relationshipFactory, spatialDatabaseService, false);
        this.transportData = transportData;
        boardings = new HashMap<>();
        departs = new HashMap<>();
        relationToSvcId = new HashMap<>();
        timesForRelationship = new HashMap<>();
    }

    public void buildGraph() {
        logger.info("Building graph from " + transportData.getFeedInfo());
        DateTime start = DateTime.now();

        createIndexs();

        Transaction tx = graphDatabaseService.beginTx();
        try {
            logger.info("Rebuilding the graph...");
            for (Route route : transportData.getRoutes()) {
                for (Service service : route.getServices()) {
                    for (Trip trip : service.getTrips()) {
                        AddRouteServiceTrip(route, service, trip);
                    }
                }
            }
            tx.success();
            Seconds duration = Seconds.secondsBetween(start, DateTime.now());
            logger.info("Graph rebuild finished, took " + duration.getSeconds());

        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
        } finally {
            tx.close();
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

            Node from = getOrCreateRouteStation(currentStop.getStation(), route, service);
            Node to = getOrCreateRouteStation(nextStop.getStation(), route, service);

            if (runsAtLeastADay(service.getDays())) {
                createOrUpdateRelationship(from, to, currentStop,
                        nextStop, service, route);
            }
        }
    }

    private Node getOrCreateStation(Location station) {

        String id = station.getId();
        String stationName = station.getName();
        Node node = getStationNode(id);

        if (node == null) {
            logger.info(format("Creating station node: %s ",station));
            node = graphDatabaseService.createNode(Labels.STATION);

            node.setProperty(GraphStaticKeys.STATION_TYPE, GraphStaticKeys.STATION);
            node.setProperty(GraphStaticKeys.ID, id);
            node.setProperty(GraphStaticKeys.Station.NAME, stationName);
            LatLong latLong = station.getLatLong();
            node.setProperty(GraphStaticKeys.Station.LAT, latLong.getLat());
            node.setProperty(GraphStaticKeys.Station.LONG, latLong.getLon());

            getSpatialLayer().add(node);
            //getSpatialLayer().add(node, id, stationName);
        }
        return node;
    }

    private Node getOrCreateRouteStation(Location station, Route route, Service service) {
        String routeStationId = createRouteStationId(station, route);
        Node routeStation = getRouteStationNode(routeStationId);

        if (routeStation == null) {
            routeStation = createRouteStation(station, route, routeStationId, service);
        }

        Node stationNode = getOrCreateStation(station);

        TransportRelationshipTypes boardType;
        TransportRelationshipTypes departType;
        int boardCost;
        int departCost;
        if (Interchanges.has(station)) {
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

        // boarding: station -> routeStation
        String stationId = station.getId();
        if (!hasBoarding(stationId, routeStationId, boardType)) {
            Relationship interchangeRelationshipTo = stationNode.createRelationshipTo(routeStation, boardType);
            interchangeRelationshipTo.setProperty(GraphStaticKeys.COST, boardCost);
            interchangeRelationshipTo.setProperty(GraphStaticKeys.ID, routeStationId);
            boardings.put(stationId+routeStationId, boardType);
        }

        // leave: route station -> station
        if (!hasDeparting(routeStationId, stationId, departType)) {
            Relationship departRelationship = routeStation.createRelationshipTo(stationNode, departType);
            departRelationship.setProperty(GraphStaticKeys.COST, departCost);
            departRelationship.setProperty(GraphStaticKeys.ID, routeStationId);
            departs.put(routeStationId+stationId, departType);
        }

        return routeStation;
    }

    private boolean hasDeparting(String routeStationId, String stationId, TransportRelationshipTypes departType) {
        String key = routeStationId + stationId;
        if (departs.containsKey(key)) {
            return departs.get(key).equals(departType);
        }
        return false;
    }

    private boolean hasBoarding(String stationId, String routeStationId, TransportRelationshipTypes boardType) {
        String key = stationId + routeStationId;
        if (boardings.containsKey(key)) {
            return boardings.get(key).equals(boardType);
        }
        return false;
    }

    private Node createRouteStation(Location station, Route route, String routeStationId, Service service) {
        logger.info(format("Creating route station %s route %s service %s", station.getId(),route.getId(),
                service.getServiceId()));
        Node routeStation = graphDatabaseService.createNode(Labels.ROUTE_STATION);
        routeStation.setProperty(GraphStaticKeys.STATION_TYPE, GraphStaticKeys.ROUTE_STATION);
        routeStation.setProperty(GraphStaticKeys.ID, routeStationId);
        routeStation.setProperty(GraphStaticKeys.RouteStation.STATION_NAME, station.getName());
        routeStation.setProperty(GraphStaticKeys.RouteStation.ROUTE_NAME, route.getName());
        routeStation.setProperty(GraphStaticKeys.RouteStation.ROUTE_ID, route.getId());
        return routeStation;
    }

    private String createRouteStationId(Location station, Route route) {
        return station.getId() + route.getId();
    }

    private void createOrUpdateRelationship(Node start, Node end,
                                            Stop beginStop, Stop endStop, Service service, Route route) {

        // Confusingly some services can go different routes and hence have different outbound GOES relationships from
        // the same node, so we have to check both start and end nodes for each relationship

        int fromMidnight = beginStop.getDepartureMinFromMidnight();
        Relationship relationship = getRelationship(service, start, end);

        String dest = endStop.getStation().getName();

        if (relationship == null) {
            int cost = endStop.getArriveMinsFromMidnight()-beginStop.getDepartureMinFromMidnight();
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
            int[] array = timesForRelationship.get(relationship.getId());
            if (Arrays.binarySearch(array, fromMidnight) < 0) {
                int[] newTimes = Arrays.copyOf(array, array.length + 1);
                newTimes[array.length] = fromMidnight;
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

            int[] times = new int[]{begin.getDepartureMinFromMidnight()};   // initial contents
            timesForRelationship.put(relationship.getId(), times);
            relationship.setProperty(GraphStaticKeys.TIMES, times);
            relationship.setProperty(GraphStaticKeys.COST, cost);
            relationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getServiceId());
            relationship.setProperty(GraphStaticKeys.DAYS, toBoolArray(service.getDays()));
            relationship.setProperty(GraphStaticKeys.RouteStation.ROUTE_NAME, route.getName());
            relationship.setProperty(GraphStaticKeys.ID, end.getProperty(GraphStaticKeys.ID));
            relationship.setProperty(GraphStaticKeys.ROUTE_STATION, dest);
            relationship.setProperty(GraphStaticKeys.SERVICE_START_DATE, service.getStartDate().getStringDate());
            relationship.setProperty(GraphStaticKeys.SERVICE_END_DATE, service.getEndDate().getStringDate());
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

        if (match.isPresent()) {
            return match.get();
        }

        return null;
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
