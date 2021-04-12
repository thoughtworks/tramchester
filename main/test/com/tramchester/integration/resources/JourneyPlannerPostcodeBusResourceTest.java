package com.tramchester.integration.resources;

import com.tramchester.App;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.PostcodeDTO;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.integration.testSupport.bus.BusWithPostcodesEnabled;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.TestEnv;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerPostcodeBusResourceTest {

    private static final IntegrationAppExtension appExtension = new IntegrationAppExtension(App.class,
            new BusWithPostcodesEnabled());

    private LocalDate day;
    private LocalTime time;

    @BeforeEach
    void beforeEachTestRuns() {
        day = TestEnv.testDay();
        time = LocalTime.of(9,35);
    }

    private String prefix(PostcodeLocation postcode) {
        return  "POSTCODE_"+postcode.forDTO();
    }

    @Test
    void shouldPlanJourneyFromPostcodeToPostcodeViaBus() {
        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExtension,
                prefix(TestPostcodes.CentralBury), prefix(TestPostcodes.NearPiccadillyGardens), time, day,
                null, false, 5);
        assertEquals(200, response.getStatus());

        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        Set<JourneyDTO> journeys = results.getJourneys();

        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> assertEquals(3,journeyDTO.getStages().size()));
    }

    @Test
    void shouldWalkFromPostcodeToNearbyStation() {
        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExtension,
                prefix(TestPostcodes.CentralBury), BusStations.BuryInterchange.forDTO(), time, day,
                null, false, 5);
        assertEquals(200, response.getStatus());

        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> {
            assertEquals(1, journeyDTO.getStages().size());
            assertEquals(TransportMode.Walk, journeyDTO.getStages().get(0).getMode());
            assertEquals(BusStations.BuryInterchange.getId().forDTO(), journeyDTO.getEnd().getId());
        });
    }

    @Test
    void shouldWalkFromStationToNearbyPostcode() {
        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExtension,
                BusStations.BuryInterchange.forDTO(), prefix(TestPostcodes.CentralBury),  time, day,
                null, false, 5);
        assertEquals(200, response.getStatus());

        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> {
            assertEquals(1, journeyDTO.getStages().size());
            assertEquals(TransportMode.Walk, journeyDTO.getStages().get(0).getMode());
            assertEquals(BusStations.BuryInterchange.forDTO(), journeyDTO.getBegin().getId());
        });
    }

    @Test
    void shouldPlanJourneyFromPostcodeToBusStation() {
        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExtension,
                prefix(TestPostcodes.CentralBury), BusStations.ShudehillInterchange.forDTO(), time, day,
                null, false, 5);

        assertEquals(200, response.getStatus());
        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> assertEquals(journeyDTO.getStages().get(0).getMode(), TransportMode.Walk));
        journeys.forEach(journeyDTO -> assertEquals(journeyDTO.getEnd().getId(), BusStations.ShudehillInterchange.forDTO()));
    }

    @Test
    void shouldPlanJourneyFromBusStationToPostcode() {
        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExtension,
                BusStations.ShudehillInterchange.forDTO(), prefix(TestPostcodes.CentralBury), time, day,
                null, false, 5);

        assertEquals(200, response.getStatus());
        JourneyPlanRepresentation results = response.readEntity(JourneyPlanRepresentation.class);
        Set<JourneyDTO> journeys = results.getJourneys();
        assertFalse(journeys.isEmpty());

        journeys.forEach(journeyDTO -> assertEquals(2, journeyDTO.getStages().size()));
        journeys.forEach(journeyDTO -> assertEquals(journeyDTO.getStages().get(0).getMode(), TransportMode.Bus));
        journeys.forEach(journeyDTO -> assertEquals(journeyDTO.getEnd().getId(), BusStations.ShudehillInterchange.forDTO()));
    }

}
