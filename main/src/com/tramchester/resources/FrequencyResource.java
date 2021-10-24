package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.BoundingBoxWithCost;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static java.lang.String.format;

@Api
@Path("/frequency")
@Produces(MediaType.APPLICATION_JSON)
public class FrequencyResource {
    private static final Logger logger = LoggerFactory.getLogger(FrequencyResource.class);

    @Inject
    public FrequencyResource() {

    }

    @GET
    @Timed
    @ApiOperation(value = "Get number of services for the given time period for each grid box", response = BoundingBoxWithCost.class)
    //@CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response gridCosts(@QueryParam("gridSize") int gridSize,
                              @QueryParam("startDateTime") String startDateTimeRaw,
                              @QueryParam("departureDate") String endDateTimeRaw) {
        logger.info(format("Query for %s gridsize meters, start: '%s' end: '%s", gridSize, startDateTimeRaw, endDateTimeRaw));

        Response.ResponseBuilder responseBuilder = Response.ok();
        return responseBuilder.build();
    }

}
