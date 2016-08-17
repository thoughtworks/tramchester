package com.tramchester.domain;

import java.util.Iterator;
import java.util.List;

public class RawJourney implements Iterable<TransportStage> {
    private List<TransportStage> stages;
    private int queryTime;

    public RawJourney(List<TransportStage> stages, int queryTime) {
        this.stages = stages;
        this.queryTime = queryTime;
    }
    
    public Iterator<TransportStage> iterator() {
        return stages.iterator();
    }

    public List<TransportStage> getStages() {
        return stages;
    }

    public int getQueryTime() {
        return queryTime;
    }

    @Override
    public String toString() {
        return "RawJourney{" +
                "stages=" + stages +
                ", queryTime=" + queryTime +
                '}';
    }
}
