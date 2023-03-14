package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Api
@Path("/interchanges")
@Produces(MediaType.APPLICATION_JSON)
public class InterchangeResource implements APIResource, GraphDatabaseDependencyMarker {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeResource.class);

    private final InterchangeRepository interchangeRepository;

    @Inject
    public InterchangeResource(InterchangeRepository interchangeRepository) {
        this.interchangeRepository = interchangeRepository;
    }

    @GET
    @Timed
    @Path("/all")
    @ApiOperation(value = "Get all interchanges", response = String.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS, isPrivate = false)
    public Response getByMode(@Context Request request) {
        logger.info("Get all interchange id's");

        List<IdForDTO> stationIds = interchangeRepository.getAllInterchanges().stream().
                map(interchangeStation -> IdForDTO.createFor(interchangeStation.getStation())).collect(Collectors.toList());

        return Response.ok(stationIds).build();
    }



}
