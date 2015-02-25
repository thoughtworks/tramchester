package com.tramchester.domain;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.TimeJsonSerializer;
import org.joda.time.DateTime;

import java.util.List;

public class Journey {

    private List<Stage> stages;
    private String summary;
    private int journeyIndex;

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

    @JsonSerialize(using = TimeJsonSerializer.class)
    public DateTime getFirstDepartureTime() {
        if (stages.size() == 0) {
            return new DateTime(0);
        }
        List<ServiceTime> serviceTimes = stages.get(0).getServiceTimes();
        return serviceTimes.get(0).getDepartureTime();
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public DateTime getExpectedArrivalTime() {
        if (stages.size() == 0) {
            return new DateTime(0);
        }
        return stages.get(stages.size() - 1).getExpectedArrivalTime();
    }

    public int getJourneyIndex() {
        return journeyIndex;
    }

    public void setJourneyIndex(int journeyIndex) {
        this.journeyIndex = journeyIndex;
    }
}
