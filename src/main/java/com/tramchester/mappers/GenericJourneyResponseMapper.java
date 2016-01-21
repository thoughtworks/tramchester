package com.tramchester.mappers;

import com.tramchester.domain.RawJourney;
import com.tramchester.domain.RawStage;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.Stage;
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
    protected Journey createJourney(RawJourney rawJourney, int maxNumOfServiceTimes, int journeyClock) {
        int minNumberOfTimes = Integer.MAX_VALUE;

        List<Stage> stages = new LinkedList<>();
        for (RawStage rawStage : rawJourney.getStages()) {
            String serviceId = rawStage.getServiceId();
            logger.info(format("ServiceId: %s Journey clock is now %s ", serviceId, journeyClock));

            String firstStation = rawStage.getFirstStation();
            String lastStation = rawStage.getLastStation();
            int elapsedTime = rawStage.getElapsedTime();
            SortedSet<ServiceTime> times = transportData.getTimes(serviceId, firstStation, lastStation, elapsedTime,
                    maxNumOfServiceTimes);

            if (times.isEmpty()) {
                String message = format("Cannot complete journey. stage '%s' service '%s' clock '%s'",
                        rawStage, serviceId, journeyClock);
                logger.error(message);
                return null;
            }

            if (times.size()<minNumberOfTimes) {
                minNumberOfTimes = times.size();
            }
            logger.info(format("Found %s times for service id %s", times.size(), serviceId));
            Stage stage = new Stage(rawStage,times);
            stages.add(stage);
            int departsAtMinutes = stage.findDepartureTimeForEarliestArrival();
            int duration = stage.getDuration();
            journeyClock = departsAtMinutes + duration;
            logger.info(format("Previous stage duration was %s, earliest depart is %s, new offset is %s ",
                    duration, departsAtMinutes, journeyClock));
        }
        Journey journey = new Journey(stages, rawJourney.getIndex());
        journey.setNumberOfTimes(minNumberOfTimes);
        return journey;
    }

}
