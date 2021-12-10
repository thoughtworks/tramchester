package com.tramchester.graph.caches;

import com.tramchester.graph.search.ImmutableJourneyState;

import java.util.concurrent.atomic.AtomicInteger;

public class LowestCostSeen {
    private final AtomicInteger lowestCost;
    private final AtomicInteger lowestNumChanges;
    private final AtomicInteger arrived;

    public LowestCostSeen() {
        lowestCost = new AtomicInteger(Integer.MAX_VALUE);
        lowestNumChanges = new AtomicInteger(Integer.MAX_VALUE);
        arrived = new AtomicInteger(0);
    }

    public int getLowestCost() {
        return lowestCost.get();
    }

    public void setLowestCost(int cost) {
        lowestCost.getAndSet(cost);
    }

    public int getLowestNumChanges() {
        return lowestNumChanges.get();
    }

    public boolean everArrived() {
        return arrived.get()>0;
    }

    public boolean isLower(ImmutableJourneyState journeyState) {
        // <= equals so we include multiple options and routes in the results
        // An alternative to this would be to search over a finer grained list of times and catch alternatives
        // that way
        return  (journeyState.getTotalCostSoFar() <= getLowestCost()
                && journeyState.getNumberChanges() <= getLowestNumChanges());

    }

    public void setLowestCost(ImmutableJourneyState journeyState) {
        arrived.incrementAndGet();
        lowestNumChanges.getAndSet(journeyState.getNumberChanges());
        setLowestCost(journeyState.getTotalCostSoFar());
    }

    @Override
    public String toString() {
        return "LowestCostSeen{" +
                "cost=" + lowestCost +
                ", changes=" + lowestNumChanges +
                ", arrived=" + arrived +
                '}';
    }
}
