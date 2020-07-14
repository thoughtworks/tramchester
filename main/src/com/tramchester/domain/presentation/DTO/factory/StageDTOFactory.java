package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;

// TODO Use superclass and JSON annotations (see Note class) to handle presence or not of platform
public class StageDTOFactory {

    public StageDTOFactory() {
    }

    public StageDTO build(TransportStage source, TravelAction travelAction) {

        return new StageDTO(new StationRefWithPosition(source.getFirstStation()),
                new StationRefWithPosition(source.getLastStation()),
                new StationRefWithPosition(source.getActionStation()),
                source.getBoardingPlatform().isPresent(),
                source.getBoardingPlatform().map(PlatformDTO::new).orElse(null),
                source.getFirstDepartureTime(), source.getExpectedArrivalTime(),
                source.getDuration(), source.getHeadSign(),
                source.getMode(), source.getDisplayClass(),
                source.getPassedStops(), source.getRouteName(), travelAction, source.getRouteShortName());
    }

}
