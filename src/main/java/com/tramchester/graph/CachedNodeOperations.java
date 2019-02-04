package com.tramchester.graph;

import com.tramchester.domain.TramServiceDate;
import org.neo4j.graphdb.Node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.tramchester.graph.GraphStaticKeys.HOUR;

public class CachedNodeOperations implements NodeOperations {
    private final Map<Long, Boolean> serviceNodes;
    private final Map<Long, Boolean> hourNodes;

    public CachedNodeOperations() {
        hourNodes = new ConcurrentHashMap<>();
        serviceNodes = new ConcurrentHashMap<>();
    }


    @Override
    public boolean[] getDays(Node node) {
        return (boolean[]) node.getProperty(GraphStaticKeys.DAYS);
    }

    @Override
    public TramServiceDate getServiceStartDate(Node node) {
        return new TramServiceDate(node.getProperty(GraphStaticKeys.SERVICE_START_DATE).toString());
    }

    @Override
    public TramServiceDate getServiceEndDate(Node node) {
        return new TramServiceDate(node.getProperty(GraphStaticKeys.SERVICE_END_DATE).toString());
    }

    @Override
    public int getHour(Node node) {
        return (int) node.getProperty(HOUR);
    }

    @Override
    public boolean isService(Node node) {
        return checkForLabel(serviceNodes, node, TransportGraphBuilder.Labels.SERVICE);
    }

    @Override
    public boolean isHour(Node node) {
        return checkForLabel(hourNodes, node, TransportGraphBuilder.Labels.HOUR);
    }

    public boolean checkForLabel(Map<Long, Boolean> map, Node node, TransportGraphBuilder.Labels label) {
        long id = node.getId();
        if (!map.containsKey(id)) {
            boolean flag = node.hasLabel(label);
            map.put(id, flag);
            return flag;
        }
        return map.get(id);
    }
}
