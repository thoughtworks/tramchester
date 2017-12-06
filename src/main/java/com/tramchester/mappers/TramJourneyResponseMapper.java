package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.*;
import com.tramchester.repository.ServiceTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class TramJourneyResponseMapper implements SingleJourneyMapper {
    private static final Logger logger = LoggerFactory.getLogger(TramJourneyResponseMapper.class);

    private ServiceTimes serviceTimes;

    public TramJourneyResponseMapper(ServiceTimes serviceTimes) {
        this.serviceTimes = serviceTimes;
    }

    public Optional<Journey> createJourney(RawJourney rawJourney, int withinMins) {
        List<RawStage> rawJourneyStages = rawJourney.getStages();
        List<TransportStage> stages = new LinkedList<>();
        int queryTime = rawJourney.getQueryTime();
        TimeWindow timeWindow = new TimeWindow(queryTime, withinMins);

        for (RawStage rawStage : rawJourneyStages) {
            if (rawStage.getIsAVehicle()) {
                timeWindow = mapVehicleStage(timeWindow, stages, rawStage);
            } else if (rawStage.isWalk()) {
                RawWalkingStage stage = (RawWalkingStage) rawStage;
                TransportStage walkingStage = new WalkingStage(stage, timeWindow.minsFromMidnight());
                logger.info("Adding walking stage " + stage);
                stages.add(walkingStage);

                timeWindow = timeWindow.next(TimeAsMinutes.getMinutes(walkingStage.getExpectedArrivalTime()));
            }
        }

        if (rawJourneyStages.size()!=stages.size()) {
            logger.error("Failed to create valid journey");
            return Optional.empty();
        }
        Journey journey = new Journey(stages);
        return Optional.of(journey);
    }

    private TimeWindow mapVehicleStage(TimeWindow timeWindow, List<TransportStage> stages, RawStage rawStage) {
        RawVehicleStage rawTravelStage = (RawVehicleStage) rawStage;
        String serviceId = rawTravelStage.getServiceId();
        logger.info(format("ServiceId: %s Journey clock is now %s ", serviceId, timeWindow));

        Location firstStation = rawTravelStage.getFirstStation();
        Location lastStation = rawTravelStage.getLastStation();

        Optional<ServiceTime> time = serviceTimes.getFirstServiceTime(serviceId, firstStation, lastStation, timeWindow);
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

    protected TravelAction decideAction(List<TransportStage> stagesSoFar) {
        if (stagesSoFar.isEmpty()) {
            return TravelAction.Board;
        }
        if ((stagesSoFar.get(stagesSoFar.size()-1) instanceof WalkingStage)) {
            return TravelAction.Board;
        }
        return TravelAction.Change;
    }


}
