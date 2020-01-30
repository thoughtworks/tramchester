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

    public Optional<JourneyDTO> createJourneyDTO(Journey journey, TramServiceDate tramServiceDate) {
        List<TransportStage> rawJourneyStages = journey.getStages();
        List<StageDTO> stages = new LinkedList<>();

        TramTime queryTime = journey.getQueryTime();
        for(TransportStage rawStage : rawJourneyStages)
            if (rawStage.getMode().isVehicle()) {
                StageDTO stageDTO = stageFactory.build(rawStage, decideAction(stages), queryTime, tramServiceDate);
                stages.add(stageDTO);
            } else if (rawStage.getMode().isWalk()) {
                logger.info("Adding walking stage " + rawStage);
                WalkingStage walkingStage = (WalkingStage) rawStage;
                TravelAction action = walkingStage.getTowardsMyLocation() ? TravelAction.WalkFrom : TravelAction.WalkTo ;
                StageDTO stageDTO = stageFactory.build(rawStage, action, queryTime, tramServiceDate);
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
