package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.Journey;
import org.joda.time.LocalTime;

import java.util.List;
import java.util.stream.Collectors;

public class JourneyDTOFactory {

    private StageDTOFactory stageDTOFactory;

    public JourneyDTOFactory(StageDTOFactory stageDTOFactory) {
        this.stageDTOFactory = stageDTOFactory;
    }

    public JourneyDTO build(Journey journey) {
        List<StageDTO> stages = journey.getStages().stream().map(stage -> stageDTOFactory.build(stage)).collect(Collectors.toList());
        String summary = journey.getSummary();
        String heading = journey.getHeading();
        LocalTime firstDepartureTime = journey.getFirstDepartureTime();
        LocalTime expectedArrivalTime = journey.getExpectedArrivalTime();
        LocationDTO end = new LocationDTO(journey.getEnd());
        LocationDTO begin = new LocationDTO(journey.getBegin());

        return new JourneyDTO(begin, end, stages, expectedArrivalTime, firstDepartureTime, summary, heading);
    }
}
