package com.tramchester.domain;

import com.tramchester.domain.presentation.Stage;

import java.util.Iterator;
import java.util.List;

public class RawJourney implements Iterable<Stage> {
    private List<Stage> stages;
    private int index;

    public RawJourney(List<Stage> stages, int index) {
        this.stages = stages;
        this.index = index;
    }
    
    public Iterator<Stage> iterator() {
        return stages.iterator();
    }

    public int getIndex() {
        return index;
    }

    @Deprecated
    public List<Stage> getStages() {
        return stages;
    }

    public String getFirstServiceId() {
        return stages.get(0).getServiceId();
    }
}
