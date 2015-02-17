package com.tramchester.resources;

import com.tramchester.graph.RouteCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);
    private RouteCalculator routeCalculator;

    public JourneyPlannerResource(RouteCalculator routeCalculator) {
        this.routeCalculator = routeCalculator;
    }


    @GET
    public String quickestRoute(@QueryParam("start") String startId, @QueryParam("end") String endId, @QueryParam("departureTime") String departureTime) throws Exception {

        //List<Journey> journeys = routeCalculator.calculateRoute(startId, endId, 600);
        //return journeyPlannerResponseMapper.mapFrom(journeys);
        return null;
    }
}
