package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.App;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerPostcodeTramResourceTest {

    private static final IntegrationAppExtension appExtension =
            new IntegrationAppExtension(App.class, new TramWithPostcodesEnabled());

    private LocalDate day;
    private TramTime time;
    private JourneyResourceTestFacade journeyPlanner;

    @BeforeEach
    void beforeEachTestRuns() {
        day = TestEnv.testDay();
        time = TramTime.of(9,35);
        journeyPlanner = new JourneyResourceTestFacade(appExtension);
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcode() {

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(day, time, TestPostcodes.CentralBury, TestPostcodes.NearPiccadillyGardens,
                false, 5);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        // TODO WIP
        journeys.forEach(journeyDTO -> Assertions.assertEquals(3,journeyDTO.getStages().size()));
    }

    @Test
    void shouldPlanJourneyFromPostcodeToStation() {
        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(day, time, TestPostcodes.CentralBury, TramStations.Piccadilly,
                false, 5);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        // TODO WIP
        journeys.forEach(journeyDTO -> Assertions.assertEquals(2,journeyDTO.getStages().size()));
    }

    @Test
    void shouldPlanJourneyFromStationToPostcode() {

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(day, time, TramStations.Piccadilly, TestPostcodes.CentralBury,
                false, 5);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> Assertions.assertEquals(2, journeyDTO.getStages().size()));
    }

}
