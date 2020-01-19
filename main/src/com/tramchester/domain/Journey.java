package com.tramchester.domain;

import com.tramchester.domain.presentation.TransportStage;

import java.util.Iterator;
import java.util.List;

public class Journey implements Iterable<TransportStage> {

    private final List<TransportStage> stages;
    private final TramTime queryTime;
    private final double totalCost;

    public Journey(List<TransportStage> stages, TramTime queryTime, double totalCost) {
        this.stages = stages;
        this.queryTime = queryTime;
        this.totalCost = totalCost;
    }
    
    public Iterator<TransportStage> iterator() {
        return stages.iterator();
    }

    public List<TransportStage> getStages() {
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
