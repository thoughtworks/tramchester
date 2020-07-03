package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.mappers.HeadsignMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class JourneyDTOFactory {
    private static final Logger logger = LoggerFactory.getLogger(JourneyDTOFactory.class);

    private static int INCLUDE_LIVE_TIME_LIMIT = 15;

    private final HeadsignMapper headsignMapper;

    public JourneyDTOFactory(HeadsignMapper headsignMapper) {
        this.headsignMapper = headsignMapper;
    }

    public JourneyDTO build(List<StageDTO> stages, TramTime queryTime, List<Note> notes) {
        boolean isDirect = isDirect(stages);
        LocationDTO begin = getBegin(stages);
        LocationDTO end = getEnd(stages);

        JourneyDTO journeyDTO = new JourneyDTO(begin, end, stages, getExpectedArrivalTime(stages),
                getFirstDepartureTime(stages),
                isDirect, getChangeStationNames(stages), queryTime, notes);

        addTopLevelDueTramToJourney(journeyDTO);

        return journeyDTO;
    }

    private void addTopLevelDueTramToJourney(JourneyDTO journeyDTO) {
        Optional<StageDTO> maybeFirstTram = journeyDTO.getStages().stream().
                filter(stage -> TransportMode.Tram.equals(stage.getMode())).
                findFirst();
        if (!maybeFirstTram.isPresent()) {
            return;
        }

        StageDTO firstTramStage = maybeFirstTram.get();
        if (!firstTramStage.getHasPlatform()) {
            logger.warn("No platform present for stage" + firstTramStage );
            return;
        }

        StationDepartureInfoDTO departInfo = firstTramStage.getPlatform().getStationDepartureInfo();
        if (departInfo==null) {
            return;
        }

        TramTime firstDepartTime = journeyDTO.getFirstDepartureTime();
        String headsign = headsignMapper.mapToDestination(firstTramStage.getHeadSign()).toLowerCase();

        Comparator<? super DepartureDTO> nearestMatchByDueTime = createDueTramComparator(firstDepartTime);

        Optional<DepartureDTO> maybeDueTram = departInfo.getDueTrams().stream().
                filter(dueTram -> filterDueTram(headsign, dueTram, firstDepartTime)).min(nearestMatchByDueTime);

        if (maybeDueTram.isPresent()) {
            DepartureDTO dueTram = maybeDueTram.get();
            TramTime when = dueTram.getWhen();
            journeyDTO.setDueTram(format("%s tram %s at %s", dueTram.getCarriages(), dueTram.getStatus(),
                    when.toPattern()));
        }
    }

    @Deprecated
    private Comparator<DepartureDTO> createDueTramComparator(TramTime departTime) {
        return (departA, departB) -> {
            int gapToDepartA = TramTime.diffenceAsMinutes(departA.getWhen(), departTime);
            int gapToDepartB = TramTime.diffenceAsMinutes(departB.getWhen(), departTime);
            return Integer.compare(gapToDepartA,gapToDepartB);
        };
    }

    private boolean filterDueTram(String headsign, DepartureDTO dueTram, TramTime firstDepartTime) {
        String destination = dueTram.getDestination().toLowerCase();

        TramTime limit = firstDepartTime.plusMinutes(INCLUDE_LIVE_TIME_LIMIT);
        boolean withinTime =  dueTram.getWhen().between(firstDepartTime, limit);

//        boolean withinTime = dueTram.getWhen().isBefore(firstDepartTime.plusMinutes(TIME_LIMIT));
        
        return headsign.equals(destination) &&
                ("Due".equals(dueTram.getStatus()) && withinTime);
    }

    private LocationDTO getBegin(List<StageDTO> allStages) {
        if (firstStageIsWalk(allStages)) {
            if (allStages.size()>1) {
                // the first station
                return allStages.get(1).getFirstStation();
            } else {
                return allStages.get(0).getFirstStation();
            }
        }
        return getFirstStage(allStages).getFirstStation();
    }

    private LocationDTO getEnd(List<StageDTO> allStages) {
        return getLastStage(allStages).getLastStation();
    }

    private boolean isDirect(List<StageDTO> allStages) {
        int size = allStages.size();
        // Direct first
        if (size == 1) {
            return true;
        } else if (size == 2 && firstStageIsWalk(allStages)) {
            return true;
        }
        return false;
    }

    private List<String> getChangeStationNames(List<StageDTO> allStages) {
        List<String> result = new ArrayList<>();

        if (isDirect(allStages)) {
            return result;
        }

        for(int index = 1; index< allStages.size(); index++) {
            result.add(allStages.get(index).getFirstStation().getName());
        }

        return result;
    }

    private TramTime getFirstDepartureTime(List<StageDTO> allStages) {
        if (allStages.size() == 0) {
            return TramTime.midnight();
        }
        return getFirstStage(allStages).getFirstDepartureTime();
    }

    private TramTime getExpectedArrivalTime(List<StageDTO> allStages) {
        if (allStages.size() == 0) {
            return TramTime.of(0,0);
        }
        return getLastStage(allStages).getExpectedArrivalTime();
    }

    private StageDTO getLastStage(List<StageDTO> allStages) {
        int index = allStages.size()-1;
        return allStages.get(index);
    }

    private StageDTO getFirstStage(List<StageDTO> allStages) {
        return allStages.get(0);
    }

    private boolean firstStageIsWalk(List<StageDTO> allStages) {
        if (allStages.isEmpty()) {
            logger.error("No stages in the journey");
            return false;
        }

        return allStages.get(0).getMode()==TransportMode.Walk;
    }

}
