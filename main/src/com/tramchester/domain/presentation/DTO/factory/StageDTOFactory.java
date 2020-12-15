package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;

import java.time.LocalDate;
import java.time.LocalDateTime;

// TODO Use superclass and JSON annotations (see Note class) to handle presence or not of platform
public class StageDTOFactory {

    public StageDTOFactory() {
    }

    public StageDTO build(TransportStage<?,?> source, TravelAction travelAction, LocalDate queryDate) {

        StationRefWithPosition firstStation = new StationRefWithPosition(source.getFirstStation());
        StationRefWithPosition lastStation = new StationRefWithPosition(source.getLastStation());
        StationRefWithPosition actionStation = new StationRefWithPosition(source.getActionStation());
        LocalDateTime firstDepartureTime = source.getFirstDepartureTime().toDate(queryDate);
        LocalDateTime expectedArrivalTime = source.getExpectedArrivalTime().toDate(queryDate);
        RouteRefDTO routeRefDTO = new RouteRefDTO(source.getRoute());

        if (source.hasBoardingPlatform()) {
            PlatformDTO boardingPlatform = new PlatformDTO(source.getBoardingPlatform());

            return new StageDTO(firstStation,
                    lastStation,
                    actionStation,
                    boardingPlatform,
                    firstDepartureTime, expectedArrivalTime,
                    source.getDuration(), source.getHeadSign(),
                    source.getMode(),
                    source.getPassedStops(), routeRefDTO, travelAction, queryDate);
        } else {
            return new StageDTO(firstStation,
                    lastStation,
                    actionStation,
                    firstDepartureTime, expectedArrivalTime,
                    source.getDuration(), source.getHeadSign(),
                    source.getMode(),
                    source.getPassedStops(), routeRefDTO, travelAction, queryDate);
        }

    }

}
