package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Journey;
import com.tramchester.domain.WalkingStage;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@LazySingleton
public class TramJourneyToDTOMapper {
    private static final Logger logger = LoggerFactory.getLogger(TramJourneyToDTOMapper.class);
    private final JourneyDTOFactory journeyFactory;
    private final StageDTOFactory stageFactory;
    private final ProvidesNotes providesNotes;

    @Inject
    public TramJourneyToDTOMapper(JourneyDTOFactory journeyFactory, StageDTOFactory stageFactory, ProvidesNotes providesNotes) {
        this.journeyFactory = journeyFactory;
        this.stageFactory = stageFactory;
        this.providesNotes = providesNotes;
    }

    public JourneyDTO createJourneyDTO(Journey journey, TramServiceDate queryDate) {
        List<StageDTO> stages = new ArrayList<>();

        List<TransportStage<?,?>> rawJourneyStages = journey.getStages();
        TramTime queryTime = journey.getQueryTime();

        for(TransportStage<?,?> rawStage : rawJourneyStages) {
            logger.info("Adding stage " + rawStage);
            TravelAction action = decideTravelAction(stages, rawStage);
            StageDTO stageDTO = stageFactory.build(rawStage, action, queryDate.getDate());
            stages.add(stageDTO);
        }

        List<StationRefWithPosition> mappedPath = journey.getPath().stream().map(StationRefWithPosition::new).collect(Collectors.toList());

        List<Note> notes = providesNotes.createNotesForJourney(journey, queryDate);
        return journeyFactory.build(stages, queryTime, notes, mappedPath, queryDate.getDate());
    }

    private TravelAction decideTravelAction(List<StageDTO> stages, TransportStage<?,?> rawStage) {
        switch (rawStage.getMode()) {
            case Tram:
            case Bus:
            case Train:
                return decideActionForStations(stages);
            case Walk:
                return decideWalkingAction(rawStage);
            case Connect:
                return TravelAction.ConnectTo;
            default:
                throw new RuntimeException("Not defined for " + rawStage.getMode());
        }
    }

    private TravelAction decideWalkingAction(TransportStage<?,?> rawStage) {
        WalkingStage<?,?> walkingStage = (WalkingStage<?,?>) rawStage;
        return walkingStage.getTowardsMyLocation() ? TravelAction.WalkFrom : TravelAction.WalkTo;
    }

    private TravelAction decideActionForStations(List<StageDTO> stagesSoFar) {
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
