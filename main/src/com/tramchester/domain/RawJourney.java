package com.tramchester.domain;

import java.time.LocalTime;
import java.util.Iterator;
import java.util.List;

public class RawJourney implements Iterable<RawStage> {
    private final List<RawStage> stages;
    private final LocalTime queryTime;

    public RawJourney(List<RawStage> stages, LocalTime queryTime) {
        this.stages = stages;
        this.queryTime = queryTime;
    }
    
    public Iterator<RawStage> iterator() {
        return stages.iterator();
    }

    public List<RawStage> getStages() {
        return stages;
    }

    public LocalTime getQueryTime() {
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
