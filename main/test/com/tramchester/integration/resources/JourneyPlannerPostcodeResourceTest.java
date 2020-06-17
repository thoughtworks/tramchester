package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.testSupport.Postcodes;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.SortedSet;

import static com.tramchester.testSupport.TestEnv.dateFormatDashes;

@ExtendWith(DropwizardExtensionsSupport.class)
public class JourneyPlannerPostcodeResourceTest {

    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    private LocalDate when;
    private LocalDateTime now;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        now = TestEnv.LocalNow();
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcode() {
        String date = when.format(dateFormatDashes);
        String time = now.format(TestEnv.timeFormatter);
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, Postcodes.CentralBury,
                Postcodes.NearPiccadily, time, date,
                null, false, 5);

        Assertions.assertEquals(200, response.getStatus());
        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        SortedSet<JourneyDTO> journeys = results.getJourneys();

        // TODO WIP
        journeys.forEach(journeyDTO -> Assertions.assertEquals(3,journeyDTO.getStages().size()));
    }

    @Test
    void shouldPlanJourneyFromPostcodeToStation() {
        String date = when.format(dateFormatDashes);
        String time = now.format(TestEnv.timeFormatter);
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, Postcodes.CentralBury,
                Stations.Piccadilly.getId(), time, date,
                null, false, 5);

        Assertions.assertEquals(200, response.getStatus());
        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        SortedSet<JourneyDTO> journeys = results.getJourneys();

        // TODO WIP
        journeys.forEach(journeyDTO -> Assertions.assertEquals(2,journeyDTO.getStages().size()));
    }

    @Test
    void shouldPlanJourneyFromStationToPostcode() {
        String date = when.format(dateFormatDashes);
        String time = now.format(TestEnv.timeFormatter);
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, Stations.Piccadilly.getId(),
                Postcodes.CentralBury, time, date,
                null, false, 5);

        Assertions.assertEquals(200, response.getStatus());
        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        SortedSet<JourneyDTO> journeys = results.getJourneys();

        // TODO WIP
        journeys.forEach(journeyDTO -> Assertions.assertEquals(2,journeyDTO.getStages().size()));
    }

}
