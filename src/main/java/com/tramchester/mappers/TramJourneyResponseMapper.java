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
import java.util.Optional;

import static java.lang.String.format;

public class TramJourneyResponseMapper extends JourneyResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(TramJourneyResponseMapper.class);

    public TramJourneyResponseMapper(TransportDataFromFiles transportData) {
        super(transportData);
    }

    protected Optional<Journey> createJourney(RawJourney rawJourney, int withinMins) {
        List<PresentableStage> stages = new LinkedList<>();
        List<TransportStage> rawJourneyStages = rawJourney.getStages();
        TimeWindow timeWindow = new TimeWindow(rawJourney.getQueryTime(), withinMins);
        for (TransportStage rawStage : rawJourneyStages) {
            if (rawStage.getIsAVehicle()) {
                timeWindow = mapVehicleStage(timeWindow, stages, rawStage);
            } else if (rawStage.isWalk()) {
                RawWalkingStage stage = (RawWalkingStage) rawStage;
                PresentableStage walkingStage = new WalkingStage(stage, timeWindow.minsFromMidnight());
                logger.info("Adding walking stage " + stage);
                stages.add(walkingStage);

                timeWindow = timeWindow.next(TimeAsMinutes.getMinutes(walkingStage.getExpectedArrivalTime()));
            }
        }
        if (rawJourneyStages.size()!=stages.size()) {
            logger.error("Failed to create valid journey");
            return Optional.empty();
        }
        return Optional.of(new Journey(stages));
    }

    private TimeWindow mapVehicleStage(TimeWindow timeWindow, List<PresentableStage> stages,
                                       TransportStage rawStage) {
        RawVehicleStage rawTravelStage = (RawVehicleStage) rawStage;
        String serviceId = rawTravelStage.getServiceId();
        logger.info(format("ServiceId: %s Journey clock is now %s ", serviceId, timeWindow));

        Location firstStation = rawTravelStage.getFirstStation();
        Location lastStation = rawTravelStage.getLastStation();

        Optional<ServiceTime> time = transportData.getFirstServiceTime(serviceId, firstStation, lastStation, timeWindow);
        if (!time.isPresent()) {
            String message = format("Cannot complete journey. stage '%s' service '%s' clock '%s'",
                    rawStage, serviceId, timeWindow);
            logger.error(message);
        } else {
            logger.info(format("Found time %s for service id %s", time.get(), serviceId));
            VehicleStageWithTiming stage = new VehicleStageWithTiming(rawTravelStage, time.get(), decideAction(stages));
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
