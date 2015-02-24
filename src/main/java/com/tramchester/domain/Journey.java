package com.tramchester.domain;


import java.util.List;

public class Journey {

    private List<Stage> stages;
    private String summary;

    public Journey(List<Stage> stages) {
        this.stages = stages;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummary() {
        return summary;
    }
}
