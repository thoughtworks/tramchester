package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.mappers.HeadsignMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class JourneyDTOFactory {
    private static final Logger logger = LoggerFactory.getLogger(JourneyDTOFactory.class);

    public static int TIME_LIMIT = 15;

    private StageDTOFactory stageDTOFactory;
    private HeadsignMapper headsignMapper;

    public JourneyDTOFactory(StageDTOFactory stageDTOFactory, HeadsignMapper headsignMapper) {
        this.stageDTOFactory = stageDTOFactory;
        this.headsignMapper = headsignMapper;
    }

    public JourneyDTO build(Journey journey) throws TramchesterException {
        List<TransportStage> transportStages = journey.getStages();
        List<StageDTO> stages = journey.getStages().stream().map(stage -> stageDTOFactory.build(stage)).collect(Collectors.toList());

        boolean isDirect = isDirect(transportStages);

        LocationDTO begin = new LocationDTO(getBegin(transportStages));
        LocationDTO end = new LocationDTO(getEnd(transportStages));

        JourneyDTO journeyDTO = new JourneyDTO(begin, end, stages, getExpectedArrivalTime(transportStages),
                getFirstDepartureTime(transportStages),
                isDirect, getChangeStationNames(transportStages));

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
    private Comparator<DepartureDTO> createDueTramComparator(TramTime firstDepartTime) {
        return (a, b) -> {
            int gapA = Math.abs(a.getWhen().minutesOfDay() - firstDepartTime.minutesOfDay());
            int gapB = Math.abs(b.getWhen().minutesOfDay() - firstDepartTime.minutesOfDay());
            return Integer.compare(gapA,gapB);
        };
    }

    private boolean filterDueTram(String headsign, DepartureDTO dueTram, TramTime firstDepartTime) {
        int dueAsMins = dueTram.getWhen().minutesOfDay();
        int departAsMins = firstDepartTime.minutesOfDay();
        String destination = dueTram.getDestination().toLowerCase();
        
        return headsign.equals(destination) &&
                ("Due".equals(dueTram.getStatus()) &&
                        (Math.abs(dueAsMins-departAsMins)< TIME_LIMIT));
    }

    private Location getBegin(List<TransportStage> allStages) {
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

    private Location getEnd(List<TransportStage> allStages) {
        return getLastStage(allStages).getLastStation();
    }

    private boolean isDirect(List<TransportStage> allStages) {
        int size = allStages.size();
        // Direct first
        if (size == 1) {
            return true;
        }
        if (size == 2 && firstStageIsWalk(allStages)) {
            return true;
        }
        return false;
    }

    private List<String> getChangeStationNames(List<TransportStage> allStages) {
        List<String> result = new ArrayList<>();

        if (isDirect(allStages)) {
            return result;
        }

        for(int index = 1; index< allStages.size(); index++) {
            result.add(allStages.get(index).getFirstStation().getName());
        }

        return result;
    }

    private TramTime getFirstDepartureTime(List<TransportStage> allStages) {
        if (allStages.size() == 0) {
            return TramTime.midnight();
        }
        if (firstStageIsWalk(allStages)) {
            if (allStages.size()>1) {
                return allStages.get(1).getFirstDepartureTime();
            }
        }
        return getFirstStage(allStages).getFirstDepartureTime();
    }

    private TramTime getExpectedArrivalTime(List<TransportStage> allStages) throws TramchesterException {
        if (allStages.size() == 0) {
            return TramTime.create(0,0);
        }
        return getLastStage(allStages).getExpectedArrivalTime();
    }

    private TransportStage getLastStage(List<TransportStage> allStages) {
        int index = allStages.size()-1;
        return allStages.get(index);
    }

    private TransportStage getFirstStage(List<TransportStage> allStages) {
//        if (allStages.size()==1) {
//            return allStages.get(0);
//        }
        return allStages.get(0);
    }

    private boolean firstStageIsWalk(List<TransportStage> allStages) {
        if (allStages.isEmpty()) {
            logger.error("No stages in the journey");
            return false;
        }

        return allStages.get(0).getMode()==TransportMode.Walk;
    }

}
