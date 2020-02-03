package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.TravelAction;
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

    private String headSign;

    private TransportMode mode;
    private String displayClass;
    private int passedStops;
    private String routeName;
    private String action;

    public StageDTO(LocationDTO firstStation, LocationDTO lastStation, LocationDTO actionStation, boolean hasPlatform,
                    PlatformDTO platform, TramTime firstDepartureTime, TramTime expectedArrivalTime, int duration,
                    String headSign, TransportMode mode,
                    String displayClass, int passedStops, String routeName, TravelAction action) {
        this.firstStation = firstStation;
        this.lastStation = lastStation;
        this.actionStation = actionStation;
        this.hasPlatform = hasPlatform;
        this.platform = platform;
        this.firstDepartureTime = firstDepartureTime;
        this.expectedArrivalTime = expectedArrivalTime;
        this.duration = duration;
        this.headSign = headSign;
        this.mode = mode;
        this.displayClass = displayClass;
        this.passedStops = passedStops;
        this.routeName = routeName;
        this.action = action.toString();
    }

    public StageDTO() {
        // deserialisation
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

    public int getPassedStops() {
        return passedStops;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "StageDTO{" +
                "firstStation=" + firstStation +
                ", lastStation=" + lastStation +
                ", actionStation=" + actionStation +
                ", hasPlatform=" + hasPlatform +
                ", platform=" + platform +
                ", firstDepartureTime=" + firstDepartureTime +
                ", expectedArrivalTime=" + expectedArrivalTime +
                ", duration=" + duration +
                ", headSign='" + headSign + '\'' +
                ", mode=" + mode +
                ", displayClass='" + displayClass + '\'' +
                ", passedStops=" + passedStops +
                ", routeName='" + routeName + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}
