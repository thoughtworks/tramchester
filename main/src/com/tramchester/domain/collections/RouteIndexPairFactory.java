package com.tramchester.domain.collections;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.repository.NumberOfRoutes;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LazySingleton
public class RouteIndexPairFactory {
    private final int numberOfRoutes;
    private final Map<Integer, RouteIndexPair> cache;

    @Inject
    public RouteIndexPairFactory(NumberOfRoutes repository) {
        numberOfRoutes = repository.numberOfRoutes();
        if (numberOfRoutes > Short.MAX_VALUE) {
            throw new RuntimeException("To many routes " + numberOfRoutes);
        }
        cache = new HashMap<>();
    }

    public RouteIndexPair get(int a, int b) {
        return get((short) a, (short) b);
    }

    public RouteIndexPair get(short a, short b) {
        if (a >= numberOfRoutes) {
            throw new RuntimeException("First argument " + a + " is out of range " + numberOfRoutes);
        }
        if (b >= numberOfRoutes) {
            throw new RuntimeException("Second argument " + b + " is out of range " + numberOfRoutes);
        }

        int uniqueId = getUniqueId(a, b);
        if (cache.containsKey(uniqueId)) {
            return cache.get(uniqueId);
        }
        RouteIndexPair pair = RouteIndexPair.of(a, b);
        cache.put(uniqueId, pair);
        return pair;
    }

    private int getUniqueId(short a, short b) {
        return (numberOfRoutes * a) + b;
    }

}
