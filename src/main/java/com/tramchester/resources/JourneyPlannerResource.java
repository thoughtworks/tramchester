package com.tramchester.resources;

import com.tramchester.domain.Journey;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.services.DateTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);
    private RouteCalculator routeCalculator;
    private DateTimeService dateTimeService;

    public JourneyPlannerResource(RouteCalculator routeCalculator, DateTimeService dateTimeService) {
        this.routeCalculator = routeCalculator;
        this.dateTimeService = dateTimeService;
    }


    @GET
    public String quickestRoute(@QueryParam("start") String startId, @QueryParam("end") String endId, @QueryParam("departureTime") String departureTime) throws Exception {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight(departureTime);
        List<Journey> journeys = routeCalculator.calculateRoute(startId, endId, minutesFromMidnight);
        //return journeyPlannerResponseMapper.mapFrom(journeys);
        return null;
    }
}
