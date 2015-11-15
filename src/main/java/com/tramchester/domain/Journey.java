package com.tramchester.domain;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
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
    public LocalTime getFirstDepartureTime() {
        if (stages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        List<ServiceTime> serviceTimes = stages.get(0).getServiceTimes();
        return serviceTimes.get(0).getDepartureTime();
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        if (stages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        int index = stages.size() - 1;
        return stages.get(index).getExpectedArrivalTime();
    }

    public int getJourneyIndex() {
        return journeyIndex;
    }

    public void setJourneyIndex(int journeyIndex) {
        this.journeyIndex = journeyIndex;
    }


    @Override
    public String toString() {
        return "Journey{" +
                "stages=" + stages +
                ", summary='" + summary + '\'' +
                ", journeyIndex=" + journeyIndex +
                '}';
    }
}
