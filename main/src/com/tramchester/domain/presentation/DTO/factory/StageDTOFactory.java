package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.Platform;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.livedata.EnrichPlatform;

import java.util.Optional;

public class StageDTOFactory {

    private EnrichPlatform liveDataEnricher;

    public StageDTOFactory(EnrichPlatform liveDataEnricher) {
        this.liveDataEnricher = liveDataEnricher;
    }

    public StageDTO build(TransportStage source) {
        return new StageDTO(new LocationDTO(source.getFirstStation()),
                new LocationDTO(source.getLastStation()),
                new LocationDTO(source.getActionStation()),
                source.getBoardingPlatform().isPresent(),
                createPlatform(source.getBoardingPlatform()),
                source.getFirstDepartureTime(), source.getExpectedArrivalTime(),
                source.getDuration(), source.getSummary(),source.getPrompt(), source.getHeadSign(),
                source.getMode(), source.isWalk(), source.getIsAVehicle(), source.getDisplayClass());
    }

    private PlatformDTO createPlatform(Optional<Platform> maybe) {
        if (!maybe.isPresent()) {
            return null;
        }

        Platform platform = maybe.get();
        PlatformDTO platformDTO = new PlatformDTO(platform);
        liveDataEnricher.enrich(platformDTO);
        return platformDTO;
    }
}
