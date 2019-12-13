package com.tramchester.domain;

import java.util.Iterator;
import java.util.List;

public class RawJourney implements Iterable<RawStage> {

    // TODO Ordering and Equality?

    private final List<RawStage> stages;
    private final TramTime queryTime;

    public RawJourney(List<RawStage> stages, TramTime queryTime) {
        this.stages = stages;
        this.queryTime = queryTime;
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
}
