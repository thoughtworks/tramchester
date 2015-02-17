package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.Journey;
import com.tramchester.graph.RouteCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {
    private RouteCalculator routeCalculator;
    private static final Logger logger = LoggerFactory.getLogger(TestResource.class);

    public TestResource(RouteCalculator routeCalculator) {
        this.routeCalculator = routeCalculator;
    }


    @GET
    @Timed
    public Response get() {

        //Pomona to stretford
        //routeCalculator.calculateRoute( "9400ZZMAPOM", "9400ZZMASFD",500);

        //Altringham to eccels
        List<Journey> journeys = routeCalculator.calculateRoute("9400ZZMAALT", "9400ZZMANIS", 500);

        return Response.ok(journeys).build();
    }


}


