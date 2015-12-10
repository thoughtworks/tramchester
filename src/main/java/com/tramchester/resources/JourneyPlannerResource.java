package com.tramchester.resources;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.Journey;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.graph.UnknownStationException;
import com.tramchester.mappers.JourneyResponseMapper;
import com.tramchester.representations.JourneyPlanRepresentation;
import com.tramchester.services.DateTimeService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

    public JourneyPlannerResource(RouteCalculator routeCalculator, DateTimeService dateTimeService, JourneyResponseMapper journeyResponseMapper) {
        this.routeCalculator = routeCalculator;
        this.dateTimeService = dateTimeService;
        this.journeyResponseMapper = journeyResponseMapper;
    }

    @GET
    public Response quickestRoute(@QueryParam("start") String startId, @QueryParam("end") String endId, @QueryParam("departureTime") String departureTime) throws UnknownStationException {
        DaysOfWeek dayOfWeek = DaysOfWeek.fromToday();
        // today expose this as a parameter
        TramServiceDate queryDate = new TramServiceDate(DateTime.now());
        JourneyPlanRepresentation planRepresentation = createJourneyPlan(startId, endId, departureTime, dayOfWeek, queryDate);
        Response response = Response.ok(planRepresentation).build();
        return response;
    }

    public JourneyPlanRepresentation createJourneyPlan(String startId, String endId, String departureTime,
                                                       DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws UnknownStationException {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight(departureTime);
        logger.info(String.format("start: %s end: %s departure time: %s (%s) on %s",
                startId, endId, departureTime, minutesFromMidnight, dayOfWeek));

        Set<Journey> journeys = routeCalculator.calculateRoute(startId, endId, minutesFromMidnight, dayOfWeek, queryDate);
        logger.info("number of journeys: " + journeys.size());
        return journeyResponseMapper.map(journeys, minutesFromMidnight, maxNumberOfTrips);
    }

}
