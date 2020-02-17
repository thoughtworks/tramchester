package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
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

    public JourneyDTO createJourneyDTO(Journey journey, TramServiceDate tramServiceDate) {
        List<TransportStage> rawJourneyStages = journey.getStages();
        List<StageDTO> stages = new LinkedList<>();

        TramTime queryTime = journey.getQueryTime();
        for(TransportStage rawStage : rawJourneyStages) {
            logger.info("Adding stage " + rawStage);
            TravelAction action = rawStage.getMode().isVehicle() ? decideActionTram(stages) : decideWalkingAction(rawStage);
            StageDTO stageDTO = stageFactory.build(rawStage, action, queryTime, tramServiceDate);
            stages.add(stageDTO);
        }

        return journeyFactory.build(stages, queryTime);
    }

    private TravelAction decideWalkingAction(TransportStage rawStage) {
        WalkingStage walkingStage = (WalkingStage) rawStage;
        return walkingStage.getTowardsMyLocation() ? TravelAction.WalkFrom : TravelAction.WalkTo;
    }

    private TravelAction decideActionTram(List<StageDTO> stagesSoFar) {
        if (stagesSoFar.isEmpty()) {
            return TravelAction.Board;
        }
        if ((stagesSoFar.get(stagesSoFar.size()-1).getMode()==TransportMode.Walk)) {
            return TravelAction.Board;
        }
        return TravelAction.Change;
    }
}
