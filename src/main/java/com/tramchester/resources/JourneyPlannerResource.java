package com.tramchester.resources;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.mappers.GenericJourneyResponseMapper;
import com.tramchester.mappers.JourneyResponseMapper;
import com.tramchester.mappers.TramJourneyResponseMapper;
import com.tramchester.services.DateTimeService;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);
    private RouteCalculator routeCalculator;
    private DateTimeService dateTimeService;
    private JourneyResponseMapper journeyResponseMapper;
    // user for the journey mapper
    private int maxNumberOfTrips = 5;

    public JourneyPlannerResource(RouteCalculator routeCalculator, DateTimeService dateTimeService,
                                  JourneyResponseMapper journeyResponseMapper) {
        this.routeCalculator = routeCalculator;
        this.dateTimeService = dateTimeService;
        this.journeyResponseMapper = journeyResponseMapper;
    }

    @GET
    public Response quickestRoute(@QueryParam("start") String startId, @QueryParam("end") String endId,
                                  @QueryParam("departureTime") String departureTime) throws TramchesterException {
        DaysOfWeek dayOfWeek = DaysOfWeek.fromToday();
        // today expose this as a parameter
        TramServiceDate queryDate = new TramServiceDate(LocalDate.now());
        JourneyPlanRepresentation planRepresentation = createJourneyPlan(startId, endId, departureTime, dayOfWeek, queryDate);
        Response response = Response.ok(planRepresentation).build();
        return response;
    }

    public JourneyPlanRepresentation createJourneyPlan(String startId, String endId, String queryTime,
                                                       DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws TramchesterException {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight(queryTime);
        logger.info(String.format("start: %s end: %s query time: %s (%s) on %s",
                startId, endId, queryTime, minutesFromMidnight, dayOfWeek));

        Set<RawJourney> journeys = routeCalculator.calculateRoute(startId, endId, minutesFromMidnight, dayOfWeek, queryDate);
        logger.info("number of journeys: " + journeys.size());
        return journeyResponseMapper.map(journeys, minutesFromMidnight, maxNumberOfTrips);
    }

    public JourneyPlanRepresentation createJourneyPlan(List<Node> starts, List<Node> ends, String queryTime,
                                                       DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws TramchesterException {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight(queryTime);

        Set<RawJourney> journeys = routeCalculator.calculateRoute(starts, ends, minutesFromMidnight, dayOfWeek, queryDate);
        logger.info("number of journeys: " + journeys.size());
        return journeyResponseMapper.map(journeys, minutesFromMidnight, maxNumberOfTrips);
    }
}
