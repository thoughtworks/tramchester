package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.tramchester.graph.GraphStaticKeys.SERVICE_ID;
import static com.tramchester.graph.GraphStaticKeys.TIME;
import static com.tramchester.graph.TransportRelationshipTypes.*;

public class CachedNodeOperations implements NodeOperations {
    private final Map<Long, Boolean> serviceNodes;
    private final Map<Long, Boolean> hourNodes;
    private final Map<Long, Boolean> minuteNotes;
    private final Map<Long, Boolean> stationNodes;
    private final Map<Long, String> relationshipServiceIdCache;

    private final Map<Long, Integer> relationshipCostCache;
    private final Map<Long, Integer> hourCache;

    // cached times
    private final Map<Long, LocalTime> times;

    public CachedNodeOperations() {
        hourNodes = new ConcurrentHashMap<>();
        serviceNodes = new ConcurrentHashMap<>();
        times = new ConcurrentHashMap<>();
        minuteNotes = new ConcurrentHashMap<>();
        stationNodes = new ConcurrentHashMap<>();
        relationshipServiceIdCache = new ConcurrentHashMap<>();

        hourCache = new HashMap<>();
        relationshipCostCache = new ConcurrentHashMap<>();
    }

    @Override
    public LocalTime getTime(Node node) {
        long nodeId = node.getId();
        if (times.containsKey(nodeId)) {
            return times.get(nodeId);
        }
        LocalTime value = (LocalTime) node.getProperty(TIME);
        times.put(nodeId,value);
        return value;
    }

    @Override
    public String getServiceId(Node node) {
        return node.getProperty(GraphStaticKeys.SERVICE_ID).toString();
    }

    @Override
    public String getServiceId(Relationship inbound) {
        long relationshipId = inbound.getId();
        if (relationshipServiceIdCache.containsKey(relationshipId)) {
            return relationshipServiceIdCache.get(relationshipId);
        }
        String serviceId = inbound.getProperty(SERVICE_ID).toString();
        relationshipServiceIdCache.put(relationshipId, serviceId);
        return serviceId;
    }

    @Override
    public TramTime getServiceEarliest(Node node) {
        LocalTime localTime = (LocalTime) node.getProperty(GraphStaticKeys.SERVICE_EARLIEST_TIME);
        return TramTime.of(localTime);
    }

    @Override
    public TramTime getServiceLatest(Node node) {
        LocalTime localTime = (LocalTime) node.getProperty(GraphStaticKeys.SERVICE_LATEST_TIME);
        return TramTime.of(localTime);
    }

    @Override
    public boolean isStation(Node node) {
        return checkForLabel(stationNodes, node, TransportGraphBuilder.Labels.STATION);
    }

    @Override
    public int getCost(Relationship relationship) {
        // get type is more expensive than the getProperty
//        TransportRelationshipTypes type = TransportRelationshipTypes.valueOf(relationship.getType().name());
//        // these are just link relationships
//        if (type==TO_SERVICE || type==TO_HOUR || type==TO_MINUTE ) {
//            return 0;
//        }

        long relationshipId = relationship.getId();
        if (relationshipCostCache.containsKey(relationshipId)) {
            return relationshipCostCache.get(relationshipId);
        }

        int cost = (int) relationship.getProperty(GraphStaticKeys.COST);
        relationshipCostCache.put(relationshipId,cost);
        return cost;
    }

    @Override
    public boolean isService(Node node) {
        return checkForLabel(serviceNodes, node, TransportGraphBuilder.Labels.SERVICE);
    }

    @Override
    public boolean isHour(Node node) {
        return checkForLabel(hourNodes, node, TransportGraphBuilder.Labels.HOUR);
    }

    @Override
    public boolean isTime(Node node) {
        return checkForLabel(minuteNotes, node, TransportGraphBuilder.Labels.MINUTE);
    }

    @Override
    public int getHour(Node node) {
        long id = node.getId();
        if (hourCache.containsKey(id)) {
            return hourCache.get(id);
        }
        int hour = (int) node.getProperty(GraphStaticKeys.HOUR);
        hourCache.put(id,hour);
        return hour;
    }

    public boolean checkForLabel(Map<Long, Boolean> map, Node node, TransportGraphBuilder.Labels label) {
        long id = node.getId();
        if (map.containsKey(id)) {
            return map.get(id);
        }

        boolean flag = node.hasLabel(label);
        map.put(id, flag);
        return flag;
    }
}
