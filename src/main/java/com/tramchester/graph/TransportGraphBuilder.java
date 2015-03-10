package com.tramchester.graph;


import com.sun.javafx.collections.SortHelper;
import com.tramchester.domain.*;
import javafx.collections.transformation.SortedList;
import org.neo4j.gis.spatial.indexprovider.SpatialIndexProvider;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
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
    public static final List<String> interchanges = Arrays.asList(new String[]{
            CORNBROOK, ST_PETERS_SQUARE, PIC_GARDENS, TRAF_BAR, ST_WS_ROAD, VICTORIA});

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
                        List<Stop> stops = trip.getStops();
                        for (int i = 0; i < stops.size() - 1; i++) {
                            Stop currentStop = stops.get(i);
                            Stop nextStop = stops.get(i + 1);

                            Node from = getRouteStation(currentStop.getStation(), route);
                            Node to = getRouteStation(nextStop.getStation(), route);

                            int duration = nextStop.getMinutesFromMidnight() - currentStop.getMinutesFromMidnight();
                            createOrUpdateRelationship(from, to, TransportRelationshipTypes.GOES_TO, currentStop,
                                    duration, service, route, trip);
                        }
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

    Index<Node> spatialIndex = null;

    private Index<Node> getSpatialIndex() {
        if (spatialIndex == null)
            spatialIndex = graphDatabaseService.index().forNodes("spatial_index", SpatialIndexProvider.SIMPLE_POINT_CONFIG);

        return spatialIndex;
    }

    private Node getStation(Station station) {

        Node node = getStationsIndex().get("id", station.getId()).getSingle();

        if (node == null) {
            logger.info("Creating node: " + station.getName());
            node = graphDatabaseService.createNode();
            node.setProperty("id", station.getId());
            node.setProperty("name", station.getName());
            node.setProperty("lat", station.getLatitude());
            node.setProperty("lon", station.getLongitude());
            getSpatialIndex().add(node, station.getId(), station.getName());
            getStationsIndex().add(node, "id", station.getId());
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
            routeStations = graphDatabaseService.index().forNodes("route_stations");
        }
        return routeStations;
    }

    private Node getRouteStation(Station station, Route route) {
        Node stationNode = getStation(station);

        Node node = getRouteStationsIndex().get("id", station.getId() + route.getId()).getSingle();

        if (node == null) {
            logger.info("Creating route station node: " + station.getName() + " " + route.getName());
            node = graphDatabaseService.createNode();
            node.setProperty("id", station.getId() + route.getId());
            node.setProperty("route_name", route.getName());
            node.setProperty("route_id", route.getId());
            getRouteStationsIndex().add(node, "id", station.getId() + route.getId());

            if (preferedInterchange(stationNode)) {
                Relationship boardRelationshipTo = stationNode.createRelationshipTo(node, TransportRelationshipTypes.INTERCHANGE);
                boardRelationshipTo.setProperty("cost", 3);
            } else {
                Relationship boardRelationshipTo = stationNode.createRelationshipTo(node, TransportRelationshipTypes.BOARD);
                boardRelationshipTo.setProperty("cost", 5); // was 5
            }
            Relationship departRelationship = node.createRelationshipTo(stationNode, TransportRelationshipTypes.DEPART);
            departRelationship.setProperty("cost", 0);
        }
        return node;
    }

    private boolean preferedInterchange(Node stationNode) {
        return interchanges.contains(stationNode.getProperty("id"));
    }

    private Relationship createOrUpdateRelationship(Node start, Node end, TransportRelationshipTypes transportRelationshipType,
                                                    Stop stop, int cost, Service service, Route route, Trip trip) {
        // Confusingly some services can go different routes and hence have different GOES relationships from
        // the same node, so we have to check both start and end nodes for each relationship
        SortHelper helper = new SortHelper();

        Relationship relationship = getRelationship(service, start, end);

        if (relationship == null && runsAtLeastADay(service.getDays())) {
            relationship = createRelationship(start, end, transportRelationshipType, stop, cost, service, route);
        } else if (relationship != null) {
            // services run particular days, so no need to update that here
            int fromMidnight = stop.getMinutesFromMidnight();

            int[] array = (int[]) relationship.getProperty("times");
            if (Arrays.binarySearch(array,fromMidnight)<0) {
                int[] newTimes = Arrays.copyOf(array, array.length + 1);
                newTimes[array.length] = fromMidnight;
                // keep times sorted
                Arrays.sort(newTimes);
                relationship.setProperty("times", newTimes);
            }

        }

        return relationship;
    }

    private Relationship createRelationship(Node start, Node end, TransportRelationshipTypes transportRelationshipType, Stop stop, int cost, Service service, Route route) {
        Relationship relationship;
        logger.info("create relationship from " + start.getProperty("id") + " to " + end.getProperty("id") + " for route " + route.getName());
        int minutesFromMidnight = stop.getMinutesFromMidnight();
        relationship = start.createRelationshipTo(end, transportRelationshipType);
        int[] times = new int[] {minutesFromMidnight};
        relationship.setProperty("times", times);
        relationship.setProperty("cost", cost);
        relationship.setProperty("service_id", service.getServiceId());
        relationship.setProperty("days", toBoolArray(service.getDays()));
        relationship.setProperty("route", route.getCode());
        relationship.setProperty("route_name", route.getName());
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

//    private int[] toIntArray(int[] times, int minutesFromMidnight) {
//        if (Arrays.binarySearch(times, minutesFromMidnight) < 0) {
//
//            int[] array = new int[times.length + 1];
//            for (int i = 0; i < times.length; i++) {
//                array[i] = times[i];
//            }
//            array[times.length] = minutesFromMidnight;
//            return array;
//        } else {
//            return times;
//        }
//    }

//    private int[] toIntArray(List<Integer> times) {
//        int[] array = new int[times.size()];
//        for (int i = 0; i < times.size(); i++) {
//            array[i] = times.get(i);
//        }
//        Arrays.sort(array);
//        return array;
//    }

    private Relationship getRelationship(Service service, Node startNode, Node end) {
        Iterable<Relationship> relationships = startNode.getRelationships(Direction.OUTGOING);
        for (Relationship relationship : relationships) {
            if (relationship.getEndNode().getId()==end.getId()) {
                if (relationship.hasProperty("service_id") &&
                        relationship.getProperty("service_id").toString().equals(service.getServiceId())) {
                    return relationship;
                }
            }
        }
        return null;
    }
}
