package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.presentation.DTO.RouteDTO;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.mappers.RoutesMapper;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Path("/routes")
@Produces(MediaType.APPLICATION_JSON)
public class RouteResource implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(RouteResource.class);

    private final RoutesMapper routesMapper;
    private final ProvidesNow providesNow;

    @Inject
    public RouteResource(RoutesMapper routesMapper, ProvidesNow providesNow) {
        this.providesNow = providesNow;
        logger.info("created");
        this.routesMapper = routesMapper;
    }

    @GET
    @Timed
    @Operation(description = "Return all routes")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = RouteDTO.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS)
    public Response getAll() {
        logger.info("getAll routes");
        try {
            List<RouteDTO> routes = routesMapper.getRouteDTOs(providesNow.getTramDate());

            return Response.ok(routes).build();
        }
        catch (Exception exception) {
            logger.error("Exception while getting all routes", exception);
            return Response.serverError().build();
        }
    }

    @GET
    @Timed
    @Operation(description = "return routes filtered by given query parameters")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = RouteDTO.class))))
    @Path("/filtered")
    public Response getFiltered(@QueryParam("date") String dateRaw) {
        logger.info("get filtered routes for " + dateRaw);

        try {
            TramDate date = TramDate.parse(dateRaw);
            List<RouteDTO> routes = routesMapper.getRouteDTOs(date);

            return Response.ok(routes).build();
        }
        catch (Exception exception) {
            logger.error("Exception while getting all routes", exception);
            return Response.serverError().build();
        }

    }

}
