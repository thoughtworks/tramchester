package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Postcodes;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.SortedSet;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static org.junit.Assert.assertEquals;

public class JourneyPlannerPostcodeResourceTest {
    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    private LocalDate when;
    private LocalDateTime now;

    @Before
    public void beforeEachTestRuns() {
        when = TestEnv.nextTuesday(0);
        now = TestEnv.LocalNow();
    }

    @Test
    public void shouldPlanJourneyFromPostcodeToPostcode() {
        String date = when.format(dateFormatDashes);
        String time = now.format(TestEnv.timeFormatter);
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, Postcodes.CentralBury,
                Postcodes.NearPiccadily, time, date,
                null, false, 5);

        assertEquals(200, response.getStatus());
        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        SortedSet<JourneyDTO> journeys = results.getJourneys();

        // TODO WIP
        journeys.forEach(journeyDTO -> assertEquals(3,journeyDTO.getStages().size()));
    }

    @Test
    public void shouldPlanJourneyFromPostcodeToStation() {
        String date = when.format(dateFormatDashes);
        String time = now.format(TestEnv.timeFormatter);
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, Postcodes.CentralBury,
                Stations.Piccadilly.getId(), time, date,
                null, false, 5);

        assertEquals(200, response.getStatus());
        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        SortedSet<JourneyDTO> journeys = results.getJourneys();

        // TODO WIP
        journeys.forEach(journeyDTO -> assertEquals(2,journeyDTO.getStages().size()));
    }

    @Test
    public void shouldPlanJourneyFromStationToPostcode() {
        String date = when.format(dateFormatDashes);
        String time = now.format(TestEnv.timeFormatter);
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, Stations.Piccadilly.getId(),
                Postcodes.CentralBury, time, date,
                null, false, 5);

        assertEquals(200, response.getStatus());
        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        SortedSet<JourneyDTO> journeys = results.getJourneys();

        // TODO WIP
        journeys.forEach(journeyDTO -> assertEquals(2,journeyDTO.getStages().size()));
    }

}
