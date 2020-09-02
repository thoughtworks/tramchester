package com.tramchester.integration.resources;


import com.tramchester.App;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.IntegrationAppExtension;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.TestEnv;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tramchester.testSupport.BusStations.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerBusTest {

    private static final IntegrationAppExtension appExt = new IntegrationAppExtension(App.class,
            new IntegrationBusTestConfig());

    private LocalDate when;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
    }

    @Category({BusTest.class})
    @Test
    void shouldPlanSimpleBusJourney() {
        TramTime queryTime = TramTime.of(8,45);
        JourneyPlanRepresentation plan = getJourneyPlan(AltrinchamInterchange.getId(), StockportBusStation.getId(), queryTime,
                new TramServiceDate(when), false, 3);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    @Category({BusTest.class})
    @Test
    void shouldPlanLongBusJourney() {
        TramTime queryTime = TramTime.of(8,45);
        JourneyPlanRepresentation plan = getJourneyPlan(ShudehillInterchange.getId(), StockportBusStation.getId(), queryTime,
                new TramServiceDate(when), false, 3);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    @Category({BusTest.class})
    @Test
    void shouldPlanBusJourneyNoLoops() {
        TramTime queryTime = TramTime.of(8,56);
        JourneyPlanRepresentation plan = getJourneyPlan(AltrinchamInterchange.getId(), ManchesterAirportStation.getId(), queryTime,
                new TramServiceDate(when), false, 2);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());

        found.forEach(result -> {
            Set<String> stageIds= new HashSet<>();
            result.getStages().forEach(stage -> {
                String id = stage.getActionStation().getId();
                Assertions.assertFalse(stageIds.contains(id), "duplicate stations id found during " +result);
                stageIds.add(id);
            });
        });
    }

    @Category({BusTest.class})
    @Test
    void shouldPlanSimpleBusJourneyFromLocation() {
        TramTime queryTime = TramTime.of(8,45);
        JourneyPlanRepresentation plan = getJourneyPlan(TestEnv.nearAltrincham, StockportBusStation.getId(), queryTime,
                new TramServiceDate(when), false);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    @Category({BusTest.class})
    @Test
    void shouldPlanDirectWalkToBusStopFromLocation() {
        TramTime queryTime = TramTime.of(8,15);
        JourneyPlanRepresentation plan = getJourneyPlan(TestEnv.nearAltrincham, AltrinchamInterchange.getId(), queryTime,
                new TramServiceDate(when), false);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    @Category({BusTest.class})
    @Test
    void shouldPlanSimpleJourneyArriveByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);
        JourneyPlanRepresentation plan = getJourneyPlan(StockportBusStation.getId(), AltrinchamInterchange.getId(), queryTime,
                new TramServiceDate(when), true, 3); // true => arrive by
        
        // TODO 20 mins gap? Estimation is too optimistic for Buses?
        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            Assertions.assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime));
            if (TramTime.diffenceAsMinutes(journeyDTO.getExpectedArrivalTime(),queryTime)<20) {
                found.add(journeyDTO);
            }
        });
        Assertions.assertFalse(found.isEmpty());
    }

    private List<JourneyDTO> getValidJourneysAfter(TramTime queryTime, JourneyPlanRepresentation plan) {
        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            TramTime firstDepartureTime = journeyDTO.getFirstDepartureTime();
            Assertions.assertTrue(firstDepartureTime.isAfter(queryTime)
                    || firstDepartureTime.equals(queryTime), firstDepartureTime.toString());
            if (journeyDTO.getExpectedArrivalTime().isAfter(queryTime)) {
                found.add(journeyDTO);
            }
        });
        return found;
    }

    private JourneyPlanRepresentation getJourneyPlan(IdFor<Station> startId, IdFor<Station> endId, TramTime queryTime,
                                                     TramServiceDate queryDate, boolean arriveBy, int maxChanges) {
        return getJourneyPlan(startId.forDTO(), endId.forDTO(), queryTime, queryDate, arriveBy, maxChanges);
    }


    private JourneyPlanRepresentation getJourneyPlan(String startId, String endId, TramTime queryTime,
                                                     TramServiceDate queryDate, boolean arriveBy, int maxChanges) {
        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExt, startId, endId, queryTime.asLocalTime(),
                queryDate.getDate(), null, arriveBy, maxChanges);
        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    private JourneyPlanRepresentation getJourneyPlan(LatLong startLocation, IdFor<Station> endId, TramTime queryTime,
                                                     TramServiceDate queryDate, boolean arriveBy) {
        return getJourneyPlan(startLocation, endId.forDTO(), queryTime, queryDate, arriveBy);
    }

    private JourneyPlanRepresentation getJourneyPlan(LatLong startLocation, String endId, TramTime queryTime,
                                                     TramServiceDate queryDate, boolean arriveBy) {

        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExt, MyLocation.MY_LOCATION_PLACEHOLDER_ID,
                endId, queryTime.asLocalTime(), queryDate.getDate(), startLocation, arriveBy, 3);
        Assertions.assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }


}
