package com.tramchester.integration.resources;


import com.tramchester.App;
import com.tramchester.domain.MyLocationFactory;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.integration.IntegrationTestRun;
import com.tramchester.testSupport.BusTest;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.tramchester.testSupport.BusStations.*;
import static com.tramchester.testSupport.TestEnv.dateFormatDashes;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Ignore("Experimental")
public class JourneyPlannerBusTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10*60);

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class,
            new IntegrationBusTestConfig());

    private LocalDate nextTuesday;

    @Before
    public void beforeEachTestRuns() {
        nextTuesday = TestEnv.nextTuesday(0);
        // todo NO longer needed?
    }

    @Category({BusTest.class})
    @Test
    public void shouldPlanSimpleTramJourney() {
        TramTime queryTime = TramTime.of(8,45);
        JourneyPlanRepresentation plan =  JourneyPlannerResourceTest.getJourneyPlanRepresentation(testRule,
                Stations.Deansgate, Stations.Altrincham, new TramServiceDate(nextTuesday), queryTime, false, 3);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        assertFalse(found.isEmpty());
    }

    @Category({BusTest.class})
    @Test
    public void shouldPlanSimpleBusJourney() {
        TramTime queryTime = TramTime.of(8,45);
        JourneyPlanRepresentation plan = getJourneyPlan(AltrinchamInterchange.getId(), StockportBusStation.getId(), queryTime,
                new TramServiceDate(nextTuesday), false);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        assertFalse(found.isEmpty());
    }

    @Category({BusTest.class})
    @Test
    public void shouldPlanLongBusJourney() {
        TramTime queryTime = TramTime.of(8,45);
        JourneyPlanRepresentation plan = getJourneyPlan(ShudehillInterchange.getId(), StockportBusStation.getId(), queryTime,
                new TramServiceDate(nextTuesday), false);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        assertFalse(found.isEmpty());
    }

    @Category({BusTest.class})
    @Test
    public void shouldPlanSimpleBusJourneyFromLocation() {
        TramTime queryTime = TramTime.of(8,45);
        JourneyPlanRepresentation plan = getJourneyPlan(TestEnv.nearAltrincham, StockportBusStation.getId(), queryTime,
                new TramServiceDate(nextTuesday), false);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        assertFalse(found.isEmpty());
    }

    @Category({BusTest.class})
    @Test
    public void shouldPlanDirectWalkToBusStopFromLocation() {
        TramTime queryTime = TramTime.of(8,15);
        JourneyPlanRepresentation plan = getJourneyPlan(TestEnv.nearAltrincham, AltrinchamInterchange.getId(), queryTime,
                new TramServiceDate(nextTuesday), false);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        assertFalse(found.isEmpty());
    }

    @Category({BusTest.class})
    @Test
    public void shouldPlanSimpleTramJourneyFromLocation() {
        TramTime queryTime = TramTime.of(8,45);
        JourneyPlanRepresentation plan = getJourneyPlan(TestEnv.nearAltrincham, Stations.StPetersSquare.getId(), queryTime,
                new TramServiceDate(nextTuesday), false);

        List<JourneyDTO> found = getValidJourneysAfter(queryTime, plan);
        assertFalse(found.isEmpty());
    }

    @Category({BusTest.class})
    @Test
    public void shouldPlanSimpleJourneyArriveByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);
        JourneyPlanRepresentation plan = getJourneyPlan(StockportBusStation.getId(), AltrinchamInterchange.getId(), queryTime,
                new TramServiceDate(nextTuesday), true); // true => arrive by
        
        // TODO 20 mins gap? Estimation is too optimistic for Buses?
        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime));
            if (TramTime.diffenceAsMinutes(journeyDTO.getExpectedArrivalTime(),queryTime)<20) {
                found.add(journeyDTO);
            }
        });
        assertFalse(found.isEmpty());
    }

    private List<JourneyDTO> getValidJourneysAfter(TramTime queryTime, JourneyPlanRepresentation plan) {
        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            TramTime firstDepartureTime = journeyDTO.getFirstDepartureTime();
            assertTrue(firstDepartureTime.toString(), firstDepartureTime.isAfter(queryTime)
                    || firstDepartureTime.equals(queryTime));
            if (journeyDTO.getExpectedArrivalTime().isAfter(queryTime)) {
                found.add(journeyDTO);
            }
        });
        return found;
    }

    private JourneyPlanRepresentation getJourneyPlan(String startId, String endId, TramTime queryTime,
                                                       TramServiceDate queryDate, boolean arriveBy) {
        String date = queryDate.getDate().format(dateFormatDashes);
        String time = queryTime.asLocalTime().format(TestEnv.timeFormatter);
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, startId, endId, time, date,
                null, arriveBy, 3);
        assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }

    private JourneyPlanRepresentation getJourneyPlan(LatLong startLocation, String endId, TramTime queryTime,
                                                     TramServiceDate queryDate, boolean arriveBy) {
        String date = queryDate.getDate().format(dateFormatDashes);
        String time = queryTime.asLocalTime().format(TestEnv.timeFormatter);

        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID,
                endId, time, date, startLocation, arriveBy, 3);
        assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }


}
