package com.tramchester.resources;

import com.tramchester.domain.Journey;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.mappers.JourneyResponseMapper;
import com.tramchester.services.DateTimeService;
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

    public JourneyPlannerResource(RouteCalculator routeCalculator, DateTimeService dateTimeService, JourneyResponseMapper journeyResponseMapper) {
        this.routeCalculator = routeCalculator;
        this.dateTimeService = dateTimeService;
        this.journeyResponseMapper = journeyResponseMapper;
    }

    @GET
    public Response quickestRoute(@QueryParam("start") String startId, @QueryParam("end") String endId, @QueryParam("departureTime") String departureTime) throws Exception {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight(departureTime);
        Set<Journey> journeys = routeCalculator.calculateRoute(startId, endId, minutesFromMidnight);
        return Response.ok(journeyResponseMapper.map(journeys, minutesFromMidnight)).build();
    }

}
