package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.Stage;
import com.tramchester.repository.TransportDataFromFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static java.lang.String.format;

public class TramJourneyResponseMapper extends JourneyResponseMapper {
    private static final Logger logger = LoggerFactory.getLogger(TramJourneyResponseMapper.class);

    public TramJourneyResponseMapper(TransportDataFromFiles transportData) {
        super(transportData);
    }

    protected Journey createJourney(RawJourney rawJourney, TimeWindow timeWindow) {
                                    //int maxNumOfSvcTimes, int journeyClock) {
        int minNumberOfTimes = Integer.MAX_VALUE;
        List<Stage> stages = new LinkedList<>();
        for (RawStage rawStage : rawJourney.getStages()) {
            String serviceId = rawStage.getServiceId();
            logger.info(format("ServiceId: %s Journey clock is now %s ", serviceId, timeWindow));

            String firstStation = rawStage.getFirstStation();
            String lastStation = rawStage.getLastStation();
            SortedSet<ServiceTime> times = transportData.getTimes(serviceId, firstStation, lastStation, timeWindow);
            if (times.isEmpty()) {
                String message = format("Cannot complete journey. stage '%s' service '%s' clock '%s'",
                        rawStage, serviceId, timeWindow);
                logger.error(message);
                return null;
            }

            if (times.size()<minNumberOfTimes) {
                minNumberOfTimes = times.size();
            }
            logger.info(format("Found %s times for service id %s", times.size(), serviceId));
            Stage stage = new Stage(rawStage,times);
            stages.add(stage);
            int departsAtMinutes = stage.findEarliestDepartureTime();
            int duration = stage.getDuration();
            timeWindow = timeWindow.next(departsAtMinutes + duration);
            logger.info(format("Previous stage duration was %s, earliest depart is %s, new offset is %s ",
                    duration, departsAtMinutes, timeWindow));
        }
        Journey journey = new Journey(stages);
        // we need to the least number of times we found for any one stage
        // This likely needs to change, people may want to choose an early depart as it may allow more time for a
        // connection or might (in the case of the buses) result in an early
        journey.setNumberOfTimes(minNumberOfTimes);
        return journey;
    }


}
