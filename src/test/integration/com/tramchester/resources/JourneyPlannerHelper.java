package com.tramchester.resources;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.StageWithTiming;

import java.time.LocalTime;
import java.util.Set;

import static org.junit.Assert.assertTrue;


public class JourneyPlannerHelper {
    protected JourneyPlannerResource planner;

    protected void checkDepartsAfterPreviousArrival(String message, Set<Journey> journeys) {
        for(Journey journey: journeys) {
            LocalTime previousArrive = null;
            for(StageWithTiming stage : journey.getStages()) {
                if (previousArrive!=null) {
                    String prefix  = String.format("Check arrive at '%s' and leave at '%s' " , previousArrive, stage.getFirstDepartureTime());
                    assertTrue(prefix + message, stage.getFirstDepartureTime().isAfter(previousArrive));
                }
                previousArrive = stage.getExpectedArrivalTime();
            }
        }
    }

    protected JourneyPlanRepresentation validateAtLeastOneJourney(String start, String end, int minsPastMid, DaysOfWeek dayOfWeek,
                                                                  TramServiceDate queryDate) throws TramchesterException {
        JourneyPlanRepresentation results = planner.createJourneyPlan(start, end, dayOfWeek, queryDate,minsPastMid);
        Set<Journey> journeys = results.getJourneys();

        String message = String.format("from %s to %s at %s on %s", start, end, minsPastMid, dayOfWeek);
        assertTrue("unable to find journey " + message, journeys.size() > 0);
        checkDepartsAfterPreviousArrival(message, journeys);
        return results;
    }

}
