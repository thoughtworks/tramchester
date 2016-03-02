package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.PresentableStage;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.VehicleStageWithTiming;
import com.tramchester.repository.TransportDataFromFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

import static java.lang.String.format;

public class TramJourneyResponseMapper extends JourneyResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(TramJourneyResponseMapper.class);

    public TramJourneyResponseMapper(TransportDataFromFiles transportData) {
        super(transportData);
    }

    protected Journey createJourney(RawJourney rawJourney, TimeWindow timeWindow) {
        List<PresentableStage> stages = new LinkedList<>();
        List<TransportStage> rawJourneyStages = rawJourney.getStages();
        for (TransportStage rawStage : rawJourneyStages) {
            if (rawStage.getIsAVehicle()) {
                timeWindow = mapVehicleStage(timeWindow, stages, rawJourneyStages, rawStage);
            } else if (rawStage.getMode().equals(TransportMode.Walk)) {
                WalkingStage stage = (WalkingStage) rawStage;
                logger.info("Adding walking stage " + stage);
                stages.add(stage);
                timeWindow = timeWindow.next(timeWindow.minsFromMidnight()+stage.getDuration());
            }
        }
        return new Journey(stages);
    }

    private TimeWindow mapVehicleStage(TimeWindow timeWindow, List<PresentableStage> stages, List<TransportStage> rawJourneyStages, TransportStage rawStage) {
        RawVehicleStage rawTravelStage = (RawVehicleStage) rawStage;
        String serviceId = rawTravelStage.getServiceId();
        logger.info(format("ServiceId: %s Journey clock is now %s ", serviceId, timeWindow));
        Location firstStation = rawTravelStage.getFirstStation();
        Location lastStation = rawTravelStage.getLastStation();

        SortedSet<ServiceTime> times = transportData.getTimes(serviceId, firstStation, lastStation, timeWindow);
        if (times.isEmpty()) {
            String message = format("Cannot complete journey. stage '%s' service '%s' clock '%s'",
                    rawStage, serviceId, timeWindow);
            logger.error(message);
        } else {
            logger.info(format("Found %s times for service id %s", times.size(), serviceId));
            VehicleStageWithTiming stage = new VehicleStageWithTiming(rawTravelStage, times, decideAction(stages,rawJourneyStages));
            stages.add(stage);

            int departsAtMinutes = stage.findEarliestDepartureTime();
            int duration = stage.getDuration();
            timeWindow = timeWindow.next(departsAtMinutes + duration);
            logger.info(format("Previous stage duration was %s, earliest depart is %s, new offset is %s ",
                    duration, departsAtMinutes, timeWindow));
        }
        return timeWindow;
    }


}
