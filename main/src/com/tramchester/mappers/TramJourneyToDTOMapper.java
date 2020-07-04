package com.tramchester.mappers;

import com.tramchester.domain.Journey;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.WalkingStage;
import com.tramchester.domain.presentation.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class TramJourneyToDTOMapper {
    private static final Logger logger = LoggerFactory.getLogger(TramJourneyToDTOMapper.class);
    private final JourneyDTOFactory journeyFactory;
    private final StageDTOFactory stageFactory;
    private final ProvidesNotes providesNotes;

    public TramJourneyToDTOMapper(JourneyDTOFactory journeyFactory, StageDTOFactory stageFactory, ProvidesNotes providesNotes) {
        this.journeyFactory = journeyFactory;
        this.stageFactory = stageFactory;
        this.providesNotes = providesNotes;
    }

    public JourneyDTO createJourneyDTO(Journey journey, TramServiceDate tramServiceDate) {
        List<TransportStage> rawJourneyStages = journey.getStages();
        List<StageDTO> stages = new LinkedList<>();

        TramTime queryTime = journey.getQueryTime();
        for(TransportStage rawStage : rawJourneyStages) {
            logger.info("Adding stage " + rawStage);
            TravelAction action = decideTravelAction(stages, rawStage);
            StageDTO stageDTO = stageFactory.build(rawStage, action, queryTime, tramServiceDate);
            stages.add(stageDTO);
        }

        List<Note> notes = providesNotes.createNotesForJourney(journey, tramServiceDate);
        return journeyFactory.build(stages, queryTime, notes);
    }

    private TravelAction decideTravelAction(List<StageDTO> stages, TransportStage rawStage) {
        switch (rawStage.getMode()) {
            case Tram:
            case Bus:
                return decideActionTram(stages);
            case Walk:
                return decideWalkingAction(rawStage);
            case Connect:
                return TravelAction.ConnectTo;
            default:
                throw new RuntimeException("Not defined for " + rawStage.getMode());
        }
    }

    private TravelAction decideWalkingAction(TransportStage rawStage) {
        WalkingStage walkingStage = (WalkingStage) rawStage;
        return walkingStage.getTowardsMyLocation() ? TravelAction.WalkFrom : TravelAction.WalkTo;
    }

    private TravelAction decideActionTram(List<StageDTO> stagesSoFar) {
        if (stagesSoFar.isEmpty()) {
            return TravelAction.Board;
        }
        StageDTO previousStage = stagesSoFar.get(stagesSoFar.size() - 1);
        TransportMode previousMode = previousStage.getMode();
        if ((previousMode ==TransportMode.Walk) || previousMode ==TransportMode.Connect) {
            return TravelAction.Board;
        }
        return TravelAction.Change;
    }
}
