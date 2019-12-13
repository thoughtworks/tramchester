package com.tramchester.graph;

import java.util.*;

public class NodeIdLabelMap {
    private EnumMap<TransportGraphBuilder.Labels, Set<Long>> map;

    public NodeIdLabelMap() {
        map = new EnumMap<>(TransportGraphBuilder.Labels.class);
        for (TransportGraphBuilder.Labels label: TransportGraphBuilder.Labels.values()) {
            map.put(label, new HashSet<>());
        }
    }

    public void put(long id, TransportGraphBuilder.Labels label) {
        map.get(label).add(id);
    }

    public boolean has(long nodeId, TransportGraphBuilder.Labels label) {
        return map.get(label).contains(nodeId);
    }

    public void putQueryNode(long id) {
        map.get(TransportGraphBuilder.Labels.QUERY_NODE).add(id);
    }

    public void removeQueryNode(long id) {
        map.get(TransportGraphBuilder.Labels.QUERY_NODE).remove(id);
    }
}
