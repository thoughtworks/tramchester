package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Location;
import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class Journey implements Comparable<Journey> {
    private List<PresentableStage> allStages;
    private List<PresentableStage> vehicleStages;

    public Journey(List<PresentableStage> allStages) {
        this.allStages = allStages;
        vehicleStages = allStages.stream().filter(stage -> stage.getIsAVehicle()).collect(Collectors.toList());
    }

    // used front end
    public List<PresentableStage> getStages() {
        return allStages;
    }

    // used front end
    public String getSummary() {
        long size = vehicleStages.size();
        if (size == 1) {
            return "Direct";
        }
        StringBuilder result = new StringBuilder();
        for(int index = 1; index< size; index++) {
            PresentableStage stage = vehicleStages.get(index);
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
        if (vehicleStages.size() ==1) {
            return "No Changes";
        }
        if (vehicleStages.size() ==2) {
            return "1 change";
        }
        return format("%s changes", vehicleStages.size() -1);
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getFirstDepartureTime() {
        if (vehicleStages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        return getFirstStage().getFirstDepartureTime();
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        if (vehicleStages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        return getLastStage().getExpectedArrivalTime();
    }

    private PresentableStage getLastStage() {
        int index = vehicleStages.size() - 1;
        return vehicleStages.get(index);
    }

    private PresentableStage getFirstStage() {
        return vehicleStages.get(0);
//        PresentableStage presentableStage = vehicleStages.get(0);
//        if (presentableStage.getIsAVehicle()) {
//            return presentableStage;
//        } else {
//            return vehicleStages.get(1);
//        }
    }

    @Override
    public String toString() {
        return  "Journey{" +
                "stages= [" +vehicleStages.size() +"] "+ vehicleStages +
                '}';
    }

    // used front end
    public int getNumberOfTimes() {
        int minNumberOfTimes = Integer.MAX_VALUE;
        for (PresentableStage stage : vehicleStages) {
            int size = stage.getNumberOfServiceTimes();
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
            if (this.allStages.size()<other.allStages.size()) {
                compare = -1;
            } else if (other.allStages.size()>allStages.size()) {
                compare = 1;
            }
        }
        return compare;
    }

    public Location getBegin() {
        return getFirstStage().getFirstStation();
    }

    public Location getEnd() {
        return getLastStage().getLastStation();
    }
}
