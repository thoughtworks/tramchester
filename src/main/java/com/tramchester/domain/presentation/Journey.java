package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Station;
import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
import java.util.List;
import java.util.SortedSet;

import static java.lang.String.format;

public class Journey implements Comparable<Journey> {
    private List<StageWithTiming> stages;

    public Journey(List<StageWithTiming> stages) {
        this.stages = stages;
    }

    // used front end
    public List<StageWithTiming> getStages() {
        return stages;
    }

    // used front end
    public String getSummary() {
        int size = stages.size();
        if (size == 1) {
            return "Direct";
        }
        StringBuilder result = new StringBuilder();
        for(int index = 1; index< size; index++) {
            StageWithTiming stage = stages.get(index);
            if (index>1) {
                if (index< size -1) {
                    result.append(", ");
                } else {
                    result.append(" and ");
                }
            }
            result.append(stage.getFirstStation().getName());
        }
        return format("Change at %s",result.toString());
    }

    public String getHeading() {
        return format("%s with %s - %s", getFirstStage().getMode(), getChanges(), getDuration());
    }

    private String getDuration() {
        int mins = TimeAsMinutes.timeDiffMinutes(getExpectedArrivalTime(), getFirstDepartureTime());
        return format("%s minutes", mins);
    }

    private String getChanges() {
        if (stages.size()==1) {
            return "No Changes";
        }
        if (stages.size()==2) {
            return "1 change";
        }
        return format("%s changes", stages.size()-1);
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getFirstDepartureTime() {
        if (stages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        return getFirstStage().getFirstDepartureTime();
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        if (stages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        return getLastStage().getExpectedArrivalTime();
    }

    private StageWithTiming getLastStage() {
        int index = stages.size() - 1;
        return stages.get(index);
    }

    private StageWithTiming getFirstStage() {
        return stages.get(0);
    }

    @Override
    public String toString() {
        return  "Journey{" +
                "stages= [" +stages.size() +"] "+ stages +
                '}';
    }

    // used front end
    public int getNumberOfTimes() {
        int minNumberOfTimes = Integer.MAX_VALUE;
        for (StageWithTiming stage : stages) {
            int size = stage.getServiceTimes().size();
            if (size < minNumberOfTimes) {
                minNumberOfTimes = size;
            }
        }
        return minNumberOfTimes;
    }

    @Override
    public int compareTo(Journey other) {
        // arrival first
        int compare = getExpectedArrivalTime().compareTo(other.getExpectedArrivalTime());
        // then departure time
        if (compare==0) {
            compare = getFirstDepartureTime().compareTo(other.getFirstDepartureTime());
        }
        // then number of stages
        if (compare==0) {
            // if arrival times match, put journeys with fewer stages first
            if (this.stages.size()<other.stages.size()) {
                compare = -1;
            } else if (other.stages.size()>stages.size()) {
                compare = 1;
            }
        }
        return compare;
    }

    public Station getBegin() {
        return getFirstStage().getFirstStation();
    }

    public Station getEnd() {
        return getLastStage().getLastStation();
    }
}
