package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.*;
import com.tramchester.repository.ServiceTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TramJourneyResponseWithTimesMapper {
    private static final Logger logger = LoggerFactory.getLogger(TramJourneyResponseWithTimesMapper.class);
    private final ServiceTimes serviceTimes;

    public TramJourneyResponseWithTimesMapper(ServiceTimes serviceTimes) {
        this.serviceTimes = serviceTimes;
    }

    public Optional<Journey> createJourney(RawJourney rawJourney) {
        List<RawStage> rawJourneyStages = rawJourney.getStages();
        List<TransportStage> stages = new LinkedList<>();

        TramTime currentTime = rawJourney.getQueryTime();

        for(RawStage rawStage : rawJourneyStages)
            if (rawStage.getMode().isVehicle()) {
                RawVehicleStage rawTravelStage = (RawVehicleStage) rawStage;

                currentTime = rawTravelStage.getDepartTime();
                String tripId = rawTravelStage.getTripId();

                Trip trip = serviceTimes.getTrip(tripId);
                String tripHeadsign = trip.getHeadsign();

                TramTime arriveTime = currentTime.plusMinutes(rawTravelStage.getCost());

                ServiceTime time = new ServiceTime(currentTime, arriveTime,
                        rawTravelStage.getServiceId(), tripHeadsign, tripId);

                VehicleStageWithTiming stage = new VehicleStageWithTiming(rawTravelStage, time, decideAction(stages));
                stages.add(stage);
            } else if (rawStage.getMode().isWalk()) {
                RawWalkingStage stage = (RawWalkingStage) rawStage;
                TransportStage walkingStage = new WalkingStage(stage, currentTime);
                int cost = stage.getDuration();
                currentTime = currentTime.plusMinutes(cost);
                logger.info("Adding walking stage " + stage);
                stages.add(walkingStage);
        }

        if (rawJourneyStages.size()!=stages.size()) {
            logger.error("Failed to create valid journey");
            return Optional.empty();
        }
        Journey journey = new Journey(stages);
        return Optional.of(journey);

    }

    private TravelAction decideAction(List<TransportStage> stagesSoFar) {
        if (stagesSoFar.isEmpty()) {
            return TravelAction.Board;
        }
        if ((stagesSoFar.get(stagesSoFar.size()-1) instanceof WalkingStage)) {
            return TravelAction.Board;
        }
        return TravelAction.Change;
    }
}
