package com.tramchester.domain;

import java.util.Iterator;
import java.util.List;

public class RawJourney implements Iterable<RawStage> {
    private List<RawStage> stages;
    private int index;

    public RawJourney(List<RawStage> stages, int index) {
        this.stages = stages;
        this.index = index;
    }
    
    public Iterator<RawStage> iterator() {
        return stages.iterator();
    }

    public int getIndex() {
        return index;
    }

    @Deprecated
    public List<RawStage> getStages() {
        return stages;
    }

    public String getFirstServiceId() {
        return stages.get(0).getServiceId();
    }
}
