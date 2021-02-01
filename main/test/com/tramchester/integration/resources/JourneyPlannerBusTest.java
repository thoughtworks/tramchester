package com.tramchester.integration.resources;


import com.tramchester.App;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.IntegrationClient;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.TestEnv;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.BusStations.*;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerBusTest {

    private static final IntegrationBusTestConfig configuration = new IntegrationBusTestConfig();
    private static final IntegrationAppExtension appExt = new IntegrationAppExtension(App.class, configuration);

    private LocalDate when;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
    }

    @Category({BusTest.class})
    @Test
    void shouldHaveBusStations() {
        Response result = IntegrationClient.getApiResponse(appExt, "stations/mode/Bus");

        assertEquals(200, result.getStatus());

        List<StationRefDTO> results = result.readEntity(new GenericType<>() {});

        App app =  appExt.getApplication();
        StationRepository stationRepo = app.getDependencies().get(StationRepository.class);
        Set<String> stationsIds = stationRepo.getStationsForMode(TransportMode.Bus).stream().
                map(station -> station.getId().forDTO()).collect(Collectors.toSet());

        assertEquals(stationsIds.size(), results.size());

        Set<String> resultIds = results.stream().map(StationRefDTO::getId).collect(Collectors.toSet());

        assertTrue(stationsIds.containsAll(resultIds));
    }

    @Category({BusTest.class})
    @Test
    void shouldGetTransportModes() {
        Response responce = IntegrationClient.getApiResponse(appExt, "version/modes");
        assertEquals(200, responce.getStatus());

        List<TransportMode> results = responce.readEntity(new GenericType<>() {});

        assertFalse(results.isEmpty());

        List<TransportMode> expected = Collections.singletonList(TransportMode.Bus);
        assertEquals(expected, results);
    }

    @Category({BusTest.class})
    @Test
    void shouldBusJourneyWestEast() {
        TramTime queryTime = TramTime.of(8,45);
        validateHasJourney(queryTime, AltrinchamInterchange, StockportBusStation, 3);
        validateHasJourney(queryTime, StockportBusStation, AltrinchamInterchange, 3);
    }

    @Category({BusTest.class})
    @Test
    void shouldPlanBusJourneySouthern() {
        TramTime queryTime = TramTime.of(8,45);
        validateHasJourney(queryTime, ShudehillInterchange, StockportBusStation, 3);
        validateHasJourney(queryTime, StockportBusStation, ShudehillInterchange, 3);
    }

    @Category({BusTest.class})
    @Test
    void shouldPlanBusJourneyNorthern() {
        TramTime queryTime = TramTime.of(8,45);
        validateHasJourney(queryTime, ShudehillInterchange, BuryInterchange, 3);
        validateHasJourney(queryTime, BuryInterchange, ShudehillInterchange, 3);
    }

    @Category({BusTest.class})
    @Test
    void shouldPlanBusJourneyNoLoops() {
        TramTime queryTime = TramTime.of(8,56);
        JourneyPlanRepresentation plan = createPlan(queryTime, AltrinchamInterchange, ManchesterAirportStation, 2, false);

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
        JourneyPlanRepresentation plan = createPlan(queryTime, StockportBusStation, AltrinchamInterchange, 3, true);

        // TODO 20 mins gap? Estimation is too optimistic for Buses?
        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            Assertions.assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime.toDate(when)));
            Duration duration = Duration.between(journeyDTO.getExpectedArrivalTime(), queryTime.toDate(when));
            if (duration.getSeconds() < 20*60) {
                found.add(journeyDTO);
            }
        });
        Assertions.assertFalse(found.isEmpty());
    }

    private List<JourneyDTO> getValidJourneysAfter(TramTime queryTime, JourneyPlanRepresentation plan) {
        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            LocalDateTime firstDepartureTime = journeyDTO.getFirstDepartureTime();
            Assertions.assertTrue(firstDepartureTime.isAfter(queryTime.toDate(when))
                    || firstDepartureTime.equals(queryTime.toDate(when)), firstDepartureTime.toString());
            if (journeyDTO.getExpectedArrivalTime().isAfter(queryTime.toDate(when))) {
                found.add(journeyDTO);
            }
        });
        return found;
    }


    private void validateHasJourney(TramTime queryTime, HasId<Station>  start, HasId<Station>  end, int maxChanges) {
        JourneyPlanRepresentation plan = createPlan(queryTime, start, end, maxChanges, false);
        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    private JourneyPlanRepresentation createPlan(TramTime queryTime, HasId<Station> start, HasId<Station>  end, int maxChanges, boolean arriveBy) {
        return getJourneyPlan(start.getId(), end.getId(), queryTime,
                new TramServiceDate(when), arriveBy, maxChanges);
    }

    private JourneyPlanRepresentation getJourneyPlan(StringIdFor<Station> startId, StringIdFor<Station> endId, TramTime queryTime,
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

    private JourneyPlanRepresentation getJourneyPlan(LatLong startLocation, StringIdFor<Station> endId, TramTime queryTime,
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
