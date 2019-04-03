package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphdb.Node;

import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.tramchester.graph.GraphStaticKeys.TIME;

public class CachedNodeOperations implements NodeOperations {
    private final Map<Long, Boolean> serviceNodes;
    private final Map<Long, Boolean> hourNodes;
    private final Map<Long, Boolean> minuteNotes;
    private final Map<Long, Boolean> stationNodes;

    // cached times
    private final Map<Long, LocalTime> times;

    public CachedNodeOperations() {
        hourNodes = new ConcurrentHashMap<>();
        serviceNodes = new ConcurrentHashMap<>();
        times = new ConcurrentHashMap<>();
        minuteNotes = new ConcurrentHashMap<>();
        stationNodes = new ConcurrentHashMap<>();
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
        return (int) node.getProperty(GraphStaticKeys.HOUR);
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
