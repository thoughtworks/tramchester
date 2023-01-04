package com.tramchester.domain.collections;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.repository.NumberOfRoutes;

import javax.inject.Inject;

@LazySingleton
public class RouteIndexPairFactory {
    private final int numberOfRoutes;

    @Inject
    public RouteIndexPairFactory(NumberOfRoutes repository) {
        numberOfRoutes = repository.numberOfRoutes();
    }

    public RouteIndexPair get(int a, int b) {
        if (a > numberOfRoutes) {
            throw new RuntimeException("First argument " + a + " is out of range " + numberOfRoutes);
        }
        if (b > numberOfRoutes) {
            throw new RuntimeException("Second argument " + b + " is out of range " + numberOfRoutes);
        }
        return RouteIndexPair.of(a,b);
    }

}
