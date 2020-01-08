package com.tramchester.domain;

import java.util.Iterator;
import java.util.List;

public class RawJourney implements Iterable<RawStage> {

    // TODO Ordering and Equality?

    private final List<RawStage> stages;
    private final TramTime queryTime;
    private final double totalCost;

    public RawJourney(List<RawStage> stages, TramTime queryTime, double totalCost) {
        this.stages = stages;
        this.queryTime = queryTime;
        this.totalCost = totalCost;
    }
    
    public Iterator<RawStage> iterator() {
        return stages.iterator();
    }

    public List<RawStage> getStages() {
        return stages;
    }

    public TramTime getQueryTime() {
        return queryTime;
    }

    @Override
    public String toString() {
        return "RawJourney{" +
                "stages=" + stages +
                ", queryTime=" + queryTime +
                '}';
    }

    public double getTotalCost() {
        return totalCost;
    }
}
