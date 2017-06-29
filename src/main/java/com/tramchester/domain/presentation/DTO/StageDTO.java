package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Location;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.integration.mappers.TimeJsonDeserializer;
import com.tramchester.integration.mappers.TimeJsonSerializer;
import org.joda.time.LocalTime;

public class StageDTO {
    private String summary;
    private String prompt;
    private String headSign;
    private LocationDTO actionStation;
    private LocationDTO lastStation;
    private LocationDTO firstStation;
    private LocalTime firstDepartureTime;
    private LocalTime expectedArrivalTime;
    private int duration;
    private TransportMode mode;
    private boolean walk;
    private boolean isAVehicle;
    private String displayClass;

    public StageDTO() {
        // deserialisation
    }

    public StageDTO(TransportStage source) {
        this.summary = source.getSummary();
        this.prompt = source.getPrompt();
        this.headSign = source.getHeadSign();
        this.actionStation = new LocationDTO(source.getActionStation());
        this.lastStation = new LocationDTO(source.getLastStation());
        this.firstStation = new LocationDTO(source.getFirstStation());
        this.firstDepartureTime = source.getFirstDepartureTime();
        this.expectedArrivalTime = source.getExpectedArrivalTime();
        this.duration = source.getDuration();
        this.mode = source.getMode();
        this.walk = source.isWalk();
        this.isAVehicle = source.getIsAVehicle();
        this.displayClass = source.getDisplayClass();
    }

    public String getSummary() {
        return summary;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getHeadSign() {
        return headSign;
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

    @JsonSerialize(using = TimeJsonSerializer.class)
    @JsonDeserialize(using = TimeJsonDeserializer.class)
    public LocalTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    @JsonSerialize(using = TimeJsonSerializer.class)
    @JsonDeserialize(using = TimeJsonDeserializer.class)
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

    public String getDisplayClass() {
        return displayClass;
    }
}
