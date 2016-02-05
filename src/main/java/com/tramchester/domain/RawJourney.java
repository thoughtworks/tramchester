package com.tramchester.domain;

import java.util.Iterator;
import java.util.List;

public class RawJourney implements Iterable<RawStage> {
    private List<RawStage> stages;

    public RawJourney(List<RawStage> stages) {
        this.stages = stages;
    }
    
    public Iterator<RawStage> iterator() {
        return stages.iterator();
    }

    @Override
    public String toString() {
        return "RawJourney{" +
                "stages=" + stages +
                '}';
    }

    public List<RawStage> getStages() {
        return stages;
    }

    //public String getFirstServiceId() {
//        return stages.get(0).getServiceId();
//    }
}
