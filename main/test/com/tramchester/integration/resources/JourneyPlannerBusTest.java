package com.tramchester.integration.resources;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.tramchester.App;
import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.*;
import com.tramchester.resources.JourneyPlannerResource;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

import static com.tramchester.TestConfig.dateFormatDashes;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class JourneyPlannerBusTest {


    @Rule
    public Timeout globalTimeout = Timeout.seconds(10*60);

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class,
            new IntegrationBusTestConfig("int_test_bus_tramchester.db"));

    private ObjectMapper mapper = new ObjectMapper();
    private LocalDate when;

    @Before
    public void beforeEachTestRuns() {
        when = TestConfig.nextTuesday(0);
        // todo NO longer needed?
        mapper.registerModule(new JodaModule());
    }

    @Ignore("experimental")
    @Category({BusTest.class})
    @Test
    public void shouldPlanSimpleJourney() {
        TramTime queryTime = TramTime.of(11,45);
        JourneyPlanRepresentation plan = getJourneyPlan("1800EB07881", "1800EB07921", queryTime,
                new TramServiceDate(when), false);

        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime));
            if (journeyDTO.getExpectedArrivalTime().isBefore(queryTime)) {
                found.add(journeyDTO);
            }
        });
        assertFalse(found.isEmpty());
    }

    @Ignore("experimental")
    @Category({BusTest.class})
    @Test
    public void shouldPlanSimpleJourneyArriveByHasAtLeastOneDepartByRequiredTime() {
        TramTime queryTime = TramTime.of(11,45);
        JourneyPlanRepresentation plan = getJourneyPlan("1800EB07881", "1800EB07921", queryTime,
                new TramServiceDate(when), true);

        List<JourneyDTO> found = new ArrayList<>();
        plan.getJourneys().forEach(journeyDTO -> {
            assertTrue(journeyDTO.getFirstDepartureTime().isBefore(queryTime));
            if (journeyDTO.getExpectedArrivalTime().isBefore(queryTime)) {
                found.add(journeyDTO);
            }
        });
        assertFalse(found.isEmpty());
    }

//    @Test
//    @Category({BusTest.class})
//    @Ignore("experimental")
//    public void shouldFindRoutesForLatLongToStationId() {
//        LatLong startLocation = new LatLong(53.4092, -2.2218);
//
//        String startId = formId(startLocation);
//
//        // todo currently finds far too many start points
//
//        JourneyPlanRepresentation plan = planner.createJourneyPlan(startId, Stations.PiccadillyGardens.getId(),
//                new TramServiceDate(when), TramTime.of(9,0), false);
//        SortedSet<JourneyDTO> journeys = plan.getJourneys();
//        assertTrue(journeys.size()>=1);
//        JourneyDTO journey = journeys.first();
//        List<StageDTO> stages = journey.getStages();
//        stages.forEach(stage ->
//                assertEquals(TransportMode.Bus, stage.getMode())
//        );
//    }
//
//    private static String formId(LatLong startLocation) {
//        return String.format("lat=%f&lon=%f", startLocation.getLat(), startLocation.getLon());
//    }
//
//    @Test
//    @Category({BusTest.class})
//    @Ignore("experimental")
//    public void reproduceIssueWithRoute() {
//        planner.createJourneyPlan("1800SB34231", "1800SB01681", new TramServiceDate(when),
//                TramTime.of(9,0), false);
//    }

    private JourneyPlanRepresentation getJourneyPlan(String startId, String endId, TramTime queryTime,
                                                       TramServiceDate queryDate, boolean arriveBy) {
        String date = queryDate.getDate().format(dateFormatDashes);
        String time = queryTime.asLocalTime().format(TestConfig.timeFormatter);
        Response response = JourneyPlannerResourceTest.getResponseForJourney(testRule, startId, endId, time, date,
                null, arriveBy);
        assertEquals(200, response.getStatus());
        return response.readEntity(JourneyPlanRepresentation.class);
    }


}
