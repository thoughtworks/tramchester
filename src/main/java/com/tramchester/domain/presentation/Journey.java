package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.RawJourney;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

public class Journey {

    private List<Stage> stages;
    private String summary;
    private int journeyIndex;
    private int numberOfTimes;

    public Journey(List<Stage> stages, int journeyIndex) {
        this.journeyIndex = journeyIndex;
        this.stages = stages;
    }

    // used front end
    public List<Stage> getStages() {
        return stages;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    // used front end
    public String getSummary() {
        return summary;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getFirstDepartureTime() {
        if (stages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        SortedSet<ServiceTime> serviceTimes = stages.get(0).getServiceTimes();
        return serviceTimes.first().getDepartureTime();
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        if (stages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        int index = stages.size() - 1;
        return stages.get(index).getExpectedArrivalTime();
    }

    // used front end
    public int getJourneyIndex() {
        return journeyIndex;
    }

    @Override
    public String toString() {
        return  "Journey{" +
                "stages= [" +stages.size() +"] "+ stages +
                ", summary='" + summary + '\'' +
                ", journeyIndex=" + journeyIndex +
                '}';
    }

    // used front end
    public int getNumberOfTimes() {
        return numberOfTimes;
    }

    public void setNumberOfTimes(int numberOfTimes) {
        this.numberOfTimes = numberOfTimes;
    }
}
