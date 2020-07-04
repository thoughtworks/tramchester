package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.Platform;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.repository.LiveDataRepository;

import java.util.Optional;

// TODO Use superclass and JSON annotations (see Note class) to handle presence or not of platform
public class StageDTOFactory {

    public StageDTOFactory() {
    }

    public StageDTO build(TransportStage source, TravelAction travelAction) {
        return new StageDTO(new LocationDTO(source.getFirstStation()),
                new LocationDTO(source.getLastStation()),
                new LocationDTO(source.getActionStation()),
                source.getBoardingPlatform().isPresent(),
                createPlatform(source.getBoardingPlatform()),
                source.getFirstDepartureTime(), source.getExpectedArrivalTime(),
                source.getDuration(), source.getHeadSign(),
                source.getMode(), source.getDisplayClass(),
                source.getPassedStops(), source.getRouteName(), travelAction, source.getRouteShortName());
    }

    private PlatformDTO createPlatform(Optional<Platform> maybe) {
        return maybe.map(PlatformDTO::new).orElse(null);

    }
}
