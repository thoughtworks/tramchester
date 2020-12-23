package com.tramchester.domain.presentation.DTO.factory;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Agency;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.VehicleStage;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.RouteRefDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;

// TODO Use superclass and JSON annotations (see Note class) to handle presence or not of platform

@LazySingleton
public class StageDTOFactory {
    private static final Logger logger = LoggerFactory.getLogger(StageDTOFactory.class);
    private static final String FROM = "from ";
    private static final String TO = " to ";

    private final StationRepository stationRepository;

    @Inject
    public StageDTOFactory(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public StageDTO build(TransportStage<?,?> source, TravelAction travelAction, LocalDate queryDate) {

        StationRefWithPosition firstStation = new StationRefWithPosition(source.getFirstStation());
        StationRefWithPosition lastStation = new StationRefWithPosition(source.getLastStation());
        StationRefWithPosition actionStation = new StationRefWithPosition(source.getActionStation());
        LocalDateTime firstDepartureTime = source.getFirstDepartureTime().toDate(queryDate);
        LocalDateTime expectedArrivalTime = source.getExpectedArrivalTime().toDate(queryDate);

        RouteRefDTO routeRefDTO;
        Route route = source.getRoute();
        if (TransportMode.isTrain(route)) {
            String name = expandRouteNameFor(route);
            routeRefDTO = new RouteRefDTO(route, name);
        } else {
            routeRefDTO = new RouteRefDTO(route);
        }

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
                    source.getPassedStops(), routeRefDTO, travelAction, queryDate, tripId);
        } else {
            return new StageDTO(firstStation,
                    lastStation,
                    actionStation,
                    firstDepartureTime, expectedArrivalTime,
                    source.getDuration(), source.getHeadSign(),
                    source.getMode(),
                    source.getPassedStops(), routeRefDTO, travelAction, queryDate, tripId);
        }
    }

    private String expandRouteNameFor(Route route) {
        ///
        // many train routes names have format "<AGENCY_ID> train service from <STATIONID> to <STATIONID>"
        // so replace those with names if possible

        String orginal = route.getName();
        String target = orginal;

        // agency name
        Agency agency = route.getAgency();
        String agencyId = agency.getId().forDTO();
        String prefix = agencyId + " train service";
        if (target.startsWith(prefix)) {
            target = target.replace(agencyId, agency.getName());
        }
        // station names
        int indexOfFrom = target.indexOf(FROM);
        int indexOfTo = target.indexOf(TO);
        if (indexOfFrom>0 && indexOfTo>0) {
            String from = target.substring(indexOfFrom + FROM.length(), indexOfTo);
            String to = target.substring(indexOfTo + TO.length());
            IdFor<Station> fromId = IdFor.createId(from);
            IdFor<Station> toId = IdFor.createId(to);

            if (stationRepository.hasStationId(toId) && stationRepository.hasStationId(fromId)) {
                String toName = stationRepository.getStationName(toId);
                String fromName = stationRepository.getStationName(fromId);
                target = target.substring(0, indexOfFrom) + "from " + fromName + " to " + toName;
            }
            logger.info("Mapped route name form '" + orginal + "' to '" + target + "'");
        } else {
            logger.warn("Train route name format unrecognised " + orginal);
        }
        return target;

    }

}
