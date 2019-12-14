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
    private final Map<Long, String> tripRelationshipCache;
    private final Map<Long, String> svcIdCache;

    // cached times
    private final Map<Long, TramTime> times;
    private final NodeIdLabelMap nodeIdLabelMap;

    public CachedNodeOperations(NodeIdLabelMap nodeIdLabelMap) {
        this.nodeIdLabelMap = nodeIdLabelMap;

        times = new ConcurrentHashMap<>();
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

    public TramTime getTime(Node node) {
        long nodeId = node.getId();
        if (times.containsKey(nodeId)) {
            return times.get(nodeId);
        }
        LocalTime value = (LocalTime) node.getProperty(TIME);
        TramTime tramTime = TramTime.of(value);
        times.put(nodeId,tramTime);
        return tramTime;
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

    public int getCost(Relationship relationship) {

        long relationshipId = relationship.getId();
        if (relationshipCostCache.containsKey(relationshipId)) {
            return relationshipCostCache.get(relationshipId);
        }

        int cost = (int) relationship.getProperty(GraphStaticKeys.COST);
        relationshipCostCache.put(relationshipId,cost);
        return cost;
    }

    public boolean isService(long nodeId) {
        return checkForLabel(nodeId, TransportGraphBuilder.Labels.SERVICE);
    }

    private boolean checkForLabel(long nodeId, TransportGraphBuilder.Labels label) {
        return nodeIdLabelMap.has(nodeId, label);
    }

    public boolean isHour(long nodeId) {
        return checkForLabel(nodeId, TransportGraphBuilder.Labels.HOUR);
    }

    public boolean isTime(long nodeId) {
        return checkForLabel(nodeId, TransportGraphBuilder.Labels.MINUTE);
    }

    public boolean isRouteStation(long nodeId) {
        return checkForLabel(nodeId, ROUTE_STATION);
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

    public Node createQueryNode(GraphDatabaseService graphDatabaseService) {
        Node result = graphDatabaseService.createNode(TransportGraphBuilder.Labels.QUERY_NODE);
        nodeIdLabelMap.putQueryNode(result.getId());
        return result;
    }

    public void deleteNode(Node node) {
        long id = node.getId();
        node.delete();
        nodeIdLabelMap.removeQueryNode(id);
    }

}
