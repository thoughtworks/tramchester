package com.tramchester.domain.presentation.DTO.factory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;

// TODO Use superclass and JSON annotations (see Note class) to handle presence or not of platform

@LazySingleton
public class StageDTOFactory {

    @Inject
    public StageDTOFactory() {
    }

    public StageDTO build(TransportStage<?,?> source, TravelAction travelAction, LocalDate queryDate) {

        StationRefWithPosition firstStation = new StationRefWithPosition(source.getFirstStation());
        StationRefWithPosition lastStation = new StationRefWithPosition(source.getLastStation());
        StationRefWithPosition actionStation = new StationRefWithPosition(source.getActionStation());
        LocalDateTime firstDepartureTime = source.getFirstDepartureTime().toDate(queryDate);
        LocalDateTime expectedArrivalTime = source.getExpectedArrivalTime().toDate(queryDate);

        Route route = source.getRoute();
        RouteRefDTO routeRefDTO = new RouteRefDTO(route);

        String tripId = source.getTripId().isValid() ? source.getTripId().forDTO() : "";

        if (source.hasBoardingPlatform()) {
            PlatformDTO boardingPlatform = new PlatformDTO(source.getBoardingPlatform());

            return new StageDTO(firstStation,
                    lastStation,
                    actionStation,
                    boardingPlatform,
                    firstDepartureTime, expectedArrivalTime,
                    source.getDuration(), source.getHeadSign(),
                    source.getMode(),
                    source.getPassedStopsCount(), routeRefDTO, travelAction, queryDate, tripId);
        } else {
            return new StageDTO(firstStation,
                    lastStation,
                    actionStation,
                    firstDepartureTime, expectedArrivalTime,
                    source.getDuration(), source.getHeadSign(),
                    source.getMode(),
                    source.getPassedStopsCount(), routeRefDTO, travelAction, queryDate, tripId);
        }
    }

}
