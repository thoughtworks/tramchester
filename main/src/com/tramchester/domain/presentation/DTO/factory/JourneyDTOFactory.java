package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.Note;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JourneyDTOFactory {
    private static final Logger logger = LoggerFactory.getLogger(JourneyDTOFactory.class);

    public JourneyDTOFactory() {
    }

    public JourneyDTO build(List<StageDTO> stages, TramTime queryTime, List<Note> notes,
                            List<StationRefWithPosition> path, LocalDate queryDate) {
        boolean isDirect = isDirect(stages);
        StationRefWithPosition begin = getBegin(stages);
        StationRefWithPosition end = getEnd(stages);

        return new JourneyDTO(begin, end, stages, getExpectedArrivalTime(stages, queryDate),
                getFirstDepartureTime(stages, queryDate),
                isDirect, getChangeStationNames(stages), queryTime, notes, path, queryDate);
    }


    private StationRefWithPosition getBegin(List<StageDTO> allStages) {
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

    private StationRefWithPosition getEnd(List<StageDTO> allStages) {
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

    private List<StationRefWithPosition> getChangeStationNames(List<StageDTO> allStages) {
        List<StationRefWithPosition> result = new ArrayList<>();

        if (isDirect(allStages)) {
            return result;
        }

        for(int index = 1; index< allStages.size(); index++) {
            result.add(allStages.get(index).getFirstStation());
        }

        return result;
    }

    private LocalDateTime getFirstDepartureTime(List<StageDTO> allStages, LocalDate queryDate) {
        if (allStages.size() == 0) {
            return LocalDateTime.of(queryDate, TramTime.midnight().asLocalTime());
        }
        return getFirstStage(allStages).getFirstDepartureTime();
    }

    private LocalDateTime getExpectedArrivalTime(List<StageDTO> allStages, LocalDate queryDate) {
        if (allStages.size() == 0) {
            return LocalDateTime.of(queryDate, TramTime.midnight().asLocalTime());
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
