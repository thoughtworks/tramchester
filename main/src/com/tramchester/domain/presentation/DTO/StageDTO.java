package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.mappers.serialisation.TramTimeJsonDeserializer;
import com.tramchester.mappers.serialisation.TramTimeJsonSerializer;

public class StageDTO {
    private StationRefWithPosition firstStation;
    private StationRefWithPosition lastStation;
    private StationRefWithPosition actionStation;

    private boolean hasPlatform;
    private PlatformDTO platform;

    private TramTime firstDepartureTime;
    private TramTime expectedArrivalTime;
    private int duration;

    private String headSign;

    private TransportMode mode;
    private int passedStops;
    private String action;

    // TODO Into RouteRefDTO
    private String routeName;
    private String routeShortName;

    public StageDTO(StationRefWithPosition firstStation, StationRefWithPosition lastStation, StationRefWithPosition actionStation,
                    TramTime firstDepartureTime, TramTime expectedArrivalTime, int duration,
                    String headSign, TransportMode mode, int passedStops,
                    String routeName, TravelAction action, String routeShortName) {
        this.firstStation = firstStation;
        this.lastStation = lastStation;
        this.actionStation = actionStation;
        this.hasPlatform = false;
        this.platform = null;
        this.firstDepartureTime = firstDepartureTime;
        this.expectedArrivalTime = expectedArrivalTime;
        this.duration = duration;
        this.headSign = headSign;
        this.mode = mode;
        this.passedStops = passedStops;
        this.routeName = routeName;
        this.action = action.toString();
        this.routeShortName = routeShortName;
    }


    public StageDTO(StationRefWithPosition firstStation, StationRefWithPosition lastStation, StationRefWithPosition actionStation,
                    PlatformDTO boardingPlatform, TramTime firstDepartureTime, TramTime expectedArrivalTime, int duration,
                    String headSign, TransportMode mode, int passedStops,
                    String routeName, TravelAction action, String routeShortName) {
        this.firstStation = firstStation;
        this.lastStation = lastStation;
        this.actionStation = actionStation;
        this.hasPlatform = true;
        this.platform = boardingPlatform;
        this.firstDepartureTime = firstDepartureTime;
        this.expectedArrivalTime = expectedArrivalTime;
        this.duration = duration;
        this.headSign = headSign;
        this.mode = mode;
        this.passedStops = passedStops;
        this.routeName = routeName;
        this.action = action.toString();
        this.routeShortName = routeShortName;
    }

    public StageDTO() {
        // deserialisation
    }

    public String getHeadSign() {
        return headSign;
    }

    public StationRefWithPosition getActionStation() {
        return actionStation;
    }

    public StationRefWithPosition getLastStation() {
        return lastStation;
    }

    public StationRefWithPosition getFirstStation() {
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

//    public String getDisplayClass() {
//        return displayClass;
//    }

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
                ", passedStops=" + passedStops +
                ", routeName='" + routeName + '\'' +
                ", routeShortName='" + routeShortName + '\'' +
                ", action='" + action + '\'' +
                '}';
    }

    public String getRouteShortName() {
        return routeShortName;
    }
}
