package com.tramchester.integration.resources.journeyPlanning;


import com.tramchester.App;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.StationGroup;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.APIClient;
import com.tramchester.integration.testSupport.IntegrationAppExtension;
import com.tramchester.integration.testSupport.JourneyResourceTestFacade;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.testSupport.reference.BusStations.*;
import static com.tramchester.testSupport.reference.KnownLocations.nearAltrincham;
import static org.junit.jupiter.api.Assertions.*;

@BusTest
@ExtendWith(DropwizardExtensionsSupport.class)
class JourneyPlannerBusTest {

    private static final IntegrationBusTestConfig configuration = new IntegrationBusTestConfig();
    private static final IntegrationAppExtension appExt = new IntegrationAppExtension(App.class, configuration);

    private TramDate when;
    private JourneyResourceTestFacade journeyResourceTestFacade;
    private StationGroup stockportBusStation;
    private StationRepository stationRepository;

    @BeforeEach
    void beforeEachTestRuns() {
        when = TestEnv.testDay();
        journeyResourceTestFacade = new JourneyResourceTestFacade(appExt);
        App app = appExt.getApplication();

        stationRepository = app.getDependencies().get(StationRepository.class);

        StationGroupsRepository stationGroupsRepository = app.getDependencies().get(StationGroupsRepository.class);
        stockportBusStation = stationGroupsRepository.findByName(Composites.StockportTempBusStation.getName());
    }

    @Test
    void shouldHaveBusStations() {
        Response result = APIClient.getApiResponse(appExt, "stations/mode/Bus");

        assertEquals(200, result.getStatus());

        List<LocationRefDTO> results = result.readEntity(new GenericType<>() {});

        Set<String> stationsIds = stationRepository.getStationsServing(TransportMode.Bus).stream().
                map(station -> station.getId().forDTO()).collect(Collectors.toSet());

        assertEquals(stationsIds.size(), results.size());

        Set<String> resultIds = results.stream().
                map(LocationRefDTO::getId).
                map(IdForDTO::getActualId).
                collect(Collectors.toSet());

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

        JourneyQueryDTO query1 = journeyResourceTestFacade.getQueryDTO(when, queryTime, StopAtAltrinchamInterchange, stockportBusStation, false, 2);

        JourneyPlanRepresentation planA = journeyResourceTestFacade.getJourneyPlan(query1);
        List<JourneyDTO> foundA = getValidJourneysAfter(queryTime, planA);
        Assertions.assertFalse(foundA.isEmpty());

        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, stockportBusStation, StopAtAltrinchamInterchange, false, 2);

        JourneyPlanRepresentation planB = journeyResourceTestFacade.getJourneyPlan(query);
        List<JourneyDTO> foundB = getValidJourneysAfter(queryTime, planB);
        Assertions.assertFalse(foundB.isEmpty());
    }

    @Test
    void shouldPlanBusJourneySouthern() {
        TramTime queryTime = TramTime.of(8,45);

        JourneyQueryDTO query1 = journeyResourceTestFacade.getQueryDTO(when, queryTime, StopAtShudehillInterchange, stockportBusStation, false, 2);

        JourneyPlanRepresentation southNorth = journeyResourceTestFacade.getJourneyPlan(query1);
        List<JourneyDTO> southNorthFound = getValidJourneysAfter(queryTime, southNorth);
        Assertions.assertFalse(southNorthFound.isEmpty());

        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, stockportBusStation, StopAtShudehillInterchange, false, 3);

        JourneyPlanRepresentation northSouth = journeyResourceTestFacade.getJourneyPlan(query);
        List<JourneyDTO> foundWith3Changes = getValidJourneysAfter(queryTime, northSouth);
        Assertions.assertFalse(foundWith3Changes.isEmpty());
    }

    @Test
    void shouldPlanBusJourneyNorthern() {
        TramTime queryTime = TramTime.of(8,45);

        JourneyQueryDTO query1 = journeyResourceTestFacade.getQueryDTO(when, queryTime, StopAtShudehillInterchange, BuryInterchange, false, 2);

        JourneyPlanRepresentation planA = journeyResourceTestFacade.getJourneyPlan(query1);
        List<JourneyDTO> foundA = getValidJourneysAfter(queryTime, planA);
        Assertions.assertFalse(foundA.isEmpty());


        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, BuryInterchange, StopAtShudehillInterchange, false, 2);

        JourneyPlanRepresentation planB = journeyResourceTestFacade.getJourneyPlan(query);
        List<JourneyDTO> foundB = getValidJourneysAfter(queryTime, planB);
        Assertions.assertFalse(foundB.isEmpty());
    }

    @Test
    void shouldPlanBusJourneyNoLoops() {
        TramTime queryTime = TramTime.of(8,56);

        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, StopAtAltrinchamInterchange, ManchesterAirportStation, false, 2);

        JourneyPlanRepresentation plan = journeyResourceTestFacade.getJourneyPlan(query);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());

        found.forEach(result -> {
            Set<String> stageIds= new HashSet<>();
            result.getStages().forEach(stage -> {
                String id = stage.getActionStation().getId().getActualId();
                Assertions.assertFalse(stageIds.contains(id), "duplicate stations id found during " +result);
                stageIds.add(id);
            });
        });
    }

    @Test
    void shouldPlanSimpleBusJourneyFromLocation() {
        TramTime queryTime = TramTime.of(8,45);

        JourneyQueryDTO query = JourneyQueryDTO.create(when, queryTime, nearAltrincham.location(), stockportBusStation, false, 2);

        JourneyPlanRepresentation plan = journeyResourceTestFacade.getJourneyPlan(query);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    @Test
    void shouldPlanDirectWalkToBusStopFromLocation() {
        TramTime queryTime = TramTime.of(8,15);

        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, nearAltrincham.location(), StopAtAltrinchamInterchange, false, 3);

        JourneyPlanRepresentation plan = journeyResourceTestFacade.getJourneyPlan(query);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        Assertions.assertFalse(found.isEmpty());
    }

    @Test
    void shouldPlanSimpleJourneyArriveByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);

        JourneyQueryDTO query = journeyResourceTestFacade.getQueryDTO(when, queryTime, stockportBusStation, StopAtAltrinchamInterchange, true, 3);

        JourneyPlanRepresentation plan = journeyResourceTestFacade.getJourneyPlan(query);

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

}
