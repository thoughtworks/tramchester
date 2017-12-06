package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.Location;
import com.tramchester.domain.TransportMode;
import com.tramchester.mappers.LocalTimeJsonDeserializer;
import com.tramchester.mappers.LocalTimeJsonSerializer;
import org.joda.time.LocalTime;

public class StageDTO {
    private LocationDTO firstStation;
    private LocationDTO lastStation;
    private LocationDTO actionStation;

    private boolean hasPlatform;
    private PlatformDTO platform;

    private LocalTime firstDepartureTime;
    private LocalTime expectedArrivalTime;
    private int duration;

    private String summary;
    private String prompt;
    private String headSign;

    private TransportMode mode;
    private boolean walk;
    private boolean isAVehicle;
    private String displayClass;

    public StageDTO(LocationDTO firstStation, LocationDTO lastStation, LocationDTO actionStation, boolean hasPlatform,
                    PlatformDTO platform, LocalTime firstDepartureTime, LocalTime expectedArrivalTime, int duration,
                    String summary, String prompt, String headSign, TransportMode mode, boolean walk,
                    boolean isAVehicle, String displayClass) {
        this.firstStation = firstStation;
        this.lastStation = lastStation;
        this.actionStation = actionStation;
        this.hasPlatform = hasPlatform;
        this.platform = platform;
        this.firstDepartureTime = firstDepartureTime;
        this.expectedArrivalTime = expectedArrivalTime;
        this.duration = duration;
        this.summary = summary;
        this.prompt = prompt;
        this.headSign = headSign;
        this.mode = mode;
        this.walk = walk;
        this.isAVehicle = isAVehicle;
        this.displayClass = displayClass;
    }

    public StageDTO() {
        // deserialisation
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

    @JsonSerialize(using = LocalTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalTimeJsonDeserializer.class)
    public LocalTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    @JsonSerialize(using = LocalTimeJsonSerializer.class)
    @JsonDeserialize(using = LocalTimeJsonDeserializer.class)
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

    public PlatformDTO getPlatform() {
        return platform;
    }

    // web site
    public boolean getHasPlatform() {
        return hasPlatform;
    }
}
