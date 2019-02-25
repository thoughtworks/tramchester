package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.repository.RoutesRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class RouteResource {
    private static final Logger logger = LoggerFactory.getLogger(RouteResource.class);

    private RoutesRepository repository;

    public RouteResource(RoutesRepository repository) {
        this.repository = repository;
    }

    @GET
    @Timed
    @ApiOperation(value = "Return all routes", response = RouteDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS)
    public Response getAll() {
        logger.info("getAll routes");
        List<RouteDTO> routes = repository.getAllRoutes();

        Response response = Response.ok(routes).build();
        return response;
    }

}
