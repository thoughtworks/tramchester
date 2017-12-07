package com.tramchester.domain.presentation.DTO.factory;

import com.tramchester.domain.Location;
import com.tramchester.domain.TimeAsMinutes;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.WalkingStage;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.mappers.HeadsignMapper;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public JourneyDTO build(Journey journey) {
        List<TransportStage> transportStages = journey.getStages();
        List<StageDTO> stages = journey.getStages().stream().map(stage -> stageDTOFactory.build(stage)).collect(Collectors.toList());

        long embeddedWalk = transportStages.stream().filter(stage -> (stage instanceof WalkingStage)).count();
        if (firstStageIsWalk(transportStages)) {
            embeddedWalk -= 1;
        }
        String summary = getSummary(transportStages);
        String heading = getHeading(transportStages, embeddedWalk);

        LocationDTO begin = new LocationDTO(getBegin(transportStages));
        LocationDTO end = new LocationDTO(getEnd(transportStages));

        JourneyDTO journeyDTO = new JourneyDTO(begin, end, stages, getExpectedArrivalTime(transportStages),
                getFirstDepartureTime(transportStages), summary,
                heading);

        addDueTramIfPresent(journeyDTO);

        return journeyDTO;
    }

    private void addDueTramIfPresent(JourneyDTO journeyDTO) {
        Optional<StageDTO> maybeFirstTram = journeyDTO.getStages().stream().
                filter(stage -> TransportMode.Tram.equals(stage.getMode())).
                findFirst();
        if (!maybeFirstTram.isPresent()) {
            return;
        }

        StageDTO firstTramStage = maybeFirstTram.get();
        StationDepartureInfo departInfo = firstTramStage.getPlatform().getStationDepartureInfo();
        if (departInfo==null) {
            return;
        }

        LocalTime firstDepartTime = journeyDTO.getFirstDepartureTime();
        String headsign = headsignMapper.mapToDestination(firstTramStage.getHeadSign()).toLowerCase();

        Comparator<? super DueTram> nearestMatchByDueTime = createDueTramComparator(firstDepartTime);
        Optional<DueTram> maybeDueTram = departInfo.getDueTrams().stream().
                filter(dueTram -> filterDueTram(headsign, dueTram, firstDepartTime)).
                sorted(nearestMatchByDueTime).findFirst();
        if (maybeDueTram.isPresent()) {
            DueTram dueTram = maybeDueTram.get();
            DateTime when = dueTram.getWhen();
            journeyDTO.setDueTram(format("%s tram %s at %s", dueTram.getCarriages(), dueTram.getStatus(),
                    when.toString("HH:mm")));
        }
    }

    private Comparator<DueTram> createDueTramComparator(LocalTime firstDepartTime) {
        return (a, b) -> {
            int gapA = Math.abs(a.getWhen().getMillisOfDay() - firstDepartTime.getMillisOfDay());
            int gapB = Math.abs(b.getWhen().getMillisOfDay() - firstDepartTime.getMillisOfDay());
            return Integer.compare(gapA,gapB);
        };
    }

    private boolean filterDueTram(String headsign, DueTram dueTram, LocalTime firstDepartTime) {
        int dueAsMins = TimeAsMinutes.getMinutes(dueTram.getWhen().toLocalTime());
        int departAsMins = TimeAsMinutes.getMinutes(firstDepartTime);
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

    private String getSummary(List<TransportStage> allStages) {
        int size = allStages.size();
        // Direct first
        if (size == 1) {
            return "Direct";
        } else if (size == 2 && firstStageIsWalk(allStages)) {
            return "Direct";
        }

        StringBuilder result = new StringBuilder();
        for(int index = 1; index< size; index++) {
            TransportStage stage = allStages.get(index);
            if (index>1) {
                if (index< size -1) {
                    result.append(", ");
                } else {
                    result.append(" and ");
                }
            }
            result.append(stage.getFirstStation().getName());
        }
        return format("Change at %s",result.toString());
    }

    private String getHeading(List<TransportStage> allStages, long embeddedWalk) {
        String mode;
        if (firstStageIsWalk(allStages)) {
            if (allStages.size()>1) {
                mode = allStages.get(1).getMode().toString();
                mode = "Walk and " + mode;
            }
            else {
                mode = "Walk";
            }
        } else {
            mode = getFirstStage(allStages).getMode().toString();
        }
        if (embeddedWalk >0) {
            mode = mode + " and Walk";
        }
        return format("%s with %s - %s", mode, getChanges(allStages), getDuration(allStages));
    }

    private LocalTime getFirstDepartureTime(List<TransportStage> allStages) {
        if (allStages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        if (firstStageIsWalk(allStages)) {
            if (allStages.size()>1) {
                return allStages.get(1).getFirstDepartureTime();
            }
        }
        return getFirstStage(allStages).getFirstDepartureTime();
    }

    private String getDuration(List<TransportStage> allStages) {
        int mins = TimeAsMinutes.timeDiffMinutes(getExpectedArrivalTime(allStages), getFirstStage(allStages).getFirstDepartureTime());
        return format("%s minutes", mins);
    }

    private LocalTime getExpectedArrivalTime(List<TransportStage> allStages) {
        if (allStages.size() == 0) {
            return LocalTime.MIDNIGHT;
        }
        return getLastStage(allStages).getExpectedArrivalTime();
    }

    private TransportStage getLastStage(List<TransportStage> allStages) {
        int index = allStages.size()-1;
        return allStages.get(index);
    }

    private String getChanges(List<TransportStage> allStages) {
        if (allStages.size() <= 1) {
            return "No Changes";
        }
        if (allStages.size() == 2) {
            return firstStageIsWalk(allStages) ? "No Changes" : "1 change";
        }
        return format("%s changes", allStages.size() -1);
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
        return !allStages.get(0).getIsAVehicle();
    }

}
