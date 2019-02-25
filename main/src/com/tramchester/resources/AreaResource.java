package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.repository.AreasRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Api
@Path("/areas")
@Produces(MediaType.APPLICATION_JSON)
public class AreaResource {

    private AreasRepository repository;

    public AreaResource(AreasRepository repository) {

        this.repository = repository;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all areas", response = AreaDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS)
    public Response getAll() {
        List<AreaDTO> areas = repository.getAreas();

        Response response = Response.ok(areas).build();
        return response;
    }
}
