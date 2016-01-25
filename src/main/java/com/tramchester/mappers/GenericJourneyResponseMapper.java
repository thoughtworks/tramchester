package com.tramchester.mappers;

import com.tramchester.domain.RawJourney;
import com.tramchester.domain.RawStage;
import com.tramchester.domain.TimeWindow;
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
    protected Journey createJourney(RawJourney rawJourney, TimeWindow timeWindow) {
        int minNumberOfTimes = Integer.MAX_VALUE;

        List<Stage> stages = new LinkedList<>();
        for (RawStage rawStage : rawJourney.getStages()) {
            String serviceId = rawStage.getServiceId();
            logger.info(format("ServiceId: %s Journey clock is now %s ", serviceId, timeWindow));

            String firstStation = rawStage.getFirstStation();
            String lastStation = rawStage.getLastStation();
            int elapsedTime = rawStage.getElapsedTime();
            TimeWindow newTimeWindow = timeWindow.next(elapsedTime);
            SortedSet<ServiceTime> times = transportData.getTimes(serviceId, firstStation, lastStation,
                    newTimeWindow);

            if (times.isEmpty()) {
                String message = format("Cannot complete journey. stage '%s' service '%s' clock '%s'",
                        rawStage, serviceId, newTimeWindow);
                logger.error(message);
                return null;
            }

            if (times.size()<minNumberOfTimes) {
                minNumberOfTimes = times.size();
            }
            logger.info(format("Found %s times for service id %s", times.size(), serviceId));
            Stage stage = new Stage(rawStage,times);
            stages.add(stage);
        }
        Journey journey = new Journey(stages);
        journey.setNumberOfTimes(minNumberOfTimes);
        return journey;
    }

}
