package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Journey;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.TravelAction;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.WalkingStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@LazySingleton
public class JourneyToDTOMapper {
    private static final Logger logger = LoggerFactory.getLogger(JourneyToDTOMapper.class);
    private final StageDTOFactory stageFactory;
    private final ProvidesNotes providesNotes;

    @Inject
    public JourneyToDTOMapper(StageDTOFactory stageFactory, ProvidesNotes providesNotes) {
        this.stageFactory = stageFactory;
        this.providesNotes = providesNotes;
    }

    public JourneyDTO createJourneyDTO(Journey journey, TramServiceDate queryDate) {
        List<StageDTO> stages = new ArrayList<>();

        List<TransportStage<?,?>> rawJourneyStages = journey.getStages();
        if (rawJourneyStages.isEmpty()) {
            final String msg = "Journey has no stages " + journey;
            logger.error(msg);
            throw new RuntimeException(msg);
        }

        TramTime queryTime = journey.getQueryTime();

        for(TransportStage<?,?> rawStage : rawJourneyStages) {
            logger.info("Adding stage " + rawStage);
            TravelAction action = decideTravelAction(stages, rawStage);
            StageDTO stageDTO = stageFactory.build(rawStage, action, queryDate.getDate());
            stages.add(stageDTO);
        }

        List<Note> notes = providesNotes.createNotesForJourney(journey, queryDate);

        StationRefWithPosition begin = new StationRefWithPosition(journey.getBeginning());

        List<StationRefWithPosition> changeStations = asListOf(journey.getChangeStations());

        List<StationRefWithPosition> path = asListOf(journey.getPath());

        LocalDate date = queryDate.getDate();
        return new JourneyDTO(begin, stages,
                journey.getArrivalTime().toDate(date), journey.getDepartTime().toDate(date),
                changeStations, queryTime, notes,
                path, date);
    }

    private List<StationRefWithPosition> asListOf(List<Location<?>> locations) {
        return locations.stream().map(StationRefWithPosition::new).collect(Collectors.toList());
    }

    private TravelAction decideTravelAction(List<StageDTO> stages, TransportStage<?,?> rawStage) {
        return switch (rawStage.getMode()) {
            case Tram, Bus, RailReplacementBus, Train, Ferry, Subway -> decideActionForStations(stages);
            case Walk -> decideWalkingAction(rawStage);
            case Connect -> TravelAction.ConnectTo;
            default -> throw new RuntimeException("Not defined for " + rawStage.getMode());
        };
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
