package com.tramchester.graph;


import com.sun.javafx.collections.SortHelper;
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
    private static final String PIC_GARDENS = "9400ZZMAPGD";
    private static final String TRAF_BAR = "9400ZZMATRA";
    private static final String ST_WS_ROAD = "9400ZZMASTW";
    private static final String VICTORIA = "9400ZZMAVIC";
    // piccadily is not an official interchange but several services terminate here
    private static final String PICCADILLY = "9400ZZMAPIC";

    public static final List<String> interchanges = Arrays.asList(new String[]{
            CORNBROOK, ST_PETERS_SQUARE, PIC_GARDENS, TRAF_BAR, ST_WS_ROAD, VICTORIA, PICCADILLY});

    private GraphDatabaseService graphDatabaseService;
    private TransportData transportData;
    private Index<Node> routeStations = null;
    private Index<Node> trams = null;

    public TransportGraphBuilder(GraphDatabaseService graphDatabaseService, TransportData transportData) {
        this.graphDatabaseService = graphDatabaseService;
        this.transportData = transportData;
    }

    public void buildGraph() {
        Transaction tx = graphDatabaseService.beginTx();
        try {
            logger.info("Rebuilding the graph...");
            for (Route route : transportData.getRoutes().values()) {
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
        }
        finally {
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
            createOrUpdateRelationship(from, to, TransportRelationshipTypes.GOES_TO, currentStop,
                    duration, service, route);
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
        Node node = stationsIndex.get(GraphStaticKeys.Station.ID, station.getId()).getSingle();

        if (node == null) {
            logger.info("Creating station node: " + station.getName());
            node = graphDatabaseService.createNode();
            node.setProperty(GraphStaticKeys.STATION_TYPE, GraphStaticKeys.STATION);

            node.setProperty(GraphStaticKeys.Station.ID, station.getId());
            node.setProperty(GraphStaticKeys.Station.NAME, station.getName());
            node.setProperty(GraphStaticKeys.Station.LAT, station.getLatitude());
            node.setProperty(GraphStaticKeys.Station.LONG, station.getLongitude());
            getSpatialIndex().add(node, station.getId(), station.getName());
            stationsIndex.add(node, GraphStaticKeys.Station.ID, station.getId());
        }
        return node;
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
        Node stationNode = getOrCreateStation(station);

        String routeStationId = createRouteStationId(station, route);
        Node routeStation = getRouteStationsIndex().get(GraphStaticKeys.Station.ID, routeStationId).getSingle();

        if (routeStation == null) {
            routeStation = createRouteStation(station, route, routeStationId);
        }

        // station -> route station
        if (preferedInterchange(stationNode)) {
            if (!alreadyHas(stationNode, routeStation, TransportRelationshipTypes.INTERCHANGE)) {
                Relationship interchangeRelationshipTo = stationNode.createRelationshipTo(routeStation, TransportRelationshipTypes.INTERCHANGE);
                interchangeRelationshipTo.setProperty(GraphStaticKeys.COST, 3);
            }
        } else {
            if (!alreadyHas(stationNode, routeStation, TransportRelationshipTypes.BOARD)) {
                Relationship boardRelationshipTo = stationNode.createRelationshipTo(routeStation, TransportRelationshipTypes.BOARD);
                boardRelationshipTo.setProperty(GraphStaticKeys.COST, 5); // was 5
            }
        }
        // route station -> station
        if (!alreadyHas(routeStation, stationNode, TransportRelationshipTypes.DEPART)) {
            Relationship departRelationship = routeStation.createRelationshipTo(stationNode, TransportRelationshipTypes.DEPART);
            departRelationship.setProperty(GraphStaticKeys.COST, 0);
        }

        return routeStation;
    }

    private Node createRouteStation(Station station, Route route, String routeStationId) {
        logger.info("Creating route station node: " + station.getId() + " " + route.getId());
        Node routeStation = graphDatabaseService.createNode();
        routeStation.setProperty(GraphStaticKeys.STATION_TYPE, GraphStaticKeys.ROUTE_STATION);
        routeStation.setProperty(GraphStaticKeys.Station.ID, routeStationId);
        routeStation.setProperty(GraphStaticKeys.ROUTE_NAME, route.getName());
        routeStation.setProperty(GraphStaticKeys.ROUTE_ID, route.getId());
        routeStation.setProperty(GraphStaticKeys.STATION_NAME, station.getName());
        getRouteStationsIndex().add(routeStation, GraphStaticKeys.Station.ID, routeStationId);
        return routeStation;
    }

    private boolean alreadyHas(Node startNode, Node endNode, TransportRelationshipTypes relationshipType) {
        Iterable<Relationship> relates = startNode.getRelationships(Direction.OUTGOING, relationshipType);
        for(Relationship relationship : relates) {
            if (relationship.getEndNode().getId()==endNode.getId()) {
                return true;
            }
        }
        return false;
    }

    private String createRouteStationId(Station station, Route route) {
        return station.getId() + route.getId();
    }

    private boolean preferedInterchange(Node stationNode) {
        return interchanges.contains(stationNode.getProperty(GraphStaticKeys.Station.ID));
    }

    private Relationship createOrUpdateRelationship(Node start, Node end, TransportRelationshipTypes transportRelationshipType,
                                                    Stop stop, int cost, Service service, Route route) {
        // Confusingly some services can go different routes and hence have different outbound GOES relationships from
        // the same node, so we have to check both start and end nodes for each relationship

        Relationship relationship = getRelationship(service, start, end);
        int fromMidnight = stop.getMinutesFromMidnight();

        if (relationship == null && runsAtLeastADay(service.getDays())) {
            logger.info(String.format("create relationship svc %s from %s to %s route %s time %s",
                    service.getServiceId(),
                    start.getProperty(GraphStaticKeys.Station.ID),end.getProperty(GraphStaticKeys.Station.ID),
                    route.getId(), fromMidnight));
            relationship = createRelationship(start, end, transportRelationshipType, stop, cost, service, route);
        } else if (relationship != null) {

            // add the time of this stop to the service relationship
            int[] array = (int[]) relationship.getProperty(GraphStaticKeys.TIMES);
            if (Arrays.binarySearch(array,fromMidnight)<0) {
                int[] newTimes = Arrays.copyOf(array, array.length + 1);
                newTimes[array.length] = fromMidnight;
                // keep times sorted
                Arrays.sort(newTimes);
                relationship.setProperty(GraphStaticKeys.TIMES, newTimes);

            }
        }

        return relationship;
    }

    private Relationship createRelationship(Node start, Node end, TransportRelationshipTypes transportRelationshipType,
                                            Stop stop, int cost, Service service, Route route) {

        int[] times = new int[] {stop.getMinutesFromMidnight()};   // initial contents

        Relationship relationship = start.createRelationshipTo(end, transportRelationshipType);
        relationship.setProperty(GraphStaticKeys.TIMES, times);
        relationship.setProperty(GraphStaticKeys.COST, cost);
        relationship.setProperty(GraphStaticKeys.SERVICE_ID, service.getServiceId());
        relationship.setProperty(GraphStaticKeys.DAYS, toBoolArray(service.getDays()));
        relationship.setProperty(GraphStaticKeys.ROUTE_NAME, route.getName());

        return relationship;
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
        Iterable<Relationship> relationships = startNode.getRelationships(Direction.OUTGOING);
        for (Relationship relationship : relationships) {
            if (relationship.getEndNode().getId()==end.getId()) {
                if (relationship.hasProperty(GraphStaticKeys.SERVICE_ID) &&
                        relationship.getProperty(GraphStaticKeys.SERVICE_ID).toString().equals(service.getServiceId())) {
                    return relationship;
                }
            }
        }
        return null;
    }
}
