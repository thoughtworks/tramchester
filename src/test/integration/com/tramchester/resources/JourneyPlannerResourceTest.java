package com.tramchester.resources;

import com.tramchester.*;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static junit.framework.TestCase.assertTrue;

public class JourneyPlannerResourceTest {
    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    @Test
    @Ignore("Work in progress")
    public void shouldFindAltrinchamToBury() {
        String start = Stations.Bury.getId();
        String end = Stations.ManAirport.getId();
        String time = LocalTime.now().toString("HH:mm:00");
        String date = LocalDate.now().toString("YYYY-MM-dd");
        Response result = IntegrationClient.getResponse(testRule,
                String.format("journey?start=%s&end=%s&departureTime=%s&departureDate=%s", start, end, time, date));

        JourneyPlanRepresentation representation = result.readEntity(JourneyPlanRepresentation.class);

        assertTrue(!representation.getJourneys().isEmpty());
    }
}
