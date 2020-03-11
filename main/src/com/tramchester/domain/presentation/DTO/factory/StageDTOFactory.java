package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.Platform;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.DTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.repository.LiveDataRepository;

import java.util.Optional;

public class StageDTOFactory {

    private final LiveDataRepository liveDataRepository;

    public StageDTOFactory(LiveDataRepository liveDataRepository) {
        this.liveDataRepository = liveDataRepository;
    }

    public StageDTO build(TransportStage source, TravelAction travelAction, TramTime queryTime, TramServiceDate tramServiceDate) {
        return new StageDTO(new LocationDTO(source.getFirstStation()),
                new LocationDTO(source.getLastStation()),
                new LocationDTO(source.getActionStation()),
                source.getBoardingPlatform().isPresent(),
                createPlatform(source.getBoardingPlatform(), queryTime, tramServiceDate),
                source.getFirstDepartureTime(), source.getExpectedArrivalTime(),
                source.getDuration(), source.getHeadSign(),
                source.getMode(), source.getDisplayClass(),
                source.getPassedStops(), source.getRouteName(), travelAction, source.getRouteShortName());
    }

    private DTO createPlatform(Optional<Platform> maybe, TramTime queryTime, TramServiceDate tramServiceDate) {
        if (!maybe.isPresent()) {
            return null;
        }

        Platform platform = maybe.get();
        DTO platformDTO = new DTO(platform);
        Optional<StationDepartureInfo> info = liveDataRepository.departuresFor(platform, tramServiceDate, queryTime);
        info.ifPresent(stationDepartureInfo -> platformDTO.setDepartureInfo(new StationDepartureInfoDTO(stationDepartureInfo)));
        return platformDTO;

    }
}
