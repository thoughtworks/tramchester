package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.Platform;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.livedata.EnrichPlatform;

import java.util.Optional;

public class StageDTOFactory {

    private EnrichPlatform liveDataEnricher;

    public StageDTOFactory(EnrichPlatform liveDataEnricher) {
        this.liveDataEnricher = liveDataEnricher;
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
                source.getPassedStops(), source.getRouteName(), travelAction);
    }

    private PlatformDTO createPlatform(Optional<Platform> maybe, TramTime queryTime, TramServiceDate tramServiceDate) {
        if (!maybe.isPresent()) {
            return null;
        }

        Platform platform = maybe.get();
        PlatformDTO platformDTO = new PlatformDTO(platform);
        liveDataEnricher.enrich(platformDTO, tramServiceDate, queryTime);
        return platformDTO;
    }
}
