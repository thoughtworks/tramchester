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

public class GenericJourneyResponseMapper extends JourneyResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(GenericJourneyResponseMapper.class);

    public GenericJourneyResponseMapper(TransportDataFromFiles transportData) {
        super(transportData);
    }

    @Override
    protected Journey createJourney(RawJourney rawJourney, TimeWindow timeWindow) {

        List<PresentableStage> stages = new LinkedList<>();
        List<TransportStage> rawJourneyStages = rawJourney.getStages();
        for (TransportStage rawStage : rawJourneyStages) {
            if (rawStage.getIsAVehicle()) {
                RawVehicleStage rawTravelStage = (RawVehicleStage) rawStage;
                String serviceId = rawTravelStage.getServiceId();
                logger.info(format("ServiceId: %s Journey clock is now %s ", serviceId, timeWindow));
                Location firstStation = rawTravelStage.getFirstStation();
                Location lastStation = rawTravelStage.getLastStation();

                int elapsedTime = rawTravelStage.getStartTime();
                TimeWindow newTimeWindow = timeWindow.next(elapsedTime);

                SortedSet<ServiceTime> times = transportData.getTimes(serviceId, firstStation, lastStation, newTimeWindow);
                if (times.isEmpty()) {
                    String message = format("Cannot complete journey. stage '%s' service '%s' clock '%s'",
                            rawStage, serviceId, newTimeWindow);
                    logger.error(message);
                } else {
                    logger.info(format("Found %s times for service id %s", times.size(), serviceId));
                    VehicleStageWithTiming stage = new VehicleStageWithTiming(rawTravelStage, times, decideAction(stages));
                    stages.add(stage);
                }
            }
        }
        Journey journey = new Journey(stages);
        return journey;
    }
}