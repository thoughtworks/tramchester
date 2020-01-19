package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TramJourneyToDTOMapper {
    private static final Logger logger = LoggerFactory.getLogger(TramJourneyToDTOMapper.class);
    private final JourneyDTOFactory journeyFactory;
    private final StageDTOFactory stageFactory;

    public TramJourneyToDTOMapper(JourneyDTOFactory journeyFactory, StageDTOFactory stageFactory) {
        this.journeyFactory = journeyFactory;
        this.stageFactory = stageFactory;
    }

    public Optional<JourneyDTO> createJourney(RawJourney rawJourney, TramServiceDate tramServiceDate) {
        List<RawStage> rawJourneyStages = rawJourney.getStages();
        List<StageDTO> stages = new LinkedList<>();

        TramTime queryTime = rawJourney.getQueryTime();
        for(RawStage rawStage : rawJourneyStages)
            if (rawStage.getMode().isVehicle()) {
                TransportStage rawTravelStage = (RawVehicleStage) rawStage;
                StageDTO stageDTO = stageFactory.build(rawTravelStage, decideAction(stages), queryTime, tramServiceDate);
                stages.add(stageDTO);
            } else if (rawStage.getMode().isWalk()) {
                RawWalkingStage stage = (RawWalkingStage) rawStage;
                logger.info("Adding walking stage " + stage);
                StageDTO stageDTO = stageFactory.build(stage, TravelAction.Walk, queryTime, tramServiceDate);
                stages.add(stageDTO);
            }

        if (rawJourneyStages.size()!=stages.size()) {
            logger.error("Failed to create valid journey");
            return Optional.empty();
        }
        JourneyDTO journeyDTO = journeyFactory.build(stages);
        return Optional.of(journeyDTO);
    }

    private TravelAction decideAction(List<StageDTO> stagesSoFar) {
        if (stagesSoFar.isEmpty()) {
            return TravelAction.Board;
        }
        if ((stagesSoFar.get(stagesSoFar.size()-1).getMode()==TransportMode.Walk)) {
            return TravelAction.Board;
        }
        return TravelAction.Change;
    }
}
