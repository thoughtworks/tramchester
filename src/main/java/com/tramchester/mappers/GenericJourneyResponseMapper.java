package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
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
import java.util.SortedSet;

import static java.lang.String.format;

public class GenericJourneyResponseMapper extends JourneyResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(GenericJourneyResponseMapper.class);

    public GenericJourneyResponseMapper(TransportDataFromFiles transportData) {
        super(transportData);
    }

    @Override
    protected Journey createJourney(RawJourney rawJourney, TimeWindow timeWindow) {
        TimeWindow newTimeWindow = timeWindow;

        List<PresentableStage> stages = new LinkedList<>();
        List<TransportStage> rawJourneyStages = rawJourney.getStages();
        for (TransportStage rawStage : rawJourneyStages) {
            if (rawStage.getIsAVehicle()) {
                RawVehicleStage rawTravelStage = (RawVehicleStage) rawStage;
                String serviceId = rawTravelStage.getServiceId();
                logger.info(format("ServiceId: %s Journey clock is now %s ", serviceId, timeWindow));
                Location firstStation = rawTravelStage.getFirstStation();
                Location lastStation = rawTravelStage.getLastStation();

                // TODO use the first matching time only
                Optional<ServiceTime> maybeTimes = transportData.getFirstServiceTime(serviceId, firstStation, lastStation, newTimeWindow);
                if (!maybeTimes.isPresent()) {
                    String message = format("Cannot complete journey. stage '%s' service '%s' clock '%s'",
                            rawStage, serviceId, newTimeWindow);
                    logger.error(message);
                    break;
                }

                ServiceTime time = maybeTimes.get();
                logger.info(format("Found time %s for service id %s", time, serviceId));
                VehicleStageWithTiming stage = new VehicleStageWithTiming(rawTravelStage, time, decideAction(stages));
                stages.add(stage);
                int arrivesAt = time.getFromMidnightArrives();
                newTimeWindow = newTimeWindow.next(arrivesAt);
            }
        }
        Journey journey = new Journey(stages);
        return journey;
    }
}