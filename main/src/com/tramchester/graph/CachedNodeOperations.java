package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.TransportGraphBuilder.Labels.ROUTE_STATION;

public class CachedNodeOperations {

    private final Map<Long, Integer> relationshipCostCache;
    private final Map<Long, Integer> hourNodeCache;
    private final Map<Long, LocalTime> timeRelationshipCache;
    private final Map<Long, String> tripRelationshipCache;
    private final Map<Long, String> svcIdCache;

    // cached times
    private final Map<Long, LocalTime> times;
    private final NodeIdLabelMap nodeIdLabelMap;

    public CachedNodeOperations(NodeIdLabelMap nodeIdLabelMap) {
        this.nodeIdLabelMap = nodeIdLabelMap;
        times = new ConcurrentHashMap<>();

        timeRelationshipCache = new ConcurrentHashMap<>();
        tripRelationshipCache = new ConcurrentHashMap<>();
        svcIdCache = new ConcurrentHashMap<>();

        hourNodeCache = new ConcurrentHashMap<>();
        relationshipCostCache = new ConcurrentHashMap<>();
    }

    public String getTrips(Relationship relationship) {
        long relationshipId = relationship.getId();
        if (tripRelationshipCache.containsKey(relationshipId)) {
            return tripRelationshipCache.get(relationshipId);
        }
        String trips = relationship.getProperty(TRIPS).toString();
        tripRelationshipCache.put(relationshipId, trips);
        return trips;
    }

    public String getTrip(Relationship relationship) {
        long relationshipId = relationship.getId();
        if (tripRelationshipCache.containsKey(relationshipId)) {
            return tripRelationshipCache.get(relationshipId);
        }
        String trip = relationship.getProperty(TRIP_ID).toString();
        tripRelationshipCache.put(relationshipId, trip);
        return trip;
    }

    public LocalTime getTime(Node node) {
        long nodeId = node.getId();
        if (times.containsKey(nodeId)) {
            return times.get(nodeId);
        }
        LocalTime value = (LocalTime) node.getProperty(TIME);
        times.put(nodeId,value);
        return value;
    }

    public LocalTime getTime(Relationship relationship) {
        long relationshipId = relationship.getId();
        if (timeRelationshipCache.containsKey(relationshipId)) {
            return timeRelationshipCache.get(relationshipId);
        }
        LocalTime time = (LocalTime) relationship.getProperty(TIME);
        timeRelationshipCache.put(relationshipId, time);
        return time;
    }

    public String getServiceId(Node node) {
        long id = node.getId();
        if (svcIdCache.containsKey(id)) {
            return svcIdCache.get(id);
        }
        String svcId = node.getProperty(GraphStaticKeys.SERVICE_ID).toString();
        svcIdCache.put(id, svcId);
        return svcId;
    }

    public TramTime getServiceEarliest(Node node) {
        LocalTime localTime = (LocalTime) node.getProperty(GraphStaticKeys.SERVICE_EARLIEST_TIME);
        return TramTime.of(localTime);
    }

    public TramTime getServiceLatest(Node node) {
        LocalTime localTime = (LocalTime) node.getProperty(GraphStaticKeys.SERVICE_LATEST_TIME);
        return TramTime.of(localTime);
    }

    public int getCost(Relationship relationship) {

        long relationshipId = relationship.getId();
        if (relationshipCostCache.containsKey(relationshipId)) {
            return relationshipCostCache.get(relationshipId);
        }

        int cost = (int) relationship.getProperty(GraphStaticKeys.COST);
        relationshipCostCache.put(relationshipId,cost);
        return cost;
    }

//    public int getHour(Relationship relationship) {
//        long relationshipId = relationship.getId();
//        if (hourRelationshipCache.containsKey(relationshipId)) {
//            return hourRelationshipCache.get(relationshipId);
//        }
//        int hour = (int) relationship.getProperty(HOUR);
//        hourRelationshipCache.put(relationshipId, hour);
//        return hour;
//    }

    public boolean isService(Node node) {
        return checkForLabel(node, TransportGraphBuilder.Labels.SERVICE);
    }

    private boolean checkForLabel(Node node, TransportGraphBuilder.Labels label) {
        return nodeIdLabelMap.getLabel(node.getId())==label;
    }

    public boolean isHour(Node node) {
        return checkForLabel(node, TransportGraphBuilder.Labels.HOUR);
    }

    public boolean isTime(Node node) {
        return checkForLabel(node, TransportGraphBuilder.Labels.MINUTE);
    }

    public boolean isRouteStation(Node endNode) {
        return checkForLabel(endNode, ROUTE_STATION);
    }

    public int getHour(Node node) {
        long id = node.getId();
        if (hourNodeCache.containsKey(id)) {
            return hourNodeCache.get(id);
        }
        int hour = (int) node.getProperty(GraphStaticKeys.HOUR);
        hourNodeCache.put(id,hour);
        return hour;
    }

    public String getTrip(Node endNode) {
        if (!endNode.hasProperty(TRIP_ID)) {
            return "";
        }
        return endNode.getProperty(TRIP_ID).toString();
    }

    public Node createNode(GraphDatabaseService graphDatabaseService, TransportGraphBuilder.Labels label) {
        Node result = graphDatabaseService.createNode(label);
        nodeIdLabelMap.put(result.getId(), label);
        return result;
    }

    public void deleteNode(Node node) {
        long id = node.getId();
        node.delete();
        nodeIdLabelMap.removeId(id);
    }
}
