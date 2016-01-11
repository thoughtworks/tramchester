package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.RawJourney;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;

public class Journey {

    private List<Stage> stages;
    private String summary;
    private int journeyIndex;
    private int numberOfTimes;

    public Journey(List<Stage> stages) {
        this.stages = stages;
    }

    public Journey(RawJourney rawJourney) {
        stages = new LinkedList<>();
        rawJourney.forEach(rawStage -> stages.add(new Stage(rawStage)));
        this.journeyIndex = rawJourney.getIndex();
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
        return  "Journey{" +
                "stages= [" +stages.size() +"] "+ stages +
                ", summary='" + summary + '\'' +
                ", journeyIndex=" + journeyIndex +
                '}';
    }

    public int getNumberOfTimes() {
        return numberOfTimes;
    }

    public void setNumberOfTimes(int numberOfTimes) {
        this.numberOfTimes = numberOfTimes;
    }
}
