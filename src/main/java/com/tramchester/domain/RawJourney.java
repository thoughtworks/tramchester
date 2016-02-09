package com.tramchester.domain;

import java.util.Iterator;
import java.util.List;

public class RawJourney implements Iterable<TransportStage> {
    private List<TransportStage> stages;

    public RawJourney(List<TransportStage> stages) {
        this.stages = stages;
    }
    
    public Iterator<TransportStage> iterator() {
        return stages.iterator();
    }

    @Override
    public String toString() {
        return "RawJourney{" +
                "stages=" + stages +
                '}';
    }

    public List<TransportStage> getStages() {
        return stages;
    }

}
