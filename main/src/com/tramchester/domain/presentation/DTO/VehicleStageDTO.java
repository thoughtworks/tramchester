package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.reference.TransportMode;

import java.time.Duration;
import java.time.LocalDateTime;

public class VehicleStageDTO extends SimpleStageDTO {

    private boolean hasPlatform;
    private PlatformDTO platform;
    private IdForDTO tripId;

    public VehicleStageDTO(LocationRefWithPosition firstStation, LocationRefWithPosition lastStation, LocationRefWithPosition actionStation,
                           LocalDateTime firstDepartureTime, LocalDateTime expectedArrivalTime, Duration duration,
                           String headSign, TransportMode mode, int passedStops,
                           RouteRefDTO route, TravelAction action, TramDate queryDate, IdForDTO tripId) {
        super(firstStation, lastStation, actionStation, firstDepartureTime, expectedArrivalTime, duration, headSign,
                mode, passedStops, route, action, queryDate);

        this.hasPlatform = false;
        this.platform = null;
        this.tripId = tripId;
    }

    public VehicleStageDTO(LocationRefWithPosition firstStation, LocationRefWithPosition lastStation, LocationRefWithPosition actionStation,
                           PlatformDTO boardingPlatform, LocalDateTime firstDepartureTime, LocalDateTime expectedArrivalTime,
                           Duration duration,
                           String headSign, TransportMode mode, int passedStops,
                           RouteRefDTO route, TravelAction action, TramDate queryDate, IdForDTO tripId) {
        this(firstStation, lastStation, actionStation, firstDepartureTime, expectedArrivalTime, duration, headSign, mode,
            passedStops, route, action, queryDate, tripId);

        this.hasPlatform = true;
        this.platform = boardingPlatform;
    }

    public VehicleStageDTO() {
        // deserialisation
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    public PlatformDTO getPlatform() {
        return platform;
    }

    // web site
    @Override
    public boolean getHasPlatform() {
        return hasPlatform;
    }

    // TODO Handle differently, i.e. based on transport mode?
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public IdForDTO getTripId() {
        return tripId;
    }

    @Override
    public String toString() {
        return "StageDTO{" +
                "hasPlatform=" + hasPlatform +
                ", platform=" + platform +
                ", tripId=" + tripId +
                "} " + super.toString();
    }
}
