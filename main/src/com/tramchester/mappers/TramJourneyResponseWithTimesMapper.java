package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.presentation.VehicleStageWithTiming;
import com.tramchester.repository.ServiceTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TramJourneyResponseWithTimesMapper extends SingleJourneyMapper {
    private static final Logger logger = LoggerFactory.getLogger(TramJourneyResponseWithTimesMapper.class);
    private final ServiceTimes serviceTimes;

    public TramJourneyResponseWithTimesMapper(ServiceTimes serviceTimes) {

        this.serviceTimes = serviceTimes;
    }

    @Override
    public Optional<Journey> createJourney(RawJourney rawJourney, int withinMins) {
        List<RawStage> rawJourneyStages = rawJourney.getStages();
        List<TransportStage> stages = new LinkedList<>();

        LocalTime currentTime = rawJourney.getQueryTime();

        for(RawStage rawStage : rawJourneyStages)
            if (rawStage.getIsAVehicle()) {
                RawVehicleStage rawTravelStage = (RawVehicleStage) rawStage;

                currentTime = rawTravelStage.getDepartTime();
                String tripId = rawTravelStage.getTripId();

                Trip trip = serviceTimes.getTrip(tripId);

                LocalTime arriveTime = currentTime.plusMinutes(rawTravelStage.getCost());

                ServiceTime time = new ServiceTime(TramTime.of(currentTime), TramTime.of(arriveTime),
                        rawTravelStage.getServiceId(), trip.getHeadsign(), tripId);
                VehicleStageWithTiming stage = new VehicleStageWithTiming(rawTravelStage, time, decideAction(stages));
                stages.add(stage);
            } else if (rawStage.isWalk()) {
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
}
