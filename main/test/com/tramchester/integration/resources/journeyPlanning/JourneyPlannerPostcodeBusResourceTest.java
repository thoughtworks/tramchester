package com.tramchester.integration.resources.journeyPlanning;

import com.tramchester.App;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.LocationJourneyPlannerTestFacade;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.testTags.BusTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerPostcodeBusResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new IntegrationBusTestConfig());

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
    void shouldPlanJourneyFromPostcodeToPostcodeViaBus() {

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(day, time, TestPostcodes.CentralBury,
                TestPostcodes.NearPiccadillyGardens, false, 0);

        Set<JourneyDTO> journeys = results.getJourneys();

        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> {
                assertEquals(3,journeyDTO.getStages().size(), journeyDTO.toString());
                assertEquals(TransportMode.Walk, journeyDTO.getStages().get(0).getMode());
                assertEquals(TransportMode.Walk, journeyDTO.getStages().get(2).getMode());
        });
    }

    @Test
    void shouldWalkFromPostcodeToNearbyStation() {

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(day, time, TestPostcodes.CentralBury,
                BusStations.BuryInterchange, false, 1);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> {
            assertEquals(1, journeyDTO.getStages().size());
            assertEquals(TransportMode.Walk, journeyDTO.getStages().get(0).getMode());
        });
    }

    @Test
    void shouldWalkFromStationToNearbyPostcode() {

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(day, time, BusStations.BuryInterchange,
                TestPostcodes.CentralBury, false, 1);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        Set<JourneyDTO> oneStage = journeys.stream().filter(journeyDTO -> journeyDTO.getStages().size() == 1).collect(Collectors.toSet());
        assertFalse(oneStage.isEmpty(), "no one stage in " + journeys);

        oneStage.forEach(journeyDTO -> {
            final List<StageDTO> stages = journeyDTO.getStages();
            assertEquals(TransportMode.Walk, stages.get(0).getMode(), stages.get(0).toString());
            assertEquals(BusStations.BuryInterchange.forDTO(), journeyDTO.getBegin().getId());
        });
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusStation() {

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(day, time, TestPostcodes.CentralBury,
                BusStations.ShudehillInterchange, false, 1);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journey -> {
            final List<StageDTO> stages = journey.getStages();
            assertTrue(stages.size()>=2, journey.toString());
            assertEquals(stages.get(0).getMode(), TransportMode.Walk, journey.toString());
            assertEquals(stages.get(stages.size()-1).getMode(), TransportMode.Bus);
        });
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcode() {

        JourneyPlanRepresentation results = journeyPlanner.getJourneyPlan(day, time, BusStations.ShudehillInterchange,
                TestPostcodes.CentralBury, false, 1);

        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> {
            assertEquals(2, journeyDTO.getStages().size(), journeyDTO.toString());
            assertEquals(journeyDTO.getStages().get(0).getMode(), TransportMode.Bus);
        });
    }

}
