package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.Location;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.TransportStage;
import org.joda.time.LocalTime;

public class StageDTO {
    private String summary;
    private String prompt;
    private String headsign;
    private Location actionStation;
    private Location lastStation;
    private Location firstStation;
    private LocalTime firstDepartureTime;
    private LocalTime expectedArrivalTime;
    private int duration;
    private TransportMode mode;
    private boolean walk;
    private boolean isAVehicle;

    public StageDTO(TransportStage source) {
        this.summary = source.getSummary();
        this.prompt = source.getPrompt();
        this.headsign = source.getHeadSign();
        this.actionStation = source.getActionStation();
        this.lastStation = source.getLastStation();
        this.firstStation = source.getFirstStation();
        this.firstDepartureTime = source.getFirstDepartureTime();
        this.expectedArrivalTime = source.getExpectedArrivalTime();
        this.duration = source.getDuration();
        this.mode = source.getMode();
        this.walk = source.isWalk();
        this.isAVehicle = source.getIsAVehicle();
    }

    public String getSummary() {
        return summary;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getHeadSign() {
        return headsign;
    }

    public Location getActionStation() {
        return actionStation;
    }

    public Location getLastStation() {
        return lastStation;
    }

    public Location getFirstStation() {
        return firstStation;
    }

    public LocalTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    public LocalTime getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    public int getDuration() {
        return duration;
    }

    public TransportMode getMode() {
        return mode;
    }

    public boolean getIsAVehicle() {
        return isAVehicle;
    }

    public boolean isWalk() {
        return walk;
    }
}
