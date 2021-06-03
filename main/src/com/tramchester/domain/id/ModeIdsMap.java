package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;
import com.tramchester.domain.reference.TransportMode;

import java.util.HashMap;
import java.util.Map;

public class ModeIdsMap<T extends GraphProperty>  {
    private final Map<TransportMode, IdSet<T>> theMap;

    public ModeIdsMap() {
        theMap = new HashMap<>();
    }

    public void clear() {
        theMap.clear();
    }

    public void addAll(TransportMode mode, IdSet<T> ids) {
        getOrCreateFor(mode).addAll(ids);
    }

    public void add(TransportMode mode, IdFor<T> id) {
        getOrCreateFor(mode).add(id);
    }

    private IdSet<T> getOrCreateFor(TransportMode mode) {
        if (!theMap.containsKey(mode)) {
            theMap.put(mode, new IdSet<>());
        }
        return theMap.get(mode);
    }

    public boolean containsFor(TransportMode mode, IdFor<T> id) {
        if (!theMap.containsKey(mode)) {
            return false;
        }
        return theMap.get(mode).contains(id);
    }

    public IdSet<T> get(TransportMode mode) {
        if (!theMap.containsKey(mode)) {
            throw new RuntimeException("No values held for " + mode);
        }
        return theMap.get(mode);
    }

    public long size() {
        return theMap.values().stream().map(IdSet::size).reduce(Integer::sum).orElse(0);
    }
}
