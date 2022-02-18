package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.mappers.RoutesMapper;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Api
@Path("/routes")
@Produces(MediaType.APPLICATION_JSON)
public class RouteResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(RouteResource.class);

    private final RoutesMapper routesMapper;

    @Inject
    public RouteResource(RoutesMapper routesMapper) {
        logger.info("created");
        this.routesMapper = routesMapper;
    }

    @GET
    @Timed
    @ApiOperation(value = "Return all routes", response = RouteDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS)
    public Response getAll() {
        logger.info("getAll routes");
        try {
            List<RouteDTO> routes = routesMapper.getRouteDTOs();

            return Response.ok(routes).build();
        }
        catch (Exception exception) {
            logger.error("Exception while getting all routes", exception);
            return Response.serverError().build();
        }
    }

}
