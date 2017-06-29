package com.tramchester.integration.resources;

import com.tramchester.domain.Location;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.resources.JourneyPlannerResource;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.Set;

import static org.joda.time.DateTimeConstants.MONDAY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public abstract class JourneyPlannerHelper {
    protected JourneyPlannerResource planner;

    public static void checkDepartsAfterPreviousArrival(String message, Set<JourneyDTO> journeys) {
        for(JourneyDTO journey: journeys) {
            LocalTime previousArrive = null;
            for(StageDTO stage : journey.getStages()) {
                if (previousArrive!=null) {
                    LocalTime firstDepartureTime = stage.getFirstDepartureTime();
                    String prefix  = String.format("Check first departure time %s is after arrival time %s for %s" ,
                            firstDepartureTime, previousArrive, stage);
                    if (stage.getMode()!= TransportMode.Walk) {
                        assertTrue(prefix + message, firstDepartureTime.isAfter(previousArrive));
                    }
                }
                previousArrive = stage.getExpectedArrivalTime();
            }
        }
    }

    protected JourneyPlanRepresentation validateAtLeastOneJourney(Location start, Location end, int minsPastMid,
                                                                  LocalDate date) throws TramchesterException {
        TramServiceDate queryDate = new TramServiceDate(date);
        JourneyPlanRepresentation results = getJourneyPlan(start, end, minsPastMid, queryDate);
        Set<JourneyDTO> journeys = results.getJourneys();

        String message = String.format("from %s to %s at %s on %s", start, end, minsPastMid, queryDate);
        assertTrue("Unable to find journey " + message, journeys.size() > 0);
        checkDepartsAfterPreviousArrival(message, journeys);
        journeys.forEach(journey -> assertFalse("Missing stages for journey"+journey,journey.getStages().isEmpty()));
        return results;
    }

    protected JourneyPlanRepresentation getJourneyPlan(Location start, Location end, int minsPastMid, LocalDate queryDate) throws TramchesterException {
        return getJourneyPlan(start, end, minsPastMid, new TramServiceDate(queryDate));
    }

    protected abstract JourneyPlanRepresentation getJourneyPlan(Location start, Location end, int minsPastMid, TramServiceDate queryDate) throws TramchesterException;

    public static LocalDate nextMonday(int offsetDays) {
        LocalDate now = LocalDate.now().minusDays(offsetDays);
        int offset = now.getDayOfWeek()-MONDAY;
        LocalDate nextMonday = now.minusDays(offset).plusWeeks(1);
        while (new TramServiceDate(nextMonday).isChristmasPeriod()) {
            nextMonday = nextMonday.plusWeeks(1);
        }
        return nextMonday;
    }

}
