package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Location;
import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.WalkingStage;
import com.tramchester.mappers.TimeJsonSerializer;

import java.time.LocalTime;
import java.util.List;

import static java.lang.String.format;

public class Journey implements Comparable<Journey> {
    private long containedWalking;
    private List<PresentableStage> allStages;

    public Journey(List<PresentableStage> allStages) {
        this.allStages = allStages;
        containedWalking = allStages.stream().filter(stage -> (stage instanceof WalkingStage)).count();
        if (firstStageIsWalk()) {
            containedWalking -= 1;
        }
    }

    // used front end
    public List<PresentableStage> getStages() {
        return allStages;
    }

    // used front end
    public String getSummary() {
        int size = allStages.size();
        // Direct first
        if (size == 1) {
            return "Direct";
        } else if (size == 2 && firstStageIsWalk()) {
            return "Direct";
        }

        StringBuilder result = new StringBuilder();
        for(int index = 1; index< size; index++) {
            PresentableStage stage = allStages.get(index);
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

    private boolean firstStageIsWalk() {
        return !allStages.get(0).getIsAVehicle();
    }

    public String getHeading() {
        String mode = getFirstStage().getMode().toString();
        if (containedWalking>0) {
            mode = mode + " and Walk";
        }
        return format("%s with %s - %s", mode, getChanges(), getDuration());
    }

    private String getDuration() {
        int mins = TimeAsMinutes.timeDiffMinutes(getExpectedArrivalTime(), getFirstDepartureTime());
        return format("%s minutes", mins);
    }

    private String getChanges() {
        if (allStages.size() <= 1) {
            return "No Changes";
        }
        if (allStages.size() == 2) {
            return firstStageIsWalk() ? "No Changes" : "1 change";
        }
        return format("%s changes", allStages.size() -1);
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getFirstDepartureTime() {
        if (allStages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        return getFirstStage().getFirstDepartureTime();
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    public LocalTime getExpectedArrivalTime() {
        if (allStages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        return getLastStage().getExpectedArrivalTime();
    }

    private PresentableStage getLastStage() {
        int index = allStages.size()-1;
        return allStages.get(index);
    }

    private PresentableStage getFirstStage() {
        if (allStages.size()==1) {
            return allStages.get(0);
        }
        if (firstStageIsWalk()) {
            return allStages.get(1);
        }
        return allStages.get(0);
    }

    @Override
    public String toString() {
        return  "Journey{" +
                "stages= [" +allStages.size() +"] "+ allStages +
                '}';
    }

    // used front end
    public int getNumberOfTimes() {
        int minNumberOfTimes = Integer.MAX_VALUE;
        for (PresentableStage stage : allStages) {
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
        int compare = checkArrival(other);
        // then departure time
        if (compare==0) {
            compare = checkDeparture(other);
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

    private int checkDeparture(Journey other) {
        return TimeAsMinutes.compare(getFirstDepartureTime(),other.getFirstDepartureTime());
    }

    private int checkArrival(Journey other) {
        return TimeAsMinutes.compare(getExpectedArrivalTime(), other.getExpectedArrivalTime());
    }

    public Location getBegin() {
        return getFirstStage().getFirstStation();
    }

    public Location getEnd() {
        return getLastStage().getLastStation();
    }
}
