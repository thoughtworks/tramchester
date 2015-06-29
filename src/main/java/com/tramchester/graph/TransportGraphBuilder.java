package com.tramchester.graph;


import com.tramchester.domain.*;
import org.neo4j.gis.spatial.indexprovider.SpatialIndexProvider;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class TransportGraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TransportGraphBuilder.class);

    public static final String CORNBROOK = "9400ZZMACRN";
    public static final String ST_PETERS_SQUARE = "9400ZZMASTP";
    public static final String PIC_GARDENS = "9400ZZMAPGD";
    public static final String TRAF_BAR = "9400ZZMATRA";
    public static final String ST_WS_ROAD = "9400ZZMASTW";
    public static final String VICTORIA = "9400ZZMAVIC";
    // not an official interchanges but several services terminate/branch here
    public static final String PICCADILLY = "9400ZZMAPIC";
    public static final String HARBOURCITY = "9400ZZMAHCY";

    public static final List<String> interchanges = Arrays.asList(new String[]{
            CORNBROOK, ST_PETERS_SQUARE, PIC_GARDENS, TRAF_BAR, ST_WS_ROAD, VICTORIA, PICCADILLY, HARBOURCITY});

    public static final int INTERCHANGE_DEPART_COST = 1;
    public static final int INTERCHANGE_BOARD_COST = 1;
    public static final int BOARDING_COST = 2;
    public static final int DEPARTS_COST = 2;

    private GraphDatabaseService graphDatabaseService;
    private TransportData transportData;
    private Index<Node> routeStations = null;
    private Index<Node> trams = null;

    public TransportGraphBuilder(GraphDatabaseService graphDatabaseService, TransportData transportData) {
        this.graphDatabaseService = graphDatabaseService;
        this.transportData = transportData;
    }

    public void buildGraph() {
        logger.info("Building graph from " + transportData.getFeedInfo());
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
            logger.info("Graph rebuild finished");

        } catch (Exception except) {
            logger.error("Exception while rebuilding the graph", except);
        } finally {
            tx.close();
        }
    }

    private void AddRouteServiceTrip(Route route, Service service, Trip trip) {
        List<Stop> stops = trip.getStops();
        for (int i = 0; i < stops.size() - 1; i++) {
            Stop currentStop = stops.get(i);
            Stop nextStop = stops.get(i + 1);

            Node from = getOrCreateRouteStation(currentStop.getStation(), route);
            Node to = getOrCreateRouteStation(nextStop.getStation(), route);

            int duration = nextStop.getMinutesFromMidnight() - currentStop.getMinutesFromMidnight();
            if (runsAtLeastADay(service.getDays())) {
                createOrUpdateRelationship(from, to, TransportRelationshipTypes.GOES_TO, currentStop,
                        duration, service, route, nextStop.getStation().getName());
            }
        }
    }

    Index<Node> spatialIndex = null;

    private Index<Node> getSpatialIndex() {
        if (spatialIndex == null) {
            spatialIndex = graphDatabaseService.index().forNodes("spatial_index",
                    SpatialIndexProvider.SIMPLE_POINT_CONFIG);
        }
        return spatialIndex;
    }

    private Node getOrCreateStation(Station station) {

        Index<Node> stationsIndex = getStationsIndex();
        Node node = getStationNode(station, stationsIndex);

        if (node == null) {
            logger.info("Creating station node: " + station);
            node = graphDatabaseService.createNode();
            node.setProperty(GraphStaticKeys.STATION_TYPE, GraphStaticKeys.STATION);

            node.setProperty(GraphStaticKeys.ID, station.getId());
            node.setProperty(GraphStaticKeys.Station.NAME, station.getName());
            node.setProperty(GraphStaticKeys.Station.LAT, station.getLatitude());
            node.setProperty(GraphStaticKeys.Station.LONG, station.getLongitude());
            stationsIndex.add(node, GraphStaticKeys.ID, station.getId());

            getSpatialIndex().add(node, station.getId(), station.getName());
        }
        return node;
    }

    private Node getStationNode(Station station, Index<Node> stationsIndex) {
        return stationsIndex.get(GraphStaticKeys.ID, station.getId()).getSingle();
    }

    private Index<Node> getStationsIndex() {
        if (trams == null) {
            trams = graphDatabaseService.index().forNodes(GraphStaticKeys.Station.IndexName);
        }
        return trams;
    }

    private Index<Node> getRouteStationsIndex() {
        if (routeStations == null) {
            routeStations = graphDatabaseService.index().forNodes(GraphStaticKeys.RouteStation.IndexName);
        }
        return routeStations;
    }

    private Node getOrCreateRouteStation(Station station, Route route) {
        String routeStationId = createRouteStationId(station, route);
        Node routeStation = getRouteStation(routeStationId);

        if (routeStation == null) {
            routeStation = createRouteStation(station, route, routeStationId);
        }

        Node stationNode = getOrCreateStation(station);

        TransportRelationshipTypes boardType;
        TransportRelationshipTypes departType;
        int boardCost;
        int departCost;
        if (preferedInterchange(stationNode)) {
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

        // station -> routeStation
        if (!alreadyHas(stationNode, routeStation, boardType)) {
            Relationship interchangeRelationshipTo = stationNode.createRelationshipTo(routeStation, boardType);
            interchangeRelationshipTo.setProperty(GraphStaticKeys.COST, boardCost);
            interchangeRelationshipTo.setProperty(GraphStaticKeys.ID, routeStationId);
        }

        // route station -> station
        if (!alreadyHas(routeStation, stationNode, departType)) {
            Relationship departRelationship = routeStation.createRelationshipTo(stationNode, departType);
            departRelationship.setProperty(GraphStaticKeys.COST, departCost);
            departRelationship.setProperty(GraphStaticKeys.ID, routeStationId);
        }

        return routeStation;
    }

    private Node getRouteStation(String routeStationId) {
        return getRouteStationsIndex().get(GraphStaticKeys.ID, routeStationId).getSingle();
    }

    private Node createRouteStation(Station station, Route route, String routeStationId) {
        logger.info("Creating route station node: " + station.getId() + " " + route.getId());
        Node routeStation = graphDatabaseService.createNode();
        routeStation.setProperty(GraphStaticKeys.STATION_TYPE, GraphStaticKeys.ROUTE_STATION);
        routeStation.setProperty(GraphStaticKeys.ID, routeStationId);
        routeStation.setProperty(GraphStaticKeys.RouteStation.ROUTE_NAME, route.getName());
        routeStation.setProperty(GraphStaticKeys.RouteStation.ROUTE_ID, route.getId());
        routeStation.setProperty(GraphStaticKeys.RouteStation.STATION_NAME, station.getName());
        getRouteStationsIndex().add(routeStation, GraphStaticKeys.ID, routeStationId);
        return routeStation;
    }

    private boolean alreadyHas(Node startNode, Node endNode, TransportRelationshipTypes relationshipType) {
        Iterable<Relationship> relates = startNode.getRelationships(Direction.OUTGOING, relationshipType);
        for (Relationship relationship : relates) {
            if (relationship.getEndNode().getId() == endNode.getId()) {
                return true;
            }
        }
        return false;
    }

    private String createRouteStationId(Station station, Route route) {
        return station.getId() + route.getId();
    }

    public static boolean preferedInterchange(Node stationNode) {
        return interchanges.contains(stationNode.getProperty(GraphStaticKeys.ID));
    }

    private void createOrUpdateRelationship(Node start, Node end, TransportRelationshipTypes transportRelationshipType,
                                            Stop stop, int cost, Service service, Route route, String dest) {

        // Confusingly some services can go different routes and hence have different outbound GOES relationships from
        // the same node, so we have to check both start and end nodes for each relationship

        int fromMidnight = stop.getMinutesFromMidnight();
        Relationship relationship = getRelationship(service, start, end);

        if (relationship == null) {
//            logger.info(String.format("create relationship svc %s from %s to %s route %s time %s",
//                    service.getServiceId(),
//                    start.getProperty(GraphStaticKeys.ID),end.getProperty(GraphStaticKeys.ID),
//                    route.getId(), fromMidnight));
            createRelationship(start, end, transportRelationshipType, stop, cost, service, route, dest);
        } else {
            // add the time of this stop to the service relationship
            int[] array = (int[]) relationship.getProperty(GraphStaticKeys.TIMES);
            if (Arrays.binarySearch(array, fromMidnight) < 0) {
                int[] newTimes = Arrays.copyOf(array, array.length + 1);
                newTimes[array.length] = fromMidnight;
                // keep times sorted
                Arrays.sort(newTimes);
                relationship.setProperty(GraphStaticKeys.TIMES, newTimes);
            }
        }
    }

    private void createRelationship(Node start, Node end, TransportRelationshipTypes transportRelationshipType,
                                    Stop stop, int cost, Service service,
                                    Route route, String dest) {
        if (service.isRunning()) {
            Relationship relationship = start.createRelationshipTo(end, transportRelationshipType);

            int[] times = new int[]{stop.getMinutesFromMidnight()};   // initial contents
            relationship.setProperty(GraphStaticKeys.TIMES, times);
            relationship.setProperty(GraphStaticKeys.COST, cost);
            relationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getServiceId());
            relationship.setProperty(GraphStaticKeys.DAYS, toBoolArray(service.getDays()));
            relationship.setProperty(GraphStaticKeys.RouteStation.ROUTE_NAME, route.getName());
            relationship.setProperty(GraphStaticKeys.ID, end.getProperty(GraphStaticKeys.ID));
            relationship.setProperty(GraphStaticKeys.ROUTE_STATION, dest);
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

        Iterable<Relationship> existing = startNode.getRelationships(Direction.OUTGOING);
        for (Relationship outgoing : existing) {
            if (outgoing.hasProperty(GraphStaticKeys.SERVICE_ID)) {
                String existingSvcId = outgoing.getProperty(GraphStaticKeys.SERVICE_ID).toString();
                if (existingSvcId.equals(serviceId)) {
                    long relationshipDestId = outgoing.getEndNode().getId();
                    if (relationshipDestId == end.getId()) {
                        return outgoing;
                    }
                }
            }
        }
        return null;
    }
}
