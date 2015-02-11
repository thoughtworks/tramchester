package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.dataimport.TransportDataImporter;
import com.tramchester.domain.*;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {
    private GraphDatabaseService graphDatabaseService;
    private RouteCalculator routeCalculator;
    private Index<Node> routeStations = null;
    private Index<Node> trams = null;
    private static final Logger logger = LoggerFactory.getLogger(TestResource.class);

    public TestResource(GraphDatabaseService graphDatabaseService, RouteCalculator routeCalculator) {
        this.graphDatabaseService = graphDatabaseService;
        this.routeCalculator = routeCalculator;
    }


    @GET
    @Timed
    public Response get() {

        //build();

        //Pomona to stretford
        //routeCalculator.calculateRoute( "9400ZZMAPOM", "9400ZZMASFD",500);

        //Altringham to eccels
        routeCalculator.calculateRoute("9400ZZMAALT", "9400ZZMANIS", 500);

        return Response.ok().build();
    }


    private void build() {
        TransportData transportData = new TransportDataImporter().load();
        Transaction tx = graphDatabaseService.beginTx();
        try {

            for (Route route : transportData.getRoutes().values()) {

                for (Service service : route.getServices()) {
                    for (Trip trip : service.getTrips()) {
                        List<Stop> stops = trip.getStops();
                        for (int i = 0; i < stops.size() - 1; i++) {
                            Node from = getRouteStation(stops.get(i).getStation(), route);
                            Node to = getRouteStation(stops.get(i + 1).getStation(), route);
                            createRelationship(from, to, TransportRelationshipTypes.GOES_TO, stops.get(i),
                                    stops.get(i + 1).getMinutesFromMidnight() - stops.get(i).getMinutesFromMidnight(),
                                    service, route, trip);
                        }
                    }
                }


            }


            tx.success();
        } finally {
            tx.close();
            //graphDatabaseService.shutdown();
        }
    }


    private Node getStation(Station station) {

        Node node = getStationsIndex().get("id", station.getId()).getSingle();

        if (node == null) {
            logger.info("Creating node: " + station.getName());
            node = graphDatabaseService.createNode();
            node.setProperty("id", station.getId());
            node.setProperty("name", station.getName());
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
            getRouteStationsIndex().add(node, "id", station.getId() + route.getId());


            Relationship boardRelationshipTo = stationNode.createRelationshipTo(node, TransportRelationshipTypes.BOARD);
            if (stationNode.getProperty("id").equals("9400ZZMACRN")) {
                boardRelationshipTo.setProperty("cost", 3);
            } else {
                boardRelationshipTo.setProperty("cost", 5);
            }
            Relationship departRelationship = node.createRelationshipTo(stationNode, TransportRelationshipTypes.DEPART);
            departRelationship.setProperty("cost", 0);
        }
        return node;
    }

    private Relationship createRelationship(Node node1, Node node2, TransportRelationshipTypes transportRelationshipType, Stop stop, int cost, Service service, Route route, Trip trip) {

        Relationship relationship = getRelationship(service, node1);

        if (relationship == null && runsAtLeastADay(service.getDays())) {
            logger.info("create relationship from " + node1.getProperty("id") + " to " + node2.getProperty("id") + " for route " + route.getName());
            List<Integer> times = new ArrayList<>();
            List<String> trips = new ArrayList<>();
            times.add(stop.getMinutesFromMidnight());
            trips.add(trip.getTripId());
            relationship = node1.createRelationshipTo(node2, transportRelationshipType);
            relationship.setProperty("times", toIntArray(times));
            relationship.setProperty("cost", cost);
            relationship.setProperty("service_id", service.getServiceId());
            relationship.setProperty("days", toBoolArray(service.getDays()));
            relationship.setProperty("route", route.getCode());
            relationship.setProperty("route_name", route.getName());
        } else if (relationship != null) {
            int[] times = (int[]) relationship.getProperty("times");
            relationship.setProperty("times", toIntArray(times, stop.getMinutesFromMidnight()));
        }

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

    private int[] toIntArray(int[] times, int minutesFromMidnight) {
        if (Arrays.binarySearch(times, minutesFromMidnight) < 0) {

            int[] array = new int[times.length + 1];
            for (int i = 0; i < times.length; i++) {
                array[i] = times[i];
            }
            array[times.length] = minutesFromMidnight;
            return array;
        } else {
            return times;
        }
    }

    private int[] toIntArray(List<Integer> times) {
        int[] array = new int[times.size()];
        for (int i = 0; i < times.size(); i++) {
            array[i] = times.get(i);
        }
        Arrays.sort(array);
        return array;
    }

    private Relationship getRelationship(Service service, Node node1) {
        Iterable<Relationship> relationships = node1.getRelationships(Direction.OUTGOING);
        for (Relationship relationship : relationships) {
            if (relationship.hasProperty("service_id") && relationship.getProperty("service_id").toString().equals(service.getServiceId())) {
                return relationship;
            }
        }
        return null;
    }


}


