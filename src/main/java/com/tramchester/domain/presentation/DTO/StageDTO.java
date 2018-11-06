package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.TransportMode;
import com.tramchester.mappers.serialisation.TramTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.TramTimeJsonSerializer;

public class StageDTO {
    private LocationDTO firstStation;
    private LocationDTO lastStation;
    private LocationDTO actionStation;

    private boolean hasPlatform;
    private PlatformDTO platform;

    private TramTime firstDepartureTime;
    private TramTime expectedArrivalTime;
    private int duration;

    private String summary;
    private String prompt;
    private String headSign;

    private TransportMode mode;
    private boolean walk;
    private boolean isAVehicle;
    private String displayClass;

    public StageDTO(LocationDTO firstStation, LocationDTO lastStation, LocationDTO actionStation, boolean hasPlatform,
                    PlatformDTO platform, TramTime firstDepartureTime, TramTime expectedArrivalTime, int duration,
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

    public LocationDTO getActionStation() {
        return actionStation;
    }

    public LocationDTO getLastStation() {
        return lastStation;
    }

    public LocationDTO getFirstStation() {
        return firstStation;
    }

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    public TramTime getFirstDepartureTime() {
        return firstDepartureTime;
    }

    @JsonSerialize(using = TramTimeJsonSerializer.class)
    @JsonDeserialize(using = TramTimeJsonDeserializer.class)
    public TramTime getExpectedArrivalTime() {
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
