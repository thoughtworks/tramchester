package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Location;
import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.domain.WalkingStage;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.mappers.TimeJsonSerializer;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.lang.String.format;

public class Journey {
    private static final Logger logger = LoggerFactory.getLogger(Journey.class);

    private long embeddedWalk;
    private List<TransportStage> allStages;

    public Journey(List<TransportStage> allStages) {
        this.allStages = allStages;
        embeddedWalk = allStages.stream().filter(stage -> (stage instanceof WalkingStage)).count();
        if (firstStageIsWalk()) {
            embeddedWalk -= 1;
        }
    }

    // used front end
    public List<TransportStage> getStages() {
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
            TransportStage stage = allStages.get(index);
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
        if (allStages.isEmpty()) {
            logger.error("No stages in the journey");
            return false;
        }
        return !allStages.get(0).getIsAVehicle();
    }

    public String getHeading() {
        String mode;
        if (firstStageIsWalk()) {
            if (allStages.size()>1) {
                mode = allStages.get(1).getMode().toString();
                mode = "Walk and " + mode;
            }
            else {
                mode = "Walk";
            }
        } else {
            mode = getFirstStage().getMode().toString();
        }
        if (embeddedWalk >0) {
            mode = mode + " and Walk";
        }
        return format("%s with %s - %s", mode, getChanges(), getDuration());
    }

    private String getDuration() {
        int mins = TimeAsMinutes.timeDiffMinutes(getExpectedArrivalTime(), getFirstStage().getFirstDepartureTime());
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
        if (firstStageIsWalk()) {
            if (allStages.size()>1) {
                return allStages.get(1).getFirstDepartureTime();
            }
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

    private TransportStage getLastStage() {
        int index = allStages.size()-1;
        return allStages.get(index);
    }

    private TransportStage getFirstStage() {
        if (allStages.size()==1) {
            return allStages.get(0);
        }
//        if (firstStageIsWalk()) {
//            return allStages.get(1);
//        }
        return allStages.get(0);
    }

    @Override
    public String toString() {
        return  "Journey{" +
                "stages= [" +allStages.size() +"] "+ allStages +
                '}';
    }

    public Location getBegin() {
        if (firstStageIsWalk()) {
            if (allStages.size()>1) {
                // the first station
                return allStages.get(1).getFirstStation();
            } else {
                return allStages.get(0).getFirstStation();
            }
        }
        return getFirstStage().getFirstStation();
    }

    public Location getEnd() {
        return getLastStage().getLastStation();
    }

    public JourneyDTO asDTO() {
        return new JourneyDTO(this);
    }
}
