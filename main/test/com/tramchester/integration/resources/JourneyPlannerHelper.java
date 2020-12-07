package com.tramchester.integration.resources;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.reference.TramStations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public abstract class JourneyPlannerHelper {

    static void checkDepartsAfterPreviousArrival(String message, Set<JourneyDTO> journeys) {
        for(JourneyDTO journey: journeys) {
            LocalDateTime previousArrive = null;
            for(StageDTO stage : journey.getStages()) {
                if (previousArrive!=null) {
                    LocalDateTime firstDepartureTime = stage.getFirstDepartureTime();
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

    JourneyPlanRepresentation validateAtLeastOneJourney(TramStations start, TramStations end, LocalDate date, TramTime queryTime)  {
        TramServiceDate queryDate = new TramServiceDate(date);
        JourneyPlanRepresentation results = getJourneyPlan(start, end, queryDate, queryTime.asLocalTime(), false, 3);
        Set<JourneyDTO> journeys = results.getJourneys();

        String message = String.format("from %s to %s at %s on %s", start, end, queryTime, queryDate);
        assertTrue("Unable to find journey " + message, journeys.size() > 0);
        checkDepartsAfterPreviousArrival(message, journeys);
        journeys.forEach(journey -> assertFalse("Missing stages for journey"+journey, journey.getStages().isEmpty()));
        return results;
    }

    protected JourneyPlanRepresentation getJourneyPlan(TramStations start, TramStations end, TramTime queryTime, LocalDate queryDate)  {
        return getJourneyPlan(start, end, new TramServiceDate(queryDate), queryTime.asLocalTime(), false, 3);
    }

    abstract JourneyPlanRepresentation getJourneyPlan(TramStations start, TramStations end, TramServiceDate queryDate, LocalTime queryTime,
                                                      boolean arriveBy, int maxChanges);


}
