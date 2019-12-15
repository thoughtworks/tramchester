package com.tramchester.graph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NodeIdLabelMap {
    private Map<TransportGraphBuilder.Labels, Set<Long>> map;
    private ConcurrentMap<Long, Boolean> queryNodes;

    public NodeIdLabelMap() {
        map = new EnumMap<>(TransportGraphBuilder.Labels.class);
        for (TransportGraphBuilder.Labels label: TransportGraphBuilder.Labels.values()) {
            if (label != TransportGraphBuilder.Labels.QUERY_NODE) {
                map.put(label, new HashSet<>());
            }
        }
        queryNodes = new ConcurrentHashMap<>();
    }

    public void put(long id, TransportGraphBuilder.Labels label) {
        map.get(label).add(id);
    }

    public boolean has(long nodeId, TransportGraphBuilder.Labels label) {
        if (label== TransportGraphBuilder.Labels.QUERY_NODE) {
            return queryNodes.containsKey(nodeId);
        }
        return map.get(label).contains(nodeId);
    }

    public void putQueryNode(long id) {
        queryNodes.put(id,true);
    }
    public void removeQueryNode(long id) {
        queryNodes.remove(id);
    }

    public void freeze() {
        map = Collections.unmodifiableMap(map);
    }
}
