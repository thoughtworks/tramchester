package com.tramchester.domain;


import java.util.List;

public class Journey {

    private List<Stage> stages;

    public Journey(List<Stage> stages) {
        this.stages = stages;
    }

    public List<Stage> getStages() {
        return stages;
    }
}
