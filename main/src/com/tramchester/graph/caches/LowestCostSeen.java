package com.tramchester.graph.caches;

import java.util.concurrent.atomic.AtomicInteger;

public class LowestCostSeen {
    private final AtomicInteger lowestCost;
    private final AtomicInteger lowestNumChanges;

    public LowestCostSeen() {
        lowestCost = new AtomicInteger(Integer.MAX_VALUE);
        lowestNumChanges = new AtomicInteger(Integer.MAX_VALUE);
    }

    public int getLowestCost() {
        return lowestCost.get();
    }

    public void setLowestCost(int cost) {
        lowestCost.getAndSet(cost);
    }


    public void setLowestNumChanges(int numberChanges) {
        lowestNumChanges.getAndSet(numberChanges);
    }

    public int getLowestNumChanges() {
        return lowestNumChanges.get();
    }
}
