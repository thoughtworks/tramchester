package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.StageWithTiming;
import com.tramchester.domain.presentation.TravelAction;
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
        List<StageWithTiming> stages = new LinkedList<>();
        List<TransportStage> rawJourneyStages = rawJourney.getStages();
        for (TransportStage rawStage : rawJourneyStages) {
            if (rawStage.isVehicle()) {
                RawVehicleStage rawTravelStage = (RawVehicleStage) rawStage;
                String serviceId = rawTravelStage.getServiceId();
                logger.info(format("ServiceId: %s Journey clock is now %s ", serviceId, timeWindow));
                Station firstStation = rawTravelStage.getFirstStation();
                Station lastStation = rawTravelStage.getLastStation();

                SortedSet<ServiceTime> times = transportData.getTimes(serviceId, firstStation, lastStation, timeWindow);
                if (times.isEmpty()) {
                    String message = format("Cannot complete journey. stage '%s' service '%s' clock '%s'",
                            rawStage, serviceId, timeWindow);
                    logger.error(message);
                } else {
                    logger.info(format("Found %s times for service id %s", times.size(), serviceId));
                    StageWithTiming stage = new StageWithTiming(rawTravelStage, times, decideAction(stages,rawJourneyStages));
                    stages.add(stage);

                    int departsAtMinutes = stage.findEarliestDepartureTime();
                    int duration = stage.getDuration();
                    timeWindow = timeWindow.next(departsAtMinutes + duration);
                    logger.info(format("Previous stage duration was %s, earliest depart is %s, new offset is %s ",
                            duration, departsAtMinutes, timeWindow));
                }
            }
        }
        return new Journey(stages);
    }




}
