package com.tramchester.domain;

import java.util.Iterator;
import java.util.List;

public class RawJourney implements Iterable<RawStage> {
    private List<RawStage> stages;
    private int queryTime;

    public RawJourney(List<RawStage> stages, int queryTime) {
        this.stages = stages;
        this.queryTime = queryTime;
    }
    
    public Iterator<RawStage> iterator() {
        return stages.iterator();
    }

    public List<RawStage> getStages() {
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
