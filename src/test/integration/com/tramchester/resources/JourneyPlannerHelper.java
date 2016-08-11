package com.tramchester.resources;

import com.tramchester.domain.Location;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.PresentableStage;
import org.joda.time.LocalDate;

import java.time.LocalTime;
import java.util.Set;

import static org.joda.time.DateTimeConstants.MONDAY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class JourneyPlannerHelper {
    protected JourneyPlannerResource planner;

    protected void checkDepartsAfterPreviousArrival(String message, Set<Journey> journeys) {
        for(Journey journey: journeys) {
            LocalTime previousArrive = null;
            for(PresentableStage stage : journey.getStages()) {
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
        JourneyPlanRepresentation results = planner.createJourneyPlan(start.getId(), end.getId(), queryDate,minsPastMid);
        Set<Journey> journeys = results.getJourneys();

        String message = String.format("from %s to %s at %s on %s", start, end, minsPastMid, queryDate);
        assertTrue("Unable to find journey " + message, journeys.size() > 0);
        checkDepartsAfterPreviousArrival(message, journeys);
        journeys.forEach(journey -> assertFalse("Missing stages for journey"+journey,journey.getStages().isEmpty()));
        return results;
    }

    protected void checkRouteNext7Days(Location start, Location dest, LocalDate date, int time) throws TramchesterException {
        if (!dest.equals(start)) {
            for(int day=0; day<7; day++) {
                validateAtLeastOneJourney(start, dest, time, date.plusDays(day));
            }
        }
    }

    public static LocalDate nextMonday() {
        LocalDate now = LocalDate.now();
        int offset = now.getDayOfWeek()-MONDAY;
        return now.minusDays(offset).plusWeeks(1);
    }

}
