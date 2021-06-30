package com.tramchester.integration.resources.journeyPlanning;


import com.tramchester.App;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.ConfigDTO;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
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

@BusTest
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

    @Test
    void shouldHaveBusStations() {
        Response result = APIClient.getApiResponse(appExt, "stations/mode/Bus");

        assertEquals(200, result.getStatus());

        List<StationRefDTO> results = result.readEntity(new GenericType<>() {});

        App app = appExt.getApplication();

        CompositeStationRepository stationRepo = app.getDependencies().get(CompositeStationRepository.class);
        Set<String> stationsIds = stationRepo.getStationsForMode(TransportMode.Bus).stream().
                map(station -> station.getId().forDTO()).collect(Collectors.toSet());

        assertEquals(stationsIds.size(), results.size());

        Set<String> resultIds = results.stream().map(StationRefDTO::getId).collect(Collectors.toSet());

        assertTrue(stationsIds.containsAll(resultIds));
    }

    @Test
    void shouldGetTransportModes() {
        Response response = APIClient.getApiResponse(appExt, "version/modes");
        assertEquals(200, response.getStatus());

        ConfigDTO result = response.readEntity(new GenericType<>() {});

        List<TransportMode> results = result.getModes();
        assertFalse(results.isEmpty());

        List<TransportMode> expected = Collections.singletonList(TransportMode.Bus);
        assertEquals(expected, results);
    }

    @Test
    void shouldBusJourneyWestEast() {
        TramTime queryTime = TramTime.of(8,45);
        validateHasJourney(queryTime, StopAtAltrinchamInterchange, StopAtStockportBusStation, 2);
        validateHasJourney(queryTime, StopAtStockportBusStation, StopAtAltrinchamInterchange, 2);
    }

    @Test
    void shouldPlanBusJourneySouthern() {
        TramTime queryTime = TramTime.of(8,45);
        validateHasJourney(queryTime, ShudehillInterchange, StopAtStockportBusStation, 2);
        validateHasJourney(queryTime, StopAtStockportBusStation, ShudehillInterchange, 2);
    }

    @Test
    void shouldPlanBusJourneyNorthern() {
        TramTime queryTime = TramTime.of(8,45);
        validateHasJourney(queryTime, ShudehillInterchange, BuryInterchange, 2);
        validateHasJourney(queryTime, BuryInterchange, ShudehillInterchange, 2);
    }

    @Test
    void shouldPlanBusJourneyNoLoops() {
        TramTime queryTime = TramTime.of(8,56);
        JourneyPlanRepresentation plan = getJourneyResults(queryTime, StopAtAltrinchamInterchange, ManchesterAirportStation,
                2, false);

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

    @Test
    void shouldPlanSimpleBusJourneyFromLocation() {
        TramTime queryTime = TramTime.of(8,45);
        JourneyPlanRepresentation plan = getJourneyPlan(TestEnv.nearAltrincham, StopAtStockportBusStation.getId(), queryTime,
                new TramServiceDate(when), false);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    @Test
    void shouldPlanDirectWalkToBusStopFromLocation() {
        TramTime queryTime = TramTime.of(8,15);
        JourneyPlanRepresentation plan = getJourneyPlan(TestEnv.nearAltrincham, StopAtAltrinchamInterchange.getId(), queryTime,
                new TramServiceDate(when), false);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    @Test
    void shouldPlanSimpleJourneyArriveByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);
        JourneyPlanRepresentation plan = getJourneyResults(queryTime, StopAtStockportBusStation, StopAtAltrinchamInterchange, 3, true);

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
        JourneyPlanRepresentation plan = getJourneyResults(queryTime, start, end, maxChanges, false);
        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    private JourneyPlanRepresentation getJourneyResults(TramTime queryTime, HasId<Station> start, HasId<Station>  end,
                                                        int maxChanges, boolean arriveBy) {
        Response response = JourneyPlannerResourceTest.getResponseForJourney(appExt, start.getId().forDTO(),
                end.getId().forDTO(), queryTime.asLocalTime(), when, null, arriveBy, maxChanges);

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
