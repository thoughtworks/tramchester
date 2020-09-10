package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;

// TODO Use superclass and JSON annotations (see Note class) to handle presence or not of platform
public class StageDTOFactory {

    public StageDTOFactory() {
    }

    public StageDTO build(TransportStage<?,?> source, TravelAction travelAction) {

        StationRefWithPosition firstStation = new StationRefWithPosition(source.getFirstStation());
        StationRefWithPosition lastStation = new StationRefWithPosition(source.getLastStation());
        StationRefWithPosition actionStation = new StationRefWithPosition(source.getActionStation());

        if (source.hasBoardingPlatform()) {
            PlatformDTO boardingPlatform = new PlatformDTO(source.getBoardingPlatform());
            return new StageDTO(firstStation,
                    lastStation,
                    actionStation,
                    boardingPlatform,
                    source.getFirstDepartureTime(), source.getExpectedArrivalTime(),
                    source.getDuration(), source.getHeadSign(),
                    source.getMode(),
                    source.getPassedStops(), source.getRouteName(), travelAction, source.getRouteShortName());
        } else {
            return new StageDTO(firstStation,
                    lastStation,
                    actionStation,
                    source.getFirstDepartureTime(), source.getExpectedArrivalTime(),
                    source.getDuration(), source.getHeadSign(),
                    source.getMode(),
                    source.getPassedStops(), source.getRouteName(), travelAction, source.getRouteShortName());

        }

    }

}
