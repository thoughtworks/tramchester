package com.tramchester.domain.presentation.DTO.factory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.LocationRefWithPosition;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDateTime;

// TODO Use superclass and JSON annotations (see Note class) to handle presence or not of platform

@LazySingleton
public class StageDTOFactory {

    private final DTOFactory stationDTOFactory;

    @Inject
    public StageDTOFactory(DTOFactory stationDTOFactory) {

        this.stationDTOFactory = stationDTOFactory;
    }

    public StageDTO build(TransportStage<?,?> source, TravelAction travelAction, TramDate queryDate) {

        LocationRefWithPosition firstStation = stationDTOFactory.createLocationRefWithPosition(source.getFirstStation());
        LocationRefWithPosition lastStation = stationDTOFactory.createLocationRefWithPosition(source.getLastStation());
        LocationRefWithPosition actionStation = stationDTOFactory.createLocationRefWithPosition(source.getActionStation());
        LocalDateTime firstDepartureTime = source.getFirstDepartureTime().toDate(queryDate);
        LocalDateTime expectedArrivalTime = source.getExpectedArrivalTime().toDate(queryDate);

        Route route = source.getRoute();
        RouteRefDTO routeRefDTO = new RouteRefDTO(route);

        String tripId = source.getTripId().isValid() ? source.getTripId().forDTO() : "";

        final Duration duration = source.getDuration();
        if (source.hasBoardingPlatform()) {
            PlatformDTO boardingPlatform = new PlatformDTO(source.getBoardingPlatform());

            return new StageDTO(firstStation,
                    lastStation,
                    actionStation,
                    boardingPlatform,
                    firstDepartureTime, expectedArrivalTime,
                    duration, source.getHeadSign(),
                    source.getMode(),
                    source.getPassedStopsCount(), routeRefDTO, travelAction, queryDate, tripId);
        } else {
            return new StageDTO(firstStation,
                    lastStation,
                    actionStation,
                    firstDepartureTime, expectedArrivalTime,
                    duration, source.getHeadSign(),
                    source.getMode(),
                    source.getPassedStopsCount(), routeRefDTO, travelAction, queryDate, tripId);
        }
    }

}
